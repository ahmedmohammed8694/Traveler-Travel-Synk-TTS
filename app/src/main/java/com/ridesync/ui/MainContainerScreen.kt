package com.ridesync.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.model.LatLng
import com.ridesync.data.model.*
import com.ridesync.data.remote.HybridFirebaseClient
import com.ridesync.data.repository.NotificationRepository
import com.ridesync.data.repository.TelemetryBufferRepository
import com.ridesync.engine.LiveLocationEngine
import com.ridesync.ui.home.HomeScreen
import com.ridesync.ui.hud.ConvoyAlertBanner
import com.ridesync.ui.map.LiveMapScreen
import com.ridesync.ui.notification.NotificationCenterDialog
import com.ridesync.ui.profile.ChatInboxDialog
import com.ridesync.ui.profile.DirectChatDialog
import com.ridesync.ui.profile.SocialTravelersListDialog
import com.ridesync.ui.profile.TravelerProfileDialog
import com.ridesync.ui.profile.UserProfileScreen
import com.ridesync.ui.qr.JoinTripScreen
import com.ridesync.ui.theme.HudColors
import com.ridesync.ui.theme.RideSyncTheme
import com.ridesync.ui.trip.SavedTripsHistoryScreen
import com.ridesync.ui.trip.TripConvoyChatDialog
import com.ridesync.ui.trip.TripCreationScreen
import com.ridesync.util.rememberRiderAvatarBitmap
import kotlinx.coroutines.launch

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
    // Bottom Menu Tabs: 0: Home, 1: Convoy Map, 2: Create Trip, 3: Trip History, 4: Chats, 5: Profile
    var selectedTab by remember { mutableIntStateOf(0) }
    var activeRole by remember { mutableStateOf(ConvoyRole.LEAD) }
    var activeTripId by remember { mutableStateOf<String?>(null) }

    // Dialog state controllers
    var showJoinTripDialog by remember { mutableStateOf(false) }
    var showNotificationCenterDialog by remember { mutableStateOf(false) }
    var showChatInboxDialog by remember { mutableStateOf(false) }
    var showSocialListDialog by remember { mutableStateOf(false) }
    var socialListInitialTab by remember { mutableIntStateOf(0) } // 0 = Friends, 3 = Search All
    var activeDirectChatUser by remember { mutableStateOf<UserProfile?>(null) }
    var activeTravelerProfileView by remember { mutableStateOf<UserProfile?>(null) }
    var activeConvoyChatTrip by remember { mutableStateOf<SavedTrip?>(null) }

    val activeTrip = remember(allSavedTrips, activeTripId) {
        allSavedTrips.firstOrNull { it.category == TripCategory.ONGOING }
            ?: allSavedTrips.firstOrNull { it.tripId == activeTripId }
            ?: allSavedTrips.firstOrNull()
    }

    // Real-time notification listener for current user
    LaunchedEffect(userProfile.userId) {
        if (userProfile.userId.isNotBlank()) {
            com.ridesync.data.repository.NotificationRepository.startListeningForUser(userProfile.userId)
        }
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
                    photoUrl = r.photoUrl,
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

        // Current phone user strictly uses local hardware GPS telemetry
        phoneLocationPing?.let { myPing ->
            map[userProfile.userId] = myPing
        }

        // For all other convoy members who haven't transmitted a live ping yet,
        // default their initial location to the trip starting point (never copying local user's moving GPS)
        val defaultStartLat = activeTrip?.startLatLng?.latitude ?: 17.3753
        val defaultStartLng = activeTrip?.startLatLng?.longitude ?: 78.4344

        for ((mId, member) in activeConvoyMembers) {
            if (mId != userProfile.userId && !map.containsKey(mId)) {
                map[mId] = RiderLocationPing(
                    latitude = defaultStartLat,
                    longitude = defaultStartLng,
                    speedKmh = 0f,
                    bearing = 0f,
                    timestamp = System.currentTimeMillis()
                )
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

                    NotificationRepository.addNotification(
                        AppNotification(
                            title = "🚀 New Member Joined Convoy",
                            message = "${newMember.displayName} joined the trip convoy!",
                            type = NotificationType.CONVOY_MEMBER_JOINED,
                            senderUserId = newId,
                            senderName = newMember.displayName,
                            senderPhotoUrl = newMember.photoUrl
                        )
                    )

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
                        // 0: HOME DASHBOARD
                        NavigationBarItem(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                            label = { Text("Home", fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = HudColors.CyanPrimary,
                                selectedTextColor = HudColors.CyanPrimary,
                                unselectedIconColor = HudColors.TextCoolSilver,
                                unselectedTextColor = HudColors.TextCoolSilver,
                                indicatorColor = HudColors.ObsidianElevated
                            )
                        )

                        // 1: CONVOY MAP
                        NavigationBarItem(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            icon = { Icon(Icons.Default.Map, contentDescription = "Convoy Map") },
                            label = { Text("Convoy Map", fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = HudColors.CyanPrimary,
                                selectedTextColor = HudColors.CyanPrimary,
                                unselectedIconColor = HudColors.TextCoolSilver,
                                unselectedTextColor = HudColors.TextCoolSilver,
                                indicatorColor = HudColors.ObsidianElevated
                            )
                        )

                        // 2: CREATE TRIP
                        NavigationBarItem(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            icon = { Icon(Icons.Default.Route, contentDescription = "Create Trip") },
                            label = { Text("Create Trip", fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = HudColors.CyanPrimary,
                                selectedTextColor = HudColors.CyanPrimary,
                                unselectedIconColor = HudColors.TextCoolSilver,
                                unselectedTextColor = HudColors.TextCoolSilver,
                                indicatorColor = HudColors.ObsidianElevated
                            )
                        )

                        // 3: TRIP HISTORY
                        NavigationBarItem(
                            selected = selectedTab == 3,
                            onClick = { selectedTab = 3 },
                            icon = { Icon(Icons.Default.History, contentDescription = "Trip History") },
                            label = { Text("History", fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = HudColors.CyanPrimary,
                                selectedTextColor = HudColors.CyanPrimary,
                                unselectedIconColor = HudColors.TextCoolSilver,
                                unselectedTextColor = HudColors.TextCoolSilver,
                                indicatorColor = HudColors.ObsidianElevated
                            )
                        )

                        // 4: CHATS INBOX
                        NavigationBarItem(
                            selected = selectedTab == 4,
                            onClick = {
                                selectedTab = 4
                                showChatInboxDialog = true
                            },
                            icon = { Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Chats") },
                            label = { Text("Chats", fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = HudColors.CyanPrimary,
                                selectedTextColor = HudColors.CyanPrimary,
                                unselectedIconColor = HudColors.TextCoolSilver,
                                unselectedTextColor = HudColors.TextCoolSilver,
                                indicatorColor = HudColors.ObsidianElevated
                            )
                        )

                        // 5: TRAVELER PROFILE
                        NavigationBarItem(
                            selected = selectedTab == 5,
                            onClick = { selectedTab = 5 },
                            icon = {
                                if (travelerAvatarBitmap != null) {
                                    Image(
                                        bitmap = travelerAvatarBitmap!!.asImageBitmap(),
                                        contentDescription = "Traveler Profile Photo",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(22.dp)
                                            .clip(CircleShape)
                                            .border(
                                                width = if (selectedTab == 5) 1.5.dp else 1.dp,
                                                color = if (selectedTab == 5) HudColors.CyanPrimary else HudColors.TextCoolSilver.copy(alpha = 0.5f),
                                                shape = CircleShape
                                            )
                                    )
                                } else {
                                    Icon(Icons.Default.Person, contentDescription = "Traveler Profile")
                                }
                            },
                            label = { Text("Profile", fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1) },
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
                        // Main Home Dashboard Screen
                        HomeScreen(
                            userProfile = userProfile,
                            activeTrip = activeTrip,
                            onNavigateToTab = { tabIdx -> selectedTab = tabIdx },
                            onOpenNotificationCenter = { showNotificationCenterDialog = true },
                            onOpenChatInbox = { showChatInboxDialog = true },
                            onOpenSocialList = { tabIdx ->
                                socialListInitialTab = tabIdx
                                showSocialListDialog = true
                            },
                            onOpenConvoyChat = { trip -> activeConvoyChatTrip = trip },
                            onOpenJoinTrip = { showJoinTripDialog = true }
                        )
                    }

                    1 -> {
                        // Convoy Map + Live Telemetry HUD Overlay
                        Box(modifier = Modifier.fillMaxSize()) {
                            LiveMapScreen(
                                routePolyline = activeRoutePolyline,
                                riderLocations = mergedLocations,
                                convoyMembers = activeConvoyMembers,
                                stopEvents = if (liveStops.isNotEmpty()) liveStops else stopEvents,
                                isOnline = isOnline,
                                activeTripId = activeTripId,
                                currentUserPhotoUrl = userProfile.photoUrl,
                                onToggleFullScreen = { isMapFullScreen = it },
                                onStopReported = { reason ->
                                    val myPing = phoneLocationPing
                                    val evt = StopEvent(
                                        stopId = "evt-${System.currentTimeMillis()}",
                                        riderId = userProfile.userId,
                                        riderName = userProfile.displayName.ifBlank { "Rider" },
                                        reason = reason,
                                        latitude = myPing?.latitude ?: 17.3753,
                                        longitude = myPing?.longitude ?: 78.4344,
                                        timestamp = System.currentTimeMillis()
                                    )
                                    stopEvents.add(evt)
                                    alertBannerText = "Stop Reported: ${reason.name} by ${userProfile.displayName.ifBlank { "Rider" }}"

                                    NotificationRepository.addNotification(
                                        AppNotification(
                                            title = "🛑 Convoy Stop Alert",
                                            message = "${userProfile.displayName} reported a ${reason.name} stop.",
                                            type = NotificationType.TRIP_UPDATE
                                        )
                                    )
                                },
                                onSosReported = {
                                    alertBannerText = "🚨 EMERGENCY SOS BROADCAST SENT BY ${userProfile.displayName.ifBlank { "Rider" }}!"
                                    NotificationRepository.addNotification(
                                        AppNotification(
                                            title = "🚨 EMERGENCY SOS BROADCAST",
                                            message = "EMERGENCY SOS broadcast sent by ${userProfile.displayName}!",
                                            type = NotificationType.SOS_ALERT
                                        )
                                    )
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

                    2 -> {
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
                                selectedTab = 1 // Switch to Convoy Map
                            },
                            onShareLobbyClick = { code ->
                                // Trigger Join Screen / Share
                            }
                        )
                    }

                    3 -> {
                        // Saved Trips History Screen
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
                                selectedTab = 1 // Switch to Convoy Map
                            },
                            onShareLobbyClick = { code -> },
                            onOpenJoinTrip = { showJoinTripDialog = true }
                        )
                    }

                    4 -> {
                        // Chats Inbox Screen / Direct Messages List
                        ChatInboxDialog(
                            currentUserProfile = userProfile,
                            onOpenChat = { targetUser -> activeDirectChatUser = targetUser },
                            onSearchTravelers = {
                                socialListInitialTab = 3
                                showSocialListDialog = true
                            },
                            onDismiss = { selectedTab = 0 }
                        )
                    }

                    5 -> {
                        // User Profile Dashboard
                        UserProfileScreen(
                            userProfile = userProfile,
                            onSaveProfile = onSaveUserProfile,
                            onSignOut = onSignOut
                        )
                    }
                }

                // DIALOG OVERLAYS
                if (showNotificationCenterDialog) {
                    NotificationCenterDialog(
                        currentUserProfile = userProfile,
                        onSaveCurrentUserProfile = onSaveUserProfile,
                        onOpenTravelerProfileById = { senderId ->
                            coroutineScope.launch {
                                val loaded = com.ridesync.data.repository.SocialRepository.fetchProfilesByIds(listOf(senderId))
                                val target = loaded.firstOrNull()
                                    ?: com.ridesync.data.repository.SocialRepository.knownTravelers.value.firstOrNull { it.userId == senderId }
                                if (target != null) {
                                    activeTravelerProfileView = target
                                } else {
                                    socialListInitialTab = 0
                                    showSocialListDialog = true
                                }
                            }
                        },
                        onOpenSocialTab = { tab ->
                            socialListInitialTab = tab
                            showSocialListDialog = true
                        },
                        onDismiss = { showNotificationCenterDialog = false }
                    )
                }

                if (showChatInboxDialog && selectedTab != 4) {
                    ChatInboxDialog(
                        currentUserProfile = userProfile,
                        onOpenChat = { targetUser ->
                            showChatInboxDialog = false
                            activeDirectChatUser = targetUser
                        },
                        onSearchTravelers = {
                            showChatInboxDialog = false
                            socialListInitialTab = 3
                            showSocialListDialog = true
                        },
                        onDismiss = { showChatInboxDialog = false }
                    )
                }

                if (showSocialListDialog) {
                    SocialTravelersListDialog(
                        initialTab = socialListInitialTab,
                        currentUserProfile = userProfile,
                        onSaveCurrentUserProfile = onSaveUserProfile,
                        onSelectTraveler = { targetUser ->
                            showSocialListDialog = false
                            activeTravelerProfileView = targetUser
                        },
                        onOpenChat = { targetUser ->
                            showSocialListDialog = false
                            activeDirectChatUser = targetUser
                        },
                        onDismiss = { showSocialListDialog = false }
                    )
                }

                if (activeDirectChatUser != null) {
                    DirectChatDialog(
                        currentUserProfile = userProfile,
                        targetTraveler = activeDirectChatUser!!,
                        onDismiss = { activeDirectChatUser = null }
                    )
                }

                if (activeTravelerProfileView != null) {
                    TravelerProfileDialog(
                        traveler = activeTravelerProfileView!!,
                        currentUserProfile = userProfile,
                        onSaveCurrentUserProfile = onSaveUserProfile,
                        onOpenChat = { targetUser ->
                            activeTravelerProfileView = null
                            activeDirectChatUser = targetUser
                        },
                        onDismiss = { activeTravelerProfileView = null }
                    )
                }

                if (activeConvoyChatTrip != null) {
                    TripConvoyChatDialog(
                        trip = activeConvoyChatTrip!!,
                        currentUserProfile = userProfile,
                        onDismiss = { activeConvoyChatTrip = null }
                    )
                }

                if (showJoinTripDialog) {
                    androidx.compose.ui.window.Dialog(
                        onDismissRequest = { showJoinTripDialog = false },
                        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
                    ) {
                        JoinTripScreen(
                            userProfile = userProfile,
                            onTripJoined = { joinedTrip ->
                                showJoinTripDialog = false
                                activeTripId = joinedTrip.tripId
                                selectedTab = 1 // Switch to Convoy Map
                                android.widget.Toast.makeText(context, "🎉 Successfully joined ${joinedTrip.title}!", android.widget.Toast.LENGTH_LONG).show()
                            },
                            onCancel = { showJoinTripDialog = false }
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
