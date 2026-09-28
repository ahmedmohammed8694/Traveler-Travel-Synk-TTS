package com.ridesync.ui.trip

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ridesync.data.model.TripMetadata
import com.ridesync.data.model.TripRouteSegment

@Composable
fun TripItineraryCards(
    trip: TripMetadata,
    activeSegmentId: String?,
    onSelectSegment: (TripRouteSegment) -> Unit,
    modifier: Modifier = Modifier
) {
    val segments = trip.routeSegments
    val accentColor = com.ridesync.ui.theme.HudColors.CyanPrimary
    val surfaceColor = com.ridesync.ui.theme.HudColors.ObsidianSurface

    if (segments.isEmpty()) {
        Surface(
            color = surfaceColor,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, com.ridesync.ui.theme.HudColors.ObsidianBorder),
            modifier = modifier.fillMaxWidth()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(20.dp)
            ) {
                Icon(imageVector = Icons.Default.Route, contentDescription = null, tint = accentColor, modifier = Modifier.size(36.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Standard Master Route",
                    fontWeight = FontWeight.Bold,
                    color = com.ridesync.ui.theme.HudColors.TextCrispWhite,
                    fontSize = 16.sp
                )
                Text(
                    text = "No custom day segments created yet. Ride the master convoy route.",
                    color = com.ridesync.ui.theme.HudColors.TextCoolSilver,
                    fontSize = 13.sp
                )
            }
        }
        return
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Text(
                text = "Trip Route Schedule (${segments.size} Days)",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = com.ridesync.ui.theme.HudColors.TextCrispWhite
            )
            Surface(
                color = Color(0xFF0284C7).copy(alpha = 0.2f),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color(0xFF0284C7).copy(alpha = 0.5f))
            ) {
                Text(
                    text = "Multi-Day Plan",
                    color = Color(0xFF38BDF8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        segments.forEachIndexed { index, segment ->
            val isActive = (segment.segmentId == activeSegmentId) || (activeSegmentId.isNullOrBlank() && index == 0)
            val cardBorder = if (isActive) BorderStroke(1.5.dp, accentColor) else BorderStroke(1.dp, com.ridesync.ui.theme.HudColors.ObsidianBorder)
            val cardBg = if (isActive) Color(0xFF0F172A) else surfaceColor

            Surface(
                color = cardBg,
                shape = RoundedCornerShape(16.dp),
                border = cardBorder,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .clickable { onSelectSegment(segment) }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = if (isActive) accentColor else Color(0xFF334155),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "${index + 1}",
                                        color = if (isActive) Color.Black else Color.White,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = segment.segmentName.ifBlank { "Day ${index + 1} Route" },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = com.ridesync.ui.theme.HudColors.TextCrispWhite
                                )
                                Text(
                                    text = "${segment.originName.ifBlank { "Origin" }} → ${segment.destinationName.ifBlank { "Destination" }}",
                                    fontSize = 13.sp,
                                    color = com.ridesync.ui.theme.HudColors.TextCoolSilver
                                )
                                if (segment.waypoints.isNotEmpty()) {
                                    Text(
                                        text = "📍 Stops (${segment.waypoints.size}): ${segment.waypoints.joinToString(" ➔ ")}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF38BDF8)
                                    )
                                }
                            }
                        }

                        if (isActive) {
                            Surface(
                                color = accentColor.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, accentColor)
                            ) {
                                Text(
                                    text = "ACTIVE",
                                    color = accentColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Stats row: Distance & Duration
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.DirectionsBike, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${"%.1f".format(segment.distanceKm)} km",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = com.ridesync.ui.theme.HudColors.TextCrispWhite
                            )

                            Spacer(modifier = Modifier.width(16.dp))

                            Icon(imageVector = Icons.Default.Schedule, contentDescription = null, tint = com.ridesync.ui.theme.HudColors.TextCoolSilver, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            val hrs = segment.estimatedDurationMinutes / 60
                            val mins = segment.estimatedDurationMinutes % 60
                            Text(
                                text = "${if (hrs > 0) "${hrs}h " else ""}${mins}m",
                                fontSize = 13.sp,
                                color = com.ridesync.ui.theme.HudColors.TextCoolSilver
                            )
                        }

                        // Load in App Map Button
                        Button(
                            onClick = { onSelectSegment(segment) },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isActive) Color(0xFF10B981) else accentColor,
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isActive) "Active Route" else "Open in Map",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
