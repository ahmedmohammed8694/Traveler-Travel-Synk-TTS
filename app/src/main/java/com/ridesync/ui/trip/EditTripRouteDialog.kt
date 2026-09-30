package com.ridesync.ui.trip

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.android.gms.maps.model.LatLng
import com.ridesync.data.model.*
import com.ridesync.data.repository.TripRepository
import com.ridesync.engine.GoogleMapsUrlParser
import com.ridesync.ui.theme.HudColors
import com.ridesync.ui.theme.frostedGlassHud
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Interactive Route & Itinerary Editor Dialog.
 * Supports:
 * 1. Mode A: Edit Entire Full Trip Route (Stops, Title, Dates, Master Link).
 * 2. Mode B: Edit Single Day Route (Leg Map link, Day Stops, Day Title).
 */
@Composable
fun EditTripRouteDialog(
    trip: SavedTrip,
    initialDayNumber: Int? = null,
    onDismiss: () -> Unit,
    onTripUpdated: (SavedTrip) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var editScopeMode by remember { mutableIntStateOf(if (initialDayNumber != null) 1 else 0) } // 0: Full Trip, 1: Single Day
    var tripTitle by remember { mutableStateOf(trip.title) }
    var tripStartDate by remember { mutableStateOf(trip.scheduledDate.ifBlank { "15 Oct 2026" }) }
    var originName by remember { mutableStateOf(trip.originName) }
    var destName by remember { mutableStateOf(trip.destinationName) }
    var masterMapUrl by remember { mutableStateOf(trip.routeSegments.firstOrNull()?.googleMapsUrl ?: "") }

    // Full Trip Waypoints List
    val fullWaypoints = remember { mutableStateListOf<String>().apply { addAll(trip.waypoints) } }
    var newWaypointInput by remember { mutableStateOf("") }

    // Single Day Segments State
    val daySegments = remember {
        mutableStateListOf<TripRouteSegment>().apply {
            if (trip.routeSegments.isNotEmpty()) {
                addAll(trip.routeSegments)
            } else {
                add(
                    TripRouteSegment(
                        segmentId = "seg_1",
                        segmentName = "Day 1 Route",
                        googleMapsUrl = "",
                        originName = trip.originName,
                        destinationName = trip.destinationName,
                        waypoints = trip.waypoints,
                        orderIndex = 0
                    )
                )
            }
        }
    }

    var selectedDayIndex by remember {
        mutableIntStateOf(
            if (initialDayNumber != null && initialDayNumber in 1..daySegments.size) {
                initialDayNumber - 1
            } else 0
        )
    }

    var isResolvingLinks by remember { mutableStateOf(false) }
    var newDayWaypointInput by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = Color(0xFF0B1120),
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // Top Header Bar
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
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Edit Route & Stops", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Black)
                                Text("Update points, links, or specific day routes", color = Color(0xFF38BDF8), fontSize = 11.sp)
                            }
                        }

                        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF94A3B8))
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Scope Chooser Tab Bar: "Full Trip" vs "Single Day Route"
                    TabRow(
                        selectedTabIndex = editScopeMode,
                        containerColor = Color(0xFF1E293B),
                        contentColor = Color(0xFFF59E0B),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFF334155), RoundedCornerShape(12.dp))
                    ) {
                        Tab(
                            selected = editScopeMode == 0,
                            onClick = { editScopeMode = 0 },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AltRoute, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Full Trip Route", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        )
                        Tab(
                            selected = editScopeMode == 1,
                            onClick = { editScopeMode = 1 },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("One Day Route (${daySegments.size} Days)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        )
                    }

                    // ====================================================
                    // TAB 0: EDIT FULL TRIP ROUTE
                    // ====================================================
                    if (editScopeMode == 0) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Color(0xFF334155)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("Trip Overview", color = Color(0xFFFBBF24), fontSize = 14.sp, fontWeight = FontWeight.Bold)

                                OutlinedTextField(
                                    value = tripTitle,
                                    onValueChange = { tripTitle = it },
                                    label = { Text("Trip Title", color = Color(0xFF94A3B8), fontSize = 11.sp) },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = Color(0xFF0F172A),
                                        unfocusedContainerColor = Color(0xFF0F172A),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedLabelColor = Color(0xFFFBBF24),
                                        unfocusedLabelColor = Color(0xFF94A3B8),
                                        focusedPlaceholderColor = Color(0xFF64748B),
                                        unfocusedPlaceholderColor = Color(0xFF64748B),
                                        cursorColor = Color(0xFF00F0FF),
                                        focusedBorderColor = Color(0xFFF59E0B),
                                        unfocusedBorderColor = Color(0xFF334155)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = tripStartDate,
                                    onValueChange = { tripStartDate = it },
                                    label = { Text("Start Date", color = Color(0xFF94A3B8), fontSize = 11.sp) },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = Color(0xFF0F172A),
                                        unfocusedContainerColor = Color(0xFF0F172A),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedLabelColor = Color(0xFFFBBF24),
                                        unfocusedLabelColor = Color(0xFF94A3B8),
                                        focusedPlaceholderColor = Color(0xFF64748B),
                                        unfocusedPlaceholderColor = Color(0xFF64748B),
                                        cursorColor = Color(0xFF00F0FF),
                                        focusedBorderColor = Color(0xFFF59E0B),
                                        unfocusedBorderColor = Color(0xFF334155)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = originName,
                                        onValueChange = { originName = it },
                                        label = { Text("Origin", color = Color(0xFF94A3B8), fontSize = 11.sp) },
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = Color(0xFF0F172A),
                                            unfocusedContainerColor = Color(0xFF0F172A),
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedLabelColor = Color(0xFF22C55E),
                                            unfocusedLabelColor = Color(0xFF94A3B8),
                                            focusedPlaceholderColor = Color(0xFF64748B),
                                            unfocusedPlaceholderColor = Color(0xFF64748B),
                                            cursorColor = Color(0xFF00F0FF),
                                            focusedBorderColor = Color(0xFF22C55E),
                                            unfocusedBorderColor = Color(0xFF334155)
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )

                                    OutlinedTextField(
                                        value = destName,
                                        onValueChange = { destName = it },
                                        label = { Text("Destination", color = Color(0xFF94A3B8), fontSize = 11.sp) },
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = Color(0xFF0F172A),
                                            unfocusedContainerColor = Color(0xFF0F172A),
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedLabelColor = Color(0xFFEF4444),
                                            unfocusedLabelColor = Color(0xFF94A3B8),
                                            focusedPlaceholderColor = Color(0xFF64748B),
                                            unfocusedPlaceholderColor = Color(0xFF64748B),
                                            cursorColor = Color(0xFF00F0FF),
                                            focusedBorderColor = Color(0xFFEF4444),
                                            unfocusedBorderColor = Color(0xFF334155)
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                OutlinedTextField(
                                    value = masterMapUrl,
                                    onValueChange = { masterMapUrl = it },
                                    label = { Text("Update Route Map Link (maps.app.goo.gl or full URL)", color = Color(0xFF94A3B8), fontSize = 11.sp) },
                                    maxLines = 2,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = Color(0xFF0F172A),
                                        unfocusedContainerColor = Color(0xFF0F172A),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedLabelColor = Color(0xFF38BDF8),
                                        unfocusedLabelColor = Color(0xFF94A3B8),
                                        focusedPlaceholderColor = Color(0xFF64748B),
                                        unfocusedPlaceholderColor = Color(0xFF64748B),
                                        cursorColor = Color(0xFF00F0FF),
                                        focusedBorderColor = Color(0xFF38BDF8),
                                        unfocusedBorderColor = Color(0xFF334155)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                // Milestones / Waypoints Editor
                                Text("Trip Stops & Milestones (${fullWaypoints.size}):", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)

                                fullWaypoints.forEachIndexed { index, wp ->
                                    Surface(
                                        color = Color(0xFF020617),
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, Color(0xFF334155)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                Icon(Icons.Default.Place, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("${index + 1}. $wp", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                            }

                                            IconButton(
                                                onClick = { fullWaypoints.removeAt(index) },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }

                                // Add New Waypoint Row
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    OutlinedTextField(
                                        value = newWaypointInput,
                                        onValueChange = { newWaypointInput = it },
                                        placeholder = { Text("Add new stop/milestone name", fontSize = 11.sp, color = Color(0xFF64748B)) },
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = Color(0xFF0F172A),
                                            unfocusedContainerColor = Color(0xFF0F172A),
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedLabelColor = Color(0xFF38BDF8),
                                            unfocusedLabelColor = Color(0xFF94A3B8),
                                            focusedPlaceholderColor = Color(0xFF64748B),
                                            unfocusedPlaceholderColor = Color(0xFF64748B),
                                            cursorColor = Color(0xFF00F0FF),
                                            focusedBorderColor = Color(0xFF38BDF8),
                                            unfocusedBorderColor = Color(0xFF334155)
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )

                                    Button(
                                        onClick = {
                                            if (newWaypointInput.isNotBlank()) {
                                                fullWaypoints.add(newWaypointInput.trim())
                                                newWaypointInput = ""
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7), contentColor = Color.White),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Add", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    // ====================================================
                    // TAB 1: EDIT SINGLE DAY ROUTE
                    // ====================================================
                    if (editScopeMode == 1) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Color(0xFF334155)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Select Day to Edit:", color = Color(0xFFFBBF24), fontSize = 13.sp, fontWeight = FontWeight.Bold)

                                    TextButton(
                                        onClick = {
                                            val newDayNum = daySegments.size + 1
                                            daySegments.add(
                                                TripRouteSegment(
                                                    segmentId = "seg_${System.currentTimeMillis()}",
                                                    segmentName = "Day $newDayNum Route",
                                                    googleMapsUrl = "",
                                                    originName = "Start Day $newDayNum",
                                                    destinationName = "End Day $newDayNum",
                                                    orderIndex = daySegments.size
                                                )
                                            )
                                            selectedDayIndex = daySegments.size - 1
                                        }
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("+ Add Day", color = Color(0xFF38BDF8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                // Day Selector Pills
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    itemsIndexed(daySegments) { idx, seg ->
                                        val isSelected = idx == selectedDayIndex
                                        Surface(
                                            color = if (isSelected) Color(0xFFF59E0B).copy(alpha = 0.25f) else Color(0xFF1E293B),
                                            shape = RoundedCornerShape(10.dp),
                                            border = BorderStroke(1.dp, if (isSelected) Color(0xFFF59E0B) else Color(0xFF334155)),
                                            modifier = Modifier.clickable { selectedDayIndex = idx }
                                        ) {
                                            Text(
                                                text = "Day ${idx + 1}",
                                                color = if (isSelected) Color(0xFFFBBF24) else Color(0xFFCBD5E1),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                            )
                                        }
                                    }
                                }

                                val activeSegment = daySegments.getOrNull(selectedDayIndex)
                                if (activeSegment != null) {
                                    Spacer(modifier = Modifier.height(4.dp))

                                    OutlinedTextField(
                                        value = activeSegment.segmentName,
                                        onValueChange = {
                                            daySegments[selectedDayIndex] = activeSegment.copy(segmentName = it)
                                        },
                                        label = { Text("Day ${selectedDayIndex + 1} Title", color = Color(0xFF94A3B8), fontSize = 11.sp) },
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = Color(0xFF0F172A),
                                            unfocusedContainerColor = Color(0xFF0F172A),
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedLabelColor = Color(0xFF38BDF8),
                                            unfocusedLabelColor = Color(0xFF94A3B8),
                                            focusedPlaceholderColor = Color(0xFF64748B),
                                            unfocusedPlaceholderColor = Color(0xFF64748B),
                                            cursorColor = Color(0xFF00F0FF),
                                            focusedBorderColor = Color(0xFF38BDF8),
                                            unfocusedBorderColor = Color(0xFF334155)
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    OutlinedTextField(
                                        value = activeSegment.googleMapsUrl,
                                        onValueChange = {
                                            daySegments[selectedDayIndex] = activeSegment.copy(googleMapsUrl = it)
                                        },
                                        label = { Text("Day ${selectedDayIndex + 1} Google Maps Route Link", color = Color(0xFF94A3B8), fontSize = 11.sp) },
                                        placeholder = { Text("https://maps.app.goo.gl/... or full URL", color = Color(0xFF64748B), fontSize = 11.sp) },
                                        maxLines = 2,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = Color(0xFF0F172A),
                                            unfocusedContainerColor = Color(0xFF0F172A),
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedLabelColor = Color(0xFF38BDF8),
                                            unfocusedLabelColor = Color(0xFF94A3B8),
                                            focusedPlaceholderColor = Color(0xFF64748B),
                                            unfocusedPlaceholderColor = Color(0xFF64748B),
                                            cursorColor = Color(0xFF00F0FF),
                                            focusedBorderColor = Color(0xFF38BDF8),
                                            unfocusedBorderColor = Color(0xFF334155)
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = activeSegment.originName,
                                            onValueChange = {
                                                daySegments[selectedDayIndex] = activeSegment.copy(originName = it)
                                            },
                                            label = { Text("Day Start Leg", color = Color(0xFF94A3B8), fontSize = 11.sp) },
                                            singleLine = true,
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedContainerColor = Color(0xFF0F172A),
                                                unfocusedContainerColor = Color(0xFF0F172A),
                                                focusedTextColor = Color.White,
                                                unfocusedTextColor = Color.White,
                                                focusedLabelColor = Color(0xFF22C55E),
                                                unfocusedLabelColor = Color(0xFF94A3B8),
                                                focusedPlaceholderColor = Color(0xFF64748B),
                                                unfocusedPlaceholderColor = Color(0xFF64748B),
                                                cursorColor = Color(0xFF00F0FF),
                                                focusedBorderColor = Color(0xFF22C55E),
                                                unfocusedBorderColor = Color(0xFF334155)
                                            ),
                                            modifier = Modifier.weight(1f)
                                        )

                                        OutlinedTextField(
                                            value = activeSegment.destinationName,
                                            onValueChange = {
                                                daySegments[selectedDayIndex] = activeSegment.copy(destinationName = it)
                                            },
                                            label = { Text("Day End Leg", color = Color(0xFF94A3B8), fontSize = 11.sp) },
                                            singleLine = true,
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedContainerColor = Color(0xFF0F172A),
                                                unfocusedContainerColor = Color(0xFF0F172A),
                                                focusedTextColor = Color.White,
                                                unfocusedTextColor = Color.White,
                                                focusedLabelColor = Color(0xFFEF4444),
                                                unfocusedLabelColor = Color(0xFF94A3B8),
                                                focusedPlaceholderColor = Color(0xFF64748B),
                                                unfocusedPlaceholderColor = Color(0xFF64748B),
                                                cursorColor = Color(0xFF00F0FF),
                                                focusedBorderColor = Color(0xFFEF4444),
                                                unfocusedBorderColor = Color(0xFF334155)
                                            ),
                                            modifier = Modifier.weight(1f)
                                        )
                                    }

                                    // Day Waypoints
                                    Text("Day ${selectedDayIndex + 1} Stops (${activeSegment.waypoints.size}):", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)

                                    activeSegment.waypoints.forEachIndexed { wIdx, wp ->
                                        Surface(
                                            color = Color(0xFF020617),
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(1.dp, Color(0xFF334155)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                            ) {
                                                Text("${wIdx + 1}. $wp", color = Color.White, fontSize = 12.sp)
                                                IconButton(
                                                    onClick = {
                                                        val updatedWp = activeSegment.waypoints.toMutableList().apply { removeAt(wIdx) }
                                                        daySegments[selectedDayIndex] = activeSegment.copy(waypoints = updatedWp, waypointsCount = updatedWp.size)
                                                    },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(14.dp))
                                                }
                                            }
                                        }
                                    }

                                    // Add Stop to this Day
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        OutlinedTextField(
                                            value = newDayWaypointInput,
                                            onValueChange = { newDayWaypointInput = it },
                                            placeholder = { Text("Add stop to Day ${selectedDayIndex + 1}", fontSize = 11.sp, color = Color(0xFF64748B)) },
                                            singleLine = true,
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedContainerColor = Color(0xFF0F172A),
                                                unfocusedContainerColor = Color(0xFF0F172A),
                                                focusedTextColor = Color.White,
                                                unfocusedTextColor = Color.White,
                                                focusedLabelColor = Color(0xFF38BDF8),
                                                unfocusedLabelColor = Color(0xFF94A3B8),
                                                focusedPlaceholderColor = Color(0xFF64748B),
                                                unfocusedPlaceholderColor = Color(0xFF64748B),
                                                cursorColor = Color(0xFF00F0FF),
                                                focusedBorderColor = Color(0xFF38BDF8),
                                                unfocusedBorderColor = Color(0xFF334155)
                                            ),
                                            modifier = Modifier.weight(1f)
                                        )

                                        Button(
                                            onClick = {
                                                if (newDayWaypointInput.isNotBlank()) {
                                                    val updatedWp = activeSegment.waypoints.toMutableList().apply { add(newDayWaypointInput.trim()) }
                                                    daySegments[selectedDayIndex] = activeSegment.copy(waypoints = updatedWp, waypointsCount = updatedWp.size)
                                                    newDayWaypointInput = ""
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7), contentColor = Color.White),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Text("Add", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Save / Apply Changes Button
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                isResolvingLinks = true
                                try {
                                    // If map links were provided, resolve any short links
                                    val finalSegments = daySegments.mapIndexed { idx, seg ->
                                        var url = seg.googleMapsUrl.trim()
                                        if (url.contains("goo.gl") || url.contains("maps.app")) {
                                            url = withContext(Dispatchers.IO) {
                                                GoogleMapsUrlParser.resolveShortLink(url)
                                            }
                                        }
                                        val parsed = GoogleMapsUrlParser.parseUrl(url)
                                        seg.copy(
                                            googleMapsUrl = url,
                                            originName = if (seg.originName.isNotBlank()) seg.originName else (parsed?.originQuery ?: "Start Leg ${idx + 1}"),
                                            destinationName = if (seg.destinationName.isNotBlank()) seg.destinationName else (parsed?.destinationQuery ?: "End Leg ${idx + 1}"),
                                            waypoints = if (seg.waypoints.isNotEmpty()) seg.waypoints else (parsed?.waypoints ?: emptyList())
                                        )
                                    }

                                    // Build updated ItineraryTripPlan
                                    val itineraryDays = finalSegments.mapIndexed { idx, seg ->
                                        val dayStops = mutableListOf<ItineraryStop>()
                                        dayStops.add(
                                            ItineraryStop(
                                                stopId = "stop_${idx}_0",
                                                stopName = seg.originName,
                                                activityDescription = "Starting Point",
                                                googleMapsUrl = seg.googleMapsUrl,
                                                orderIndex = 0
                                            )
                                        )
                                        seg.waypoints.forEachIndexed { wIdx, wp ->
                                            dayStops.add(
                                                ItineraryStop(
                                                    stopId = "stop_${idx}_${wIdx + 1}",
                                                    stopName = wp,
                                                    activityDescription = "Milestone Stop",
                                                    googleMapsUrl = seg.googleMapsUrl,
                                                    orderIndex = wIdx + 1
                                                )
                                            )
                                        }
                                        dayStops.add(
                                            ItineraryStop(
                                                stopId = "stop_${idx}_${seg.waypoints.size + 1}",
                                                stopName = seg.destinationName,
                                                activityDescription = "Destination",
                                                googleMapsUrl = seg.googleMapsUrl,
                                                orderIndex = seg.waypoints.size + 1
                                            )
                                        )
                                        ItineraryDay(
                                            dayNumber = idx + 1,
                                            dayTitle = seg.segmentName,
                                            stops = dayStops
                                        )
                                    }

                                    val updatedTrip = trip.copy(
                                        title = tripTitle.ifBlank { trip.title },
                                        scheduledDate = tripStartDate.ifBlank { trip.scheduledDate },
                                        originName = originName.ifBlank { trip.originName },
                                        destinationName = destName.ifBlank { trip.destinationName },
                                        waypoints = fullWaypoints.toList(),
                                        routeSegments = finalSegments,
                                        itineraryPlan = ItineraryTripPlan(
                                            planId = trip.itineraryPlan?.planId ?: "plan_${System.currentTimeMillis()}",
                                            creationMode = trip.itineraryPlan?.creationMode ?: TripCreationMode.MANUAL_SEARCH,
                                            tripTitle = tripTitle.ifBlank { trip.title },
                                            startDate = tripStartDate.ifBlank { trip.scheduledDate },
                                            totalDuration = "${finalSegments.size} Days",
                                            days = itineraryDays
                                        )
                                    )

                                    TripRepository.updateTrip(updatedTrip)
                                    onTripUpdated(updatedTrip)
                                    Toast.makeText(context, "Route & Itinerary updated successfully!", Toast.LENGTH_SHORT).show()
                                    onDismiss()
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    Toast.makeText(context, "Error updating route: ${e.message}", Toast.LENGTH_SHORT).show()
                                } finally {
                                    isResolvingLinks = false
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B), contentColor = Color.Black),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        if (isResolvingLinks) {
                            CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Saving & Resolving Route...", fontWeight = FontWeight.Black, fontSize = 14.sp)
                        } else {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("💾 Save Route Updates", fontWeight = FontWeight.Black, fontSize = 14.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}
