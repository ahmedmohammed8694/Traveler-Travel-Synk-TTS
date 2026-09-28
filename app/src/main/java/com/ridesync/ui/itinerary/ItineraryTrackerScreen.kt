package com.ridesync.ui.itinerary

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.AltRoute
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.ridesync.data.model.*
import com.ridesync.data.repository.DirectionsRepository
import com.ridesync.data.repository.TripRepository
import com.ridesync.ui.theme.HudColors
import com.ridesync.ui.theme.frostedGlassHud
import com.ridesync.ui.theme.hud3dCard
import kotlinx.coroutines.launch

/**
 * Interactive Real-Time Itinerary Tracker Screen.
 * Provides day-by-day milestone tracking, status management (Pending, Completed, Skipped, Ignored),
 * dynamic turn-by-turn route redrawing, and one-tap external Google Maps navigation launching.
 */
@Composable
fun ItineraryTrackerScreen(
    tripId: String,
    onNavigateBack: () -> Unit,
    onLaunchCockpit: (SavedTrip) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val trips by TripRepository.tripsFlow.collectAsState()
    val trip = trips.firstOrNull { it.tripId == tripId }

    if (trip == null) {
        Box(
            modifier = modifier.fillMaxSize().background(HudColors.ObsidianCanvas),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Trip not found", color = HudColors.TextCrispWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = onNavigateBack) {
                    Text("Go Back")
                }
            }
        }
        return
    }

    val itineraryPlan = trip.itineraryPlan ?: ItineraryTripPlan(
        planId = "plan_${trip.tripId}",
        tripTitle = trip.title,
        totalDuration = "${trip.distanceKm.toInt()} KM • ${trip.durationMinutes} mins",
        days = if (trip.routeSegments.isNotEmpty()) {
            trip.routeSegments.mapIndexed { idx, seg ->
                ItineraryDay(
                    dayNumber = idx + 1,
                    dayTitle = seg.segmentName,
                    stops = seg.waypoints.mapIndexed { wIdx, wp ->
                        ItineraryStop(
                            stopId = "stop_${idx}_$wIdx",
                            stopName = wp,
                            activityDescription = "Scheduled milestone",
                            rawLocationText = wp,
                            orderIndex = wIdx
                        )
                    }
                )
            }
        } else {
            listOf(
                ItineraryDay(
                    dayNumber = 1,
                    dayTitle = "Day 1: ${trip.originName} to ${trip.destinationName}",
                    stops = listOf(
                        ItineraryStop("s_start", trip.originName, "Assembly & start", latitude = trip.startLatLng.latitude, longitude = trip.startLatLng.longitude, orderIndex = 0),
                        ItineraryStop("s_dest", trip.destinationName, "Final destination", latitude = trip.destLatLng.latitude, longitude = trip.destLatLng.longitude, orderIndex = 1)
                    )
                )
            )
        }
    )

    var selectedDayIndex by remember { mutableIntStateOf(0) }
    val currentDay = itineraryPlan.days.getOrNull(selectedDayIndex) ?: itineraryPlan.days.firstOrNull()

    // Filter active stops (PENDING or COMPLETED) for live polyline routing
    val activeStops = remember(currentDay?.stops) {
        currentDay?.stops?.filter { it.status == ItineraryStopStatus.PENDING || it.status == ItineraryStopStatus.COMPLETED } ?: emptyList()
    }

    val initialPos = activeStops.firstOrNull()?.let { LatLng(it.latitude, it.longitude) }
        ?: trip.startLatLng

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialPos, 11f)
    }

    var dayPolyline by remember { mutableStateOf<List<LatLng>>(emptyList()) }

    // Recalculate dynamic route line through active stops only
    LaunchedEffect(activeStops) {
        if (activeStops.size >= 2) {
            val origin = LatLng(activeStops.first().latitude, activeStops.first().longitude)
            val destination = LatLng(activeStops.last().latitude, activeStops.last().longitude)
            val waypoints = if (activeStops.size > 2) {
                activeStops.subList(1, activeStops.size - 1).map { LatLng(it.latitude, it.longitude) }
            } else emptyList()

            try {
                val routeDetails = DirectionsRepository.getDirectionsRoute(origin, destination, waypoints)
                dayPolyline = routeDetails.polylinePoints
            } catch (_: Exception) {
                dayPolyline = activeStops.map { LatLng(it.latitude, it.longitude) }
            }
        } else {
            dayPolyline = emptyList()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(HudColors.ObsidianCanvas)
    ) {
        // Top App Bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFF1E293B), CircleShape)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = HudColors.TextCrispWhite,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = itineraryPlan.tripTitle,
                        color = HudColors.TextCrispWhite,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (itineraryPlan.totalDuration.isNotBlank()) itineraryPlan.totalDuration else "${itineraryPlan.days.size} Days Planned",
                        color = HudColors.CyanLight,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Button(
                onClick = { onLaunchCockpit(trip) },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = HudColors.CyanPrimary, contentColor = Color.Black),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Cockpit Map", fontSize = 12.sp, fontWeight = FontWeight.Black)
            }
        }

        // Day Selector Tabs
        if (itineraryPlan.days.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            ) {
                items(itineraryPlan.days.indices.toList()) { index ->
                    val day = itineraryPlan.days[index]
                    val isSelected = index == selectedDayIndex
                    Box(
                        modifier = Modifier
                            .frostedGlassHud(
                                shape = RoundedCornerShape(12.dp),
                                backgroundColor = if (isSelected) HudColors.CyanPrimary.copy(alpha = 0.25f) else Color(0xFF1E293B),
                                borderColor = if (isSelected) HudColors.CyanPrimary else Color(0xFF334155)
                            )
                            .clickable { selectedDayIndex = index }
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = "Day ${day.dayNumber}: ${day.dayTitle.substringAfter("Day ${day.dayNumber}:").take(18)}",
                            color = if (isSelected) HudColors.CyanLight else HudColors.TextCoolSilver,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Embedded Dynamic Google Map View
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, Color(0xFF334155), RoundedCornerShape(16.dp))
        ) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = false,
                    compassEnabled = true,
                    myLocationButtonEnabled = false
                ),
                properties = MapProperties(mapType = MapType.NORMAL)
            ) {
                // Render Dynamic Active Route
                if (dayPolyline.isNotEmpty()) {
                    Polyline(
                        points = dayPolyline,
                        color = HudColors.CyanPrimary,
                        width = 10f,
                        geodesic = true
                    )
                }

                // Render Stop Markers
                currentDay?.stops?.forEachIndexed { index, stop ->
                    if (stop.latitude != 0.0 && stop.longitude != 0.0) {
                        val markerColor = when (stop.status) {
                            ItineraryStopStatus.COMPLETED -> BitmapDescriptorFactory.HUE_GREEN
                            ItineraryStopStatus.SKIPPED, ItineraryStopStatus.IGNORED -> BitmapDescriptorFactory.HUE_VIOLET
                            ItineraryStopStatus.PENDING -> BitmapDescriptorFactory.HUE_CYAN
                        }

                        Marker(
                            state = MarkerState(position = LatLng(stop.latitude, stop.longitude)),
                            title = "#${index + 1}: ${stop.stopName}",
                            snippet = "${stop.status.name} • ${stop.estimatedVisitTime}",
                            icon = BitmapDescriptorFactory.defaultMarker(markerColor)
                        )
                    }
                }
            }

            // Map Overlay Badge
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .frostedGlassHud(shape = RoundedCornerShape(8.dp), backgroundColor = Color(0xDD090D16))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "ROAD CORRIDOR: ${activeStops.size} ACTIVE STOPS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = HudColors.CyanPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Sequential Stop Cards List
        if (currentDay == null || currentDay.stops.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No stops scheduled for this day.", color = HudColors.TextCoolSilver, fontSize = 13.sp)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(currentDay.stops, key = { it.stopId }) { stop ->
                    ItineraryStopCard(
                        stop = stop,
                        onStatusChange = { newStatus ->
                            TripRepository.updateStopStatus(trip.tripId, currentDay.dayNumber, stop.stopId, newStatus)
                            Toast.makeText(context, "Marked '${stop.stopName}' as $newStatus", Toast.LENGTH_SHORT).show()
                        },
                        onFocusMap = {
                            if (stop.latitude != 0.0 && stop.longitude != 0.0) {
                                coroutineScope.launch {
                                    cameraPositionState.animate(
                                        CameraUpdateFactory.newLatLngZoom(LatLng(stop.latitude, stop.longitude), 14f),
                                        600
                                    )
                                }
                            } else {
                                Toast.makeText(context, "Location coordinates not available", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onOpenGoogleMaps = {
                            launchGoogleMapsNavigation(context, stop)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ItineraryStopCard(
    stop: ItineraryStop,
    onStatusChange: (ItineraryStopStatus) -> Unit,
    onFocusMap: () -> Unit,
    onOpenGoogleMaps: () -> Unit
) {
    val isStruckThrough = stop.status == ItineraryStopStatus.IGNORED || stop.status == ItineraryStopStatus.SKIPPED
    val statusColor = when (stop.status) {
        ItineraryStopStatus.PENDING -> HudColors.CyanPrimary
        ItineraryStopStatus.COMPLETED -> Color(0xFF10B981)
        ItineraryStopStatus.SKIPPED -> Color(0xFFF59E0B)
        ItineraryStopStatus.IGNORED -> Color(0xFF94A3B8)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (stop.status == ItineraryStopStatus.COMPLETED) Color(0xFF10B981) else Color(0xFF334155),
                RoundedCornerShape(16.dp)
            )
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            // Stop Header: Index, Title, Time
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(statusColor.copy(alpha = 0.2f), CircleShape)
                            .border(1.dp, statusColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${stop.orderIndex + 1}",
                            color = statusColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = stop.stopName,
                            color = if (isStruckThrough) Color(0xFF64748B) else HudColors.TextCrispWhite,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            textDecoration = if (stop.status == ItineraryStopStatus.IGNORED) TextDecoration.LineThrough else TextDecoration.None
                        )
                        if (stop.estimatedVisitTime.isNotBlank()) {
                            Text(
                                text = "⏱️ ${stop.estimatedVisitTime}",
                                color = HudColors.CyanLight,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Current Status Badge
                Box(
                    modifier = Modifier
                        .background(statusColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = stop.status.name,
                        color = statusColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            if (stop.activityDescription.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stop.activityDescription,
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }

            if (stop.rawLocationText.isNotBlank() && stop.rawLocationText != stop.stopName) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "📍 ${stop.rawLocationText}",
                    color = Color(0xFF64748B),
                    fontSize = 10.sp
                )
            }

            HorizontalDivider(color = Color(0xFF1E293B), modifier = Modifier.padding(vertical = 10.dp))

            // Action Buttons: Status Selector + Navigation Triggers
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Status Switcher Chips
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StatusChip("Done", Icons.Default.Check, stop.status == ItineraryStopStatus.COMPLETED, Color(0xFF10B981)) {
                        onStatusChange(ItineraryStopStatus.COMPLETED)
                    }
                    StatusChip("Skip", Icons.Default.SkipNext, stop.status == ItineraryStopStatus.SKIPPED, Color(0xFFF59E0B)) {
                        onStatusChange(ItineraryStopStatus.SKIPPED)
                    }
                    StatusChip("Ignore", Icons.Default.Block, stop.status == ItineraryStopStatus.IGNORED, Color(0xFF94A3B8)) {
                        onStatusChange(ItineraryStopStatus.IGNORED)
                    }
                    if (stop.status != ItineraryStopStatus.PENDING) {
                        StatusChip("Reset", Icons.Default.Refresh, false, HudColors.CyanPrimary) {
                            onStatusChange(ItineraryStopStatus.PENDING)
                        }
                    }
                }

                // Navigation Buttons
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    IconButton(
                        onClick = onFocusMap,
                        modifier = Modifier.size(32.dp).background(Color(0xFF1E293B), CircleShape)
                    ) {
                        Icon(Icons.Default.CenterFocusStrong, contentDescription = "Focus In-App Map", tint = HudColors.CyanPrimary, modifier = Modifier.size(16.dp))
                    }

                    IconButton(
                        onClick = onOpenGoogleMaps,
                        modifier = Modifier.size(32.dp).background(Color(0xFF1E293B), CircleShape)
                    ) {
                        Icon(Icons.Default.Directions, contentDescription = "Open Google Maps", tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusChip(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .background(if (isSelected) color.copy(alpha = 0.25f) else Color(0xFF1E293B), RoundedCornerShape(8.dp))
            .border(1.dp, if (isSelected) color else Color(0xFF334155), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = if (isSelected) color else Color(0xFF94A3B8), modifier = Modifier.size(12.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = title,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) color else Color(0xFF94A3B8)
            )
        }
    }
}

private fun launchGoogleMapsNavigation(context: Context, stop: ItineraryStop) {
    try {
        val uri = if (stop.latitude != 0.0 && stop.longitude != 0.0) {
            Uri.parse("google.navigation:q=${stop.latitude},${stop.longitude}")
        } else if (stop.rawLocationText.isNotBlank()) {
            Uri.parse("google.navigation:q=${Uri.encode(stop.rawLocationText)}")
        } else {
            Uri.parse("google.navigation:q=${Uri.encode(stop.stopName)}")
        }

        val mapIntent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.google.android.apps.maps")
        }

        if (mapIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(mapIntent)
        } else {
            // Fallback to web browser Google Maps
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${Uri.encode(stop.rawLocationText.ifBlank { stop.stopName })}"))
            context.startActivity(webIntent)
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Cannot open Google Maps: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
