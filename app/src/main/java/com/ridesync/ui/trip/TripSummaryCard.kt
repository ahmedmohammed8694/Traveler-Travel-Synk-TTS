package com.ridesync.ui.trip

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ridesync.data.model.SavedTrip
import com.ridesync.data.model.TripCategory
import com.ridesync.ui.theme.HudColors

/**
 * Modern High-Contrast Trip Summary Card.
 * Displays:
 * 1. Trip Name & Category Badge (Live Convoy, Upcoming, Completed)
 * 2. Start Date & Route Telemetry (Distance & Duration)
 * 3. Joined Riders Count with visual avatar stack
 * 4. 1-Tap "Share QR" & "Share Link" buttons (Deep link + Google Drive download fallback)
 * 5. "Trip Details" button (Opens Full Day-by-Day & Itinerary view)
 * 6. "Launch Convoy" button (3D Live Map Navigation)
 */
@Composable
fun TripSummaryCard(
    trip: SavedTrip,
    onViewDetails: () -> Unit,
    onLaunchTrip: () -> Unit,
    onEditTrip: () -> Unit,
    onDeleteTrip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isOngoing = trip.category == TripCategory.ONGOING
    var showQrDialog by remember { mutableStateOf(false) }

    val cardBorder = if (isOngoing) {
        BorderStroke(2.dp, Color(0xFF22C55E))
    } else {
        BorderStroke(1.dp, Color(0xFF334155))
    }

    val cardBg = if (isOngoing) Color(0xFF0F172A) else Color(0xFF1E293B)
    val accentColor = if (isOngoing) Color(0xFF22C55E) else Color(0xFFF59E0B)

    Card(
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = RoundedCornerShape(18.dp),
        border = cardBorder,
        modifier = modifier
            .fillMaxWidth()
            .clickable { onViewDetails() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Category Badge & Edit/Delete Icons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Category Status Badge
                Surface(
                    color = when (trip.category) {
                        TripCategory.ONGOING -> Color(0xFF22C55E).copy(alpha = 0.2f)
                        TripCategory.UPCOMING -> Color(0xFFF59E0B).copy(alpha = 0.2f)
                        TripCategory.COMPLETED -> Color(0xFF38BDF8).copy(alpha = 0.2f)
                    },
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(
                        1.dp,
                        when (trip.category) {
                            TripCategory.ONGOING -> Color(0xFF22C55E)
                            TripCategory.UPCOMING -> Color(0xFFF59E0B)
                            TripCategory.COMPLETED -> Color(0xFF38BDF8)
                        }
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .background(
                                    when (trip.category) {
                                        TripCategory.ONGOING -> Color(0xFF22C55E)
                                        TripCategory.UPCOMING -> Color(0xFFF59E0B)
                                        TripCategory.COMPLETED -> Color(0xFF38BDF8)
                                    },
                                    CircleShape
                                )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = when (trip.category) {
                                TripCategory.ONGOING -> "● LIVE CONVOY"
                                TripCategory.UPCOMING -> "UPCOMING RIDE"
                                TripCategory.COMPLETED -> "COMPLETED TOUR"
                            },
                            color = when (trip.category) {
                                TripCategory.ONGOING -> Color(0xFF22C55E)
                                TripCategory.UPCOMING -> Color(0xFFFBBF24)
                                TripCategory.COMPLETED -> Color(0xFF38BDF8)
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onEditTrip,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Trip", tint = Color(0xFF38BDF8), modifier = Modifier.size(17.dp))
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = onDeleteTrip,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Trip", tint = Color(0xFFEF4444), modifier = Modifier.size(17.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Trip Title
            Text(
                text = trip.title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
            )

            // Origin -> Destination
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
            ) {
                Icon(Icons.Default.Place, contentDescription = null, tint = Color(0xFF22C55E), modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${trip.originName} ➔ ${trip.destinationName}",
                    fontSize = 13.sp,
                    color = Color(0xFFCBD5E1),
                    fontWeight = FontWeight.Medium
                )
            }

            // Key Trip Details Pill Row (Start Date, Distance, Duration, Days)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                // Start Date Badge
                Surface(
                    color = Color(0xFF0F172A),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFF334155))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.CalendarToday, contentDescription = null, tint = Color(0xFFFBBF24), modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = trip.scheduledDate.ifBlank { "Oct 2026" },
                            color = Color(0xFFFBBF24),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Distance & Duration Badge
                Surface(
                    color = Color(0xFF0F172A),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFF334155))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Route, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${trip.distanceKm.toInt()} KM • ${trip.durationMinutes / 60}h ${trip.durationMinutes % 60}m",
                            color = Color(0xFFE2E8F0),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Multi-Day Badge (if segments exist)
                if (trip.routeSegments.isNotEmpty()) {
                    Surface(
                        color = Color(0xFF0284C7).copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFF0284C7))
                    ) {
                        Text(
                            text = "${trip.routeSegments.size} Days",
                            color = Color(0xFF38BDF8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Intermediate Stops summary
            if (trip.waypoints.isNotEmpty()) {
                Text(
                    text = "📍 Stops (${trip.waypoints.size}): ${trip.waypoints.joinToString(" ➔ ")}",
                    fontSize = 11.sp,
                    color = Color(0xFF38BDF8),
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            // Joined Riders Bar & Invite Buttons (Share Link & Share QR)
            Surface(
                color = Color(0xFF0F172A),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFF334155)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Group, contentDescription = null, tint = Color(0xFF22C55E), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        val ridersCount = trip.joinedRiders.size.coerceAtLeast(1)
                        Text(
                            text = "$ridersCount ${if (ridersCount == 1) "Rider Joined" else "Riders Joined"}",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        // Share QR Button
                        TextButton(
                            onClick = { showQrDialog = true },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.QrCode, contentDescription = null, tint = Color(0xFFFBBF24), modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("QR Code", color = Color(0xFFFBBF24), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        // Share Trip Link Button
                        TextButton(
                            onClick = {
                                shareTripLink(context, trip)
                            },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Share Link", color = Color(0xFF38BDF8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Bottom Action Buttons: "Trip Details" & "Launch Convoy"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Trip Details Button
                OutlinedButton(
                    onClick = onViewDetails,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8)),
                    border = BorderStroke(1.dp, Color(0xFF38BDF8))
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Trip Details", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                // Launch Convoy Navigation Button
                Button(
                    onClick = onLaunchTrip,
                    modifier = Modifier
                        .weight(1.2f)
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.Black)
                ) {
                    Icon(
                        imageVector = if (isOngoing) Icons.Default.PlayArrow else Icons.Default.Navigation,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isOngoing) "Rejoin Convoy" else "Launch Ride",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }

    if (showQrDialog) {
        ShareTripQrDialog(
            tripTitle = trip.title,
            lobbyCode = trip.lobbyCode.ifBlank { "RRS-${trip.tripId.takeLast(4)}" },
            startDate = trip.scheduledDate,
            routeDescription = "${trip.originName} to ${trip.destinationName}",
            onDismiss = { showQrDialog = false }
        )
    }
}

/**
 * Triggers native Android Share Sheet with trip join link, deep link, and Google Drive download redirect link.
 */
fun shareTripLink(context: Context, trip: SavedTrip) {
    val code = trip.lobbyCode.ifBlank { "RRS-${trip.tripId.takeLast(4)}" }
    val deepLink = "ridesync://trip/join?code=$code"
    val shareText = "🏍️ Join my motorcycle tour: '${trip.title}' on RIDERsYNK!\n\n" +
            "📅 Date: ${trip.scheduledDate.ifBlank { "Upcoming" }}\n" +
            "📍 Route: ${trip.originName} to ${trip.destinationName}\n" +
            "🔑 Lobby Code: $code\n\n" +
            "📲 Open in App (if installed):\n" +
            "$deepLink\n\n" +
            "📥 Download & Install RIDERsYNK (if not installed):\n" +
            "$RIDERsYNK_DRIVE_DOWNLOAD_URL\n" +
            "(After installing, open the app and enter code '$code' to join the convoy!)"

    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, shareText)
        type = "text/plain"
    }
    val shareIntent = Intent.createChooser(sendIntent, "Share Trip Invite Link")
    context.startActivity(shareIntent)
}
