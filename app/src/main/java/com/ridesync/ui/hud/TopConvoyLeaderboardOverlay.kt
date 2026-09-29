package com.ridesync.ui.hud

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.google.android.gms.maps.model.LatLng
import com.ridesync.data.model.ConvoyMember
import com.ridesync.data.model.RiderLocationPing
import com.ridesync.data.model.RiderStatus
import com.ridesync.engine.ConvoyRadarEngine
import com.ridesync.engine.ConvoyRadarRiderInfo
import com.ridesync.ui.theme.HudColors
import com.ridesync.util.rememberRiderAvatarBitmap
import kotlin.math.roundToInt

/**
 * Draggable, Minimizable Convoy Position Leaderboard.
 * Renders a horizontal row of round profile photo circles (◯ ◯ ◯ ◯ ◯ ◯).
 * Clicking any profile circle opens a full contact/details dialog for that rider.
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
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var isMinimized by remember { mutableStateOf(false) }
    var selectedRiderForDetails by remember { mutableStateOf<ConvoyRadarRiderInfo?>(null) }

    val userPos = myLocation ?: LatLng(17.3753, 78.4344)
    val radarList = ConvoyRadarEngine.calculateRelativePositions(
        myLocation = userPos,
        myBearing = myBearing,
        riders = riderLocations,
        members = convoyMembers
    )

    if (radarList.isEmpty() && riderLocations.isEmpty()) return

    Box(
        modifier = modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    offsetX += dragAmount.x
                    offsetY += dragAmount.y
                }
            }
    ) {
        if (isMinimized) {
            // MINIMIZED FLOATING PILL
            Surface(
                color = Color(0xF00F172A),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, HudColors.CyanPrimary),
                shadowElevation = 8.dp,
                modifier = Modifier.padding(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DragHandle,
                        contentDescription = "Move",
                        tint = HudColors.TextCoolSilver,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "👥 Convoy (${radarList.size})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = HudColors.CyanPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            isMinimized = false
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Expand", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }
        } else {
            // EXPANDED HORIZONTAL PROFILE CIRCLES ROW
            Surface(
                color = Color(0xDD090D16),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(
                    1.dp,
                    Brush.horizontalGradient(listOf(HudColors.CyanPrimary.copy(alpha = 0.6f), Color(0xFF0284C7).copy(alpha = 0.6f)))
                ),
                shadowElevation = 10.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                    // Header Bar with Drag Handle & Minimize Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.DragHandle,
                                contentDescription = "Drag overlay anywhere",
                                tint = HudColors.TextCoolSilver.copy(alpha = 0.7f),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "🏆 CONVOY RIDERS (${radarList.size})",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = HudColors.CyanPrimary,
                                letterSpacing = 0.5.sp
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Tap profile to view details • Slide ➡️",
                                fontSize = 9.sp,
                                color = HudColors.TextCoolSilver
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            IconButton(
                                onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    isMinimized = true
                                },
                                modifier = Modifier.size(22.dp)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Minimize", tint = Color.White, modifier = Modifier.size(14.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // HORIZONTAL SCROLLABLE ROW OF ROUND PROFILE CIRCLES (◯ ◯ ◯ ◯ ◯ ◯)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        itemsIndexed(radarList) { idx, rider ->
                            val posRank = idx + 1
                            val avatarBitmap by rememberRiderAvatarBitmap(rider.photoUrl)
                            val borderColor = when (posRank) {
                                1 -> Color(0xFFEAB308) // Gold
                                2 -> Color(0xFFCBD5E1) // Silver
                                3 -> Color(0xFFF59E0B) // Bronze
                                else -> HudColors.CyanPrimary
                            }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clickable {
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        selectedRiderForDetails = rider
                                    }
                                    .padding(vertical = 4.dp)
                            ) {
                                // Small Top Tag: Rank + Name
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = CircleShape,
                                        color = borderColor,
                                        modifier = Modifier.size(14.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "$posRank",
                                                color = Color.Black,
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = rider.displayName.take(7),
                                        color = HudColors.TextCrispWhite,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Spacer(modifier = Modifier.height(3.dp))

                                // ROUND PROFILE PHOTO CIRCLE ◯
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .border(2.dp, borderColor, CircleShape)
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
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(2.dp))

                                // Distance tag underneath
                                Text(
                                    text = rider.formattedDistance,
                                    color = when (rider.relativePosition.name) {
                                        "AHEAD" -> Color(0xFF00E5FF)
                                        "BEHIND" -> Color(0xFFFBBF24)
                                        else -> Color(0xFF10B981)
                                    },
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // RIDER FULL CONTACT & DETAILS MODAL
    selectedRiderForDetails?.let { rider ->
        val avatarBitmap by rememberRiderAvatarBitmap(rider.photoUrl)
        val member = convoyMembers[rider.riderId]

        Dialog(onDismissRequest = { selectedRiderForDetails = null }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF0F172A),
                border = BorderStroke(1.5.dp, HudColors.CyanPrimary),
                shadowElevation = 16.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Large Profile Circle Avatar
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .border(3.dp, HudColors.CyanPrimary, CircleShape)
                            .background(Color(0xFF1E293B))
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
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = rider.displayName,
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "Role: ${member?.role?.name ?: "Rider"} • Status: ${member?.status?.name ?: "ACTIVE"}",
                        color = HudColors.CyanLight,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Telemetry Details Grid
                    Surface(
                        color = Color(0xFF020617),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color(0xFF334155)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Position:", color = Color(0xFF94A3B8), fontSize = 12.sp)
                                Text("${rider.relativePosition.name} (${rider.formattedDistance})", color = Color(0xFF00E5FF), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Current Speed:", color = Color(0xFF94A3B8), fontSize = 12.sp)
                                Text("${rider.speedKmh.toInt()} KM/H", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            if (member?.vehicleModel?.isNotBlank() == true) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Vehicle Model:", color = Color(0xFF94A3B8), fontSize = 12.sp)
                                    Text(member.vehicleModel, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Battery:", color = Color(0xFF94A3B8), fontSize = 12.sp)
                                Text("${member?.batteryPercent ?: 95}% 🔋", color = Color(0xFF10B981), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Action Buttons: Call / Contact & Focus Map
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                onFocusRider(rider.latLng, rider.displayName)
                                selectedRiderForDetails = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = HudColors.CyanPrimary, contentColor = Color.Black),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).height(46.dp)
                        ) {
                            Icon(Icons.Default.CenterFocusWeak, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Focus Map", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                val phone = rider.phoneNumber.trim()
                                if (phone.isNotBlank()) {
                                    try {
                                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                } else {
                                    Toast.makeText(context, "No contact phone available for ${rider.displayName}", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981), contentColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).height(46.dp)
                        ) {
                            Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Call Rider", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    TextButton(onClick = { selectedRiderForDetails = null }) {
                        Text("Close", color = Color(0xFF94A3B8), fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

