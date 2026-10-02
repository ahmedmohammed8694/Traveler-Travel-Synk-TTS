package com.ridesync.ui.profile

import android.widget.Toast
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ridesync.data.model.UserProfile
import com.ridesync.data.repository.SocialRepository
import com.ridesync.ui.theme.HudColors
import com.ridesync.util.rememberRiderAvatarBitmap

/**
 * Full Profile View Screen for any Traveler / Companion.
 * Allows users to:
 * 1. View full profile (Photo, Name, Profile Code TTS-xxxx, Vehicle, Bio).
 * 2. Copy Profile Code.
 * 3. Add as Friend / Accept Friend Request / Unfriend.
 * 4. Follow / Unfollow Traveler.
 * 5. Block / Unblock User.
 * 6. Launch 1-on-1 Direct Chat.
 */
@Composable
fun TravelerProfileDialog(
    traveler: UserProfile,
    currentUserProfile: UserProfile,
    onSaveCurrentUserProfile: (UserProfile) -> Unit,
    onOpenChat: (UserProfile) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val avatarBitmap by rememberRiderAvatarBitmap(traveler.photoUrl)

    var profileState by remember(currentUserProfile) { mutableStateOf(currentUserProfile) }

    val isFriend = remember(profileState.friends, traveler.userId) {
        profileState.friends.contains(traveler.userId)
    }

    val isFriendRequestSent = remember(profileState.friendRequestsSent, traveler.userId) {
        profileState.friendRequestsSent.contains(traveler.userId)
    }

    val isFriendRequestReceived = remember(profileState.friendRequestsReceived, traveler.userId) {
        profileState.friendRequestsReceived.contains(traveler.userId)
    }

    val isFollowing = remember(profileState.following, traveler.userId) {
        profileState.following.contains(traveler.userId)
    }

    val isBlocked = remember(profileState.blockedUsers, traveler.userId) {
        profileState.blockedUsers.contains(traveler.userId)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0F172A))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // Top Header Bar
                Surface(
                    color = Color(0xFF1E293B),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Traveler Profile",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        IconButton(onClick = {
                            val updated = SocialRepository.toggleBlockUser(profileState, traveler.userId)
                            profileState = updated
                            onSaveCurrentUserProfile(updated)
                            val status = if (isBlocked) "Unblocked" else "Blocked"
                            Toast.makeText(context, "${traveler.displayName} $status", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(
                                imageVector = if (isBlocked) Icons.Default.Block else Icons.Default.MoreVert,
                                contentDescription = "Block",
                                tint = if (isBlocked) Color(0xFFEF4444) else Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Profile Avatar Photo Card
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(110.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(110.dp)
                                .clip(CircleShape)
                                .border(3.dp, Color(0xFF0052CC), CircleShape)
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
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Text(
                                        text = traveler.displayName.take(1).uppercase(),
                                        color = Color.White,
                                        fontSize = 40.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = traveler.displayName,
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = traveler.bio.ifBlank { "Passionate Traveler & Motorcyclist" },
                        color = Color(0xFF94A3B8),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Instagram-style Social & Rides Stat Counter Row
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFF334155)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "0",
                                    color = Color(0xFF38BDF8),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Text(text = "Rides", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color(0xFF334155)))

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${traveler.followers.size}",
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Text(text = "Followers", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color(0xFF334155)))

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${traveler.following.size}",
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Text(text = "Following", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color(0xFF334155)))

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${traveler.friends.size}",
                                    color = Color(0xFF22C55E),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Text(text = "Friends", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Profile Code Card (e.g. TTS-8899)
                    Surface(
                        color = Color(0xFF1E293B),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color(0xFF334155)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Column {
                                Text("Traveler Profile Code", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                Text(
                                    text = traveler.safeProfileCode,
                                    color = Color(0xFF38BDF8),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }

                            Button(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(traveler.safeProfileCode))
                                    Toast.makeText(context, "Profile Code Copied: ${traveler.safeProfileCode}", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0052CC), contentColor = Color.White),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Copy Code", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Vehicle & Contact Details Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFF334155)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.TwoWheeler, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Primary Vehicle", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                    Text(
                                        text = traveler.displayVehicleModel.ifBlank { "Motorcycle / Touring Bike" },
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            if (traveler.email.isNotBlank()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Email, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text("Email Address", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                        Text(text = traveler.email, color = Color.White, fontSize = 13.sp)
                                    }
                                }
                            }

                            if (traveler.mobileNumber.isNotBlank()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Phone, contentDescription = null, tint = Color(0xFF22C55E), modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text("Phone Number", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                        Text(text = traveler.mobileNumber, color = Color.White, fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // ACTION BUTTONS GRID (Add Friend, Follow, Chat)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isFriendRequestReceived) {
                            // Accept Friend Request Button
                            Button(
                                onClick = {
                                    val updated = SocialRepository.acceptFriendRequest(profileState, traveler.userId)
                                    profileState = updated
                                    onSaveCurrentUserProfile(updated)
                                    Toast.makeText(context, "Connected as Friends with ${traveler.displayName}!", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A), contentColor = Color.White),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Accept", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            // Decline Friend Request Button
                            Button(
                                onClick = {
                                    val updated = SocialRepository.declineFriendRequest(profileState, traveler.userId)
                                    profileState = updated
                                    onSaveCurrentUserProfile(updated)
                                    Toast.makeText(context, "Declined request", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626), contentColor = Color.White),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Decline", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            // Normal Friend Action Button
                            Button(
                                onClick = {
                                    val updated = when {
                                        isFriend -> SocialRepository.removeFriend(profileState, traveler.userId)
                                        isFriendRequestSent -> profileState
                                        else -> SocialRepository.sendFriendRequest(profileState, traveler.userId)
                                    }
                                    profileState = updated
                                    onSaveCurrentUserProfile(updated)
                                    val msg = when {
                                        isFriend -> "Removed from Friends"
                                        isFriendRequestSent -> "Request already pending"
                                        else -> "Friend request sent to ${traveler.displayName}!"
                                    }
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = when {
                                        isFriend -> Color(0xFF16A34A)
                                        isFriendRequestSent -> Color(0xFF475569)
                                        else -> Color(0xFF0052CC)
                                    },
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                            ) {
                                Icon(
                                    imageVector = when {
                                        isFriend -> Icons.Default.Check
                                        isFriendRequestSent -> Icons.Default.HourglassTop
                                        else -> Icons.Default.PersonAdd
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = when {
                                        isFriend -> "Friends ✓"
                                        isFriendRequestSent -> "Request Sent"
                                        else -> "Add Friend"
                                    },
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Follow Action Button
                        Button(
                            onClick = {
                                val updated = SocialRepository.toggleFollowUser(profileState, traveler.userId)
                                profileState = updated
                                onSaveCurrentUserProfile(updated)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isFollowing) Color(0xFF334155) else Color(0xFF38BDF8),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                        ) {
                            Icon(
                                imageVector = if (isFollowing) Icons.Default.Check else Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isFollowing) "Following" else "Follow",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 1-ON-1 CHAT / SEND MESSAGE BUTTON
                    Button(
                        onClick = {
                            onDismiss()
                            onOpenChat(traveler)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB), contentColor = Color.White),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Send Direct Message / Chat", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}
