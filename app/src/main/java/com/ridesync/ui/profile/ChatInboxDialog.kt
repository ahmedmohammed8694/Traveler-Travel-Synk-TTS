package com.ridesync.ui.profile

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
import androidx.compose.material.icons.filled.PersonSearch
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
import androidx.compose.ui.window.Dialog
import com.ridesync.data.model.UserProfile
import com.ridesync.data.repository.SocialRepository
import com.ridesync.util.rememberRiderAvatarBitmap
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Full Direct Messages Chat Inbox Dialog.
 * Shows all active traveler conversations, recent message previews, and instant 1-on-1 chat launcher.
 */
@Composable
fun ChatInboxDialog(
    currentUserProfile: UserProfile,
    onOpenChat: (UserProfile) -> Unit,
    onSearchTravelers: () -> Unit,
    onDismiss: () -> Unit
) {
    val allChats by SocialRepository.chatMessagesMap.collectAsState()
    val knownTravelers by SocialRepository.knownTravelers.collectAsState()

    // Find all travelers that have chat history or friends
    val activeChatsList = remember(allChats, knownTravelers, currentUserProfile) {
        val userIdsWithChats = mutableSetOf<String>()
        allChats.keys.forEach { chatId ->
            val parts = chatId.split("_")
            parts.forEach { id ->
                if (id != currentUserProfile.userId && !id.startsWith("trip")) {
                    userIdsWithChats.add(id)
                }
            }
        }
        // Include friends as default quick chat candidates if no chat history yet
        currentUserProfile.friends.forEach { userIdsWithChats.add(it) }

        val result = knownTravelers.filter { userIdsWithChats.contains(it.userId) }.toMutableList()
        if (result.isEmpty()) {
            result.addAll(knownTravelers.take(4))
        }
        result
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Chat, contentDescription = null, tint = Color(0xFF38BDF8))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "💬 Direct Messages & Inbox",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF94A3B8))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Find New Traveler Button
                Button(
                    onClick = {
                        onDismiss()
                        onSearchTravelers()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0052CC), contentColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.PersonSearch, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("🔍 Search Travelers to Chat (Code, Email, Phone)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(14.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 340.dp)
                ) {
                    items(activeChatsList) { traveler ->
                        val chatId = SocialRepository.getChatId(currentUserProfile.userId, traveler.userId)
                        val msgs = allChats[chatId] ?: emptyList()
                        val lastMsg = msgs.lastOrNull()
                        val avatarBitmap by rememberRiderAvatarBitmap(traveler.photoUrl)

                        val timeStr = remember(lastMsg?.timestamp) {
                            if (lastMsg != null) SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(lastMsg.timestamp)) else ""
                        }

                        Surface(
                            color = Color(0xFF0F172A),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFF334155)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onDismiss()
                                    onOpenChat(traveler)
                                }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
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

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = traveler.displayName,
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (timeStr.isNotBlank()) {
                                            Text(timeStr, color = Color(0xFF64748B), fontSize = 10.sp)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(2.dp))

                                    Text(
                                        text = lastMsg?.text ?: "Tap to start conversation with ${traveler.displayName}",
                                        color = if (lastMsg != null) Color(0xFF94A3B8) else Color(0xFF38BDF8),
                                        fontSize = 12.sp,
                                        maxLines = 1
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
