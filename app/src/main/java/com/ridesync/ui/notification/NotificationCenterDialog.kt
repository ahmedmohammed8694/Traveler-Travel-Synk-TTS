package com.ridesync.ui.notification

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ridesync.data.model.AppNotification
import com.ridesync.data.model.NotificationType
import com.ridesync.data.repository.NotificationRepository
import com.ridesync.ui.theme.HudColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Notifications Center Dialog for TTS (Traveler Travel Synk).
 * Displays real-time notifications aligned with Alpine Pearl & Sapphire Azure UI Theme.
 */
@Composable
fun NotificationCenterDialog(
    onDismiss: () -> Unit
) {
    val notificationsList by NotificationRepository.notifications.collectAsState()
    var selectedFilter by remember { mutableStateOf("ALL") }

    val filteredList = remember(notificationsList, selectedFilter) {
        when (selectedFilter) {
            "SOCIAL" -> notificationsList.filter { it.type == NotificationType.FRIEND_REQUEST || it.type == NotificationType.FRIEND_ACCEPTED }
            "MESSAGES" -> notificationsList.filter { it.type == NotificationType.NEW_MESSAGE }
            "TRIPS" -> notificationsList.filter { it.type == NotificationType.CONVOY_MEMBER_JOINED || it.type == NotificationType.TRIP_UPDATE || it.type == NotificationType.SOS_ALERT }
            else -> notificationsList
        }
    }

    val unreadCount = remember(notificationsList) {
        notificationsList.count { !it.isRead }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(HudColors.ObsidianCanvas)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Header Bar
                Surface(
                    color = HudColors.ObsidianSurface,
                    border = BorderStroke(1.dp, HudColors.ObsidianBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = onDismiss) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = HudColors.TextCrispWhite
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = null,
                                tint = HudColors.CyanPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Notifications Center",
                                    color = HudColors.TextCrispWhite,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = if (unreadCount > 0) "$unreadCount Unread Updates" else "All caught up",
                                    color = HudColors.CyanPrimary,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        if (notificationsList.isNotEmpty()) {
                            TextButton(
                                onClick = { NotificationRepository.markAllAsRead() }
                            ) {
                                Text(
                                    text = "Mark All Read",
                                    color = HudColors.CyanPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Filter Category Chips
                Surface(
                    color = HudColors.ObsidianElevated,
                    border = BorderStroke(1.dp, HudColors.ObsidianBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        listOf("ALL" to "All", "SOCIAL" to "Social", "MESSAGES" to "Messages", "TRIPS" to "Trips").forEach { (key, label) ->
                            val isSel = selectedFilter == key
                            FilterChip(
                                selected = isSel,
                                onClick = { selectedFilter = key },
                                label = {
                                    Text(
                                        text = label,
                                        fontSize = 11.5.sp,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = HudColors.CyanPrimary,
                                    selectedLabelColor = Color.White,
                                    containerColor = HudColors.ObsidianSurface,
                                    labelColor = HudColors.TextCoolSilver
                                ),
                                border = if (!isSel) BorderStroke(1.dp, HudColors.ObsidianBorder) else null,
                                shape = RoundedCornerShape(20.dp)
                            )
                        }
                    }
                }

                // Notifications Feed List
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    if (filteredList.isEmpty()) {
                        item {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 60.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.NotificationsNone,
                                        contentDescription = null,
                                        tint = HudColors.TextMuted,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "No notifications in this tab yet!",
                                        color = HudColors.TextCoolSilver,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    items(filteredList) { item ->
                        val timeFormatted = remember(item.timestamp) {
                            SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(item.timestamp))
                        }

                        Surface(
                            color = HudColors.ObsidianSurface,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(
                                width = if (item.isRead) 1.dp else 1.5.dp,
                                color = if (item.isRead) HudColors.ObsidianBorder else HudColors.CyanPrimary
                            ),
                            shadowElevation = if (item.isRead) 1.dp else 3.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    NotificationRepository.markAsRead(item.id)
                                }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(14.dp)
                            ) {
                                // Notification Type Badge Icon
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when (item.type) {
                                                NotificationType.FRIEND_REQUEST -> Color(0xFF0284C7)
                                                NotificationType.FRIEND_ACCEPTED -> Color(0xFF16A34A)
                                                NotificationType.NEW_MESSAGE -> HudColors.CyanLight
                                                NotificationType.CONVOY_MEMBER_JOINED -> Color(0xFFD97706)
                                                NotificationType.TRIP_UPDATE -> Color(0xFF7C3AED)
                                                NotificationType.SOS_ALERT -> Color(0xFFDC2626)
                                            }
                                        )
                                ) {
                                    Icon(
                                        imageVector = when (item.type) {
                                            NotificationType.FRIEND_REQUEST -> Icons.Default.PersonAdd
                                            NotificationType.FRIEND_ACCEPTED -> Icons.Default.CheckCircle
                                            NotificationType.NEW_MESSAGE -> Icons.AutoMirrored.Filled.Chat
                                            NotificationType.CONVOY_MEMBER_JOINED -> Icons.Default.GroupAdd
                                            NotificationType.TRIP_UPDATE -> Icons.AutoMirrored.Filled.DirectionsBike
                                            NotificationType.SOS_ALERT -> Icons.Default.Warning
                                        },
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
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
                                            text = item.title,
                                            color = HudColors.TextCrispWhite,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = timeFormatted,
                                            color = HudColors.TextMuted,
                                            fontSize = 10.5.sp
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = item.message,
                                        color = HudColors.TextCoolSilver,
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp
                                    )
                                }

                                if (!item.isRead) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(HudColors.CyanPrimary)
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
