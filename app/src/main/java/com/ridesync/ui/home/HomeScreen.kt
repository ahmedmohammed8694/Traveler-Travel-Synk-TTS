package com.ridesync.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ridesync.data.model.SavedTrip
import com.ridesync.data.model.UserProfile
import com.ridesync.data.repository.NotificationRepository
import com.ridesync.ui.theme.HudColors
import com.ridesync.util.rememberRiderAvatarBitmap

/**
 * Main Home Page Dashboard for Traveler Travel Synk (TTS).
 * Strict theme consistency matching 3D Glassy Alpine White & Sapphire Azure theme system.
 * All text elements guaranteed 100% crisp visibility with WCAG AAA legibility.
 */
@Composable
fun HomeScreen(
    userProfile: UserProfile,
    activeTrip: SavedTrip?,
    onNavigateToTab: (Int) -> Unit,
    onOpenNotificationCenter: () -> Unit,
    onOpenChatInbox: () -> Unit,
    onOpenSocialList: (Int) -> Unit, // 0 = Friends, 3 = Search All
    onOpenConvoyChat: (SavedTrip) -> Unit,
    onOpenJoinTrip: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val avatarBitmap by rememberRiderAvatarBitmap(userProfile.photoUrl)
    val notificationsList by NotificationRepository.notifications.collectAsState()
    val unreadCount = remember(notificationsList) { notificationsList.count { !it.isRead } }

    Surface(
        color = HudColors.ObsidianCanvas,
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(HudColors.ObsidianCanvas)
                .verticalScroll(rememberScrollState())
        ) {
            // TOP APP BAR & HEADER
            Surface(
                color = HudColors.ObsidianSurface,
                border = BorderStroke(1.dp, HudColors.ObsidianBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .border(2.dp, HudColors.CyanPrimary, CircleShape)
                                .background(HudColors.ObsidianElevated)
                                .clickable { onNavigateToTab(5) }
                        ) {
                            if (avatarBitmap != null) {
                                Image(
                                    bitmap = avatarBitmap!!.asImageBitmap(),
                                    contentDescription = userProfile.displayName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                    Text(
                                        text = userProfile.displayName.take(1).uppercase().ifBlank { "T" },
                                        color = HudColors.CyanPrimary,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = "Traveler Travel Synk",
                                color = HudColors.CyanPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Hello, ${userProfile.displayName.ifBlank { "Traveler" }} 👋",
                                color = HudColors.TextCrispWhite,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Profile Code Badge
                        Surface(
                            color = HudColors.ObsidianElevated,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, HudColors.ObsidianBorder),
                            modifier = Modifier.padding(end = 10.dp)
                        ) {
                            Text(
                                text = userProfile.safeProfileCode,
                                color = HudColors.CyanPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        // Notification Bell Icon with Badge
                        BadgedBox(
                            badge = {
                                if (unreadCount > 0) {
                                    Badge(
                                        containerColor = Color(0xFFEF4444),
                                        contentColor = Color.White
                                    ) {
                                        Text(
                                            text = if (unreadCount > 9) "9+" else unreadCount.toString(),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        ) {
                            IconButton(onClick = onOpenNotificationCenter) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = "Notifications",
                                    tint = HudColors.TextCrispWhite,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // SOCIAL & RIDES COUNTER STATS ROW
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = HudColors.ObsidianSurface),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, HudColors.ObsidianBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val allTrips = remember { com.ridesync.data.repository.TripRepository.getAllTrips() }
                    val ridesCount = remember(allTrips) { allTrips.size }

                    Row(
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 14.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onNavigateToTab(3) }) {
                            Text("$ridesCount", color = HudColors.CyanPrimary, fontSize = 20.sp, fontWeight = FontWeight.Black)
                            Text("Rides", color = HudColors.TextCoolSilver, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Box(modifier = Modifier.width(1.dp).height(26.dp).background(HudColors.ObsidianBorder))

                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onOpenSocialList(1) }) {
                            Text("${userProfile.followers.size}", color = HudColors.TextCrispWhite, fontSize = 20.sp, fontWeight = FontWeight.Black)
                            Text("Followers", color = HudColors.TextCoolSilver, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Box(modifier = Modifier.width(1.dp).height(26.dp).background(HudColors.ObsidianBorder))

                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onOpenSocialList(2) }) {
                            Text("${userProfile.following.size}", color = HudColors.TextCrispWhite, fontSize = 20.sp, fontWeight = FontWeight.Black)
                            Text("Following", color = HudColors.TextCoolSilver, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Box(modifier = Modifier.width(1.dp).height(26.dp).background(HudColors.ObsidianBorder))

                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onOpenSocialList(0) }) {
                            Text("${userProfile.friends.size}", color = Color(0xFF16A34A), fontSize = 20.sp, fontWeight = FontWeight.Black)
                            Text("Friends", color = HudColors.TextCoolSilver, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // FIND TRAVELER PROFILES SEARCH BANNER
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = HudColors.ObsidianSurface),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.5.dp, HudColors.CyanPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenSocialList(3) }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(HudColors.CyanPrimary)
                        ) {
                            Icon(Icons.Default.PersonSearch, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Find Traveler Profiles",
                                color = HudColors.TextCrispWhite,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Search non-friends by Traveler Code (TTSP8694), Phone, or Email",
                                color = HudColors.TextCoolSilver,
                                fontSize = 11.5.sp
                            )
                        }

                        Icon(Icons.Default.Search, contentDescription = null, tint = HudColors.CyanPrimary, modifier = Modifier.size(22.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ACTIVE / UPCOMING CONVOY TRIP HERO CARD
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                if (activeTrip != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = HudColors.ObsidianSurface),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.5.dp, HudColors.CyanPrimary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF16A34A))
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("ACTIVE CONVOY TRIP", color = Color(0xFF16A34A), fontSize = 11.sp, fontWeight = FontWeight.Black)
                                }

                                Surface(
                                    color = HudColors.CyanPrimary,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = activeTrip.formattedTripCode,
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = activeTrip.title,
                                color = HudColors.TextCrispWhite,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, tint = HudColors.CyanPrimary, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${activeTrip.originName} ➔ ${activeTrip.destinationName}",
                                    color = HudColors.TextCoolSilver,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Button(
                                    onClick = { onNavigateToTab(1) }, // Open Convoy Map
                                    colors = ButtonDefaults.buttonColors(containerColor = HudColors.CyanPrimary, contentColor = Color.White),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1.3f)
                                        .height(44.dp)
                                ) {
                                    Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Open Live Map", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = { onOpenConvoyChat(activeTrip) },
                                    colors = ButtonDefaults.buttonColors(containerColor = HudColors.ObsidianElevated, contentColor = HudColors.TextCrispWhite),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, HudColors.ObsidianBorder),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, tint = HudColors.CyanPrimary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Convoy Chat", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                } else {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = HudColors.ObsidianSurface),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, HudColors.ObsidianBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.DirectionsBike, contentDescription = null, tint = HudColors.CyanPrimary, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No Active Convoy Trip Currently", color = HudColors.TextCrispWhite, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Text("Start a new adventure or join your friends' convoy code.", color = HudColors.TextCoolSilver, fontSize = 12.sp)

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { onNavigateToTab(2) },
                                    colors = ButtonDefaults.buttonColors(containerColor = HudColors.CyanPrimary, contentColor = Color.White),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Create", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = onOpenJoinTrip,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A), contentColor = Color.White),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Join Trip", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = { onNavigateToTab(3) },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = HudColors.CyanPrimary),
                                    border = BorderStroke(1.dp, HudColors.CyanPrimary),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("History", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // QUICK ACTION GRID
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(
                    text = "⚡ Quick Actions",
                    color = HudColors.TextCrispWhite,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(bottom = 10.dp)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // 1. Join Trip (Code / QR)
                    QuickActionCard(
                        title = "Join Trip",
                        subtitle = "Code / QR Scan",
                        icon = Icons.Default.QrCodeScanner,
                        iconTint = Color(0xFF16A34A),
                        modifier = Modifier.weight(1f),
                        onClick = onOpenJoinTrip
                    )

                    // 2. Create Trip
                    QuickActionCard(
                        title = "Create Trip",
                        subtitle = "Route Builder",
                        icon = Icons.Default.AddCircle,
                        iconTint = HudColors.CyanPrimary,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateToTab(2) }
                    )

                    // 3. Trip History
                    QuickActionCard(
                        title = "Trip History",
                        subtitle = "Saved Rides",
                        icon = Icons.Default.History,
                        iconTint = Color(0xFFD97706),
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateToTab(3) }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // 4. Live Map
                    QuickActionCard(
                        title = "Live Map",
                        subtitle = "Convoy GPS",
                        icon = Icons.Default.Map,
                        iconTint = Color(0xFF0284C7),
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateToTab(1) }
                    )

                    // 5. Chats & Messages
                    QuickActionCard(
                        title = "Chats",
                        subtitle = "Direct & Group",
                        icon = Icons.AutoMirrored.Filled.Chat,
                        iconTint = Color(0xFFDB2777),
                        modifier = Modifier.weight(1f),
                        onClick = onOpenChatInbox
                    )

                    // 6. Find Profiles
                    QuickActionCard(
                        title = "Find Profiles",
                        subtitle = "Code / Email",
                        icon = Icons.Default.PersonSearch,
                        iconTint = Color(0xFF9333EA),
                        modifier = Modifier.weight(1f),
                        onClick = { onOpenSocialList(3) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // NOTIFICATION FEED PREVIEW
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "🔔 Recent Notifications & Friend Alerts",
                        color = HudColors.TextCrispWhite,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold
                    )

                    TextButton(onClick = onOpenNotificationCenter) {
                        Text("View All", color = HudColors.CyanPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                if (notificationsList.isEmpty()) {
                    Text("No new notifications.", color = HudColors.TextCoolSilver, fontSize = 13.sp)
                } else {
                    notificationsList.take(3).forEach { item ->
                        Surface(
                            color = HudColors.ObsidianSurface,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, HudColors.ObsidianBorder),
                            shadowElevation = 1.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { onOpenNotificationCenter() }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Icon(
                                    imageVector = when (item.type) {
                                        com.ridesync.data.model.NotificationType.FRIEND_REQUEST -> Icons.Default.PersonAdd
                                        com.ridesync.data.model.NotificationType.FRIEND_ACCEPTED -> Icons.Default.CheckCircle
                                        com.ridesync.data.model.NotificationType.NEW_MESSAGE -> Icons.AutoMirrored.Filled.Chat
                                        else -> Icons.Default.Notifications
                                    },
                                    contentDescription = null,
                                    tint = HudColors.CyanPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.title, color = HudColors.TextCrispWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Text(item.message, color = HudColors.TextCoolSilver, fontSize = 11.5.sp, maxLines = 1)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun QuickActionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = HudColors.ObsidianSurface),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, HudColors.ObsidianBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.clickable { onClick() }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 6.dp)
        ) {
            Icon(icon, contentDescription = title, tint = iconTint, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(6.dp))
            Text(title, color = HudColors.TextCrispWhite, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(subtitle, color = HudColors.TextCoolSilver, fontSize = 9.5.sp, fontWeight = FontWeight.Medium, maxLines = 1)
        }
    }
}
