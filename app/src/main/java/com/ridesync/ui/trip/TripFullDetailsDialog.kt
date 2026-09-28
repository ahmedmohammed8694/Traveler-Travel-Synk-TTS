package com.ridesync.ui.trip

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ridesync.data.model.*
import com.ridesync.data.repository.TripRepository
import com.ridesync.ui.theme.HudColors

/**
 * Full-Page Trip Details Screen / Dialog.
 * Provides complete breakdown of:
 * 1. Trip metadata (Title, dates, total distance/time, lobby join code with 1-tap QR/share).
 * 2. Joined rider profiles (Vehicles, roles, emergency call action).
 * 3. Multi-day breakdown (Day 1, Day 2, etc.) with Google Maps links and "Open in Google Maps" action.
 * 4. Automated itinerary milestones per day with status checkboxes (Pending, Completed, Skipped).
 * 5. Quick launch into Convoy Live 3D Navigation.
 * 6. Interactive Route Editing (Full Trip or Single Day Route).
 */
@Composable
fun TripFullDetailsDialog(
    trip: SavedTrip,
    currentUserId: String = "user_me",
    onDismiss: () -> Unit,
    onLoadInPlanner: () -> Unit,
    onLaunchTrip: () -> Unit,
    onEditTrip: () -> Unit,
    onDeleteTrip: () -> Unit,
    onExitTrip: () -> Unit,
    onViewItinerary: () -> Unit,
    onLaunchSegment: ((TripRouteSegment) -> Unit)? = null
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val accentColor = Color(0xFFF59E0B)

    // Collect latest trip state in case stop statuses or routes are modified
    val allTrips by TripRepository.tripsFlow.collectAsState()
    val liveTrip = allTrips.firstOrNull { it.tripId == trip.tripId } ?: trip

    var showShareQrDialog by remember { mutableStateOf(false) }
    var showEditRouteDialog by remember { mutableStateOf(false) }
    var editDayNumberTarget by remember { mutableStateOf<Int?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0B1120))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // Top App Bar
                Surface(
                    color = Color(0xFF0F172A),
                    border = BorderStroke(1.dp, Color(0xFF1E293B)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Column {
                                Text(
                                    text = "Trip Full Details",
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = liveTrip.title,
                                    color = Color(0xFF38BDF8),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Category Badge
                        Surface(
                            color = when (liveTrip.category) {
                                TripCategory.ONGOING -> Color(0xFF22C55E).copy(alpha = 0.2f)
                                TripCategory.UPCOMING -> Color(0xFFF59E0B).copy(alpha = 0.2f)
                                TripCategory.COMPLETED -> Color(0xFF38BDF8).copy(alpha = 0.2f)
                            },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(
                                1.dp,
                                when (liveTrip.category) {
                                    TripCategory.ONGOING -> Color(0xFF22C55E)
                                    TripCategory.UPCOMING -> Color(0xFFF59E0B)
                                    TripCategory.COMPLETED -> Color(0xFF38BDF8)
                                }
                            )
                        ) {
                            Text(
                                text = when (liveTrip.category) {
                                    TripCategory.ONGOING -> "● LIVE CONVOY"
                                    TripCategory.UPCOMING -> "UPCOMING"
                                    TripCategory.COMPLETED -> "COMPLETED"
                                },
                                color = when (liveTrip.category) {
                                    TripCategory.ONGOING -> Color(0xFF22C55E)
                                    TripCategory.UPCOMING -> Color(0xFFFBBF24)
                                    TripCategory.COMPLETED -> Color(0xFF38BDF8)
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // ==========================================
                    // 1. TRIP SUMMARY & TELEMETRY HEADER CARD
                    // ==========================================
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, Color(0xFF334155)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = liveTrip.title,
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Place, contentDescription = null, tint = Color(0xFF22C55E), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${liveTrip.originName} ➔ ${liveTrip.destinationName}",
                                    color = Color(0xFFE2E8F0),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Stats Grid: Distance, Duration, Start Date
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF0F172A), RoundedCornerShape(12.dp))
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Start Date", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                    Text(
                                        text = liveTrip.scheduledDate.ifBlank { "Oct 2026" },
                                        color = Color(0xFFFBBF24),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Total Distance", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                    Text(
                                        text = "${liveTrip.distanceKm.toInt()} KM",
                                        color = Color(0xFF00E5FF),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Travel Time", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                    Text(
                                        text = "${liveTrip.durationMinutes / 60}h ${liveTrip.durationMinutes % 60}m",
                                        color = Color(0xFF10B981),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Lobby Code & QR / Share Buttons
                            Surface(
                                color = Color(0xFF0F172A),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color(0xFF334155)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                ) {
                                    Column {
                                        Text("Lobby Invite Code", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                        Text(
                                            text = liveTrip.lobbyCode.ifBlank { "RRS-${liveTrip.tripId.takeLast(4)}" },
                                            color = Color(0xFF38BDF8),
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        // Copy Code
                                        IconButton(
                                            onClick = {
                                                val code = liveTrip.lobbyCode.ifBlank { "RRS-${liveTrip.tripId.takeLast(4)}" }
                                                clipboardManager.setText(AnnotatedString(code))
                                                Toast.makeText(context, "Lobby Code copied: $code", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = Color(0xFFCBD5E1), modifier = Modifier.size(18.dp))
                                        }

                                        // Share QR Code Button
                                        Button(
                                            onClick = { showShareQrDialog = true },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B), contentColor = Color(0xFFFBBF24)),
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("QR", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }

                                        // Share Invite Link
                                        Button(
                                            onClick = { shareTripLink(context, liveTrip) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7), contentColor = Color.White),
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Share", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ==========================================
                    // 2. DAY-BY-DAY ROUTES & AUTO ITINERARY
                    // ==========================================
                    val daysList = liveTrip.itineraryPlan?.days ?: if (liveTrip.routeSegments.isNotEmpty()) {
                        liveTrip.routeSegments.mapIndexed { idx, seg ->
                            ItineraryDay(
                                dayNumber = idx + 1,
                                dayTitle = seg.segmentName,
                                stops = listOf(
                                    ItineraryStop(
                                        stopId = "stop_${idx}_0",
                                        stopName = seg.originName.ifBlank { "Start Leg ${idx + 1}" },
                                        activityDescription = "Starting Location",
                                        googleMapsUrl = seg.googleMapsUrl,
                                        orderIndex = 0
                                    )
                                ) + seg.waypoints.mapIndexed { wIdx, wp ->
                                    ItineraryStop(
                                        stopId = "stop_${idx}_${wIdx + 1}",
                                        stopName = wp,
                                        activityDescription = "Scheduled Milestone / Attraction",
                                        googleMapsUrl = seg.googleMapsUrl,
                                        orderIndex = wIdx + 1
                                    )
                                } + listOf(
                                    ItineraryStop(
                                        stopId = "stop_${idx}_${seg.waypoints.size + 1}",
                                        stopName = seg.destinationName.ifBlank { "End Leg ${idx + 1}" },
                                        activityDescription = "Destination",
                                        googleMapsUrl = seg.googleMapsUrl,
                                        orderIndex = seg.waypoints.size + 1
                                    )
                                )
                            )
                        }
                    } else {
                        listOf(
                            ItineraryDay(
                                dayNumber = 1,
                                dayTitle = "Day 1: ${liveTrip.originName} to ${liveTrip.destinationName}",
                                stops = listOf(
                                    ItineraryStop(
                                        stopId = "stop_0_0",
                                        stopName = liveTrip.originName,
                                        activityDescription = "Starting Location",
                                        orderIndex = 0
                                    )
                                ) + liveTrip.waypoints.mapIndexed { wIdx, wp ->
                                    ItineraryStop(
                                        stopId = "stop_0_${wIdx + 1}",
                                        stopName = wp,
                                        activityDescription = "Intermediate Stop",
                                        orderIndex = wIdx + 1
                                    )
                                } + listOf(
                                    ItineraryStop(
                                        stopId = "stop_0_${liveTrip.waypoints.size + 1}",
                                        stopName = liveTrip.destinationName,
                                        activityDescription = "Destination",
                                        orderIndex = liveTrip.waypoints.size + 1
                                    )
                                )
                            )
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "🗓️ Day Routes & Itinerary (${daysList.size} Days)",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black
                        )

                        TextButton(
                            onClick = {
                                editDayNumberTarget = null
                                showEditRouteDialog = true
                            }
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Edit All Routes", color = Color(0xFF38BDF8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    daysList.forEach { day ->
                        val matchingSegment = liveTrip.routeSegments.getOrNull(day.dayNumber - 1)
                        val googleMapsUrl = matchingSegment?.googleMapsUrl ?: day.stops.firstOrNull { it.googleMapsUrl.isNotBlank() }?.googleMapsUrl ?: ""

                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Color(0xFF334155)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                // Day Header
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            color = Color(0xFFF59E0B),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = "${day.dayNumber}",
                                                    color = Color.Black,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Black
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = day.dayTitle.ifBlank { "Day ${day.dayNumber} Route" },
                                                color = Color.White,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            if (matchingSegment != null) {
                                                Text(
                                                    text = "${matchingSegment.originName} ➔ ${matchingSegment.destinationName}",
                                                    color = Color(0xFF94A3B8),
                                                    fontSize = 12.sp
                                                )
                                            }
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        // Edit single day route button
                                        IconButton(
                                            onClick = {
                                                editDayNumberTarget = day.dayNumber
                                                showEditRouteDialog = true
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit Day Route", tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                                        }

                                        // Open in Google Maps Link Button
                                        if (googleMapsUrl.isNotBlank()) {
                                            IconButton(
                                                onClick = {
                                                    try {
                                                        val mapIntent = Intent(Intent.ACTION_VIEW, Uri.parse(googleMapsUrl))
                                                        context.startActivity(mapIntent)
                                                    } catch (e: Exception) {
                                                        Toast.makeText(context, "Could not open map URL: ${e.message}", Toast.LENGTH_SHORT).show()
                                                    }
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Open in Google Maps", tint = Color(0xFF38BDF8), modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    }
                                }

                                if (googleMapsUrl.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Surface(
                                        color = Color(0xFF1E293B),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Icon(Icons.Default.AddLink, contentDescription = null, tint = Color(0xFFFBBF24), modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = googleMapsUrl,
                                                color = Color(0xFF94A3B8),
                                                fontSize = 11.sp,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Auto-Generated Stops List
                                Text(
                                    text = "Milestones & Stops (${day.stops.size}):",
                                    color = Color(0xFFCBD5E1),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )

                                day.stops.forEachIndexed { sIdx, stop ->
                                    val isCompleted = stop.status == ItineraryStopStatus.COMPLETED
                                    val isSkipped = stop.status == ItineraryStopStatus.SKIPPED

                                    Surface(
                                        color = Color(0xFF020617),
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(
                                            1.dp,
                                            if (isCompleted) Color(0xFF10B981) else Color(0xFF334155)
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 3.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                                        ) {
                                            // Checkbox to toggle status
                                            IconButton(
                                                onClick = {
                                                    val nextStatus = when (stop.status) {
                                                        ItineraryStopStatus.PENDING -> ItineraryStopStatus.COMPLETED
                                                        ItineraryStopStatus.COMPLETED -> ItineraryStopStatus.SKIPPED
                                                        ItineraryStopStatus.SKIPPED -> ItineraryStopStatus.PENDING
                                                        ItineraryStopStatus.IGNORED -> ItineraryStopStatus.PENDING
                                                    }
                                                    TripRepository.updateStopStatus(liveTrip.tripId, day.dayNumber, stop.stopId, nextStatus)
                                                },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    imageVector = when (stop.status) {
                                                        ItineraryStopStatus.COMPLETED -> Icons.Default.CheckCircle
                                                        ItineraryStopStatus.SKIPPED -> Icons.Default.Cancel
                                                        else -> Icons.Default.RadioButtonUnchecked
                                                    },
                                                    contentDescription = "Status",
                                                    tint = when (stop.status) {
                                                        ItineraryStopStatus.COMPLETED -> Color(0xFF10B981)
                                                        ItineraryStopStatus.SKIPPED -> Color(0xFFEF4444)
                                                        else -> Color(0xFF64748B)
                                                    },
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }

                                            Spacer(modifier = Modifier.width(8.dp))

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "${sIdx + 1}. ${stop.stopName}",
                                                    color = if (isCompleted || isSkipped) Color(0xFF94A3B8) else Color.White,
                                                    textDecoration = if (isCompleted || isSkipped) TextDecoration.LineThrough else TextDecoration.None,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                if (stop.activityDescription.isNotBlank()) {
                                                    Text(
                                                        text = stop.activityDescription,
                                                        color = Color(0xFF64748B),
                                                        fontSize = 11.sp
                                                    )
                                                }
                                            }

                                            // Status Tag
                                            Surface(
                                                color = when (stop.status) {
                                                    ItineraryStopStatus.COMPLETED -> Color(0xFF10B981).copy(alpha = 0.2f)
                                                    ItineraryStopStatus.SKIPPED -> Color(0xFFEF4444).copy(alpha = 0.2f)
                                                    else -> Color(0xFF0284C7).copy(alpha = 0.2f)
                                                },
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = stop.status.name,
                                                    color = when (stop.status) {
                                                        ItineraryStopStatus.COMPLETED -> Color(0xFF10B981)
                                                        ItineraryStopStatus.SKIPPED -> Color(0xFFEF4444)
                                                        else -> Color(0xFF38BDF8)
                                                    },
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Launch Day Navigation Button
                                if (matchingSegment != null && onLaunchSegment != null) {
                                    Button(
                                        onClick = { onLaunchSegment(matchingSegment) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF), contentColor = Color.Black),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth().height(40.dp)
                                    ) {
                                        Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Launch Day ${day.dayNumber} in 3D Map", fontSize = 12.sp, fontWeight = FontWeight.Black)
                                    }
                                }
                            }
                        }
                    }

                    // ==========================================
                    // 3. JOINED RIDERS & COMPANIONS SECTION
                    // ==========================================
                    Text(
                        text = "👥 Joined Riders (${liveTrip.joinedRiders.size})",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black
                    )

                    if (liveTrip.joinedRiders.isEmpty()) {
                        Surface(
                            color = Color(0xFF0F172A),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, Color(0xFF334155)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "No other riders joined yet. Share the invite link above to invite companions!",
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    } else {
                        liveTrip.joinedRiders.forEach { rider ->
                            Surface(
                                color = Color(0xFF0F172A),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, Color(0xFF334155)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(12.dp)
                                ) {
                                    // Rider Initial Avatar
                                    Surface(
                                        color = when (rider.role) {
                                            ConvoyRole.LEAD -> Color(0xFFF59E0B)
                                            ConvoyRole.SWEEP -> Color(0xFF00E5FF)
                                            ConvoyRole.MEMBER -> Color(0xFF22C55E)
                                        },
                                        shape = CircleShape,
                                        modifier = Modifier.size(42.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = rider.displayName.take(1).uppercase(),
                                                color = Color.Black,
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Black
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = rider.displayName,
                                                color = Color.White,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                color = Color(0xFF1E293B),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = rider.role.name,
                                                    color = Color(0xFF38BDF8),
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = "🏍️ ${rider.bikeModel}",
                                            color = Color(0xFFCBD5E1),
                                            fontSize = 12.sp
                                        )
                                        Text(
                                            text = "Status: ${rider.status}",
                                            color = Color(0xFF10B981),
                                            fontSize = 11.sp
                                        )
                                    }

                                    // Direct Emergency Call Action
                                    if (rider.emergencyContact.isNotBlank()) {
                                        IconButton(
                                            onClick = {
                                                try {
                                                    val callIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${rider.emergencyContact}"))
                                                    context.startActivity(callIntent)
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, "Call error: ${e.message}", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(Icons.Default.Phone, contentDescription = "Call", tint = Color(0xFF22C55E), modifier = Modifier.size(20.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ==========================================
                    // 4. PLANNER / RIDER ACTION CONTROLS
                    // ==========================================
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                editDayNumberTarget = null
                                showEditRouteDialog = true
                            },
                            modifier = Modifier.weight(1f).height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8)),
                            border = BorderStroke(1.dp, Color(0xFF38BDF8))
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Edit Route", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = onExitTrip,
                            modifier = Modifier.weight(1f).height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFF59E0B)),
                            border = BorderStroke(1.dp, Color(0xFFF59E0B))
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Exit Trip", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = onDeleteTrip,
                            modifier = Modifier.weight(1f).height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                            border = BorderStroke(1.dp, Color(0xFFEF4444))
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Delete", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Primary Launch Button
                    Button(
                        onClick = onLaunchTrip,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF), contentColor = Color.Black),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("🚀 Launch Master Convoy Live Map", fontSize = 14.sp, fontWeight = FontWeight.Black)
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }

    if (showShareQrDialog) {
        ShareTripQrDialog(
            tripTitle = liveTrip.title,
            lobbyCode = liveTrip.lobbyCode.ifBlank { "RRS-${liveTrip.tripId.takeLast(4)}" },
            startDate = liveTrip.scheduledDate,
            routeDescription = "${liveTrip.originName} to ${liveTrip.destinationName}",
            onDismiss = { showShareQrDialog = false }
        )
    }

    if (showEditRouteDialog) {
        EditTripRouteDialog(
            trip = liveTrip,
            initialDayNumber = editDayNumberTarget,
            onDismiss = { showEditRouteDialog = false },
            onTripUpdated = { /* TripRepository automatically emits via Flow */ }
        )
    }
}
