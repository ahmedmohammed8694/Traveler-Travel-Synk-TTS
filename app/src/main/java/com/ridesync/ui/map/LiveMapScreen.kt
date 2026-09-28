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
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val context = LocalContext.current

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

    // Extract all stops for the active day / trip
    val currentDayStops = remember(currentActiveTrip, selectedDayNumber) {
        val days = currentActiveTrip?.itineraryPlan?.days
        if (!days.isNullOrEmpty()) {
            val matchingDay = days.firstOrNull { it.dayNumber == selectedDayNumber } ?: days.first()
            matchingDay.stops
        } else if (currentActiveTrip != null) {
            listOf(
                ItineraryStop(
                    stopId = "stop_0",
                    stopName = currentActiveTrip.originName,
                    activityDescription = "Starting Location",
                    latitude = currentActiveTrip.startLatLng.latitude,
                    longitude = currentActiveTrip.startLatLng.longitude,
                    status = ItineraryStopStatus.PENDING,
                    orderIndex = 0
                )
            ) + currentActiveTrip.waypoints.mapIndexed { idx, wp ->
                val latLng = currentActiveTrip.waypointLatLngs.getOrNull(idx) ?: LatLng(17.1856 + idx * 0.1, 78.6473 + idx * 0.1)
                ItineraryStop(
                    stopId = "stop_${idx + 1}",
                    stopName = wp,
                    activityDescription = "Scheduled Milestone",
                    latitude = latLng.latitude,
                    longitude = latLng.longitude,
                    status = ItineraryStopStatus.PENDING,
                    orderIndex = idx + 1
                )
            } + listOf(
                ItineraryStop(
                    stopId = "stop_${currentActiveTrip.waypoints.size + 1}",
                    stopName = currentActiveTrip.destinationName,
                    activityDescription = "Destination",
                    latitude = currentActiveTrip.destLatLng.latitude,
                    longitude = currentActiveTrip.destLatLng.longitude,
                    status = ItineraryStopStatus.PENDING,
                    orderIndex = currentActiveTrip.waypoints.size + 1
                )
            )
        } else {
            emptyList()
        }
    }

    // Filter only PENDING stops to be displayed on the map
    val pendingStopsOnMap = remember(currentDayStops) {
        currentDayStops.filter { it.status == ItineraryStopStatus.PENDING }
    }

    // Live Phone GPS location
    val livePhonePing by LiveLocationEngine.liveLocationPing.collectAsState()
    val myPing = livePhonePing ?: riderLocations.values.firstOrNull()

    // Dynamic start point: starts from real current phone GPS location if available
    val dynamicStartPos = remember(myPing, routePolyline, currentActiveTrip) {
        if (myPing != null && myPing.latitude != 0.0 && myPing.longitude != 0.0) {
            LatLng(myPing.latitude, myPing.longitude)
        } else if (currentActiveTrip != null) {
            currentActiveTrip.startLatLng
        } else {
            routePolyline.firstOrNull() ?: LatLng(17.3753, 78.4344)
        }
    }

    val dynamicDestPos = remember(pendingStopsOnMap, routePolyline, currentActiveTrip) {
        val lastPending = pendingStopsOnMap.lastOrNull()
        if (lastPending != null && lastPending.latitude != 0.0) {
            LatLng(lastPending.latitude, lastPending.longitude)
        } else if (currentActiveTrip != null) {
            currentActiveTrip.destLatLng
        } else {
            routePolyline.lastOrNull() ?: LatLng(16.5772, 79.3125)
        }
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.Builder()
            .target(dynamicStartPos)
            .zoom(16f)
            .tilt(50f)
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

    // Dynamic Route Calculation from Current Phone GPS -> Remaining Pending Stops -> Destination
    LaunchedEffect(dynamicStartPos, dynamicDestPos, pendingStopsOnMap.size) {
        try {
            val intermediateWaypoints = pendingStopsOnMap
                .drop(1)
                .dropLast(1)
                .filter { it.latitude != 0.0 && it.longitude != 0.0 }
                .map { LatLng(it.latitude, it.longitude) }

            val details = withContext(Dispatchers.IO) {
                DirectionsRepository.getDirectionsRoute(
                    origin = dynamicStartPos,
                    destination = dynamicDestPos,
                    waypoints = intermediateWaypoints
                )
            }
            currentRouteDetails = details
            if (details.polylinePoints.isNotEmpty()) {
                activeDynamicPolyline = details.polylinePoints
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

    // Follow lead/phone rider location automatically
    LaunchedEffect(dynamicStartPos, isFollowMode, is3dTilt, isLiveNavigating) {
        if (isFollowMode || isLiveNavigating) {
            val tiltAngle = if (is3dTilt || isLiveNavigating) 55f else 0f
            val bearingAngle = myPing?.bearing ?: 0f
            val targetCam = CameraPosition.Builder()
                .target(dynamicStartPos)
                .zoom(if (isLiveNavigating) 17.5f else 16.5f)
                .tilt(tiltAngle)
                .bearing(bearingAngle)
                .build()
            cameraPositionState.animate(CameraUpdateFactory.newCameraPosition(targetCam), 800)
        }
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

            // Render Only Active PENDING Stops on the Map (Completed/Skipped stops are removed)
            pendingStopsOnMap.forEachIndexed { sIdx, stop ->
                if (stop.latitude != 0.0 && stop.longitude != 0.0) {
                    val isFirst = sIdx == 0
                    val isLast = sIdx == pendingStopsOnMap.size - 1

                    val pinHue = when {
                        isFirst -> BitmapDescriptorFactory.HUE_GREEN
                        isLast -> BitmapDescriptorFactory.HUE_RED
                        else -> BitmapDescriptorFactory.HUE_CYAN
                    }

                    Marker(
                        state = MarkerState(position = LatLng(stop.latitude, stop.longitude)),
                        title = "${stop.orderIndex + 1}. ${stop.stopName}",
                        snippet = stop.activityDescription.ifBlank { "Tap for stop actions" },
                        icon = BitmapDescriptorFactory.defaultMarker(pinHue),
                        onClick = {
                            selectedStopForModal = stop
                            true
                        }
                    )
                }
            }

            // Render 3D Rider Markers
            riderLocations.forEach { (userId, ping) ->
                val member = convoyMembers[userId]
                val status = member?.status ?: RiderStatus.RIDING

                InterpolatedRiderMarker3D(
                    targetLocation = LatLng(ping.latitude, ping.longitude),
                    bearing = ping.bearing,
                    displayName = member?.displayName ?: "Rider",
                    status = status,
                    photoUrl = member?.photoUrl ?: "",
                    vehicleModel = member?.vehicleModel ?: ""
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

        // Top Right: Floating Frosted HUD Map Controls
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 16.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
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
        }

        // Top Left: Status Indicators
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 16.dp, start = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // HUD Mode Indicator
            Box(
                modifier = Modifier
                    .frostedGlassHud(shape = RoundedCornerShape(20.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(
                                if (isFollowMode) HudColors.StatusRiding else HudColors.StatusStopped,
                                CircleShape
                            )
                            .border(1.dp, Color.White.copy(alpha = 0.6f), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isFollowMode) "HUD 3D: GPS LOCKED" else "HUD 3D: FREE PAN",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = HudColors.TextCrispWhite
                    )
                }
            }

            // Active Trip & Next Stop Quick Badge
            if (currentActiveTrip != null) {
                val nextStop = pendingStopsOnMap.firstOrNull()
                Box(
                    modifier = Modifier
                        .frostedGlassHud(shape = RoundedCornerShape(20.dp), backgroundColor = Color(0xDD0F172A))
                        .clickable { showStopsDrawer = true }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Place, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (nextStop != null) "Next Stop: ${nextStop.stopName}" else "Trip Complete 🎉",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            // Live Phone GPS Telemetry Indicator Badge
            if (myPing != null) {
                Box(
                    modifier = Modifier
                        .frostedGlassHud(
                            shape = RoundedCornerShape(20.dp),
                            backgroundColor = Color(0xDD0F172A)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Color(0xFF00E5FF), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "📡 GPS: ${"%.4f".format(myPing.latitude)}, ${"%.4f".format(myPing.longitude)} • ${myPing.speedKmh.toInt()} KM/H",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00E5FF)
                        )
                    }
                }
            }
        }

        // Left Center: Convoy Radar Overlay
        ConvoyRadarOverlay(
            myLocation = myPing?.let { LatLng(it.latitude, it.longitude) } ?: dynamicStartPos,
            myBearing = myPing?.bearing ?: 0f,
            riderLocations = riderLocations,
            convoyMembers = convoyMembers,
            onFocusRider = { targetPos ->
                coroutineScope.launch {
                    cameraPositionState.animate(
                        CameraUpdateFactory.newCameraPosition(
                            CameraPosition.Builder()
                                .target(targetPos)
                                .zoom(17f)
                                .tilt(if (is3dTilt) 50f else 0f)
                                .build()
                        ),
                        600
                    )
                }
            },
            onCallRider = { phone ->
                try {
                    val callIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                    context.startActivity(callIntent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            },
            modifier = Modifier.align(Alignment.TopStart)
        )

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

        // Google Maps Turn-by-Turn Live Navigation HUD Overlay
        if (isLiveNavigating) {
            GoogleMapsNavigationHUD(
                routeDetails = currentRouteDetails,
                currentSpeedKmh = myPing?.speedKmh?.toDouble() ?: 0.0,
                currentStepIndex = currentNavStepIndex,
                onStepSelected = { idx -> currentNavStepIndex = idx },
                onExitNavigation = { isLiveNavigating = false },
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
