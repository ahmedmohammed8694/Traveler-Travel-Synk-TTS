# Google Maps Link Import & Multi-Segment Trip Planning Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Allow trip planners to import Google Maps route links, configure multi-day named route segments (e.g. Day 1, Day 2), save multi-segment trips to Cloudflare Edge, and enable joined riders to view and load any route segment directly into the in-app HUD/3D Map.

**Architecture:** 
- A dedicated `GoogleMapsUrlParser` engine decodes coordinates, queries, and waypoints from various Google Maps link formats.
- `TripRouteSegment` and updated `TripMetadata` models represent multi-day journeys.
- Cloudflare Workers Edge API stores and serves multi-segment trip itineraries.
- `TripCreationScreen` provides an interactive "Add Route Link" modal with real-time polyline preview, while `LiveMapScreen` & `MainContainerScreen` display Multi-Day Route Cards with one-tap in-app navigation loading.

**Tech Stack:** Kotlin, Jetpack Compose, Google Maps Compose SDK, Cloudflare Workers (TypeScript), JUnit 4, Kotlinx Coroutines Flow.

## Global Constraints
- Namespace: `com.ridesync`
- Android minSdk 26, targetSdk 35
- Kotlin 2.0 / Compose Multiplatform / Java 17
- Cloudflare Edge API: `https://ahmedmohammed8694-riders-ride-sync.mdahmed08061994.workers.dev`

---

### Task 1: Trip Route Segment & Metadata Data Models

**Files:**
- Create: `app/src/main/java/com/ridesync/data/model/TripRouteSegment.kt`
- Modify: `app/src/main/java/com/ridesync/data/model/TripMetadata.kt`
- Test: `app/src/test/java/com/ridesync/data/model/TripRouteSegmentTest.kt`

**Interfaces:**
- Produces: `data class TripRouteSegment(val segmentId: String, val segmentName: String, val googleMapsUrl: String, val originName: String, val destinationName: String, val encodedPolyline: String, val distanceKm: Double, val estimatedDurationMinutes: Int, val waypointsCount: Int, val orderIndex: Int)`
- Updates: `TripMetadata(..., val routeSegments: List<TripRouteSegment> = emptyList(), val activeSegmentId: String = "")`

- [ ] **Step 1: Write the unit test for TripRouteSegment**

```kotlin
package com.ridesync.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class TripRouteSegmentTest {
    @Test
    fun testSegmentCreationAndDefaults() {
        val segment = TripRouteSegment(
            segmentId = "seg_1",
            segmentName = "Day 1: Coast Highway",
            googleMapsUrl = "https://maps.google.com/dir/19.0760,72.8777/18.5204,73.8567",
            originName = "Mumbai",
            destinationName = "Pune",
            encodedPolyline = "_p~iF~ps|U_ulLnnqC_mqNvxq`@",
            distanceKm = 148.5,
            estimatedDurationMinutes = 180,
            waypointsCount = 2,
            orderIndex = 0
        )
        assertEquals("seg_1", segment.segmentId)
        assertEquals("Day 1: Coast Highway", segment.segmentName)
        assertEquals(148.5, segment.distanceKm, 0.01)
    }
}
```

- [ ] **Step 2: Run unit test to verify failure**

Run: `.\gradlew testDebugUnitTest --tests "com.ridesync.data.model.TripRouteSegmentTest"`
Expected: FAIL (TripRouteSegment not found)

- [ ] **Step 3: Implement TripRouteSegment and update TripMetadata**

```kotlin
// app/src/main/java/com/ridesync/data/model/TripRouteSegment.kt
package com.ridesync.data.model

import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class TripRouteSegment(
    val segmentId: String = "",
    val segmentName: String = "Day 1 Route",
    val googleMapsUrl: String = "",
    val originName: String = "",
    val destinationName: String = "",
    val encodedPolyline: String = "",
    val distanceKm: Double = 0.0,
    val estimatedDurationMinutes: Int = 0,
    val waypointsCount: Int = 0,
    val orderIndex: Int = 0
)
```

- [ ] **Step 4: Run unit test to verify it passes**

Run: `.\gradlew testDebugUnitTest --tests "com.ridesync.data.model.TripRouteSegmentTest"`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/ridesync/data/model/TripRouteSegment.kt app/src/main/java/com/ridesync/data/model/TripMetadata.kt app/src/test/java/com/ridesync/data/model/TripRouteSegmentTest.kt
git commit -m "feat(model): add TripRouteSegment and update TripMetadata with multi-route support"
```

---

### Task 2: Google Maps Link Parser Engine (`GoogleMapsUrlParser`)

**Files:**
- Create: `app/src/main/java/com/ridesync/engine/GoogleMapsUrlParser.kt`
- Test: `app/src/test/java/com/ridesync/engine/GoogleMapsUrlParserTest.kt`

**Interfaces:**
- Produces: 
  `data class ParsedRouteQuery(val originQuery: String?, val destinationQuery: String?, val waypoints: List<String>, val originLat: Double?, val originLng: Double?, val destLat: Double?, val destLng: Double?)`
  `object GoogleMapsUrlParser { fun parseUrl(rawUrl: String): ParsedRouteQuery? }`

- [ ] **Step 1: Write unit tests for GoogleMapsUrlParser**

```kotlin
package com.ridesync.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class GoogleMapsUrlParserTest {
    @Test
    fun testParseDirectCoordinatesUrl() {
        val url = "https://www.google.com/maps/dir/19.0760,72.8777/18.5204,73.8567"
        val parsed = GoogleMapsUrlParser.parseUrl(url)
        assertNotNull(parsed)
        assertEquals(19.0760, parsed!!.originLat!!, 0.001)
        assertEquals(72.8777, parsed.originLng!!, 0.001)
        assertEquals(18.5204, parsed.destLat!!, 0.001)
        assertEquals(73.8567, parsed.destLng!!, 0.001)
    }

    @Test
    fun testParseQueryParamsUrl() {
        val url = "https://www.google.com/maps/dir/?api=1&origin=Mumbai&destination=Pune&waypoints=Lonavala|Khandala"
        val parsed = GoogleMapsUrlParser.parseUrl(url)
        assertNotNull(parsed)
        assertEquals("Mumbai", parsed!!.originQuery)
        assertEquals("Pune", parsed.destinationQuery)
        assertEquals(2, parsed.waypoints.size)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew testDebugUnitTest --tests "com.ridesync.engine.GoogleMapsUrlParserTest"`
Expected: FAIL

- [ ] **Step 3: Implement GoogleMapsUrlParser**

Implement regex coordinate parsing, query string parsing (`origin`, `destination`, `waypoints`), place URL extraction, and redirect unshortening support.

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew testDebugUnitTest --tests "com.ridesync.engine.GoogleMapsUrlParserTest"`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/ridesync/engine/GoogleMapsUrlParser.kt app/src/test/java/com/ridesync/engine/GoogleMapsUrlParserTest.kt
git commit -m "feat(engine): implement GoogleMapsUrlParser for route links"
```

---

### Task 3: Cloudflare Edge Backend Multi-Segment Trip Endpoints

**Files:**
- Modify: `cloudflare-backend/src/index.ts`

**Interfaces:**
- Produces:
  `POST /api/trip/create`: Saves trip metadata including `routeSegments` to `CONVOY_CACHE` or D1.
  `GET /api/trip/get?tripId=...`: Returns full trip with all segments.
  `POST /api/trip/segment/select`: Updates `activeSegmentId`.

- [ ] **Step 1: Add trip endpoints to Cloudflare Worker `src/index.ts`**
- [ ] **Step 2: Verify syntax & build**
- [ ] **Step 3: Commit**

```bash
git add cloudflare-backend/src/index.ts
git commit -m "feat(backend): add multi-segment trip sync endpoints to Cloudflare Worker"
```

---

### Task 4: Multi-Route Segment Input & Importer UI in `TripCreationScreen`

**Files:**
- Create: `app/src/main/java/com/ridesync/ui/trip/AddRouteSegmentDialog.kt`
- Modify: `app/src/main/java/com/ridesync/ui/trip/TripCreationScreen.kt`

**Interfaces:**
- Produces: `AddRouteSegmentDialog(onDismiss: () -> Unit, onSegmentAdded: (TripRouteSegment) -> Unit)`
- Updates: `TripCreationScreen` to show the list of added Day segments, "Add Route Link" button, segment reordering/deletion, and saving full multi-day trip.

- [ ] **Step 1: Create `AddRouteSegmentDialog` with Google Maps link paste, auto-parse, name input, and polyline preview**
- [ ] **Step 2: Wire `AddRouteSegmentDialog` into `TripCreationScreen`**
- [ ] **Step 3: Compile debug build to verify Compose UI**
- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/ridesync/ui/trip/AddRouteSegmentDialog.kt app/src/main/java/com/ridesync/ui/trip/TripCreationScreen.kt
git commit -m "feat(ui): add multi-route segment creation and Google Maps link importer"
```

---

### Task 5: Multi-Day Route Cards & In-App Navigation Opener in HUD Map

**Files:**
- Create: `app/src/main/java/com/ridesync/ui/trip/TripItineraryCards.kt`
- Modify: `app/src/main/java/com/ridesync/ui/map/LiveMapScreen.kt`
- Modify: `app/src/main/java/com/ridesync/ui/MainContainerScreen.kt`

**Interfaces:**
- Produces: `TripItineraryCards(trip: TripMetadata, activeSegmentId: String, onSelectSegment: (TripRouteSegment) -> Unit)`
- Updates: When a rider clicks a Day card, `LiveMapScreen` immediately updates the active polyline, recalculates along-track progress, and begins live convoy telemetry tracking for that leg.

- [ ] **Step 1: Create `TripItineraryCards` UI component**
- [ ] **Step 2: Connect route loading and switching inside `LiveMapScreen` & `MainContainerScreen`**
- [ ] **Step 3: Run full build and test verification**

Run: `.\gradlew assembleDebug`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/ridesync/ui/trip/TripItineraryCards.kt app/src/main/java/com/ridesync/ui/map/LiveMapScreen.kt app/src/main/java/com/ridesync/ui/MainContainerScreen.kt
git commit -m "feat(ui): integrate multi-day route cards and in-app navigation loader"
```
