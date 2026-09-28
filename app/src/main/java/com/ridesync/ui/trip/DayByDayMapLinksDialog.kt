package com.ridesync.ui.trip

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ridesync.data.model.*
import com.ridesync.engine.GoogleMapsUrlParser
import com.ridesync.ui.theme.HudColors
import com.ridesync.ui.theme.frostedGlassHud
import com.ridesync.ui.theme.hud3dCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class DayMapLinkEntry(
    var dayNumber: Int,
    var dayTitle: String,
    var googleMapsUrl: String
)

/**
 * Option B: Day-by-Day Google Maps Links Builder Dialog.
 * Stepper interface for entering Google Maps links for multiple days (Day 1, Day 2, etc.),
 * resolving shortlinks and waypoints into structured ItineraryDay & TripRouteSegments.
 */
@Composable
fun DayByDayMapLinksDialog(
    initialTripTitle: String = "Multi-Day Tour",
    initialStartDate: String = "15 Oct 2026",
    onDismiss: () -> Unit,
    onItineraryCreated: (tripTitle: String, segments: List<TripRouteSegment>, plan: ItineraryTripPlan) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var tripTitle by remember { mutableStateOf(initialTripTitle) }
    var tripStartDate by remember { mutableStateOf(initialStartDate) }
    val dayEntries = remember {
        mutableStateListOf(
            DayMapLinkEntry(1, "Day 1 Route", ""),
            DayMapLinkEntry(2, "Day 2 Route", "")
        )
    }
    var selectedDayIndex by remember { mutableIntStateOf(0) }
    var isResolvingLinks by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = HudColors.ObsidianModal,
        shape = RoundedCornerShape(24.dp),
        title = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFFF59E0B).copy(alpha = 0.2f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AddLink, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Day-by-Day Map Links",
                                color = HudColors.TextCrispWhite,
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "Option B: Multi-Day Google Maps Links",
                                color = Color(0xFFFBBF24),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = HudColors.TextCoolSilver, modifier = Modifier.size(18.dp))
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Trip Title Input
                OutlinedTextField(
                    value = tripTitle,
                    onValueChange = { tripTitle = it },
                    label = { Text("Trip Title", color = HudColors.TextCoolSilver, fontSize = 12.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFF59E0B),
                        unfocusedBorderColor = Color(0xFF334155)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Start Date Input
                OutlinedTextField(
                    value = tripStartDate,
                    onValueChange = { tripStartDate = it },
                    label = { Text("Start Date", color = HudColors.TextCoolSilver, fontSize = 12.sp) },
                    placeholder = { Text("e.g. 15 Oct 2026") },
                    leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(16.dp)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFF59E0B),
                        unfocusedBorderColor = Color(0xFF334155)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Day Stepper Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Days (${dayEntries.size}):",
                        color = HudColors.TextCrispWhite,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )

                    TextButton(
                        onClick = {
                            val nextDay = dayEntries.size + 1
                            dayEntries.add(DayMapLinkEntry(nextDay, "Day $nextDay Route", ""))
                            selectedDayIndex = dayEntries.size - 1
                        }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+ Add Day", color = Color(0xFF38BDF8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Day Stepper Pills
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    itemsIndexed(dayEntries) { index, entry ->
                        val isSelected = index == selectedDayIndex
                        Box(
                            modifier = Modifier
                                .frostedGlassHud(
                                    shape = RoundedCornerShape(10.dp),
                                    backgroundColor = if (isSelected) Color(0xFFF59E0B).copy(alpha = 0.25f) else Color(0xFF1E293B),
                                    borderColor = if (isSelected) Color(0xFFF59E0B) else Color(0xFF334155)
                                )
                                .clickable { selectedDayIndex = index }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Day ${entry.dayNumber}",
                                color = if (isSelected) Color(0xFFFBBF24) else HudColors.TextCoolSilver,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold
                            )
                        }
                    }
                }

                // Active Day Inputs Card
                val activeEntry = dayEntries.getOrNull(selectedDayIndex)
                if (activeEntry != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFF334155), RoundedCornerShape(14.dp))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Day ${activeEntry.dayNumber} Settings",
                                    color = Color(0xFFFBBF24),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                if (dayEntries.size > 1) {
                                    IconButton(
                                        onClick = {
                                            dayEntries.removeAt(selectedDayIndex)
                                            if (selectedDayIndex >= dayEntries.size) {
                                                selectedDayIndex = (dayEntries.size - 1).coerceAtLeast(0)
                                            }
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete Day", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = activeEntry.dayTitle,
                                onValueChange = {
                                    activeEntry.dayTitle = it
                                    dayEntries[selectedDayIndex] = activeEntry.copy(dayTitle = it)
                                },
                                label = { Text("Day Title / Leg Name", color = HudColors.TextCoolSilver, fontSize = 11.sp) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFF38BDF8),
                                    unfocusedBorderColor = Color(0xFF334155)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = activeEntry.googleMapsUrl,
                                onValueChange = {
                                    activeEntry.googleMapsUrl = it
                                    dayEntries[selectedDayIndex] = activeEntry.copy(googleMapsUrl = it)
                                },
                                label = { Text("Google Maps Route Link (maps.app.goo.gl or full URL)", color = HudColors.TextCoolSilver, fontSize = 11.sp) },
                                placeholder = { Text("https://maps.app.goo.gl/... or google.com/maps/dir/...", color = Color(0xFF64748B), fontSize = 11.sp) },
                                maxLines = 2,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFF38BDF8),
                                    unfocusedBorderColor = Color(0xFF334155)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    coroutineScope.launch {
                        isResolvingLinks = true
                        try {
                            val parsedSegments = mutableListOf<TripRouteSegment>()
                            val itineraryDays = mutableListOf<ItineraryDay>()

                            for ((idx, entry) in dayEntries.withIndex()) {
                                var effectiveUrl = entry.googleMapsUrl.trim()
                                if (effectiveUrl.contains("goo.gl") || effectiveUrl.contains("maps.app")) {
                                    effectiveUrl = withContext(Dispatchers.IO) {
                                        GoogleMapsUrlParser.resolveShortLink(effectiveUrl)
                                    }
                                }

                                val parsed = GoogleMapsUrlParser.parseUrl(effectiveUrl)
                                val originName = parsed?.originQuery ?: "Start Leg ${idx + 1}"
                                val destName = parsed?.destinationQuery ?: "End Leg ${idx + 1}"
                                val waypoints = parsed?.waypoints ?: emptyList()

                                val dayStops = mutableListOf<ItineraryStop>()
                                dayStops.add(
                                    ItineraryStop(
                                        stopId = "stop_${idx}_0",
                                        stopName = originName,
                                        activityDescription = "Leg Start Point",
                                        rawLocationText = originName,
                                        latitude = parsed?.originLat ?: 0.0,
                                        longitude = parsed?.originLng ?: 0.0,
                                        orderIndex = 0
                                    )
                                )

                                waypoints.forEachIndexed { wIdx, wp ->
                                    dayStops.add(
                                        ItineraryStop(
                                            stopId = "stop_${idx}_${wIdx + 1}",
                                            stopName = wp,
                                            activityDescription = "Scheduled Milestone",
                                            rawLocationText = wp,
                                            orderIndex = wIdx + 1
                                        )
                                    )
                                }

                                dayStops.add(
                                    ItineraryStop(
                                        stopId = "stop_${idx}_${dayStops.size}",
                                        stopName = destName,
                                        activityDescription = "Leg Destination",
                                        rawLocationText = destName,
                                        latitude = parsed?.destLat ?: 0.0,
                                        longitude = parsed?.destLng ?: 0.0,
                                        orderIndex = dayStops.size
                                    )
                                )

                                parsedSegments.add(
                                    TripRouteSegment(
                                        segmentId = "seg_${System.currentTimeMillis()}_$idx",
                                        segmentName = entry.dayTitle.ifBlank { "Day ${idx + 1}" },
                                        googleMapsUrl = entry.googleMapsUrl,
                                        originName = originName,
                                        destinationName = destName,
                                        waypoints = waypoints,
                                        waypointsCount = waypoints.size,
                                        orderIndex = idx
                                    )
                                )

                                itineraryDays.add(
                                    ItineraryDay(
                                        dayNumber = idx + 1,
                                        dayTitle = entry.dayTitle.ifBlank { "Day ${idx + 1}" },
                                        stops = dayStops
                                    )
                                )
                            }

                            val plan = ItineraryTripPlan(
                                planId = "plan_${System.currentTimeMillis()}",
                                creationMode = TripCreationMode.MAP_LINKS,
                                tripTitle = tripTitle.ifBlank { "Multi-Day Tour" },
                                startDate = tripStartDate.ifBlank { "15 Oct 2026" },
                                totalDuration = "${dayEntries.size} Days Planned",
                                days = itineraryDays
                            )

                            onItineraryCreated(tripTitle, parsedSegments, plan)
                            onDismiss()
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Toast.makeText(context, "Error resolving map links: ${e.message}", Toast.LENGTH_SHORT).show()
                        } finally {
                            isResolvingLinks = false
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B), contentColor = Color.Black),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isResolvingLinks) {
                    CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Resolving Maps Links...", fontWeight = FontWeight.Black, fontSize = 13.sp)
                } else {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Build Multi-Day Itinerary (${dayEntries.size} Days)", fontWeight = FontWeight.Black, fontSize = 13.sp)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Cancel", color = HudColors.TextCoolSilver, fontSize = 12.sp)
            }
        }
    )
}
