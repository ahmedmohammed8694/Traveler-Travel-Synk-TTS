package com.ridesync.ui.map

import android.content.Context
import android.content.Intent
import android.location.Location
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.ridesync.data.model.*
import com.ridesync.data.repository.DirectionsRepository
import com.ridesync.data.repository.GoogleMapsNavigationHelper
import com.ridesync.data.repository.RouteDetails
import com.ridesync.data.repository.TripRepository
import com.ridesync.engine.LiveLocationEngine
import com.ridesync.ui.hud.ConvoyRadarOverlay
import com.ridesync.ui.hud.GoogleMapsDirectionsOptionsModal
import com.ridesync.ui.hud.GoogleMapsNavigationHUD
import com.ridesync.ui.hud.TopConvoyLeaderboardOverlay
import com.ridesync.ui.theme.HudColors
import com.ridesync.ui.theme.frostedGlassHud
import com.ridesync.ui.theme.hud3dCard
import com.ridesync.ui.trip.EditTripRouteDialog
import com.ridesync.ui.trip.ShareTripQrDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Modern High-Contrast Cockpit HUD Live Map Screen.
 * Features:
 * 1. Takes starting point dynamically from live phone GPS location.
 * 2. Displays active itinerary stops as interactive markers.
 * 3. Marking a point as Completed or Skipped removes it from the map and dynamically updates the route.
 * 4. Geofencing arrival detection triggers an automatic Arrival Popup to mark completed or skip.
 * 5. In-Map Milestones management drawer and "Edit Route" for Full Trip or Single Day routes.
 * 6. Scannable Convoy QR Code & Share invite dialog.
 */
@Composable
fun LiveMapScreen(
    routePolyline: List<LatLng>,
    riderLocations: Map<String, RiderLocationPing>,
    convoyMembers: Map<String, ConvoyMember>,
    stopEvents: List<StopEvent>,
    isOnline: Boolean = true,
    activeTripId: String? = null,
    onToggleFullScreen: (Boolean) -> Unit = {},
    onStopReported: (StopReason) -> Unit = {},
    onSosReported: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val context = LocalContext.current

    var isFullScreenMap by remember { mutableStateOf(false) }

    LaunchedEffect(isFullScreenMap) {
        onToggleFullScreen(isFullScreenMap)
    }

    // Observe all saved trips from repository
    val allTrips by TripRepository.tripsFlow.collectAsState()
    val currentActiveTrip = remember(allTrips, activeTripId) {
        if (activeTripId != null) {
            allTrips.firstOrNull { it.tripId == activeTripId }
        } else {
            allTrips.firstOrNull { it.category == TripCategory.ONGOING } ?: allTrips.firstOrNull()
        }
    }

    // Active Day Number (Default: 1)
    var selectedDayNumber by remember { mutableIntStateOf(1) }

    // Dynamic State for Fully Resolved Day Stops (with 100% valid LatLng coordinates)
    var currentDayStops by remember(currentActiveTrip?.tripId, selectedDayNumber) {
        mutableStateOf<List<ItineraryStop>>(emptyList())
    }

    // Extraction and Geo-resolution effect for ALL 3 creation methods: Upload Itinerary, Map Link, Direct Search
    LaunchedEffect(currentActiveTrip, selectedDayNumber) {
        val trip = currentActiveTrip ?: return@LaunchedEffect
        val days = trip.itineraryPlan?.days
        val segments = trip.routeSegments

        val rawStopsList = when {
            // Method 1: Upload Itinerary with multi-day breakdown
            !days.isNullOrEmpty() -> {
                val matchingDay = days.firstOrNull { it.dayNumber == selectedDayNumber } ?: days.first()
                val dayStops = matchingDay.stops.toMutableList()

                if (dayStops.isEmpty()) {
                    listOf(
                        ItineraryStop(stopId = "stop_orig", stopName = trip.originName, latitude = trip.startLatLng.latitude, longitude = trip.startLatLng.longitude, status = ItineraryStopStatus.PENDING, orderIndex = 0),
                        ItineraryStop(stopId = "stop_dest", stopName = trip.destinationName, latitude = trip.destLatLng.latitude, longitude = trip.destLatLng.longitude, status = ItineraryStopStatus.PENDING, orderIndex = 1)
                    )
                } else {
                    // Prepend origin if missing
                    if (dayStops.none { it.stopName.equals(trip.originName, ignoreCase = true) } && trip.startLatLng.latitude != 0.0) {
                        dayStops.add(0, ItineraryStop(stopId = "stop_start", stopName = trip.originName, latitude = trip.startLatLng.latitude, longitude = trip.startLatLng.longitude, status = ItineraryStopStatus.PENDING, orderIndex = 0))
                    }
                    // Append destination if missing
                    if (dayStops.none { it.stopName.equals(trip.destinationName, ignoreCase = true) } && trip.destLatLng.latitude != 0.0) {
                        dayStops.add(ItineraryStop(stopId = "stop_end", stopName = trip.destinationName, latitude = trip.destLatLng.latitude, longitude = trip.destLatLng.longitude, status = ItineraryStopStatus.PENDING, orderIndex = dayStops.size))
                    }
                    dayStops
                }
            }

            // Method 2: Map Link with multi-day route segments
            segments.isNotEmpty() -> {
                val matchingSeg = segments.firstOrNull { it.orderIndex + 1 == selectedDayNumber }
                    ?: segments.firstOrNull { it.orderIndex == selectedDayNumber - 1 }
                    ?: segments.first()

                val segStops = mutableListOf<ItineraryStop>()
                segStops.add(ItineraryStop(stopId = "seg_${matchingSeg.segmentId}_orig", stopName = matchingSeg.originName, status = ItineraryStopStatus.PENDING, orderIndex = 0))
                matchingSeg.waypoints.forEachIndexed { idx, wp ->
                    segStops.add(ItineraryStop(stopId = "seg_${matchingSeg.segmentId}_wp_$idx", stopName = wp, status = ItineraryStopStatus.PENDING, orderIndex = idx + 1))
                }
                segStops.add(ItineraryStop(stopId = "seg_${matchingSeg.segmentId}_dest", stopName = matchingSeg.destinationName, status = ItineraryStopStatus.PENDING, orderIndex = matchingSeg.waypoints.size + 1))
                segStops
            }

            // Method 3: Direct Search Option or Single Leg Trip
            else -> {
                val stops = mutableListOf<ItineraryStop>()
                stops.add(ItineraryStop(stopId = "stop_0", stopName = trip.originName, latitude = trip.startLatLng.latitude, longitude = trip.startLatLng.longitude, activityDescription = "Starting Location", status = ItineraryStopStatus.PENDING, orderIndex = 0))
                trip.waypoints.forEachIndexed { idx, wpName ->
                    val knownLatLng = trip.waypointLatLngs.getOrNull(idx)
                    stops.add(
                        ItineraryStop(
                            stopId = "stop_${idx + 1}",
                            stopName = wpName,
                            activityDescription = "Scheduled Milestone",
                            latitude = knownLatLng?.latitude ?: 0.0,
                            longitude = knownLatLng?.longitude ?: 0.0,
                            status = ItineraryStopStatus.PENDING,
                            orderIndex = idx + 1
                        )
                    )
                }
                stops.add(ItineraryStop(stopId = "stop_${trip.waypoints.size + 1}", stopName = trip.destinationName, latitude = trip.destLatLng.latitude, longitude = trip.destLatLng.longitude, activityDescription = "Destination", status = ItineraryStopStatus.PENDING, orderIndex = trip.waypoints.size + 1))
                stops
            }
        }

        // Multi-tier coordinate resolution (DirectionsRepository -> Android Geocoder -> Route Corridor Fallback)
        val startPt = if (trip.startLatLng.latitude != 0.0) trip.startLatLng else LatLng(17.3753, 78.4344)
        val destPt = if (trip.destLatLng.latitude != 0.0) trip.destLatLng else LatLng(16.5772, 79.3125)

        val resolvedList = withContext(Dispatchers.IO) {
            rawStopsList.mapIndexed { idx, stop ->
                var lat = stop.latitude
                var lng = stop.longitude

                if (lat == 0.0 && lng == 0.0) {
                    val resolvedLatLng = DirectionsRepository.resolveLocationNameToLatLng(
                        stop.stopName.ifBlank { stop.rawLocationText },
                        null
                    )
                    if (resolvedLatLng.latitude != 17.3753 || resolvedLatLng.longitude != 78.4344 || stop.stopName.contains("Attapur", ignoreCase = true)) {
                        lat = resolvedLatLng.latitude
                        lng = resolvedLatLng.longitude
                    } else {
                        try {
                            val geocoder = android.location.Geocoder(context, java.util.Locale.getDefault())
                            val addrs = geocoder.getFromLocationName(stop.stopName.ifBlank { stop.rawLocationText }, 1)
                            if (!addrs.isNullOrEmpty()) {
                                lat = addrs[0].latitude
                                lng = addrs[0].longitude
                            }
                        } catch (_: Exception) {}

                        if (lat == 0.0 && lng == 0.0) {
                            val ratio = (idx.toDouble()) / (rawStopsList.size - 1).coerceAtLeast(1)
                            lat = startPt.latitude + (destPt.latitude - startPt.latitude) * ratio
                            lng = startPt.longitude + (destPt.longitude - startPt.longitude) * ratio
                        }
                    }
                }
                stop.copy(latitude = lat, longitude = lng, orderIndex = idx)
            }
        }

        currentDayStops = resolvedList
    }

    // Filter only PENDING stops to be displayed on the map
    val pendingStopsOnMap = remember(currentDayStops) {
        currentDayStops.filter { it.status == ItineraryStopStatus.PENDING }
    }

    // Live Phone GPS location
    val livePhonePing by LiveLocationEngine.liveLocationPing.collectAsState()
    val myPing = livePhonePing ?: riderLocations.values.firstOrNull()

    // Trip Navigation State (false = Route Preview Mode from Planned Origin, true = Live Navigation Mode from Current GPS)
    var isTripStarted by remember(currentActiveTrip?.tripId, selectedDayNumber) { mutableStateOf(false) }

    // Planned origin and destination from trip itinerary / day route details
    val plannedStartPos = remember(currentDayStops, currentActiveTrip, routePolyline) {
        val firstStop = currentDayStops.firstOrNull()
        if (firstStop != null && firstStop.latitude != 0.0 && firstStop.longitude != 0.0) {
            LatLng(firstStop.latitude, firstStop.longitude)
        } else if (currentActiveTrip != null && currentActiveTrip.startLatLng.latitude != 0.0) {
            currentActiveTrip.startLatLng
        } else {
            routePolyline.firstOrNull() ?: LatLng(17.3753, 78.4344)
        }
    }

    val plannedDestPos = remember(currentDayStops, currentActiveTrip, routePolyline) {
        val lastStop = currentDayStops.lastOrNull()
        if (lastStop != null && lastStop.latitude != 0.0 && lastStop.longitude != 0.0) {
            LatLng(lastStop.latitude, lastStop.longitude)
        } else if (currentActiveTrip != null && currentActiveTrip.destLatLng.latitude != 0.0) {
            currentActiveTrip.destLatLng
        } else {
            routePolyline.lastOrNull() ?: LatLng(16.5772, 79.3125)
        }
    }

    // Dynamic start point: Uses planned start pos in preview mode, and real current phone GPS in live navigation mode
    val dynamicStartPos = remember(isTripStarted, myPing, plannedStartPos) {
        if (isTripStarted && myPing != null && myPing.latitude != 0.0 && myPing.longitude != 0.0) {
            LatLng(myPing.latitude, myPing.longitude)
        } else {
            plannedStartPos
        }
    }

    val dynamicDestPos = remember(isTripStarted, pendingStopsOnMap, plannedDestPos) {
        if (isTripStarted) {
            val lastPending = pendingStopsOnMap.lastOrNull()
            if (lastPending != null && lastPending.latitude != 0.0 && lastPending.longitude != 0.0) {
                LatLng(lastPending.latitude, lastPending.longitude)
            } else {
                plannedDestPos
            }
        } else {
            plannedDestPos
        }
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.Builder()
            .target(plannedStartPos)
            .zoom(15f)
            .tilt(30f)
            .bearing(0f)
            .build()
    }

    // Interactive Map & Google Maps Navigation State
    var selectedMapType by remember { mutableStateOf(MapType.NORMAL) }
    var isTrafficEnabled by remember { mutableStateOf(false) }
    var isFollowMode by remember { mutableStateOf(true) }
    var is3dTilt by remember { mutableStateOf(true) }
    var showMapTypePickerModal by remember { mutableStateOf(false) }
    var showKeySetupDialog by remember { mutableStateOf(false) }
    var isLiveNavigating by remember { mutableStateOf(false) }
    var showDirectionsModal by remember { mutableStateOf(false) }
    var showStopsDrawer by remember { mutableStateOf(false) }
    var showEditRouteDialog by remember { mutableStateOf(false) }
    var showShareQrDialog by remember { mutableStateOf(false) }
    var currentNavStepIndex by remember { mutableIntStateOf(0) }
    var currentRouteDetails by remember { mutableStateOf<RouteDetails?>(null) }
    var plannedRoadPolyline by remember { mutableStateOf<List<LatLng>>(routePolyline) }
    var activeDynamicPolyline by remember { mutableStateOf(routePolyline) }

    // Arrival Notification Geofence State
    var arrivalPopupStop by remember { mutableStateOf<ItineraryStop?>(null) }
    val notifiedStopIds = remember { mutableSetOf<String>() }

    // Selected stop for quick details modal
    var selectedStopForModal by remember { mutableStateOf<ItineraryStop?>(null) }

    val isDefaultKey = remember {
        com.ridesync.BuildConfig.MAPS_API_KEY == "AIzaSyBYs6gsD4eKkKIvsMMC4YpukGC5XIh9UkU" ||
                com.ridesync.BuildConfig.MAPS_API_KEY.isBlank()
    }

    // Dynamic Route Calculation: Planned Trip Details (Preview Mode) OR Current Phone GPS (Live Navigation Mode)
    LaunchedEffect(isTripStarted, plannedStartPos, plannedDestPos, myPing, pendingStopsOnMap.size, selectedDayNumber, currentDayStops) {
        try {
            val origin = if (isTripStarted && myPing != null && myPing.latitude != 0.0 && myPing.longitude != 0.0) {
                LatLng(myPing.latitude, myPing.longitude)
            } else {
                plannedStartPos
            }

            val destination = if (isTripStarted) {
                pendingStopsOnMap.lastOrNull()?.let { LatLng(it.latitude, it.longitude) } ?: plannedDestPos
            } else {
                plannedDestPos
            }

            val stopsList = if (isTripStarted) pendingStopsOnMap else currentDayStops
            val intermediateWaypoints = if (stopsList.size > 2) {
                stopsList.subList(1, stopsList.size - 1)
                    .filter { it.latitude != 0.0 && it.longitude != 0.0 }
                    .map { LatLng(it.latitude, it.longitude) }
            } else {
                emptyList()
            }

            val details = withContext(Dispatchers.IO) {
                DirectionsRepository.getDirectionsRoute(
                    origin = origin,
                    destination = destination,
                    waypoints = intermediateWaypoints
                )
            }
            currentRouteDetails = details

            if (!isTripStarted) {
                // Preview Mode: Save high-density planned road polyline
                if (details.isRealGoogleRoute && details.polylinePoints.size > 10) {
                    plannedRoadPolyline = details.polylinePoints
                    activeDynamicPolyline = details.polylinePoints
                } else if (routePolyline.size > 10) {
                    plannedRoadPolyline = routePolyline
                    activeDynamicPolyline = routePolyline
                } else if (details.polylinePoints.isNotEmpty()) {
                    activeDynamicPolyline = details.polylinePoints
                }
            } else {
                // Live Navigation Mode: Use live road route if available, otherwise snap myPing to planned road polyline
                if (details.isRealGoogleRoute && details.polylinePoints.size > 10) {
                    activeDynamicPolyline = details.polylinePoints
                } else {
                    val basePolyline = when {
                        plannedRoadPolyline.size > 10 -> plannedRoadPolyline
                        activeDynamicPolyline.size > 10 -> activeDynamicPolyline
                        routePolyline.size > 10 -> routePolyline
                        else -> emptyList()
                    }

                    if (basePolyline.isNotEmpty() && myPing != null && myPing.latitude != 0.0 && myPing.longitude != 0.0) {
                        val myPos = LatLng(myPing.latitude, myPing.longitude)
                        val closestIdx = findClosestPointIndex(basePolyline, myPos)
                        val snappedPoints = listOf(myPos) + basePolyline.drop(closestIdx)
                        if (snappedPoints.size > 1) {
                            activeDynamicPolyline = snappedPoints
                        }
                    } else if (basePolyline.isNotEmpty()) {
                        activeDynamicPolyline = basePolyline
                    } else if (details.polylinePoints.isNotEmpty()) {
                        activeDynamicPolyline = details.polylinePoints
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Geofencing Arrival Detection: Trigger popup when within 150m of any pending stop
    LaunchedEffect(myPing, pendingStopsOnMap) {
        val ping = myPing ?: return@LaunchedEffect
        if (ping.latitude == 0.0 && ping.longitude == 0.0) return@LaunchedEffect

        val phoneLoc = Location("GPS").apply {
            latitude = ping.latitude
            longitude = ping.longitude
        }

        for (stop in pendingStopsOnMap) {
            if (stop.latitude != 0.0 && stop.longitude != 0.0 && !notifiedStopIds.contains(stop.stopId)) {
                val stopLoc = Location("Stop").apply {
                    latitude = stop.latitude
                    longitude = stop.longitude
                }
                val distanceMeters = phoneLoc.distanceTo(stopLoc)
                if (distanceMeters <= 150f) {
                    notifiedStopIds.add(stop.stopId)
                    arrivalPopupStop = stop
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    break
                }
            }
        }
    }

    // Follow lead/phone rider location automatically in live mode, or center on planned origin in preview mode
    LaunchedEffect(plannedStartPos, dynamicStartPos, isFollowMode, is3dTilt, isLiveNavigating, isTripStarted) {
        if (isTripStarted && (isFollowMode || isLiveNavigating)) {
            val tiltAngle = if (is3dTilt || isLiveNavigating) 55f else 0f
            val bearingAngle = myPing?.bearing ?: 0f
            val targetCam = CameraPosition.Builder()
                .target(dynamicStartPos)
                .zoom(if (isLiveNavigating) 17.5f else 16.5f)
                .tilt(tiltAngle)
                .bearing(bearingAngle)
                .build()
            cameraPositionState.animate(CameraUpdateFactory.newCameraPosition(targetCam), 800)
        } else if (!isTripStarted && isFollowMode) {
            val targetCam = CameraPosition.Builder()
                .target(plannedStartPos)
                .zoom(15f)
                .tilt(30f)
                .bearing(0f)
                .build()
            cameraPositionState.animate(CameraUpdateFactory.newCameraPosition(targetCam), 800)
        }
    }

    // Calculate convoy rider position ranks (#1, #2, #3...) along route
    val riderRanks = remember(dynamicStartPos, riderLocations, convoyMembers) {
        val list = com.ridesync.engine.ConvoyRadarEngine.calculateRelativePositions(
            myLocation = dynamicStartPos,
            myBearing = myPing?.bearing ?: 0f,
            riders = riderLocations,
            members = convoyMembers
        )
        val rankMap = mutableMapOf<String, Int>()
        list.forEachIndexed { idx, info ->
            rankMap[info.riderId] = idx + 1
        }
        rankMap
    }

    Box(modifier = modifier.fillMaxSize().background(HudColors.ObsidianCanvas)) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            uiSettings = MapUiSettings(
                zoomControlsEnabled = false,
                myLocationButtonEnabled = false,
                compassEnabled = true,
                mapToolbarEnabled = false
            ),
            properties = MapProperties(
                mapType = selectedMapType,
                isTrafficEnabled = isTrafficEnabled,
                isMyLocationEnabled = LiveLocationEngine.hasLocationPermission(context)
            )
        ) {
            // Multi-Layered Neon Route Polyline (Glowing Underlayer + Vibrant Core Ribbon)
            val polylineToShow = if (activeDynamicPolyline.isNotEmpty()) activeDynamicPolyline else routePolyline
            if (polylineToShow.isNotEmpty()) {
                // Outer Cyan Glow Line (width = 16f, alpha = 0.3)
                Polyline(
                    points = polylineToShow,
                    color = HudColors.CyanGlow,
                    width = 16f,
                    geodesic = true
                )
                // Core Sharp Neon Ribbon Line (width = 8f, #06B6D4)
                Polyline(
                    points = polylineToShow,
                    color = HudColors.CyanPrimary,
                    width = 8f,
                    geodesic = true
                )
            }

            // Render Only Active PENDING Stops on the Map displaying actual Stop Name
            pendingStopsOnMap.forEachIndexed { sIdx, stop ->
                if (stop.latitude != 0.0 && stop.longitude != 0.0) {
                    val isFirst = sIdx == 0
                    val isLast = sIdx == pendingStopsOnMap.size - 1

                    RouteStopMarker(
                        position = LatLng(stop.latitude, stop.longitude),
                        stopName = stop.stopName,
                        stopNumber = stop.orderIndex + 1,
                        isFirstStop = isFirst,
                        isLastStop = isLast,
                        onClick = {
                            selectedStopForModal = stop
                        }
                    )
                }
            }

            // Render 3D Rider Markers with Position Rank Badge (#1, #2, #3...) & Profile Photo
            riderLocations.forEach { (userId, ping) ->
                val member = convoyMembers[userId]
                val status = member?.status ?: RiderStatus.RIDING
                val posRank = riderRanks[userId] ?: 0

                InterpolatedRiderMarker3D(
                    targetLocation = LatLng(ping.latitude, ping.longitude),
                    bearing = ping.bearing,
                    displayName = member?.displayName ?: "Rider",
                    status = status,
                    photoUrl = member?.photoUrl ?: "",
                    vehicleModel = member?.vehicleModel ?: "",
                    positionNumber = posRank
                )
            }

            // Render stop event markers
            stopEvents.forEach { stop ->
                Marker(
                    state = MarkerState(position = LatLng(stop.latitude, stop.longitude)),
                    title = "Stop: ${stop.reason.name}",
                    snippet = "Rider: ${stop.riderName}"
                )
            }
        }

        // TOP SIDE RACING CONVOY LEADERBOARD OVERLAY (Click any rider circle to focus map)
        TopConvoyLeaderboardOverlay(
            myLocation = dynamicStartPos,
            myBearing = myPing?.bearing ?: 0f,
            riderLocations = riderLocations,
            convoyMembers = convoyMembers,
            onFocusRider = { targetPos, riderName ->
                isFollowMode = false
                coroutineScope.launch {
                    cameraPositionState.animate(
                        CameraUpdateFactory.newCameraPosition(
                            CameraPosition.Builder()
                                .target(targetPos)
                                .zoom(17.5f)
                                .tilt(if (is3dTilt) 50f else 0f)
                                .build()
                        ),
                        600
                    )
                }
                Toast.makeText(context, "Map camera focused on $riderName 🎯", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 10.dp)
        )

        // Top Right: Floating Frosted HUD Map Controls
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 16.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // "I'm Stopping" Warning Button (Same 52.dp size & shape as map controls)
            HudMapOptionIconButton(
                icon = Icons.Default.Warning,
                contentDescription = "I'm Stopping",
                active = true,
                activeColor = Color(0xFFF59E0B),
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onStopReported(StopReason.REST)
                }
            )

            // "Emergency SOS" Alert Button (Same 52.dp size & shape as map controls)
            HudMapOptionIconButton(
                icon = Icons.Default.ReportProblem,
                contentDescription = "Emergency SOS",
                active = true,
                activeColor = Color(0xFFEF4444),
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onSosReported()
                }
            )

            // My Live Phone GPS Re-center Button
            HudMapOptionIconButton(
                icon = Icons.Default.MyLocation,
                contentDescription = "My Live GPS Location",
                active = false,
                activeColor = HudColors.CyanPrimary,
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    coroutineScope.launch {
                        val phoneLoc = LiveLocationEngine.getCurrentPhoneLocation(context, forceFresh = true)
                        if (phoneLoc != null) {
                            cameraPositionState.animate(
                                CameraUpdateFactory.newCameraPosition(
                                    CameraPosition.Builder()
                                        .target(phoneLoc)
                                        .zoom(17f)
                                        .tilt(if (is3dTilt) 50f else 0f)
                                        .build()
                                ),
                                600
                            )
                        }
                    }
                }
            )

            // Planned Stops & Milestones Drawer Toggle Button
            HudMapOptionIconButton(
                icon = Icons.Default.Place,
                contentDescription = "Planned Stops",
                active = showStopsDrawer || pendingStopsOnMap.isNotEmpty(),
                activeColor = Color(0xFFF59E0B),
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    showStopsDrawer = !showStopsDrawer
                }
            )

            // Edit Route & Milestones Button
            HudMapOptionIconButton(
                icon = Icons.Default.EditLocationAlt,
                contentDescription = "Edit Route",
                active = showEditRouteDialog,
                activeColor = Color(0xFF38BDF8),
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    showEditRouteDialog = true
                }
            )

            // Share QR Code & Convoy Invite Button
            HudMapOptionIconButton(
                icon = Icons.Default.QrCode,
                contentDescription = "Share Convoy QR Code",
                active = showShareQrDialog,
                activeColor = Color(0xFFFBBF24),
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    showShareQrDialog = true
                }
            )

            // Directions & Google Maps Navigation Button
            HudMapOptionIconButton(
                icon = Icons.Default.Directions,
                contentDescription = "Directions & Navigation Options",
                active = isLiveNavigating || showDirectionsModal,
                activeColor = Color(0xFF00E5FF),
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    showDirectionsModal = true
                }
            )

            // Map Type Selector Button
            HudMapOptionIconButton(
                icon = Icons.Default.Layers,
                contentDescription = "Map Options",
                active = showMapTypePickerModal,
                activeColor = HudColors.StatusStopped,
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    showMapTypePickerModal = true
                }
            )

            // Follow Mode / Free Pan Lock Toggle
            HudMapOptionIconButton(
                icon = if (isFollowMode) Icons.Default.Lock else Icons.Default.LockOpen,
                contentDescription = "Interaction Mode",
                active = isFollowMode,
                activeColor = HudColors.StatusRiding,
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    isFollowMode = !isFollowMode
                }
            )

            // 3D Tilt Perspective Toggle
            HudMapOptionIconButton(
                icon = Icons.Default.Explore,
                contentDescription = "3D Camera Perspective",
                active = is3dTilt,
                activeColor = HudColors.CyanPrimary,
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    is3dTilt = !is3dTilt
                }
            )

            // Fullscreen Map Mode Toggle Button
            HudMapOptionIconButton(
                icon = if (isFullScreenMap) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                contentDescription = "Full Screen Map",
                active = isFullScreenMap,
                activeColor = Color(0xFF10B981),
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    isFullScreenMap = !isFullScreenMap
                }
            )
        }

        // Top Left: Compact Semi-Transparent HUD & GPS Status Badge (Does not cover map)
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0x35000000), // High transparency (20% opacity) so map is completely visible
            border = BorderStroke(1.dp, Color(0x22FFFFFF)),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 16.dp, start = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(if (isFollowMode) HudColors.StatusRiding else HudColors.StatusStopped, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isFollowMode) "GPS LOCKED" else "FREE PAN",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                    if (currentActiveTrip != null) {
                        val nextStop = pendingStopsOnMap.firstOrNull()
                        Text(
                            text = " • Next: ${nextStop?.stopName ?: "Destination"}",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFFBBF24),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.clickable { showStopsDrawer = true }
                        )
                    }
                }
                if (myPing != null) {
                    Text(
                        text = "📡 ${"%.4f".format(myPing.latitude)}, ${"%.4f".format(myPing.longitude)} • ${myPing.speedKmh.toInt()} km/h",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFF67E8F9)
                    )
                }
            }
        }



        // Bottom Expandable Planned Stops Drawer
        AnimatedVisibility(
            visible = showStopsDrawer,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 90.dp, start = 16.dp, end = 16.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xEE0F172A)),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color(0xFF334155)),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 280.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Route, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Planned Milestones (${pendingStopsOnMap.size} Remaining)",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp
                            )
                        }
                        IconButton(onClick = { showStopsDrawer = false }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (currentDayStops.isEmpty()) {
                        Text("No stops loaded for active route.", color = Color(0xFF94A3B8), fontSize = 12.sp)
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            itemsIndexed(currentDayStops) { sIdx, stop ->
                                val isCompleted = stop.status == ItineraryStopStatus.COMPLETED
                                val isSkipped = stop.status == ItineraryStopStatus.SKIPPED

                                Surface(
                                    color = if (isCompleted || isSkipped) Color(0xFF1E293B) else Color(0xFF020617),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, if (isCompleted) Color(0xFF10B981) else Color(0xFF334155)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "${sIdx + 1}. ${stop.stopName}",
                                                color = if (isCompleted || isSkipped) Color(0xFF94A3B8) else Color.White,
                                                textDecoration = if (isCompleted || isSkipped) TextDecoration.LineThrough else TextDecoration.None,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                            if (stop.activityDescription.isNotBlank()) {
                                                Text(stop.activityDescription, color = Color(0xFF64748B), fontSize = 10.sp)
                                            }
                                        }

                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            // Complete button
                                            IconButton(
                                                onClick = {
                                                    val tripId = currentActiveTrip?.tripId ?: "active_trip_101"
                                                    val next = if (isCompleted) ItineraryStopStatus.PENDING else ItineraryStopStatus.COMPLETED
                                                    TripRepository.updateStopStatus(tripId, selectedDayNumber, stop.stopId, next)
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.CheckCircle,
                                                    contentDescription = "Complete",
                                                    tint = if (isCompleted) Color(0xFF10B981) else Color(0xFF64748B),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }

                                            // Skip button
                                            IconButton(
                                                onClick = {
                                                    val tripId = currentActiveTrip?.tripId ?: "active_trip_101"
                                                    val next = if (isSkipped) ItineraryStopStatus.PENDING else ItineraryStopStatus.SKIPPED
                                                    TripRepository.updateStopStatus(tripId, selectedDayNumber, stop.stopId, next)
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Cancel,
                                                    contentDescription = "Skip",
                                                    tint = if (isSkipped) Color(0xFFEF4444) else Color(0xFF64748B),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Route Preview & "START TRIP NAVIGATION" HUD Card
        AnimatedVisibility(
            visible = !isTripStarted,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 90.dp, start = 16.dp, end = 16.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xF00F172A)),
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.5.dp, Color(0xFF00E5FF)),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            color = Color(0xFF0284C7).copy(alpha = 0.2f),
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, Color(0xFF38BDF8))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(Color(0xFF38BDF8), CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "🗺️ PLANNED ROUTE PREVIEW",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF38BDF8)
                                )
                            }
                        }

                        if (currentActiveTrip != null && (currentActiveTrip.itineraryPlan?.days?.size ?: 0) > 1) {
                            Text(
                                text = "Day $selectedDayNumber of ${currentActiveTrip.itineraryPlan?.days?.size}",
                                color = Color(0xFFF59E0B),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = currentActiveTrip?.title ?: "Planned Trip Route",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    val startName = currentDayStops.firstOrNull()?.stopName ?: currentActiveTrip?.originName ?: "Origin"
                    val destName = currentDayStops.lastOrNull()?.stopName ?: currentActiveTrip?.destinationName ?: "Destination"
                    val stopCount = (currentDayStops.size - 2).coerceAtLeast(0)

                    Text(
                        text = "From $startName to $destName" + (if (stopCount > 0) " ($stopCount intermediate stop${if (stopCount > 1) "s" else ""})" else ""),
                        fontSize = 13.sp,
                        color = Color(0xFFCBD5E1)
                    )

                    if (currentDayStops.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            currentDayStops.take(4).forEachIndexed { idx, stop ->
                                Surface(
                                    color = Color(0xFF1E293B),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, Color(0xFF334155))
                                ) {
                                    Text(
                                        text = "${idx + 1}. ${stop.stopName}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF94A3B8),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        maxLines = 1
                                    )
                                }
                            }
                            if (currentDayStops.size > 4) {
                                Text(
                                    text = "+${currentDayStops.size - 4} more",
                                    fontSize = 10.sp,
                                    color = Color(0xFF64748B),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            isTripStarted = true
                            isLiveNavigating = true
                            isFollowMode = true
                            is3dTilt = true
                            Toast.makeText(context, "🚀 Trip Started! Navigating from current GPS location", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00E5FF),
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Icon(
                            Icons.Default.Navigation,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "🚀 START TRIP NAVIGATION",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }

        // Google Maps Turn-by-Turn Live Navigation HUD Overlay
        if (isLiveNavigating) {
            GoogleMapsNavigationHUD(
                routeDetails = currentRouteDetails,
                currentSpeedKmh = myPing?.speedKmh?.toDouble() ?: 0.0,
                currentStepIndex = currentNavStepIndex,
                onStepSelected = { idx -> currentNavStepIndex = idx },
                onExitNavigation = {
                    isLiveNavigating = false
                    isTripStarted = false
                },
                onOpenGoogleMapsApp = {
                    GoogleMapsNavigationHelper.launchGoogleMapsTurnByTurn(context, dynamicDestPos)
                },
                onOpenGoogleMapsWeb = {
                    GoogleMapsNavigationHelper.launchGoogleMapsRouteUrl(context, dynamicStartPos, dynamicDestPos)
                }
            )
        }
    }

    // ========================================================
    // 1. ARRIVAL POPUP (Geofence Arrival Detection Alert)
    // ========================================================
    arrivalPopupStop?.let { stop ->
        AlertDialog(
            onDismissRequest = { arrivalPopupStop = null },
            containerColor = Color(0xFF0F172A),
            shape = RoundedCornerShape(20.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Celebration, contentDescription = null, tint = Color(0xFF22C55E), modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "You Have Reached!",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        color = Color(0xFF020617),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFF22C55E)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = stop.stopName,
                                color = Color(0xFF22C55E),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black
                            )
                            if (stop.activityDescription.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(stop.activityDescription, color = Color(0xFFCBD5E1), fontSize = 12.sp)
                            }
                        }
                    }

                    Text(
                        text = "Marking this point completed will remove it from the live map and update your convoy navigation to the next stop.",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val tripId = currentActiveTrip?.tripId ?: "active_trip_101"
                        TripRepository.updateStopStatus(tripId, selectedDayNumber, stop.stopId, ItineraryStopStatus.COMPLETED)
                        Toast.makeText(context, "Marked '${stop.stopName}' as Completed!", Toast.LENGTH_SHORT).show()
                        arrivalPopupStop = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E), contentColor = Color.Black),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("✅ Mark as Completed", fontWeight = FontWeight.Black)
                }
            },
            dismissButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(
                        onClick = {
                            val tripId = currentActiveTrip?.tripId ?: "active_trip_101"
                            TripRepository.updateStopStatus(tripId, selectedDayNumber, stop.stopId, ItineraryStopStatus.SKIPPED)
                            Toast.makeText(context, "Skipped '${stop.stopName}'", Toast.LENGTH_SHORT).show()
                            arrivalPopupStop = null
                        }
                    ) {
                        Text("Skip Stop", color = Color(0xFFEF4444), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    TextButton(onClick = { arrivalPopupStop = null }) {
                        Text("Later / Dismiss", color = Color(0xFF94A3B8), fontSize = 12.sp)
                    }
                }
            }
        )
    }

    // ========================================================
    // 2. STOP DETAILS & ACTION MODAL (When clicking a pin)
    // ========================================================
    selectedStopForModal?.let { stop ->
        AlertDialog(
            onDismissRequest = { selectedStopForModal = null },
            containerColor = Color(0xFF0F172A),
            shape = RoundedCornerShape(18.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Place, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stop.stopName,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stop.activityDescription.ifBlank { "Planned Stop / Milestone on Route" }, color = Color(0xFFCBD5E1), fontSize = 13.sp)
                    Text("Status: ${stop.status.name}", color = Color(0xFF38BDF8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val tripId = currentActiveTrip?.tripId ?: "active_trip_101"
                        TripRepository.updateStopStatus(tripId, selectedDayNumber, stop.stopId, ItineraryStopStatus.COMPLETED)
                        Toast.makeText(context, "Marked as Completed! Removed from active map.", Toast.LENGTH_SHORT).show()
                        selectedStopForModal = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981), contentColor = Color.Black),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Mark Completed", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Button(
                    onClick = {
                        val tripId = currentActiveTrip?.tripId ?: "active_trip_101"
                        TripRepository.updateStopStatus(tripId, selectedDayNumber, stop.stopId, ItineraryStopStatus.SKIPPED)
                        Toast.makeText(context, "Marked as Skipped! Removed from active map.", Toast.LENGTH_SHORT).show()
                        selectedStopForModal = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444), contentColor = Color.White),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Skip Stop", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // ========================================================
    // 3. EDIT ROUTE DIALOG (Full Trip or One Day Route)
    // ========================================================
    if (showEditRouteDialog && currentActiveTrip != null) {
        EditTripRouteDialog(
            trip = currentActiveTrip,
            initialDayNumber = selectedDayNumber,
            onDismiss = { showEditRouteDialog = false },
            onTripUpdated = { updated ->
                Toast.makeText(context, "Route updated and synced on live map!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // ========================================================
    // 4. SHARE QR & TRIP INVITE DIALOG
    // ========================================================
    if (showShareQrDialog && currentActiveTrip != null) {
        ShareTripQrDialog(
            tripTitle = currentActiveTrip.title,
            lobbyCode = currentActiveTrip.lobbyCode.ifBlank { "RRS-${currentActiveTrip.tripId.takeLast(4)}" },
            startDate = currentActiveTrip.scheduledDate,
            routeDescription = "${currentActiveTrip.originName} to ${currentActiveTrip.destinationName}",
            onDismiss = { showShareQrDialog = false }
        )
    }

    // Google Map Options Dialog
    if (showMapTypePickerModal) {
        AlertDialog(
            onDismissRequest = { showMapTypePickerModal = false },
            containerColor = HudColors.ObsidianModal,
            title = {
                Text(
                    text = "3D HUD Map Options",
                    color = HudColors.TextCrispWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Select Map Type", color = HudColors.TextCoolSilver, fontSize = 13.sp, fontWeight = FontWeight.Bold)

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        HudMapTypeCard(
                            title = "Default",
                            icon = Icons.Default.Map,
                            isSelected = selectedMapType == MapType.NORMAL,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                selectedMapType = MapType.NORMAL
                                showMapTypePickerModal = false
                            }
                        )
                        HudMapTypeCard(
                            title = "Satellite",
                            icon = Icons.Default.Satellite,
                            isSelected = selectedMapType == MapType.SATELLITE,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                selectedMapType = MapType.SATELLITE
                                showMapTypePickerModal = false
                            }
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        HudMapTypeCard(
                            title = "Terrain",
                            icon = Icons.Default.Terrain,
                            isSelected = selectedMapType == MapType.TERRAIN,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                selectedMapType = MapType.TERRAIN
                                showMapTypePickerModal = false
                            }
                        )
                        HudMapTypeCard(
                            title = "Hybrid",
                            icon = Icons.Default.Layers,
                            isSelected = selectedMapType == MapType.HYBRID,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                selectedMapType = MapType.HYBRID
                                showMapTypePickerModal = false
                            }
                        )
                    }

                    HorizontalDivider(color = HudColors.ObsidianBorder, modifier = Modifier.padding(vertical = 4.dp))

                    Text("3D Camera & Layer Controls", color = HudColors.TextCoolSilver, fontSize = 13.sp, fontWeight = FontWeight.Bold)

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Live Traffic Layer", color = HudColors.TextCrispWhite, fontSize = 15.sp)
                        Switch(
                            checked = isTrafficEnabled,
                            onCheckedChange = { isTrafficEnabled = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = HudColors.StatusStopped)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("3D Driving Pitch (50°)", color = HudColors.TextCrispWhite, fontSize = 15.sp)
                        Switch(
                            checked = is3dTilt,
                            onCheckedChange = { is3dTilt = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = HudColors.CyanPrimary)
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showMapTypePickerModal = false }) {
                    Text("Close", color = HudColors.TextCoolSilver, fontSize = 15.sp)
                }
            }
        )
    }

    // Google Maps Directions & Routing Modal
    if (showDirectionsModal) {
        GoogleMapsDirectionsOptionsModal(
            origin = dynamicStartPos,
            destination = dynamicDestPos,
            waypoints = pendingStopsOnMap.drop(1).dropLast(1).map { LatLng(it.latitude, it.longitude) },
            routeDetails = currentRouteDetails,
            onStartInAppNav = {
                showDirectionsModal = false
                isLiveNavigating = true
                isFollowMode = true
                is3dTilt = true
            },
            onOpenGoogleMapsApp = {
                showDirectionsModal = false
                GoogleMapsNavigationHelper.launchGoogleMapsTurnByTurn(context, dynamicDestPos)
            },
            onOpenGoogleMapsWeb = {
                showDirectionsModal = false
                GoogleMapsNavigationHelper.launchGoogleMapsRouteUrl(context, dynamicStartPos, dynamicDestPos)
            },
            onDismiss = { showDirectionsModal = false }
        )
    }
}

@Composable
private fun HudMapOptionIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    active: Boolean,
    activeColor: Color = HudColors.CyanPrimary,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(52.dp)
            .frostedGlassHud(
                shape = RoundedCornerShape(16.dp),
                backgroundColor = if (active) activeColor else HudColors.FrostedOverlay,
                borderColor = if (active) activeColor else HudColors.FrostedBorder
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (active) Color.Black else HudColors.TextCrispWhite,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun HudMapTypeCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(64.dp)
            .hud3dCard(
                shape = RoundedCornerShape(14.dp),
                startColor = if (isSelected) HudColors.CyanPrimary.copy(alpha = 0.2f) else HudColors.ObsidianElevated,
                endColor = HudColors.ObsidianSurface,
                rimColor = if (isSelected) HudColors.CyanPrimary else HudColors.RimHighlight
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) HudColors.CyanPrimary else HudColors.TextCoolSilver,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) HudColors.TextCrispWhite else HudColors.TextCoolSilver
            )
        }
    }
}

private fun findClosestPointIndex(points: List<LatLng>, target: LatLng): Int {
    var minDist = Double.MAX_VALUE
    var bestIdx = 0
    points.forEachIndexed { idx, pt ->
        val dLat = pt.latitude - target.latitude
        val dLng = pt.longitude - target.longitude
        val dist = dLat * dLat + dLng * dLng
        if (dist < minDist) {
            minDist = dist
            bestIdx = idx
        }
    }
    return bestIdx
}

