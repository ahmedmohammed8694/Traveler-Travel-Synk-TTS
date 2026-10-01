package com.ridesync.ui.hud

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.model.LatLng
import com.ridesync.data.repository.GoogleMapsNavigationHelper
import com.ridesync.data.repository.RouteDetails
import com.ridesync.data.repository.RouteStep
import com.ridesync.ui.theme.HudColors
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Modern Google Maps Style Live Turn-by-Turn Navigation HUD.
 * Overlays on the Map screen providing:
 * 1. Top Green/Dark Turn Maneuver Banner (Next turn arrow, distance to turn, street name, next step teaser).
 * 2. Bottom Navigation Control Bar (ETA, remaining duration in green, remaining distance, speed, steps button, exit).
 * 3. Step-by-Step Directions Modal & Direct Google Maps Launch Options.
 */
@Composable
fun GoogleMapsNavigationHUD(
    routeDetails: RouteDetails?,
    currentSpeedKmh: Double,
    currentStepIndex: Int,
    onStepSelected: (Int) -> Unit,
    onExitNavigation: () -> Unit,
    onOpenGoogleMapsApp: () -> Unit,
    onOpenGoogleMapsWeb: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    var showStepsModal by remember { mutableStateOf(false) }

    val steps = routeDetails?.steps ?: emptyList()
    val currentStep = steps.getOrNull(currentStepIndex) ?: steps.firstOrNull() ?: RouteStep(
        instruction = "Continue onto route corridor",
        distanceText = "${"%.1f".format(routeDetails?.distanceKm ?: 0.0)} km",
        durationText = "${routeDetails?.durationMinutes ?: 0} min",
        maneuver = "straight",
        roadName = routeDetails?.summary ?: "Main Route"
    )
    val nextStep = steps.getOrNull(currentStepIndex + 1)

    // Calculate ETA clock time from now + duration
    val etaString = remember(routeDetails?.durationMinutes) {
        val durMin = routeDetails?.durationMinutes ?: 0
        val etaMillis = System.currentTimeMillis() + (durMin * 60 * 1000L)
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(etaMillis))
    }

    var bannerOffsetX by remember { mutableFloatStateOf(0f) }
    var bannerOffsetY by remember { mutableFloatStateOf(0f) }
    var bottomOffsetX by remember { mutableFloatStateOf(0f) }
    var bottomOffsetY by remember { mutableFloatStateOf(0f) }
    var isBannerMinimized by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        // ==========================================
        // 1. TOP TURN-BY-TURN MANEUVER BANNER (DRAGGABLE & MINIMIZABLE)
        // ==========================================
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset { IntOffset(bannerOffsetX.roundToInt(), bannerOffsetY.roundToInt()) }
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        bannerOffsetX += dragAmount.x
                        bannerOffsetY += dragAmount.y
                    }
                }
                .padding(top = 12.dp, start = 12.dp, end = 12.dp)
        ) {
            if (isBannerMinimized) {
                // Compact Minimized Pill (Draggable & Expandable - 100% Transparent No Border)
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color.Transparent,
                    shadowElevation = 0.dp,
                    modifier = Modifier.clickable { isBannerMinimized = false }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(28.dp)
                                .background(Brush.radialGradient(listOf(Color(0xFF0052CC), Color(0xFF003399))), CircleShape)
                        ) {
                            Icon(
                                imageVector = getManeuverIcon(currentStep.maneuver),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "In ${currentStep.distanceText}",
                                color = Color(0xFF0052CC),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = currentStep.roadName.ifBlank { currentStep.instruction },
                                color = Color(0xFF0F172A),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.OpenInFull,
                            contentDescription = "Expand Navigation HUD",
                            tint = Color(0xFF0052CC),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            } else {
                // Full Compact Expanded Navigation Banner (100% Transparent No Border)
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color.Transparent,
                    shadowElevation = 0.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Turn Direction Arrow Puck
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(
                                        Brush.radialGradient(listOf(Color(0xFF0052CC), Color(0xFF003399))),
                                        CircleShape
                                    )
                                    .border(1.5.dp, Color.White, CircleShape)
                                    .clickable { showStepsModal = true }
                            ) {
                                Icon(
                                    imageVector = getManeuverIcon(currentStep.maneuver),
                                    contentDescription = "Maneuver",
                                    tint = Color.White,
                                    modifier = Modifier.size(26.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { showStepsModal = true }
                            ) {
                                Text(
                                    text = "In ${currentStep.distanceText}",
                                    color = Color(0xFF0052CC),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.3.sp
                                )
                                Text(
                                    text = currentStep.instruction,
                                    color = Color(0xFF0F172A),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // Steps button
                            Surface(
                                color = Color(0xFFEFF6FF),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0xFF0052CC)),
                                modifier = Modifier
                                    .padding(start = 4.dp)
                                    .clickable { showStepsModal = true }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = "STEPS",
                                        color = Color(0xFF0052CC),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Icon(
                                        Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = Color(0xFF0052CC),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            // Minimize Action Button
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(Color(0xFFF1F5F9), CircleShape)
                                    .clickable { isBannerMinimized = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Remove,
                                    contentDescription = "Minimize Navigation Banner",
                                    tint = Color(0xFF0F172A),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        if (nextStep != null) {
                            HorizontalDivider(
                                color = Color(0xFFE2E8F0),
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showStepsModal = true }
                            ) {
                                Icon(
                                    imageVector = getManeuverIcon(nextStep.maneuver),
                                    contentDescription = null,
                                    tint = Color(0xFF475569),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Then: ${nextStep.instruction} (${nextStep.distanceText})",
                                    color = Color(0xFF475569),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // 2. BOTTOM NAVIGATION BAR (COMPACT 3D GLASSY ALPINE WHITE & SAPPHIRE AZURE)
        // ==========================================
        AnimatedVisibility(
            visible = true,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset { IntOffset(bottomOffsetX.roundToInt(), bottomOffsetY.roundToInt()) }
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        bottomOffsetX += dragAmount.x
                        bottomOffsetY += dragAmount.y
                    }
                }
                .padding(bottom = 16.dp, start = 12.dp, end = 12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.Transparent,
                shadowElevation = 0.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    // Left: Remaining Duration, Distance & ETA Clock Time
                    Column {
                        Row(verticalAlignment = Alignment.Bottom) {
                            val durMin = routeDetails?.durationMinutes ?: 0
                            val durHours = durMin / 60
                            val durRemainingMin = durMin % 60
                            val durDisplay = if (durHours > 0) "${durHours}h ${durRemainingMin}m" else "${durMin} min"

                            Text(
                                text = durDisplay,
                                color = Color(0xFF059669), // Crisp emerald green duration
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "• ${"%.1f".format(routeDetails?.distanceKm ?: 0.0)} km",
                                color = Color(0xFF1E293B),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "ETA: $etaString • On Time",
                            color = Color(0xFF475569),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Center / Right Controls
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Live Speed Badge
                        Surface(
                            color = Color(0xFF0F172A),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFF0052CC))
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "${currentSpeedKmh.toInt()}",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = "KM/H",
                                    color = Color(0xFFCBD5E1),
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Google Maps External Launch Icon Button
                        IconButton(
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                onOpenGoogleMapsApp()
                            },
                            modifier = Modifier
                                .size(38.dp)
                                .background(Color(0xFF0052CC), CircleShape)
                        ) {
                            Icon(
                                Icons.Default.Directions,
                                contentDescription = "Open Google Maps Navigation",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Exit Navigation Button (Red X)
                        IconButton(
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                onExitNavigation()
                            },
                            modifier = Modifier
                                .size(38.dp)
                                .background(Color(0xFFDC2626), CircleShape)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Exit Navigation",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // ==========================================
    // 3. STEP-BY-STEP DIRECTIONS LIST MODAL
    // ==========================================
    if (showStepsModal) {
        AlertDialog(
            onDismissRequest = { showStepsModal = false },
            containerColor = Color(0xFF0F172A),
            shape = RoundedCornerShape(24.dp),
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Route, contentDescription = null, tint = Color(0xFF00E5FF))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Turn-by-Turn Steps",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = { showStepsModal = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF94A3B8))
                    }
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Total Route: ${"%.1f".format(routeDetails?.distanceKm ?: 0.0)} km • ${routeDetails?.durationMinutes} min",
                        color = Color(0xFF38BDF8),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    if (steps.isEmpty()) {
                        Text("No detailed steps available for this segment.", color = Color(0xFF94A3B8), fontSize = 13.sp)
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 350.dp)
                        ) {
                            itemsIndexed(steps) { idx, step ->
                                val isCurrent = idx == currentStepIndex
                                Surface(
                                    color = if (isCurrent) Color(0xFF1E293B) else Color(0xFF020617),
                                    shape = RoundedCornerShape(12.dp),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isCurrent) Color(0xFF00E5FF) else Color(0xFF334155)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onStepSelected(idx)
                                            showStepsModal = false
                                        }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(12.dp)
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .size(34.dp)
                                                .background(
                                                    if (isCurrent) Color(0xFF10B981) else Color(0xFF334155),
                                                    CircleShape
                                                )
                                        ) {
                                            Icon(
                                                imageVector = getManeuverIcon(step.maneuver),
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(10.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = step.instruction,
                                                color = Color.White,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "${step.distanceText} • ${step.durationText}",
                                                color = Color(0xFF94A3B8),
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showStepsModal = false
                        onOpenGoogleMapsApp()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7), contentColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Open in Google Maps", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showStepsModal = false }) {
                    Text("Close", color = Color(0xFF94A3B8))
                }
            }
        )
    }
}

/**
 * Google Maps Directions & Navigation Options Dialog.
 * Allows switching travel mode (Motorbike, Car, Highway) and choosing between:
 * 1. "Start In-App Live Navigation HUD"
 * 2. "Open Google Maps App Navigation"
 * 3. "Open Google Maps Route URL"
 */
@Composable
fun GoogleMapsDirectionsOptionsModal(
    origin: LatLng,
    destination: LatLng,
    waypoints: List<LatLng>,
    routeDetails: RouteDetails?,
    onStartInAppNav: () -> Unit,
    onOpenGoogleMapsApp: () -> Unit,
    onOpenGoogleMapsWeb: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTravelMode by remember { mutableStateOf("BIKE") } // "BIKE", "CAR", "HIGHWAY"

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F172A),
        shape = RoundedCornerShape(24.dp),
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(52.dp)
                        .background(Color(0x3300E5FF), CircleShape)
                        .border(1.5.dp, Color(0xFF00E5FF), CircleShape)
                ) {
                    Icon(
                        Icons.Default.Directions,
                        contentDescription = null,
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Map Directions & Navigation",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "Google Maps Routing & Live HUD Navigation",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Travel Mode Selector Tabs
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf(
                        Triple("BIKE", "🏍️ Motorbike", Color(0xFF00E5FF)),
                        Triple("CAR", "🚗 Driving", Color(0xFF10B981)),
                        Triple("HIGHWAY", "🛣️ Highway", Color(0xFFF59E0B))
                    ).forEach { (modeKey, modeTitle, modeColor) ->
                        val isSel = selectedTravelMode == modeKey
                        Surface(
                            onClick = { selectedTravelMode = modeKey },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSel) modeColor.copy(alpha = 0.2f) else Color(0xFF1E293B),
                            border = androidx.compose.foundation.BorderStroke(
                                1.5.dp,
                                if (isSel) modeColor else Color(0xFF334155)
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                            ) {
                                Text(
                                    text = modeTitle,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) Color.White else Color(0xFF94A3B8)
                                )
                            }
                        }
                    }
                }

                // Route Distance & Duration Summary Card
                Surface(
                    color = Color(0xFF020617),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Column {
                            Text("Fastest Route", color = Color(0xFF94A3B8), fontSize = 11.sp)
                            Text(
                                text = "${"%.1f".format(routeDetails?.distanceKm ?: 159.0)} KM",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            val durMin = routeDetails?.durationMinutes ?: 214
                            Text("Est. Travel Time", color = Color(0xFF94A3B8), fontSize = 11.sp)
                            Text(
                                text = "${durMin / 60}h ${durMin % 60}m",
                                color = Color(0xFF10B981),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }

                // Navigation Action 1: In-App Live HUD Navigation
                Button(
                    onClick = onStartInAppNav,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF), contentColor = Color.Black),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Start In-App Live Navigation HUD", fontWeight = FontWeight.Black, fontSize = 13.sp)
                }

                // Navigation Action 2: Open Google Maps App Turn-by-Turn
                Button(
                    onClick = onOpenGoogleMapsApp,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7), contentColor = Color.White),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Launch Official Google Maps App", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                // Navigation Action 3: Open Google Maps Route URL
                OutlinedButton(
                    onClick = onOpenGoogleMapsWeb,
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFF59E0B)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.AddLink, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Open Multi-Stop Route Link", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Cancel", color = Color(0xFF94A3B8))
            }
        }
    )
}

/**
 * Returns matching icon for maneuver types.
 */
private fun getManeuverIcon(maneuver: String): ImageVector {
    val m = maneuver.lowercase()
    return when {
        m.contains("right") || m.contains("slight right") -> Icons.Default.TurnRight
        m.contains("left") || m.contains("slight left") -> Icons.Default.TurnLeft
        m.contains("uturn") -> Icons.Default.TurnSharpLeft
        m.contains("roundabout") -> Icons.Default.RotateRight
        m.contains("merge") -> Icons.Default.AltRoute
        m.contains("arrive") -> Icons.Default.Flag
        m.contains("depart") -> Icons.Default.NearMe
        else -> Icons.Default.Straight
    }
}
