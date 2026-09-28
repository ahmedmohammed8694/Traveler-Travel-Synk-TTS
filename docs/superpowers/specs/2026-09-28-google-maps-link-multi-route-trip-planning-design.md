# Design Spec: Google Maps Link Import & Multi-Segment Trip Planning for RIDERsYNK

## 1. Overview & Objectives
RIDERsYNK enables motorcycle convoy riders and trip organizers to plan, organize, and execute multi-day or multi-stage expeditions. This feature allows users to:
1. **Import routes via Google Maps URLs** (e.g., `https://maps.app.goo.gl/...`, `https://www.google.com/maps/dir/...`, or custom waypoints).
2. **Add multiple route legs to a single trip** with custom names (e.g., *"Day 1: Coast Highway"*, *"Day 2: Mountain Pass"*, *"Day 3: Return Loop"*).
3. **Synchronize multi-segment trip plans** to Cloudflare Edge Backend.
4. **Interactive Trip Details View for Joined Riders:** View all route legs with distance, waypoints, and ETA summary cards, and open any selected route directly in the in-app 3D / HUD Map for live navigation and convoy telemetry tracking.

---

## 2. Architecture & Data Model

### 2.1 TripRouteSegment Model
```kotlin
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

### 2.2 Updated TripMetadata Model
```kotlin
@IgnoreExtraProperties
data class TripMetadata(
    val tripId: String = "",
    val tripName: String = "",
    val encodedPolyline: String = "",
    val leadUserId: String = "",
    val sweepUserId: String = "",
    val status: String = "ACTIVE",
    val createdTimestamp: Long = System.currentTimeMillis(),
    val members: Map<String, ConvoyMember> = emptyMap(),
    val routeSegments: List<TripRouteSegment> = emptyList(),
    val activeSegmentId: String = ""
)
```

---

## 3. Google Maps Link Parsing Engine (`GoogleMapsUrlParser`)

### 3.1 Supported URL Formats:
1. **Direct Coordinates in Path:**
   `https://www.google.com/maps/dir/19.0760,72.8777/18.5204,73.8567`
2. **Query Parameters (`api=1`):**
   `https://www.google.com/maps/dir/?api=1&origin=Mumbai&destination=Pune&waypoints=Lonavala`
3. **Location / Place URLs:**
   `https://www.google.com/maps/place/18.5204,73.8567` or `@18.5204,73.8567,15z`
4. **Short Links (`maps.app.goo.gl` / `goo.gl/maps`):**
   Resolved via HTTP redirect in background coroutines to retrieve final query/coordinates.

### 3.2 Parsing Algorithm:
- Detect URL format using regex.
- If short link, follow HTTP 301/302 redirects to obtain target canonical URL.
- Extract origin, destination, and intermediate waypoints.
- Call `DirectionsRepository` to compute exact road geometry, turn-by-turn polyline, total km, and duration.
- Fallback: If link is a single location or place name, set as Destination and prompt for Origin or use current GPS location.

---

## 4. UI / UX Design

### 4.1 Trip Creation / Planning Screen:
- **Trip Info Card:** Trip Name, Description, Dates.
- **Route Segments Section:**
  - List of created segments: *"Day 1: Coastline Route"*, *"Day 2: Hill Climb"*.
  - **"Add Route / Day Link" Button**: Opens dialog/modal to:
    - Enter custom name (e.g. *"Day 1"*, *"Day 2: Morning Leg"*).
    - Paste Google Maps URL OR manually search Origin & Destination.
    - Click **"Analyze & Fetch Route"** → Previews distance, ETA, and mini map polyline.
    - Save segment to the trip list.
  - Re-order / Delete segments.
  - Save Full Trip button.

### 4.2 Trip Details / Joined Riders Screen:
- Displays Trip Overview with all joined convoy members.
- **"Trip Itinerary & Routes" Section:**
  - Card for each Day/Segment showing Name, Origin → Destination, Distance (km), and Estimated Time.
  - Active / selected indicator.
  - **"Load Route in App Map" (Start Ride Button):** Loads the selected day's polyline into the HUD Live Convoy Map and initiates GPS tracking for that segment.

---

## 5. Cloudflare Edge Backend Sync

### 5.1 Endpoint Updates in `cloudflare-backend/src/index.ts`:
- `/api/trip/create` (POST): Accepts trip metadata with `routeSegments`.
- `/api/trip/get` (GET): Returns trip details and all route segments.
- `/api/trip/segment/select` (POST): Sets the active segment for the convoy session.

---

## 6. Testing & Validation Strategy
- Unit tests for `GoogleMapsUrlParser` covering coordinate links, query param links, place links, and short links.
- Unit tests for `TripRouteSegment` serialization and parsing.
- UI tests for multi-segment creation and day switching.
- End-to-end verification in debug APK.
