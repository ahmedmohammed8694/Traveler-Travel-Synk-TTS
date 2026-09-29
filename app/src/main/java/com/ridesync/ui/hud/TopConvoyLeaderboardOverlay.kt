package com.ridesync.ui.hud

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.model.LatLng
import com.ridesync.data.model.ConvoyMember
import com.ridesync.data.model.RiderLocationPing
import com.ridesync.data.model.RiderStatus
import com.ridesync.engine.ConvoyRadarEngine
import com.ridesync.ui.theme.HudColors
import com.ridesync.util.rememberRiderAvatarBitmap

/**
 * Top Side Racing-Style Convoy Leaderboard Bar.
 * Renders circular avatar cards for riders in position order (#1, #2, #3...).
 * Tapping any rider's card immediately focuses/centers the live map camera on that person!
 */
@Composable
fun TopConvoyLeaderboardOverlay(
    myLocation: LatLng?,
    myBearing: Float,
    riderLocations: Map<String, RiderLocationPing>,
    convoyMembers: Map<String, ConvoyMember>,
    onFocusRider: (LatLng, displayName: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    val userPos = myLocation ?: LatLng(17.3753, 78.4344)

    val radarList = ConvoyRadarEngine.calculateRelativePositions(
        myLocation = userPos,
        myBearing = myBearing,
        riders = riderLocations,
        members = convoyMembers
    )

    if (radarList.isEmpty() && riderLocations.isEmpty()) return

    Surface(
        color = Color(0xCC090D16),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            Brush.horizontalGradient(listOf(HudColors.CyanPrimary.copy(alpha = 0.6f), Color(0xFF0284C7).copy(alpha = 0.6f)))
        ),
        shadowElevation = 8.dp,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🏆 CONVOY POSITION LEADERBOARD",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = HudColors.CyanPrimary,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "Tap rider circle to focus map 🎯",
                    fontSize = 9.sp,
                    color = HudColors.TextCoolSilver
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                itemsIndexed(radarList) { idx, rider ->
                    val posRank = idx + 1
                    val avatarBitmap by rememberRiderAvatarBitmap(rider.photoUrl)

                    Surface(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onFocusRider(rider.latLng, rider.displayName)
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF141D2F),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            when (posRank) {
                                1 -> Color(0xFFEAB308) // Gold
                                2 -> Color(0xFFCBD5E1) // Silver
                                3 -> Color(0xFFF59E0B) // Bronze
                                else -> HudColors.CyanPrimary
                            }
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Position Number Badge (#1, #2, #3...)
                            Surface(
                                shape = CircleShape,
                                color = when (posRank) {
                                    1 -> Color(0xFFEAB308)
                                    2 -> Color(0xFF94A3B8)
                                    3 -> Color(0xFFD97706)
                                    else -> HudColors.CyanPrimary
                                },
                                modifier = Modifier.size(18.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "#$posRank",
                                        color = Color.Black,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            // Profile Photo Avatar Circle
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .border(1.dp, HudColors.CyanPrimary, CircleShape)
                                    .background(Color(0xFF0F172A))
                            ) {
                                if (avatarBitmap != null) {
                                    Image(
                                        bitmap = avatarBitmap!!.asImageBitmap(),
                                        contentDescription = rider.displayName,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Text(
                                        text = rider.displayName.take(1).uppercase(),
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            // Name & Speed
                            Column {
                                Text(
                                    text = rider.displayName,
                                    color = HudColors.TextCrispWhite,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = rider.formattedDistance,
                                        color = when (rider.relativePosition.name) {
                                            "AHEAD" -> Color(0xFF00E5FF)
                                            "BEHIND" -> Color(0xFFFBBF24)
                                            else -> Color(0xFF10B981)
                                        },
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    if (rider.speedKmh > 0) {
                                        Text(
                                            text = " • ${rider.speedKmh.toInt()}km/h",
                                            color = HudColors.TextCoolSilver,
                                            fontSize = 9.sp
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
