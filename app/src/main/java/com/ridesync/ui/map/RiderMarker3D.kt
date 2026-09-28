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
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.Icon
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
 * 3D Beveled Glowing Rider Avatar Puck Marker with Directional Bearing Arrow & Status Halo Ring.
 * Displays the rider's uploaded Profile/Bike Photo directly on the live map at their GPS location!
 */
@Composable
fun InterpolatedRiderMarker3D(
    targetLocation: LatLng,
    bearing: Float,
    displayName: String,
    status: RiderStatus,
    photoUrl: String = "",
    vehicleModel: String = ""
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
                modifier = Modifier.size(60.dp)
            ) {
                // Outer Glow Pulse Halo
                Box(
                    modifier = Modifier
                        .size(56.dp)
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

                // Directional Bearing Pointer Arrow Indicator (Positioned on top rim of the puck)
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

            // Frosted Glass Name Badge
            Box(
                modifier = Modifier
                    .shadow(4.dp, RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp))
                    .background(HudColors.FrostedOverlay)
                    .border(1.dp, statusCore.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
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
