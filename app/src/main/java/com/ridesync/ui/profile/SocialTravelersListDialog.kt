package com.ridesync.ui.profile

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.ridesync.data.model.UserProfile
import com.ridesync.data.repository.SocialRepository
import com.ridesync.util.rememberRiderAvatarBitmap

/**
 * Instagram-Style Friends, Followers & Following List Dialog with Search Travelers option.
 */
@Composable
fun SocialTravelersListDialog(
    initialTab: Int = 0, // 0 = Friends, 1 = Followers, 2 = Following, 3 = Search All
    currentUserProfile: UserProfile,
    onSaveCurrentUserProfile: (UserProfile) -> Unit,
    onSelectTraveler: (UserProfile) -> Unit,
    onOpenChat: (UserProfile) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(initialTab) }
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<UserProfile>>(emptyList()) }

    val knownTravelers by SocialRepository.knownTravelers.collectAsState()

    val friendsList = remember(knownTravelers, currentUserProfile.friends) {
        knownTravelers.filter { currentUserProfile.friends.contains(it.userId) }
    }

    val followersList = remember(knownTravelers, currentUserProfile.followers) {
        knownTravelers.filter { currentUserProfile.followers.contains(it.userId) }
    }

    val followingList = remember(knownTravelers, currentUserProfile.following) {
        knownTravelers.filter { currentUserProfile.following.contains(it.userId) }
    }

    LaunchedEffect(searchQuery, selectedTab) {
        if (searchQuery.trim().length >= 2) {
            searchResults = SocialRepository.searchUsers(searchQuery.trim())
        } else {
            searchResults = emptyList()
        }
    }

    val displayList = remember(selectedTab, searchQuery, searchResults, friendsList, followersList, followingList, knownTravelers) {
        if (searchQuery.trim().length >= 2) {
            searchResults
        } else {
            when (selectedTab) {
                0 -> friendsList.ifEmpty { knownTravelers.take(3) }
                1 -> followersList.ifEmpty { knownTravelers.take(2) }
                2 -> followingList.ifEmpty { knownTravelers.take(2) }
                else -> knownTravelers
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, Color(0xFF334155)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "👥 Travelers & Social Network",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF94A3B8))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search Code (TTS-8899), Email, Phone...", color = Color(0xFF64748B), fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF38BDF8)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF0F172A),
                        unfocusedContainerColor = Color(0xFF0F172A),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF0052CC),
                        unfocusedBorderColor = Color(0xFF334155)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Instagram-Style Tab Row
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color(0xFF0F172A),
                    contentColor = Color(0xFF38BDF8)
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Friends (${currentUserProfile.friends.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Followers (${currentUserProfile.followers.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Following (${currentUserProfile.following.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // List of Travelers
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp)
                ) {
                    if (displayList.isEmpty()) {
                        item {
                            Text(
                                text = "No travelers found in this section.",
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }

                    items(displayList) { traveler ->
                        val avatarBitmap by rememberRiderAvatarBitmap(traveler.photoUrl)

                        Surface(
                            color = Color(0xFF0F172A),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFF334155)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onDismiss()
                                    onSelectTraveler(traveler)
                                }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF334155))
                                ) {
                                    if (avatarBitmap != null) {
                                        Image(
                                            bitmap = avatarBitmap!!.asImageBitmap(),
                                            contentDescription = traveler.displayName,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                            Text(
                                                text = traveler.displayName.take(1).uppercase(),
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = traveler.displayName,
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${traveler.safeProfileCode} • ${traveler.displayVehicleModel}",
                                        color = Color(0xFF38BDF8),
                                        fontSize = 11.sp
                                    )
                                }

                                IconButton(onClick = {
                                    onDismiss()
                                    onOpenChat(traveler)
                                }) {
                                    Icon(Icons.Default.Chat, contentDescription = "Chat", tint = Color(0xFF38BDF8), modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
