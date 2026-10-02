package com.ridesync.ui.profile

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import kotlinx.coroutines.launch

/**
 * Travelers & Social Network Search Dialog.
 * Beautiful Dark Obsidian & Sapphire Azure UI matching application frontend theme.
 * Handles Friends List, Followers, Following, Friend Requests (Accept/Decline), and Search All.
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
    val coroutineScope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(initialTab) }
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }

    val knownTravelers by SocialRepository.knownTravelers.collectAsState()

    // Ensure all connected traveler profiles (friends, followers, following, pending requests) are fetched
    LaunchedEffect(currentUserProfile) {
        val allIds = (currentUserProfile.friends +
                currentUserProfile.followers +
                currentUserProfile.following +
                currentUserProfile.friendRequestsReceived +
                currentUserProfile.friendRequestsSent).distinct()
        if (allIds.isNotEmpty()) {
            SocialRepository.fetchProfilesByIds(allIds)
        }
    }

    val friendsList = remember(knownTravelers, currentUserProfile.friends) {
        knownTravelers.filter { currentUserProfile.friends.contains(it.userId) }
    }

    val pendingRequestsList = remember(knownTravelers, currentUserProfile.friendRequestsReceived) {
        knownTravelers.filter { currentUserProfile.friendRequestsReceived.contains(it.userId) }
    }

    val followersList = remember(knownTravelers, currentUserProfile.followers) {
        knownTravelers.filter { currentUserProfile.followers.contains(it.userId) }
    }

    val followingList = remember(knownTravelers, currentUserProfile.following) {
        knownTravelers.filter { currentUserProfile.following.contains(it.userId) }
    }

    LaunchedEffect(searchQuery, selectedTab) {
        if (searchQuery.trim().length >= 2) {
            isSearching = true
            searchResults = SocialRepository.searchUsers(searchQuery.trim())
            isSearching = false
        } else {
            searchResults = emptyList()
        }
    }

    val displayList = remember(selectedTab, searchQuery, searchResults, friendsList, followersList, followingList) {
        if (searchQuery.trim().length >= 2) {
            searchResults
        } else {
            when (selectedTab) {
                0 -> friendsList
                1 -> followersList
                2 -> followingList
                else -> if (searchQuery.isBlank()) emptyList() else searchResults
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.5.dp, Color(0xFF38BDF8)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
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
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF94A3B8))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Search Bar
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = {
                            searchQuery = it
                            if (selectedTab != 3 && it.isNotBlank()) {
                                selectedTab = 3
                            }
                        },
                        placeholder = { Text("Enter Email, Code (TTSP8694), Phone...", color = Color(0xFF94A3B8), fontSize = 11.5.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF38BDF8)) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF1E293B),
                            unfocusedContainerColor = Color(0xFF1E293B),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedBorderColor = Color(0xFF334155)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            if (searchQuery.isNotBlank()) {
                                selectedTab = 3
                                coroutineScope.launch {
                                    isSearching = true
                                    searchResults = SocialRepository.searchUsers(searchQuery.trim())
                                    isSearching = false
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0052CC), contentColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        if (isSearching) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Search", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 4-Tab Row (Friends, Followers, Following, Search All)
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color(0xFF1E293B),
                    contentColor = Color(0xFF38BDF8),
                    indicator = { tabPositions ->
                        if (selectedTab < tabPositions.size) {
                            TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                color = Color(0xFF38BDF8)
                            )
                        }
                    }
                ) {
                    val tabs = listOf(
                        "Friends (${friendsList.size})",
                        "Followers (${followersList.size})",
                        "Following (${followingList.size})",
                        "Search All"
                    )
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    text = title,
                                    fontSize = 10.5.sp,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selectedTab == index) Color(0xFF38BDF8) else Color(0xFF94A3B8)
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (isSearching) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(color = Color(0xFF38BDF8), modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Searching Live Database...", color = Color(0xFF38BDF8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // List of Travelers
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 340.dp)
                ) {
                    // Pending Friend Requests Section (Only shown in Friends tab when there are incoming requests)
                    if (selectedTab == 0 && searchQuery.isBlank() && pendingRequestsList.isNotEmpty()) {
                        item {
                            Surface(
                                color = Color(0xFF1E293B),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color(0xFFF59E0B)),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("📩 Friend Requests", color = Color(0xFFF59E0B), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(color = Color(0xFFF59E0B), shape = CircleShape) {
                                            Text(
                                                text = "${pendingRequestsList.size}",
                                                color = Color.Black,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Black,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    pendingRequestsList.forEach { sender ->
                                        val reqAvatarBmp by rememberRiderAvatarBitmap(sender.photoUrl)
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFF0F172A))
                                                    .border(1.dp, Color(0xFFF59E0B), CircleShape)
                                            ) {
                                                if (reqAvatarBmp != null) {
                                                    Image(bitmap = reqAvatarBmp!!.asImageBitmap(), contentDescription = sender.displayName, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                                } else {
                                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                                        Text(sender.displayName.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }

                                            Spacer(modifier = Modifier.width(8.dp))

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(sender.displayName, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                                Text("${sender.safeProfileCode} • ${sender.displayVehicleModel}", color = Color(0xFF94A3B8), fontSize = 10.5.sp)
                                            }

                                            // Accept Request Button
                                            Button(
                                                onClick = {
                                                    val updated = SocialRepository.acceptFriendRequest(currentUserProfile, sender.userId)
                                                    onSaveCurrentUserProfile(updated)
                                                    Toast.makeText(context, "Connected as Friends with ${sender.displayName}!", Toast.LENGTH_SHORT).show()
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981), contentColor = Color.White),
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                            ) {
                                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(13.dp))
                                                Spacer(modifier = Modifier.width(2.dp))
                                                Text("Accept", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }

                                            Spacer(modifier = Modifier.width(4.dp))

                                            // Decline Request Button
                                            IconButton(
                                                onClick = {
                                                    val updated = SocialRepository.declineFriendRequest(currentUserProfile, sender.userId)
                                                    onSaveCurrentUserProfile(updated)
                                                },
                                                modifier = Modifier.size(30.dp)
                                            ) {
                                                Icon(Icons.Default.Close, contentDescription = "Decline", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (displayList.isEmpty() && !isSearching) {
                        item {
                            Text(
                                text = if (searchQuery.isNotBlank()) "No travelers found matching '$searchQuery'. Double-check Email ID, Phone Number, or Profile Code (e.g. TTSP8694)." else when (selectedTab) {
                                    0 -> "No friends added yet. Search travelers by Profile Code (TTSP8694), Phone, or Email to add friends!"
                                    1 -> "No followers yet. Share your Profile Code with travelers to gain followers!"
                                    2 -> "Not following anyone yet. Search travelers to follow them!"
                                    else -> "Enter Email ID, Phone, or Code above to search live database."
                                },
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }

                    items(displayList) { traveler ->
                        val avatarBitmap by rememberRiderAvatarBitmap(traveler.photoUrl)
                        val isFriend = currentUserProfile.friends.contains(traveler.userId)
                        val isFollowing = currentUserProfile.following.contains(traveler.userId)
                        val isFollower = currentUserProfile.followers.contains(traveler.userId)
                        val isReqSent = currentUserProfile.friendRequestsSent.contains(traveler.userId)

                        Surface(
                            color = Color(0xFF1E293B),
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
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF0F172A))
                                        .border(1.5.dp, Color(0xFF38BDF8), CircleShape)
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
                                                fontWeight = FontWeight.Black
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
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                // Follow / Friend Action Buttons
                                if (traveler.userId != currentUserProfile.userId) {
                                    if (isFriend) {
                                        Surface(
                                            color = Color(0xFF10B981).copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(1.dp, Color(0xFF10B981))
                                        ) {
                                            Text("Friend ✓", color = Color(0xFF10B981), fontSize = 10.5.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                        }
                                    } else if (isReqSent) {
                                        Surface(
                                            color = Color(0xFFF59E0B).copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(1.dp, Color(0xFFF59E0B))
                                        ) {
                                            Text("Requested", color = Color(0xFFF59E0B), fontSize = 10.5.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                        }
                                    } else {
                                        Button(
                                            onClick = {
                                                val updated = SocialRepository.sendFriendRequest(currentUserProfile, traveler.userId)
                                                onSaveCurrentUserProfile(updated)
                                                Toast.makeText(context, "Friend Request Sent to ${traveler.displayName}!", Toast.LENGTH_SHORT).show()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0052CC), contentColor = Color.White),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text("+ Friend", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(6.dp))

                                    // Follow / Unfollow Button
                                    Button(
                                        onClick = {
                                            val updated = SocialRepository.toggleFollowUser(currentUserProfile, traveler.userId)
                                            onSaveCurrentUserProfile(updated)
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isFollowing) Color(0xFF334155) else Color(0xFF2563EB),
                                            contentColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(if (isFollowing) "Following" else if (isFollower) "Follow Back" else "Follow", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Spacer(modifier = Modifier.width(6.dp))
                                }

                                // Direct Chat Button
                                IconButton(
                                    onClick = {
                                        onDismiss()
                                        onOpenChat(traveler)
                                    },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(Color(0xFF2563EB).copy(alpha = 0.25f), CircleShape)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Chat", tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
