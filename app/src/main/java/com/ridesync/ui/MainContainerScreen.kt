package com.ridesync.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.model.LatLng
import com.ridesync.data.model.*
import com.ridesync.data.remote.HybridFirebaseClient
import com.ridesync.data.repository.TelemetryBufferRepository
import kotlinx.coroutines.launch
import com.ridesync.engine.LiveLocationEngine
import com.ridesync.ui.hud.ConvoyAlertBanner
import com.ridesync.ui.hud.ConvoyStatusBottomSheet
import com.ridesync.ui.hud.GloveFriendlyActionPad
import com.ridesync.ui.map.LiveMapScreen
import com.ridesync.ui.profile.UserProfileScreen
import com.ridesync.ui.qr.JoinTripScreen
import com.ridesync.ui.qr.QrCodeScannerScreen
import com.ridesync.ui.theme.HudColors
import com.ridesync.ui.theme.RideSyncTheme
import com.ridesync.ui.trip.TripCreationScreen

import androidx.compose.material.icons.filled.History
import com.ridesync.ui.trip.SavedTripsHistoryScreen
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.ridesync.util.rememberRiderAvatarBitmap

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainContainerScreen(
    userProfile: UserProfile,
    onSaveUserProfile: (UserProfile) -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val telemetryRepository = remember(context) { TelemetryBufferRepository(context) }
    val firebaseClient = remember { HybridFirebaseClient() }
    val travelerAvatarBitmap by rememberRiderAvatarBitmap(userProfile.photoUrl)

    // Live Phone Mobile GPS Tracking Engine
    val phoneLocationPing by LiveLocationEngine.liveLocationPing.collectAsState()

    // Runtime Permission Launcher for Real Phone GPS
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            LiveLocationEngine.startLiveLocationUpdates(context)
        }
    }

    // First-Time App Install & Privacy Policy Permissions Onboarding State
    var showPermissionsOnboarding by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences("ridesync_app_prefs", android.content.Context.MODE_PRIVATE)
        val hasCompletedOnboarding = prefs.getBoolean("has_completed_permissions_onboarding", false)
        val hasLocation = androidx.core.content.ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val hasCamera = androidx.core.content.ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.CAMERA
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (!hasCompletedOnboarding || !hasLocation || !hasCamera) {
            showPermissionsOnboarding = true
        } else {
            LiveLocationEngine.startLiveLocationUpdates(context)
        }
    }

    val isOnline by telemetryRepository.isOnline.collectAsState()
    val allSavedTrips by com.ridesync.data.repository.TripRepository.tripsFlow.collectAsState()

    val coroutineScope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) }
    var activeRole by remember { mutableStateOf(ConvoyRole.LEAD) }
    var activeTripId by remember { mutableStateOf<String?>(null) }

    val activeTrip = remember(allSavedTrips, activeTripId) {
        allSavedTrips.firstOrNull { it.category == TripCategory.ONGOING }
            ?: allSavedTrips.firstOrNull { it.tripId == activeTripId }
            ?: allSavedTrips.firstOrNull()
    }

    // Real-time background poller for online trips and joined rider roster updates
    LaunchedEffect(activeTrip?.tripId) {
        while (true) {
            com.ridesync.data.repository.TripRepository.fetchOnlineTripsAsync()
            activeTrip?.tripId?.let { tid ->
                com.ridesync.data.repository.TripRepository.fetchTripDetailsOnlineAsync(tid)
            }
            kotlinx.coroutines.delay(3000)
        }
    }

    val liveTelemetryChannel = activeTrip?.tripId ?: "active_trip_101"
    val liveTelemetry by firebaseClient.observeLiveConvoyTelemetry(liveTelemetryChannel).collectAsState(initial = emptyMap<String, RiderLocationPing>())
    val liveStops by firebaseClient.observeStopEvents(liveTelemetryChannel).collectAsState(initial = emptyList<StopEvent>())

    // Active Real Google Maps Road Polyline State
    var activeRoutePolyline by remember { mutableStateOf<List<LatLng>>(emptyList()) }

    // Buffer real mobile phone GPS telemetry automatically
    LaunchedEffect(phoneLocationPing, activeTrip?.tripId) {
        phoneLocationPing?.let { ping ->
            telemetryRepository.processIncomingPing(
                tripId = activeTrip?.tripId ?: "active_trip_101",
                userId = userProfile.userId,
                ping = ping
            )
        }
    }

    val activeConvoyMembers = remember(userProfile, activeRole, phoneLocationPing, allSavedTrips, activeTrip) {
        val map = mutableMapOf<String, ConvoyMember>()
        map[userProfile.userId] = ConvoyMember(
            userId = userProfile.userId,
            displayName = userProfile.displayName.ifBlank { "Rider (You)" },
            photoUrl = userProfile.photoUrl,
            vehicleModel = userProfile.displayVehicleModel,
            role = activeRole,
            status = if ((phoneLocationPing?.speedKmh ?: 0f) > 3f) RiderStatus.RIDING else RiderStatus.STOPPED,
            batteryPercent = 100,
            lastSeenTimestamp = System.currentTimeMillis()
        )

        val joinedList = activeTrip?.joinedRiders ?: emptyList()
        for (r in joinedList) {
            if (r.riderId.isNotBlank() && r.riderId != userProfile.userId) {
                map[r.riderId] = ConvoyMember(
                    userId = r.riderId,
                    displayName = r.displayName.ifBlank { "Rider" },
                    photoUrl = "",
                    vehicleModel = r.bikeModel.ifBlank { "Motorcycle" },
                    role = r.role,
                    status = if (r.status.contains("Riding", ignoreCase = true)) RiderStatus.RIDING else RiderStatus.STOPPED,
                    batteryPercent = 100,
                    lastSeenTimestamp = System.currentTimeMillis()
                )
            }
        }
        map
    }

    val mergedLocations = remember(liveTelemetry, userProfile, phoneLocationPing, activeConvoyMembers, activeTrip) {
        val map = liveTelemetry.toMutableMap()
        val myPing = phoneLocationPing ?: RiderLocationPing(
            latitude = activeTrip?.startLatLng?.latitude ?: 17.3753,
            longitude = activeTrip?.startLatLng?.longitude ?: 78.4344,
            speedKmh = 0f,
            bearing = 0f,
            timestamp = System.currentTimeMillis()
        )
        map[userProfile.userId] = myPing

        val baseLat = myPing.latitude
        val baseLng = myPing.longitude
        var offsetIdx = 1
        for ((mId, member) in activeConvoyMembers) {
            if (!map.containsKey(mId) && mId != userProfile.userId) {
                map[mId] = RiderLocationPing(
                    latitude = baseLat + (offsetIdx * 0.0004),
                    longitude = baseLng + (offsetIdx * 0.0004),
                    speedKmh = 0f,
                    bearing = 0f,
                    timestamp = System.currentTimeMillis()
                )
                offsetIdx++
            }
        }
        map
    }

    val stopEvents = remember { mutableStateListOf<StopEvent>() }
    var alertBannerText by remember { mutableStateOf<String?>(null) }
    var isMapFullScreen by remember { mutableStateOf(false) }

    // Real-Time Member Join Notifications
    var previousRiderIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    LaunchedEffect(activeConvoyMembers.keys) {
        val currentIds = activeConvoyMembers.keys.toSet()
        if (previousRiderIds.isNotEmpty()) {
            val newlyJoined = currentIds - previousRiderIds
            for (newId in newlyJoined) {
                val newMember = activeConvoyMembers[newId]
                if (newMember != null && newId != userProfile.userId) {
                    alertBannerText = "🎉 New Convoy Member Joined: ${newMember.displayName} (${newMember.vehicleModel})!"
                    android.widget.Toast.makeText(
                        context,
                        "🚀 ${newMember.displayName} joined the trip! Total Members: ${activeConvoyMembers.size}",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
        previousRiderIds = currentIds
    }

    RideSyncTheme {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = HudColors.ObsidianCanvas,
            bottomBar = {
                if (!isMapFullScreen) {
                    NavigationBar(
                        containerColor = HudColors.ObsidianSurface,
                        contentColor = HudColors.TextCrispWhite
                    ) {
                        NavigationBarItem(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            icon = { Icon(Icons.Default.Map, contentDescription = "Convoy Map") },
                            label = { Text("Convoy Map", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = HudColors.CyanPrimary,
                                selectedTextColor = HudColors.CyanPrimary,
                                unselectedIconColor = HudColors.TextCoolSilver,
                                unselectedTextColor = HudColors.TextCoolSilver,
                                indicatorColor = HudColors.ObsidianElevated
                            )
                        )

                        NavigationBarItem(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            icon = { Icon(Icons.Default.Route, contentDescription = "Create Trip") },
                            label = { Text("Create Trip", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = HudColors.CyanPrimary,
                                selectedTextColor = HudColors.CyanPrimary,
                                unselectedIconColor = HudColors.TextCoolSilver,
                                unselectedTextColor = HudColors.TextCoolSilver,
                                indicatorColor = HudColors.ObsidianElevated
                            )
                        )

                        NavigationBarItem(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            icon = { Icon(Icons.Default.History, contentDescription = "Trip History") },
                            label = { Text("Trip History", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = HudColors.CyanPrimary,
                                selectedTextColor = HudColors.CyanPrimary,
                                unselectedIconColor = HudColors.TextCoolSilver,
                                unselectedTextColor = HudColors.TextCoolSilver,
                                indicatorColor = HudColors.ObsidianElevated
                            )
                        )

                        NavigationBarItem(
                            selected = selectedTab == 3,
                            onClick = { selectedTab = 3 },
                            icon = { Icon(Icons.Default.QrCodeScanner, contentDescription = "Join Trip") },
                            label = { Text("Join Trip", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = HudColors.CyanPrimary,
                                selectedTextColor = HudColors.CyanPrimary,
                                unselectedIconColor = HudColors.TextCoolSilver,
                                unselectedTextColor = HudColors.TextCoolSilver,
                                indicatorColor = HudColors.ObsidianElevated
                            )
                        )

                        NavigationBarItem(
                            selected = selectedTab == 4,
                            onClick = { selectedTab = 4 },
                            icon = {
                                if (travelerAvatarBitmap != null) {
                                    Image(
                                        bitmap = travelerAvatarBitmap!!.asImageBitmap(),
                                        contentDescription = "Traveler Profile Photo",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .border(
                                                width = if (selectedTab == 4) 1.5.dp else 1.dp,
                                                color = if (selectedTab == 4) HudColors.CyanPrimary else HudColors.TextCoolSilver.copy(alpha = 0.5f),
                                                shape = CircleShape
                                            )
                                    )
                                } else {
                                    Icon(Icons.Default.Person, contentDescription = "Traveler Profile")
                                }
                            },
                            label = { Text("Traveler Profile", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = HudColors.CyanPrimary,
                                selectedTextColor = HudColors.CyanPrimary,
                                unselectedIconColor = HudColors.TextCoolSilver,
                                unselectedTextColor = HudColors.TextCoolSilver,
                                indicatorColor = HudColors.ObsidianElevated
                            )
                        )
                    }
                }
            }
        ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isMapFullScreen) PaddingValues(0.dp) else innerPadding)
        ) {
            when (selectedTab) {
                0 -> {
                    // Convoy Map + Live Telemetry HUD Overlay
                    Box(modifier = Modifier.fillMaxSize()) {
                        LiveMapScreen(
                            routePolyline = activeRoutePolyline,
                            riderLocations = mergedLocations,
                            convoyMembers = activeConvoyMembers,
                            stopEvents = if (liveStops.isNotEmpty()) liveStops else stopEvents,
                            isOnline = isOnline,
                            activeTripId = activeTripId,
                            onToggleFullScreen = { isMapFullScreen = it },
                            onStopReported = { reason ->
                                val myPing = phoneLocationPing
                                stopEvents.add(
                                    StopEvent(
                                        stopId = "evt-${System.currentTimeMillis()}",
                                        riderId = userProfile.userId,
                                        riderName = userProfile.displayName.ifBlank { "Rider" },
                                        reason = reason,
                                        latitude = myPing?.latitude ?: 17.3753,
                                        longitude = myPing?.longitude ?: 78.4344,
                                        timestamp = System.currentTimeMillis()
                                    )
                                )
                                alertBannerText = "Stop Reported: ${reason.name} by ${userProfile.displayName.ifBlank { "Rider" }}"
                            },
                            onSosReported = {
                                alertBannerText = "🚨 EMERGENCY SOS BROADCAST SENT BY ${userProfile.displayName.ifBlank { "Rider" }}!"
                            }
                        )

                        if (!isMapFullScreen && alertBannerText != null) {
                            val banner = AlertBanner(
                                title = "Convoy Broadcast",
                                message = alertBannerText!!,
                                severity = if (alertBannerText!!.contains("SOS", ignoreCase = true)) AlertSeverity.CRITICAL else AlertSeverity.WARNING
                            )
                            ConvoyAlertBanner(
                                banner = banner,
                                onDismiss = { alertBannerText = null },
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(16.dp)
                            )
                        }
                    }
                }

                1 -> {
                    // Create Trip & Route Builder Page
                    TripCreationScreen(
                        userProfile = userProfile,
                        initialTab = 0,
                        onStartTripClick = { title, role, origin, dest, waypoints, routePolyline ->
                            activeRole = role
                            stopEvents.clear()

                            val originPt = com.ridesync.data.repository.DirectionsRepository.resolveLocationNameToLatLng(origin, routePolyline.firstOrNull())
                            val destPt = com.ridesync.data.repository.DirectionsRepository.resolveLocationNameToLatLng(dest, routePolyline.lastOrNull())
                            val waypointPts = waypoints.map { wpName -> com.ridesync.data.repository.DirectionsRepository.resolveLocationNameToLatLng(wpName) }

                            // Create stop markers with actual Stop Names along the specific route
                            waypoints.forEachIndexed { idx, wpName ->
                                val stopLatLng = waypointPts.getOrNull(idx) ?: if (routePolyline.size > 2) {
                                    val targetIndex = (routePolyline.size * (idx + 1) / (waypoints.size + 1)).coerceIn(0, routePolyline.lastIndex)
                                    routePolyline[targetIndex]
                                } else {
                                    LatLng(
                                        originPt.latitude + (destPt.latitude - originPt.latitude) * (idx + 1) / (waypoints.size + 1),
                                        originPt.longitude + (destPt.longitude - originPt.longitude) * (idx + 1) / (waypoints.size + 1)
                                    )
                                }
                                stopEvents.add(
                                    StopEvent(
                                        stopId = "stop_evt_$idx",
                                        riderId = userProfile.userId,
                                        riderName = wpName,
                                        reason = StopReason.REST,
                                        latitude = stopLatLng.latitude,
                                        longitude = stopLatLng.longitude,
                                        timestamp = System.currentTimeMillis()
                                    )
                                )
                            }

                            coroutineScope.launch {
                                val realRoute = com.ridesync.data.repository.DirectionsRepository.getDirectionsRoute(originPt, destPt, waypointPts)
                                if (realRoute.polylinePoints.isNotEmpty()) {
                                    activeRoutePolyline = realRoute.polylinePoints
                                } else if (routePolyline.size > 2) {
                                    activeRoutePolyline = routePolyline
                                } else {
                                    activeRoutePolyline = listOf(originPt, destPt)
                                }
                            }
                            alertBannerText = "Started Trip: $title as ${role.name}!"
                            selectedTab = 0 // Switch to Convoy Map
                        },
                        onShareLobbyClick = { code ->
                            selectedTab = 3 // Switch to Join Trip tab
                        }
                    )
                }

                2 -> {
                    // Trip History & Saved Rides Page
                    SavedTripsHistoryScreen(
                        userProfile = userProfile,
                        onStartTripClick = { title, role, origin, dest, waypoints, routePolyline ->
                            activeRole = role
                            stopEvents.clear()

                            val originPt = com.ridesync.data.repository.DirectionsRepository.resolveLocationNameToLatLng(origin, routePolyline.firstOrNull())
                            val destPt = com.ridesync.data.repository.DirectionsRepository.resolveLocationNameToLatLng(dest, routePolyline.lastOrNull())
                            val waypointPts = waypoints.map { wpName -> com.ridesync.data.repository.DirectionsRepository.resolveLocationNameToLatLng(wpName) }

                            waypoints.forEachIndexed { idx, wpName ->
                                val stopLatLng = waypointPts.getOrNull(idx) ?: if (routePolyline.size > 2) {
                                    val targetIndex = (routePolyline.size * (idx + 1) / (waypoints.size + 1)).coerceIn(0, routePolyline.lastIndex)
                                    routePolyline[targetIndex]
                                } else {
                                    LatLng(
                                        originPt.latitude + (destPt.latitude - originPt.latitude) * (idx + 1) / (waypoints.size + 1),
                                        originPt.longitude + (destPt.longitude - originPt.longitude) * (idx + 1) / (waypoints.size + 1)
                                    )
                                }
                                stopEvents.add(
                                    StopEvent(
                                        stopId = "stop_evt_$idx",
                                        riderId = userProfile.userId,
                                        riderName = wpName,
                                        reason = StopReason.REST,
                                        latitude = stopLatLng.latitude,
                                        longitude = stopLatLng.longitude,
                                        timestamp = System.currentTimeMillis()
                                    )
                                )
                            }

                            coroutineScope.launch {
                                val realRoute = com.ridesync.data.repository.DirectionsRepository.getDirectionsRoute(originPt, destPt, waypointPts)
                                if (realRoute.polylinePoints.isNotEmpty()) {
                                    activeRoutePolyline = realRoute.polylinePoints
                                } else if (routePolyline.size > 2) {
                                    activeRoutePolyline = routePolyline
                                } else {
                                    activeRoutePolyline = listOf(originPt, destPt)
                                }
                            }
                            alertBannerText = "Started Trip: $title as ${role.name}!"
                            selectedTab = 0 // Switch to Convoy Map
                        },
                        onShareLobbyClick = { code ->
                            selectedTab = 3 // Switch to Join Trip tab
                        }
                    )
                }

                3 -> {
                    // Join Trip (QR Scanner / Trip Code / Join Link)
                    JoinTripScreen(
                        userProfile = userProfile,
                        onTripJoined = { joinedTrip ->
                            alertBannerText = "Joined Trip: ${joinedTrip.title}"
                            selectedTab = 2 // Switch to Trip History tab to show the joined trip!
                        },
                        onCancel = {
                            selectedTab = 0
                        }
                    )
                }

                4 -> {
                    // User Profile Dashboard
                    UserProfileScreen(
                        userProfile = userProfile,
                        onSaveProfile = onSaveUserProfile,
                        onSignOut = onSignOut
                    )
                }
            }

            if (showPermissionsOnboarding) {
                com.ridesync.ui.permissions.PermissionsOnboardingDialog(
                    onAllGranted = {
                        val prefs = context.getSharedPreferences("ridesync_app_prefs", android.content.Context.MODE_PRIVATE)
                        prefs.edit().putBoolean("has_completed_permissions_onboarding", true).apply()
                        showPermissionsOnboarding = false
                        LiveLocationEngine.startLiveLocationUpdates(context)
                    },
                    onDismiss = {
                        showPermissionsOnboarding = false
                    }
                )
            }
        }
    }
}
}
