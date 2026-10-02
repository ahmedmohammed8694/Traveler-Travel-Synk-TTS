package com.ridesync.ui.trip

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.window.DialogProperties
import com.ridesync.data.model.SavedTrip
import com.ridesync.data.model.UserProfile
import com.ridesync.data.repository.ChatMessage
import com.ridesync.data.repository.SocialRepository
import com.ridesync.util.rememberRiderAvatarBitmap
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Real-Time Group Chat Dialog for all Trip Convoy Members.
 * Allows members inside a trip to chat, send status alerts, and share location pins.
 */
@Composable
fun TripConvoyChatDialog(
    trip: SavedTrip,
    currentUserProfile: UserProfile,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val groupChatId = remember(trip.tripId) { "trip_group_${trip.tripId}" }

    val allChats by SocialRepository.chatMessagesMap.collectAsState()
    val groupMessages = remember(allChats, groupChatId) {
        allChats[groupChatId] ?: emptyList()
    }

    var messageText by remember { mutableStateOf("") }

    // Scroll to latest message
    LaunchedEffect(groupMessages.size) {
        if (groupMessages.isNotEmpty()) {
            listState.animateScrollToItem(groupMessages.size - 1)
        }
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
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Convoy Bar
                Surface(
                    color = Color(0xFF1E293B),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.Group, contentDescription = null, tint = Color(0xFF38BDF8))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "💬 Convoy Group Chat",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = "${trip.title} • ${trip.joinedRiders.size + 1} Members",
                                    color = Color(0xFF38BDF8),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Quick Rider Status Shortcut Chips (I'm Stopping, Fueling, SOS)
                Surface(
                    color = Color(0xFF1E293B).copy(alpha = 0.8f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    SocialRepository.sendMessage(
                                        senderId = currentUserProfile.userId,
                                        senderName = currentUserProfile.displayName,
                                        senderPhotoUrl = currentUserProfile.photoUrl,
                                        receiverId = groupChatId,
                                        text = "🛑 REST STOP: Pulling over for a short rest break!"
                                    )
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706), contentColor = Color.White),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("🛑 Stop", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    SocialRepository.sendMessage(
                                        senderId = currentUserProfile.userId,
                                        senderName = currentUserProfile.displayName,
                                        senderPhotoUrl = currentUserProfile.photoUrl,
                                        receiverId = groupChatId,
                                        text = "⛽ FUEL STOP: Stopping at upcoming petrol pump."
                                    )
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0052CC), contentColor = Color.White),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("⛽ Fuel", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    SocialRepository.sendMessage(
                                        senderId = currentUserProfile.userId,
                                        senderName = currentUserProfile.displayName,
                                        senderPhotoUrl = currentUserProfile.photoUrl,
                                        receiverId = groupChatId,
                                        text = "🚨 EMERGENCY SOS: Rider needs immediate assistance!"
                                    )
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626), contentColor = Color.White),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("🚨 SOS", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Chat Messages List
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    if (groupMessages.isEmpty()) {
                        item {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 40.dp)
                            ) {
                                Text(
                                    text = "Welcome to the trip group chat! 🎉\nChat live with all riders in ${trip.title}.",
                                    color = Color(0xFF64748B),
                                    fontSize = 13.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }

                    items(groupMessages) { msg ->
                        val isMe = msg.senderId == currentUserProfile.userId
                        val timeStr = remember(msg.timestamp) {
                            SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(msg.timestamp))
                        }
                        val senderAvatarBmp by rememberRiderAvatarBitmap(msg.senderPhotoUrl)

                        Row(
                            horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (!isMe) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF334155))
                                ) {
                                    if (senderAvatarBmp != null) {
                                        Image(
                                            bitmap = senderAvatarBmp!!.asImageBitmap(),
                                            contentDescription = msg.senderName,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                            Text(
                                                text = msg.senderName.take(1).uppercase().ifBlank { "R" },
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                            }

                            Surface(
                                color = if (isMe) Color(0xFF0052CC) else Color(0xFF1E293B),
                                shape = RoundedCornerShape(
                                    topStart = 14.dp,
                                    topEnd = 14.dp,
                                    bottomStart = if (isMe) 14.dp else 4.dp,
                                    bottomEnd = if (isMe) 4.dp else 14.dp
                                ),
                                modifier = Modifier.widthIn(max = 280.dp)
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                    if (!isMe) {
                                        Text(
                                            text = msg.senderName,
                                            color = Color(0xFF38BDF8),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                    }

                                    Text(
                                        text = msg.text,
                                        color = Color.White,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = timeStr,
                                        color = if (isMe) Color(0xFF93C5FD) else Color(0xFF94A3B8),
                                        fontSize = 9.sp,
                                        modifier = Modifier.align(Alignment.End)
                                    )
                                }
                            }
                        }
                    }
                }

                // Input Bar
                Surface(
                    color = Color(0xFF1E293B),
                    modifier = Modifier
                        .fillMaxWidth()
                        .imePadding()
                        .navigationBarsPadding()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        IconButton(onClick = {
                            coroutineScope.launch {
                                SocialRepository.sendMessage(
                                    senderId = currentUserProfile.userId,
                                    senderName = currentUserProfile.displayName,
                                    senderPhotoUrl = currentUserProfile.photoUrl,
                                    receiverId = groupChatId,
                                    text = "📍 Live Convoy Location Pin: ${trip.originName} ➔ ${trip.destinationName}"
                                )
                            }
                        }) {
                            Icon(Icons.Default.LocationOn, contentDescription = "Share Location Pin", tint = Color(0xFF38BDF8))
                        }

                        OutlinedTextField(
                            value = messageText,
                            onValueChange = { messageText = it },
                            placeholder = { Text("Message convoy riders...", color = Color(0xFF64748B), fontSize = 13.sp) },
                            singleLine = true,
                            maxLines = 3,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF0F172A),
                                unfocusedContainerColor = Color(0xFF0F172A),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF0052CC),
                                unfocusedBorderColor = Color(0xFF334155)
                            ),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.weight(1f)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = {
                                if (messageText.isNotBlank()) {
                                    val textToSend = messageText.trim()
                                    messageText = ""
                                    coroutineScope.launch {
                                        SocialRepository.sendMessage(
                                            senderId = currentUserProfile.userId,
                                            senderName = currentUserProfile.displayName,
                                            senderPhotoUrl = currentUserProfile.photoUrl,
                                            receiverId = groupChatId,
                                            text = textToSend
                                        )
                                    }
                                }
                            },
                            modifier = Modifier
                                .size(42.dp)
                                .background(if (messageText.isNotBlank()) Color(0xFF0052CC) else Color(0xFF334155), CircleShape)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}
