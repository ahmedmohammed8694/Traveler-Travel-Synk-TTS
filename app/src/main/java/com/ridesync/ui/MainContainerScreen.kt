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
import com.ridesync.engine.LiveLocationEngine
import com.ridesync.ui.hud.ConvoyAlertBanner
import com.ridesync.ui.hud.ConvoyStatusBottomSheet
import com.ridesync.ui.hud.GloveFriendlyActionPad
import com.ridesync.ui.map.LiveMapScreen
import com.ridesync.ui.profile.UserProfileScreen
import com.ridesync.ui.qr.QrCodeScannerScreen
import com.ridesync.ui.theme.HudColors
import com.ridesync.ui.theme.RideSyncTheme
import com.ridesync.ui.trip.TripCreationScreen

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

    LaunchedEffect(Unit) {
        if (!LiveLocationEngine.hasLocationPermission(context)) {
            locationPermissionLauncher.launch(
                arrayOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else {
            LiveLocationEngine.startLiveLocationUpdates(context)
        }
    }

    val isOnline by telemetryRepository.isOnline.collectAsState()
    val liveTelemetry by firebaseClient.observeLiveConvoyTelemetry("active_trip_101").collectAsState(initial = emptyMap<String, RiderLocationPing>())
    val liveStops by firebaseClient.observeStopEvents("active_trip_101").collectAsState(initial = emptyList<StopEvent>())

    var selectedTab by remember { mutableIntStateOf(0) }
    var activeRole by remember { mutableStateOf(ConvoyRole.LEAD) }

    // Active Real Google Maps Road Polyline State
    var activeRoutePolyline by remember { mutableStateOf<List<LatLng>>(emptyList()) }

    LaunchedEffect(Unit) {
        val defaultRoute = com.ridesync.data.repository.DirectionsRepository.getDirectionsRoute(
            origin = LatLng(17.3753, 78.4344), // Attapur, Hyderabad
            destination = LatLng(16.5772, 79.3125) // Nagarjuna Sagar Dam
        )
        activeRoutePolyline = defaultRoute.polylinePoints
    }

    // Buffer real mobile phone GPS telemetry automatically
    LaunchedEffect(phoneLocationPing) {
        phoneLocationPing?.let { ping ->
            telemetryRepository.processIncomingPing(
                tripId = "active_trip_101",
                userId = userProfile.userId,
                ping = ping
            )
        }
    }

    val mergedLocations = remember(liveTelemetry, userProfile, phoneLocationPing) {
        val map = liveTelemetry.toMutableMap()
        val myPing = phoneLocationPing ?: RiderLocationPing(
            latitude = 17.3753,
            longitude = 78.4344,
            speedKmh = 0f,
            bearing = 0f,
            timestamp = System.currentTimeMillis()
        )
        map[userProfile.userId] = myPing
        map
    }

    val mockMembers = remember(userProfile, activeRole, phoneLocationPing) {
        mapOf(
            userProfile.userId to ConvoyMember(
                userId = userProfile.userId,
                displayName = userProfile.displayName.ifBlank { "Rider (You)" },
                photoUrl = userProfile.photoUrl,
                vehicleModel = userProfile.vehicleModel,
                role = activeRole,
                status = if ((phoneLocationPing?.speedKmh ?: 0f) > 3f) RiderStatus.RIDING else RiderStatus.STOPPED,
                batteryPercent = 92,
                lastSeenTimestamp = System.currentTimeMillis()
            )
        )
    }

    val stopEvents = remember { mutableStateListOf<StopEvent>() }
    var alertBannerText by remember { mutableStateOf<String?>(null) }

    RideSyncTheme {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = HudColors.ObsidianCanvas,
            bottomBar = {
                NavigationBar(
                    containerColor = HudColors.ObsidianSurface,
                    contentColor = HudColors.TextCrispWhite
                ) {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Default.Map, contentDescription = "Convoy Map") },
                        label = { Text("Convoy Map", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
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
                        icon = { Icon(Icons.Default.Route, contentDescription = "Trip Planner") },
                        label = { Text("Trip Planner", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
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
                        icon = { Icon(Icons.Default.QrCodeScanner, contentDescription = "QR Scanner") },
                        label = { Text("Join Lobby", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
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
                        icon = { Icon(Icons.Default.Person, contentDescription = "Rider Profile") },
                        label = { Text("Rider Profile", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
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
        ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> {
                    // Convoy Map + Live Telemetry HUD Overlay
                    Box(modifier = Modifier.fillMaxSize()) {
                        LiveMapScreen(
                            routePolyline = activeRoutePolyline,
                            riderLocations = mergedLocations,
                            convoyMembers = mockMembers,
                            stopEvents = if (liveStops.isNotEmpty()) liveStops else stopEvents,
                            isOnline = isOnline
                        )

                        // Top Convoy Alert Banner
                        val banner = alertBannerText?.let { text ->
                            AlertBanner(
                                title = "Convoy Broadcast",
                                message = text,
                                severity = if (text.contains("SOS", ignoreCase = true)) AlertSeverity.CRITICAL else AlertSeverity.WARNING
                            )
                        }
                        ConvoyAlertBanner(
                            banner = banner,
                            onDismiss = { alertBannerText = null },
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(16.dp)
                        )

                        // Glove-Friendly Action Pad (Bottom Controls)
                        GloveFriendlyActionPad(
                            onStopReported = { reason ->
                                stopEvents.add(
                                    StopEvent(
                                        stopId = "evt-${System.currentTimeMillis()}",
                                        riderId = userProfile.userId,
                                        riderName = userProfile.displayName.ifBlank { "Rider" },
                                        reason = reason,
                                        latitude = 17.3753,
                                        longitude = 78.4344,
                                        timestamp = System.currentTimeMillis()
                                    )
                                )
                                alertBannerText = "Stop Reported: ${reason.name} by ${userProfile.displayName}"
                            },
                            onSosReported = {
                                alertBannerText = "🚨 EMERGENCY SOS BROADCAST SENT BY ${userProfile.displayName}!"
                            },
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 20.dp, start = 16.dp, end = 16.dp)
                        )
                    }
                }

                1 -> {
                    // Trip & Route Planner
                    TripCreationScreen(
                        userProfile = userProfile,
                        onStartTripClick = { title, role, origin, dest, waypoints, routePolyline ->
                            activeRole = role
                            if (routePolyline.isNotEmpty()) {
                                activeRoutePolyline = routePolyline
                            }
                            alertBannerText = "Started Trip: $title as ${role.name}!"
                            selectedTab = 0 // Switch to Convoy Map
                        },
                        onShareLobbyClick = { code ->
                            selectedTab = 2 // Switch to QR tab
                        }
                    )
                }

                2 -> {
                    // QR Code Scanner / Join Lobby
                    QrCodeScannerScreen(
                        onQrCodeScanned = { code ->
                            alertBannerText = "Joined Convoy Lobby: $code"
                            selectedTab = 0 // Switch to Live Map
                        },
                        onCancel = {
                            selectedTab = 0
                        }
                    )
                }

                3 -> {
                    // User Profile Dashboard
                    UserProfileScreen(
                        userProfile = userProfile,
                        onSaveProfile = onSaveUserProfile,
                        onSignOut = onSignOut
                    )
                }
            }
        }
    }
}
}
