# Automated Trip Planner, Real-Time Itinerary Tracker & Convoy Radar Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a complete automated trip planning pipeline from document uploads (PDF/DOCX/TXT/Images), real-time sequential itinerary stop management with dynamic polyline recalculation, Google Maps external turn-by-turn navigation, a side-overlay Convoy Radar showing relative rider positions (Ahead/Behind) with distance gaps, and a Google Play policy-compliant permissions onboarding flow.

**Architecture:** 
1. **Extraction & Normalization Engine:** `ItineraryParserEngine` parses uploaded documents/images and extracts structured days/stops, normalizing locations into geocoded coordinates.
2. **Data & State Management:** `ItineraryModels` and `TripRepository` manage stop statuses (`PENDING`, `COMPLETED`, `SKIPPED`, `IGNORED`), persisting them to local storage and syncing with Cloudflare Edge API.
3. **Cockpit UI & Map Overlays:** `ConvoyRadarOverlay` renders relative rider position telemetry (Ahead/Behind/Level + distance in m/km), while `ItineraryTrackerScreen` renders day-by-day stop cards with dynamic route redrawing and Google Maps intent launching.
4. **Permissions Onboarding:** `PermissionsOnboardingDialog` provides clear disclosures for location, background service, overlay HUD, camera, emergency calling, and battery optimization.

**Tech Stack:** Kotlin, Jetpack Compose, Google Maps Android SDK & Maps Compose, Coroutines/Flow, Cloudflare Workers API, JUnit4.

## Global Constraints
- **Zero Placeholder Code:** All implementations must be production-ready and fully typed.
- **APK Generation Constraint:** DO NOT automatically compile an APK. Always ask the user first when code updates are done.
- **Google Play Policy Compliance:** Explicitly disclose permission rationale prior to requesting system permissions.

---

### Task 1: Itinerary Data Models & Stop Status Management

**Files:**
- Create: `app/src/main/java/com/ridesync/data/model/ItineraryModels.kt`
- Modify: `app/src/main/java/com/ridesync/data/model/SavedTrip.kt`
- Modify: `app/src/main/java/com/ridesync/data/repository/TripRepository.kt`
- Test: `app/src/test/java/com/ridesync/data/ItineraryModelsTest.kt`

**Interfaces:**
- Produces: `ItineraryStopStatus` (`PENDING`, `COMPLETED`, `SKIPPED`, `IGNORED`), `ItineraryStop`, `ItineraryDay`, `ItineraryTripPlan`.
- Produces: `TripRepository.updateStopStatus(tripId: String, dayNumber: Int, stopId: String, newStatus: ItineraryStopStatus)`.

- [ ] **Step 1: Write the failing test**
Create `app/src/test/java/com/ridesync/data/ItineraryModelsTest.kt` to test serialization, deserialization, and status updates of itinerary days and stops.

- [ ] **Step 2: Run test to verify it fails**
Run: `.\gradlew.bat testDebugUnitTest --tests com.ridesync.data.ItineraryModelsTest`
Expected: FAIL (unresolved reference `ItineraryStopStatus`, `ItineraryStop`).

- [ ] **Step 3: Implement `ItineraryModels.kt` & Update `SavedTrip.kt` / `TripRepository.kt`**
Define `ItineraryStopStatus`, `ItineraryStop`, `ItineraryDay`, `ItineraryTripPlan` and wire JSON serialization/deserialization into `TripRepository.kt`.

- [ ] **Step 4: Run test to verify it passes**
Run: `.\gradlew.bat testDebugUnitTest --tests com.ridesync.data.ItineraryModelsTest`
Expected: PASS

- [ ] **Step 5: Commit**
Commit changes with message `"feat(itinerary): add Itinerary models and status persistence"`.

---

### Task 2: Itinerary Parsing & Geocoding Normalization Engine

**Files:**
- Create: `app/src/main/java/com/ridesync/engine/ItineraryParserEngine.kt`
- Test: `app/src/test/java/com/ridesync/engine/ItineraryParserEngineTest.kt`

**Interfaces:**
- Produces: `ItineraryParserEngine.parseTextToTripPlan(rawText: String, defaultTitle: String): ItineraryTripPlan`
- Produces: `ItineraryParserEngine.extractFromDocumentStream(inputStream: InputStream, mimeType: String): String`

- [ ] **Step 1: Write the failing test**
Create `app/src/test/java/com/ridesync/engine/ItineraryParserEngineTest.kt` testing multi-day parsing, stop extraction, time formatting, and Google Maps URL extraction.

- [ ] **Step 2: Run test to verify it fails**
Run: `.\gradlew.bat testDebugUnitTest --tests com.ridesync.engine.ItineraryParserEngineTest`
Expected: FAIL (unresolved reference `ItineraryParserEngine`).

- [ ] **Step 3: Implement `ItineraryParserEngine.kt`**
Implement rule-based heuristic parsing, day boundary splitting, stop/time/activity regex matchers, and location/Maps URL resolver.

- [ ] **Step 4: Run test to verify it passes**
Run: `.\gradlew.bat testDebugUnitTest --tests com.ridesync.engine.ItineraryParserEngineTest`
Expected: PASS

- [ ] **Step 5: Commit**
Commit changes with message `"feat(parser): implement ItineraryParserEngine for document & text extraction"`.

---

### Task 3: Convoy Radar Relative Positioning & Distance Calculation Engine

**Files:**
- Create: `app/src/main/java/com/ridesync/engine/ConvoyRadarEngine.kt`
- Test: `app/src/test/java/com/ridesync/engine/ConvoyRadarEngineTest.kt`

**Interfaces:**
- Produces: `RadarRelativePosition` (`AHEAD`, `BEHIND`, `LEVEL_WITH_YOU`), `ConvoyRadarRiderInfo`.
- Produces: `ConvoyRadarEngine.calculateRelativePositions(myLocation: LatLng, myBearing: Float, riders: Map<String, RiderLocationPing>, members: Map<String, ConvoyMember>): List<ConvoyRadarRiderInfo>`.

- [ ] **Step 1: Write the failing test**
Create `app/src/test/java/com/ridesync/engine/ConvoyRadarEngineTest.kt` verifying relative bearing calculation, front vs back identification, and distance gap formatting.

- [ ] **Step 2: Run test to verify it fails**
Run: `.\gradlew.bat testDebugUnitTest --tests com.ridesync.engine.ConvoyRadarEngineTest`
Expected: FAIL (unresolved reference `ConvoyRadarEngine`).

- [ ] **Step 3: Implement `ConvoyRadarEngine.kt`**
Implement vector projection, haversine distance computation, relative angle offset detection, and formatted distance/speed metrics.

- [ ] **Step 4: Run test to verify it passes**
Run: `.\gradlew.bat testDebugUnitTest --tests com.ridesync.engine.ConvoyRadarEngineTest`
Expected: PASS

- [ ] **Step 5: Commit**
Commit changes with message `"feat(radar): implement ConvoyRadarEngine for relative partner tracking"`.

---

### Task 4: Google Play Policy-Compliant Permissions Onboarding UI

**Files:**
- Create: `app/src/main/java/com/ridesync/ui/permissions/PermissionsOnboardingDialog.kt`
- Modify: `app/src/main/AndroidManifest.xml`

**Interfaces:**
- Produces: `PermissionsOnboardingDialog(onAllGranted: () -> Unit, onDismiss: () -> Unit)`
- Discloses: GPS Location, Background Location, Floating Overlay HUD, Camera, Phone Calling, Battery Optimization Exemption.

- [ ] **Step 1: Add missing permissions to `AndroidManifest.xml`**
Ensure `CALL_PHONE`, `SYSTEM_ALERT_WINDOW`, `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`, `FOREGROUND_SERVICE_LOCATION` are declared.

- [ ] **Step 2: Implement `PermissionsOnboardingDialog.kt`**
Create an aesthetic dark HUD modal with clear rationale cards for each required permission, status chips, and one-tap request triggers.

- [ ] **Step 3: Verify build**
Run: `.\gradlew.bat compileDebugKotlin`
Expected: SUCCESS

- [ ] **Step 4: Commit**
Commit changes with message `"feat(permissions): add policy-compliant PermissionsOnboardingDialog"`.

---

### Task 5: Side Convoy Radar Overlay for Map & Cockpit HUD

**Files:**
- Create: `app/src/main/java/com/ridesync/ui/hud/ConvoyRadarOverlay.kt`
- Modify: `app/src/main/java/com/ridesync/ui/map/LiveMapScreen.kt`

**Interfaces:**
- Produces: `ConvoyRadarOverlay(myLocation: LatLng?, myBearing: Float, riderLocations: Map<String, RiderLocationPing>, convoyMembers: Map<String, ConvoyMember>, onFocusRider: (LatLng) -> Unit, onCallRider: (phoneNumber: String) -> Unit)`

- [ ] **Step 1: Implement `ConvoyRadarOverlay.kt`**
Create a collapsible, semi-transparent frosted side HUD displaying convoy partners, relative positions (AHEAD / BEHIND / LEVEL), distance gaps in km/m, live speed, quick-call, and map-centering buttons.

- [ ] **Step 2: Embed `ConvoyRadarOverlay` into `LiveMapScreen.kt`**
Add toggle button in `LiveMapScreen` to expand/collapse the Convoy Radar side drawer over the Google Map.

- [ ] **Step 3: Verify build**
Run: `.\gradlew.bat compileDebugKotlin`
Expected: SUCCESS

- [ ] **Step 4: Commit**
Commit changes with message `"feat(hud): add side ConvoyRadarOverlay to LiveMapScreen"`.

---

### Task 6: Interactive Day-by-Day Itinerary Tracker Screen & External Map Redirection

**Files:**
- Create: `app/src/main/java/com/ridesync/ui/itinerary/ItineraryTrackerScreen.kt`
- Modify: `app/src/main/java/com/ridesync/ui/trip/TripItineraryCards.kt`

**Interfaces:**
- Produces: `ItineraryTrackerScreen(tripId: String, onNavigateBack: () -> Unit, onLaunchCockpit: () -> Unit)`
- Features: Day selector tabs, Stop cards with status toggle (`PENDING`, `COMPLETED`, `SKIPPED`, `IGNORED`), "Open in Google Maps" turn-by-turn intent, and dynamic polyline route calculation.

- [ ] **Step 1: Implement `ItineraryTrackerScreen.kt`**
Build the full day-by-day timeline view with stop cards, action buttons for status switching, external Google Maps navigation intent (`google.navigation:q=lat,lng`), and active stops routing.

- [ ] **Step 2: Verify build**
Run: `.\gradlew.bat compileDebugKotlin`
Expected: SUCCESS

- [ ] **Step 3: Commit**
Commit changes with message `"feat(itinerary): implement ItineraryTrackerScreen with stop status & navigation intents"`.

---

### Task 7: "Upload Itinerary" Button & Document Picker Integration

**Files:**
- Modify: `app/src/main/java/com/ridesync/ui/trip/TripCreationScreen.kt`
- Modify: `app/src/main/java/com/ridesync/ui/trip/AddRouteSegmentDialog.kt`

**Interfaces:**
- Adds "📁 Upload Itinerary Document / Image" button in `TripCreationScreen` supporting `.pdf`, `.docx`, `.txt`, `.jpg`, `.png` under 10MB.
- Shows multi-step loading feedback and populates extracted days and stops into the trip builder.

- [ ] **Step 1: Add document picker launcher and file reader in `TripCreationScreen.kt`**
Implement client file validation ($\le 10$ MB) and pipe content through `ItineraryParserEngine`.

- [ ] **Step 2: Connect extracted plan to `multiDaySegments` and `TripRepository`**
Populate parsed days and stops automatically into the trip creation view.

- [ ] **Step 3: Verify build**
Run: `.\gradlew.bat compileDebugKotlin`
Expected: SUCCESS

- [ ] **Step 4: Commit**
Commit changes with message `"feat(trip): integrate Upload Itinerary file picker and parser into TripCreationScreen"`.

---

### Task 8: Cloudflare Edge Backend Itinerary API Endpoints

**Files:**
- Modify: `cloudflare-backend/src/index.ts`

**Interfaces:**
- Adds `POST /api/trip/parse-itinerary` (accepts raw text / document content and returns structured JSON).
- Adds `POST /api/trip/stop/status` (updates and broadcasts stop status changes to convoy members).

- [ ] **Step 1: Update `cloudflare-backend/src/index.ts`**
Add endpoint handlers for `/api/trip/parse-itinerary` and `/api/trip/stop/status`.

- [ ] **Step 2: Commit**
Commit changes with message `"feat(backend): add itinerary parsing and stop status endpoints to Cloudflare Edge API"`.

---

### Task 9: Complete Test Suite Verification & Validation

**Files:**
- Test all unit tests across data, engine, and UI components.

- [ ] **Step 1: Run complete test suite**
Run: `.\gradlew.bat testDebugUnitTest`
Expected: `BUILD SUCCESSFUL` with all tests passing.

- [ ] **Step 2: Prompt user regarding APK build**
Ask user: *"Do you want to create an APK with this update?"*.
