package com.ridesync.ui.trip

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ridesync.ui.theme.HudColors
import com.ridesync.ui.theme.frostedGlassHud
import com.ridesync.ui.theme.hud3dCard

/**
 * Unified Trip Creation Options Modal.
 * Prompts the user with 3 distinct setup methods:
 * 1. Option A: Upload Itinerary Document (AI Extraction)
 * 2. Option B: Day-by-Day Google Maps Links (Multi-Day URL Resolver)
 * 3. Option C: Direct In-App Trip Builder (Places Origin & Destination Search)
 */
@Composable
fun TripCreationOptionsModal(
    onSelectUploadDocument: () -> Unit,
    onSelectMapLinks: () -> Unit,
    onSelectManualSearch: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFFFFFFFF),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.border(1.5.dp, Color(0xFF0052CC), RoundedCornerShape(24.dp)),
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(Color(0xFFE0E7FF), CircleShape)
                        .border(1.dp, Color(0xFF0052CC), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Explore,
                        contentDescription = null,
                        tint = Color(0xFF0052CC),
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Create New Trip",
                    color = Color(0xFF0F172A),
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Choose your preferred trip planning setup",
                    color = Color(0xFF475569),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Option A: Upload Itinerary Document
                CreationOptionCard(
                    title = "Option A: Upload Itinerary Document",
                    badge = "AI AUTO-EXTRACT",
                    badgeColor = Color(0xFF0284C7),
                    description = "Upload PDF, Word DOCX, text file, or image (<=10MB). AI automatically extracts days, stops, and coordinates.",
                    icon = Icons.Default.UploadFile,
                    accentColor = Color(0xFF0052CC),
                    onClick = onSelectUploadDocument
                )

                // Option B: Day-by-Day Google Maps Links
                CreationOptionCard(
                    title = "Option B: Day-by-Day Map Links",
                    badge = "MULTI-DAY STEPPER",
                    badgeColor = Color(0xFFD97706),
                    description = "Paste one or multiple Google Maps links for each day (Day 1, Day 2). Automatically extracts stops and waypoints.",
                    icon = Icons.Default.AddLink,
                    accentColor = Color(0xFFD97706),
                    onClick = onSelectMapLinks
                )

                // Option C: Direct In-App Trip Builder (Origin & Destination Search)
                CreationOptionCard(
                    title = "Option C: Direct In-App Search",
                    badge = "INTERACTIVE BUILDER",
                    badgeColor = Color(0xFF16A34A),
                    description = "Search Start & Destination via Places Autocomplete, pick dates, and add stops per day with road directions.",
                    icon = Icons.Default.Search,
                    accentColor = Color(0xFF16A34A),
                    onClick = onSelectManualSearch
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Cancel",
                    color = Color(0xFF64748B),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    )
}

@Composable
private fun CreationOptionCard(
    title: String,
    badge: String,
    badgeColor: Color,
    description: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFFFFFFF), RoundedCornerShape(16.dp))
            .border(1.5.dp, accentColor, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(accentColor.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                    .border(1.dp, accentColor, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = title,
                        color = Color(0xFF0F172A),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Box(
                    modifier = Modifier
                        .background(badgeColor.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = badge,
                        color = badgeColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = description,
                    color = Color(0xFF475569),
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }
        }
    }
}
