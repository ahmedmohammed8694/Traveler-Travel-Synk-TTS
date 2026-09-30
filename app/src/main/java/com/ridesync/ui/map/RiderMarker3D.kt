package com.ridesync.ui.map

import android.graphics.Bitmap
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.MarkerComposable
import com.google.maps.android.compose.MarkerState
import com.ridesync.data.model.RiderStatus
import com.ridesync.ui.theme.HudColors
import com.ridesync.util.rememberRiderAvatarBitmap

/**
 * 3D Beveled Glowing Rider Avatar Puck Marker with Directional Bearing Arrow, Status Halo Ring & Position Number (#1, #2, #3...).
 * Displays the rider's uploaded Profile/Bike Photo directly on the live map at their GPS location!
 */
@Composable
fun InterpolatedRiderMarker3D(
    targetLocation: LatLng,
    bearing: Float,
    displayName: String,
    status: RiderStatus,
    photoUrl: String = "",
    vehicleModel: String = "",
    positionNumber: Int = 0
) {
    var previousLocation by remember { mutableStateOf(targetLocation) }
    val animFraction = remember { Animatable(0f) }

    LaunchedEffect(targetLocation) {
        animFraction.snapTo(0f)
        animFraction.animateTo(1f, animationSpec = tween(durationMillis = 400))
        previousLocation = targetLocation
    }

    val currentLatLng = LatLngEvaluator.interpolate(
        animFraction.value,
        previousLocation,
        targetLocation
    )

    // Load rider's uploaded profile/bike photo
    val avatarBitmap by rememberRiderAvatarBitmap(photoUrl)

    // Status Halo Color Pair (Core + Outer Glow)
    val (statusCore, statusGlow) = when (status) {
        RiderStatus.RIDING -> HudColors.StatusRiding to HudColors.StatusRidingGlow
        RiderStatus.STOPPED -> HudColors.StatusStopped to HudColors.StatusStoppedGlow
        RiderStatus.DELAYED -> HudColors.StatusDelayed to HudColors.StatusDelayedGlow
        RiderStatus.SOS -> HudColors.StatusSos to HudColors.StatusSosGlow
    }

    // Pulsing SOS effect if status == SOS
    val pulseTransition = rememberInfiniteTransition(label = "sos_pulse")
    val pulseAlpha by pulseTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sos_alpha"
    )

    val haloAlpha = if (status == RiderStatus.SOS) pulseAlpha else 0.4f

    MarkerComposable(
        state = MarkerState(position = currentLatLng),
        title = displayName
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(64.dp)
            ) {
                // Outer Glow Pulse Halo
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .shadow(elevation = 10.dp, shape = CircleShape, clip = false)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(statusGlow.copy(alpha = haloAlpha), statusCore.copy(alpha = 0.1f))
                            ),
                            shape = CircleShape
                        )
                )

                // Main Beveled Circle Puck with Border
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .border(
                            width = 2.5.dp,
                            brush = Brush.verticalGradient(
                                colors = listOf(statusGlow, statusCore)
                            ),
                            shape = CircleShape
                        )
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color(0xFF1F2937), Color(0xFF0B0F19))
                            )
                        )
                ) {
                    if (avatarBitmap != null) {
                        // Display Uploaded Profile / Bike Photo
                        Image(
                            bitmap = avatarBitmap!!.asImageBitmap(),
                            contentDescription = displayName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                    } else {
                        // Fallback: Rider Display Initial or Navigation Icon
                        val initial = displayName.trim().take(1).uppercase()
                        if (initial.isNotBlank() && initial != "R") {
                            Text(
                                text = initial,
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Navigation,
                                contentDescription = null,
                                tint = statusGlow,
                                modifier = Modifier
                                    .size(24.dp)
                                    .rotate(bearing)
                            )
                        }
                    }
                }

                // Position Rank Badge Chip (#1, #2, #3...) at Top Right
                if (positionNumber > 0) {
                    Surface(
                        shape = CircleShape,
                        color = when (positionNumber) {
                            1 -> Color(0xFFEAB308) // Gold for Lead / #1
                            2 -> Color(0xFF94A3B8) // Silver for #2
                            3 -> Color(0xFFD97706) // Bronze for #3
                            else -> HudColors.CyanPrimary
                        },
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White),
                        modifier = Modifier
                            .size(18.dp)
                            .align(Alignment.TopEnd)
                            .offset(x = 2.dp, y = 2.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "#$positionNumber",
                                color = Color.Black,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }

                // Directional Bearing Pointer Arrow Indicator
                if (avatarBitmap != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .rotate(bearing),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Box(
                            modifier = Modifier
                                .offset(y = (-2).dp)
                                .size(12.dp)
                                .background(statusGlow, CircleShape)
                                .border(1.dp, Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Navigation,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(8.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Frosted Glass Name Badge displaying Position Number & Name
            Box(
                modifier = Modifier
                    .shadow(4.dp, RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp))
                    .background(HudColors.FrostedOverlay)
                    .border(1.dp, statusCore.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (positionNumber > 0) {
                        Text(
                            text = "#$positionNumber ",
                            color = when (positionNumber) {
                                1 -> Color(0xFFEAB308)
                                2 -> Color(0xFFCBD5E1)
                                3 -> Color(0xFFF59E0B)
                                else -> HudColors.CyanPrimary
                            },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    if (vehicleModel.isNotBlank()) {
                        Icon(
                            imageVector = Icons.Default.TwoWheeler,
                            contentDescription = null,
                            tint = statusGlow,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                    }
                    Text(
                        text = displayName,
                        color = HudColors.TextCrispWhite,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Custom Map Route Marker displaying the actual Stop Name (instead of generic A, B, C).
 */
@Composable
fun RouteStopMarker(
    position: LatLng,
    stopName: String,
    stopNumber: Int,
    isFirstStop: Boolean = false,
    isLastStop: Boolean = false,
    onClick: () -> Unit
) {
    val accentColor = when {
        isFirstStop -> Color(0xFF22C55E) // Green for start
        isLastStop -> Color(0xFFEF4444)  // Red for destination
        else -> HudColors.CyanPrimary     // Cyan for intermediate stop
    }

    MarkerComposable(
        state = MarkerState(position = position),
        title = "Stop #$stopNumber: $stopName",
        onClick = {
            onClick()
            true
        }
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xF20B132B),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, accentColor),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Stop Number Badge
                    Surface(
                        shape = CircleShape,
                        color = accentColor,
                        modifier = Modifier.size(20.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "$stopNumber",
                                color = Color.Black,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(6.dp))

                    // Actual Stop Name Displayed on Route Map
                    Text(
                        text = stopName,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            // Pointer arrow tip
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .rotate(45f)
                    .offset(y = (-4).dp)
                    .background(accentColor)
            )
        }
    }
}
