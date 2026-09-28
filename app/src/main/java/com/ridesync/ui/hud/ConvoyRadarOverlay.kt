package com.ridesync.ui.hud

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.model.LatLng
import com.ridesync.data.model.ConvoyMember
import com.ridesync.data.model.ConvoyRole
import com.ridesync.data.model.RiderLocationPing
import com.ridesync.engine.ConvoyRadarEngine
import com.ridesync.engine.ConvoyRadarRiderInfo
import com.ridesync.engine.RadarRelativePosition
import com.ridesync.ui.theme.HudColors
import com.ridesync.ui.theme.frostedGlassHud
import com.ridesync.ui.theme.hud3dCard
import com.ridesync.util.rememberRiderAvatarBitmap
import kotlin.math.roundToInt

/**
 * Floating Draggable Convoy Partner Bubble & Small Popup HUD.
 * Renders as a sleek, draggable floating pill bubble that can be moved anywhere on the screen.
 * Tapping expands into a smooth, compact floating popup bubble displaying active companion positions,
 * relative ahead/behind gap distance, vehicle telemetry, and quick focus/call buttons.
 */
@Composable
fun ConvoyRadarOverlay(
    myLocation: LatLng?,
    myBearing: Float,
    riderLocations: Map<String, RiderLocationPing>,
    convoyMembers: Map<String, ConvoyMember>,
    onFocusRider: (LatLng) -> Unit,
    onCallRider: (phoneNumber: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    var offsetX by remember { mutableFloatStateOf(16f) }
    var offsetY by remember { mutableFloatStateOf(120f) }
    val haptics = LocalHapticFeedback.current

    val userPos = myLocation ?: LatLng(17.3753, 78.4344)
    val radarList = remember(userPos, myBearing, riderLocations, convoyMembers) {
        ConvoyRadarEngine.calculateRelativePositions(
            myLocation = userPos,
            myBearing = myBearing,
            riders = riderLocations,
            members = convoyMembers
        )
    }

    val aheadCount = radarList.count { it.relativePosition == RadarRelativePosition.AHEAD }
    val behindCount = radarList.count { it.relativePosition == RadarRelativePosition.BEHIND }

    // Pulsing radar animation for live active companions
    val infiniteTransition = rememberInfiniteTransition(label = "radarPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Box(
        modifier = modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
    ) {
        if (!isExpanded) {
            // ==========================================
            // COLLAPSED: SLEEK FLOATING DRAGGABLE BUBBLE
            // ==========================================
            Surface(
                shape = RoundedCornerShape(32.dp),
                color = Color(0xF2090D16),
                border = androidx.compose.foundation.BorderStroke(
                    1.5.dp,
                    Brush.horizontalGradient(
                        listOf(HudColors.CyanPrimary, Color(0xFF0284C7), HudColors.CyanPrimary)
                    )
                ),
                shadowElevation = 10.dp,
                modifier = Modifier
                    .wrapContentSize()
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            offsetX += dragAmount.x
                            offsetY += dragAmount.y
                        }
                    }
                    .clickable {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        isExpanded = true
                    }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    // Drag indicator handle
                    Icon(
                        Icons.Default.DragIndicator,
                        contentDescription = "Drag Bubble",
                        tint = HudColors.TextCoolSilver.copy(alpha = 0.7f),
                        modifier = Modifier
                            .size(16.dp)
                            .padding(end = 4.dp)
                    )

                    // Live Pulsing Radar Beacon
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(18.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size((14 * pulseScale).dp)
                                .background(HudColors.CyanPrimary.copy(alpha = pulseAlpha * 0.35f), CircleShape)
                        )
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(HudColors.CyanPrimary, CircleShape)
                                .border(1.dp, Color.White, CircleShape)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Partner summary
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "PARTNERS",
                                color = HudColors.TextCrispWhite,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Surface(
                                color = HudColors.CyanPrimary.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = "${radarList.size}",
                                    color = HudColors.CyanPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }

                        if (radarList.isNotEmpty()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (aheadCount > 0) {
                                    Text(
                                        text = "▲ $aheadCount Front",
                                        color = Color(0xFF00E5FF),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                if (behindCount > 0) {
                                    Text(
                                        text = "▼ $behindCount Back",
                                        color = Color(0xFFFBBF24),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                if (aheadCount == 0 && behindCount == 0) {
                                    Text(
                                        text = "● With You",
                                        color = Color(0xFF10B981),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = "Expand",
                        tint = HudColors.CyanPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        } else {
            // ==========================================
            // EXPANDED: SLEEK FLOATING POPUP BUBBLE
            // ==========================================
            Surface(
                shape = RoundedCornerShape(26.dp),
                color = Color(0xF20B132B),
                border = androidx.compose.foundation.BorderStroke(
                    1.5.dp,
                    Brush.verticalGradient(
                        listOf(
                            HudColors.CyanPrimary.copy(alpha = 0.9f),
                            Color(0xFF0284C7).copy(alpha = 0.5f),
                            Color(0x331E293B)
                        )
                    )
                ),
                shadowElevation = 16.dp,
                modifier = Modifier
                    .width(315.dp)
                    .heightIn(max = 380.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    // Draggable Top Header Area
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .pointerInput(Unit) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    offsetX += dragAmount.x
                                    offsetY += dragAmount.y
                                }
                            }
                    ) {
                        // Drag Handle Pill
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .width(36.dp)
                                .height(4.dp)
                                .background(Color(0xFF475569), CircleShape)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Header Content
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size((16 * pulseScale).dp)
                                            .background(HudColors.CyanPrimary.copy(alpha = pulseAlpha * 0.35f), CircleShape)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(9.dp)
                                            .background(HudColors.CyanPrimary, CircleShape)
                                            .border(1.dp, Color.White, CircleShape)
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "ACTIVE PARTNERS",
                                            color = HudColors.TextCrispWhite,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Black,
                                            letterSpacing = 0.5.sp
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            color = HudColors.CyanPrimary.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Text(
                                                text = "${radarList.size}",
                                                color = HudColors.CyanPrimary,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Black,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                            )
                                        }
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.padding(top = 2.dp)
                                    ) {
                                        Surface(
                                            color = Color(0x3300E5FF),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = "▲ $aheadCount Front",
                                                color = Color(0xFF00E5FF),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                            )
                                        }
                                        Surface(
                                            color = Color(0x33FBBF24),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = "▼ $behindCount Back",
                                                color = Color(0xFFFBBF24),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Minimize Button
                            IconButton(
                                onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    isExpanded = false
                                },
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(Color(0xFF1E293B), CircleShape)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Minimize",
                                    tint = HudColors.TextCoolSilver,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    HorizontalDivider(
                        color = Color(0x33334155),
                        modifier = Modifier.padding(vertical = 10.dp)
                    )

                    // Companion Cards List
                    if (radarList.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.Radar,
                                    contentDescription = null,
                                    tint = HudColors.CyanPrimary.copy(alpha = 0.6f),
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Searching for Convoy Partners...",
                                    color = HudColors.TextCoolSilver,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 240.dp)
                        ) {
                            items(radarList, key = { it.riderId }) { rider ->
                                ConvoyRadarRiderCard(
                                    rider = rider,
                                    onFocusClick = { onFocusRider(rider.latLng) },
                                    onCallClick = {
                                        if (rider.phoneNumber.isNotBlank()) {
                                            onCallRider(rider.phoneNumber)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConvoyRadarRiderCard(
    rider: ConvoyRadarRiderInfo,
    onFocusClick: () -> Unit,
    onCallClick: () -> Unit
) {
    val (badgeBg, badgeText, badgeColor) = when (rider.relativePosition) {
        RadarRelativePosition.AHEAD -> Triple(Color(0x3300E5FF), "▲ FRONT: ${rider.formattedDistance}", Color(0xFF00E5FF))
        RadarRelativePosition.BEHIND -> Triple(Color(0x33FBBF24), "▼ BACK: ${rider.formattedDistance}", Color(0xFFFBBF24))
        RadarRelativePosition.LEVEL_WITH_YOU -> Triple(Color(0x3310B981), "● WITH YOU", Color(0xFF10B981))
    }

    val roleBadge = when (rider.role) {
        ConvoyRole.LEAD -> "LEAD"
        ConvoyRole.SWEEP -> "SWEEP"
        ConvoyRole.MEMBER -> "MEMBER"
    }
    val avatarBitmap by rememberRiderAvatarBitmap(rider.photoUrl)

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF0F172A),
        border = androidx.compose.foundation.BorderStroke(1.dp, badgeColor.copy(alpha = 0.4f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onFocusClick)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Name & Avatar & Role Tag
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    if (avatarBitmap != null) {
                        Image(
                            bitmap = avatarBitmap!!.asImageBitmap(),
                            contentDescription = "${rider.displayName} Avatar",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .border(1.dp, badgeColor, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    Text(
                        text = rider.displayName,
                        color = HudColors.TextCrispWhite,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        color = if (rider.role == ConvoyRole.LEAD) Color(0x3338BDF8) else Color(0x3364748B),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = roleBadge,
                            color = if (rider.role == ConvoyRole.LEAD) Color(0xFF38BDF8) else Color(0xFF94A3B8),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }

                // Relative Distance Badge
                Surface(
                    color = badgeBg,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = badgeText,
                        color = badgeColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Subtitle info: Bike model, Speed, Battery + Action Buttons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "${rider.vehicleModel} • ⚡ ${rider.speedKmh.toInt()} km/h • 🔋 ${rider.batteryPercent}%",
                    color = HudColors.TextCoolSilver,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Center camera on rider
                    IconButton(
                        onClick = onFocusClick,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.CenterFocusStrong,
                            contentDescription = "Center on Map",
                            tint = HudColors.CyanPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Call button
                    IconButton(
                        onClick = onCallClick,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Phone,
                            contentDescription = "Quick Call",
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
