package com.ridesync.ui.profile

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
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
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
import com.ridesync.data.model.UserProfile
import com.ridesync.data.repository.ChatMessage
import com.ridesync.data.repository.SocialRepository
import com.ridesync.util.rememberRiderAvatarBitmap
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 1-on-1 Real-Time Direct Traveler Chat Screen.
 * Provides direct messaging with other travelers, profile details, and location pin sharing.
 */
@Composable
fun DirectChatDialog(
    currentUserProfile: UserProfile,
    targetTraveler: UserProfile,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val avatarBitmap by rememberRiderAvatarBitmap(targetTraveler.photoUrl)
    val chatId = remember(currentUserProfile.userId, targetTraveler.userId) {
        SocialRepository.getChatId(currentUserProfile.userId, targetTraveler.userId)
    }

    val allChats by SocialRepository.chatMessagesMap.collectAsState()
    val messagesList = remember(allChats, chatId) {
        allChats[chatId] ?: emptyList()
    }

    var messageText by remember { mutableStateOf("") }

    // Scroll to bottom when new messages arrive
    LaunchedEffect(messagesList.size) {
        if (messagesList.isNotEmpty()) {
            listState.animateScrollToItem(messagesList.size - 1)
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
                // Top Header Bar
                Surface(
                    color = Color(0xFF1E293B),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }

                        // Avatar
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF334155))
                                .border(1.5.dp, Color(0xFF0052CC), CircleShape)
                        ) {
                            if (avatarBitmap != null) {
                                Image(
                                    bitmap = avatarBitmap!!.asImageBitmap(),
                                    contentDescription = targetTraveler.displayName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                    Text(
                                        text = targetTraveler.displayName.take(1).uppercase(),
                                        color = Color.White,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = targetTraveler.displayName,
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${targetTraveler.safeProfileCode} • ${targetTraveler.displayVehicleModel}",
                                color = Color(0xFF38BDF8),
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                // Messages Chat Area
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    if (messagesList.isEmpty()) {
                        item {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 40.dp)
                            ) {
                                Text(
                                    text = "Start conversation with ${targetTraveler.displayName}\nSay Hi! 👋",
                                    color = Color(0xFF64748B),
                                    fontSize = 13.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }

                    items(messagesList) { msg ->
                        val isMe = msg.senderId == currentUserProfile.userId
                        val timeStr = remember(msg.timestamp) {
                            SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(msg.timestamp))
                        }

                        Row(
                            horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                color = if (isMe) Color(0xFF0052CC) else Color(0xFF1E293B),
                                shape = RoundedCornerShape(
                                    topStart = 16.dp,
                                    topEnd = 16.dp,
                                    bottomStart = if (isMe) 16.dp else 4.dp,
                                    bottomEnd = if (isMe) 4.dp else 16.dp
                                ),
                                modifier = Modifier.widthIn(max = 280.dp)
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
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

                // Bottom Input Control Bar
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
                                    receiverId = targetTraveler.userId,
                                    text = "📍 Shared Location Pin: 17.3753° N, 78.4344° E"
                                )
                            }
                        }) {
                            Icon(Icons.Default.LocationOn, contentDescription = "Share Location", tint = Color(0xFF38BDF8))
                        }

                        OutlinedTextField(
                            value = messageText,
                            onValueChange = { messageText = it },
                            placeholder = { Text("Type a message...", color = Color(0xFF64748B), fontSize = 13.sp) },
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
                                            receiverId = targetTraveler.userId,
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
