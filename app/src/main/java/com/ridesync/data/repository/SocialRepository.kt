package com.ridesync.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.google.firebase.firestore.FirebaseFirestore
import com.ridesync.RideSyncApplication
import com.ridesync.data.model.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class ChatMessage(
    val messageId: String = java.util.UUID.randomUUID().toString(),
    val senderId: String = "",
    val senderName: String = "",
    val senderPhotoUrl: String = "",
    val receiverId: String = "",
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

object SocialRepository {

    private val firestore by lazy {
        try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            null
        }
    }

    private val prefs: SharedPreferences? by lazy {
        try {
            RideSyncApplication.appContext.getSharedPreferences("ridesync_social_prefs", Context.MODE_PRIVATE)
        } catch (e: Exception) {
            null
        }
    }

    // Known / Cached Travelers Database for fast search & offline fallback
    private val _knownTravelers = MutableStateFlow<List<UserProfile>>(emptyList())
    val knownTravelers: StateFlow<List<UserProfile>> = _knownTravelers.asStateFlow()

    // All Chat Messages map: key = chatId ("user1_user2"), value = list of messages
    private val _chatMessagesMap = MutableStateFlow<Map<String, List<ChatMessage>>>(emptyMap())
    val chatMessagesMap: StateFlow<Map<String, List<ChatMessage>>> = _chatMessagesMap.asStateFlow()

    init {
        loadDefaultTravelers()
        loadLocalChatHistory()
    }

    private fun loadDefaultTravelers() {
        val sampleTravelers = listOf(
            UserProfile(
                userId = "usr_rahul_101",
                displayName = "Rahul Sharma",
                email = "rahul.rider@gmail.com",
                mobileNumber = "+919876543210",
                profileCode = "TTS-8899",
                photoUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=500",
                vehicleModel = "BMW R1250 GS",
                bio = "Trans-Himalayan Tourer & Lead Navigator ⛰️ 🏍️"
            ),
            UserProfile(
                userId = "usr_ananya_102",
                displayName = "Ananya Roy",
                email = "ananya.roy@outlook.com",
                mobileNumber = "+919812345678",
                profileCode = "TTS-1420",
                photoUrl = "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=500",
                vehicleModel = "Royal Enfield Himalayan 450",
                bio = "Solo Overland Traveler | Western Ghats Explorer 🌲"
            ),
            UserProfile(
                userId = "usr_vikram_103",
                displayName = "Vikramaditya Singh",
                email = "vikram.singh@yahoo.com",
                mobileNumber = "+919988776655",
                profileCode = "TTS-7733",
                photoUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=500",
                vehicleModel = "KTM Adventure 390 Rally",
                bio = "Off-road Rally Rider & Certified First Responder 🚨"
            ),
            UserProfile(
                userId = "usr_priya_104",
                displayName = "Priya Mehta",
                email = "priya.mehta@gmail.com",
                mobileNumber = "+919765432109",
                profileCode = "TTS-5511",
                photoUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=500",
                vehicleModel = "Ducati Multistrada V4",
                bio = "Long-distance Asphalt Cruiser 🛣️"
            )
        )
        _knownTravelers.value = sampleTravelers
    }

    /**
     * Search Travelers by Profile Code (e.g. TTS-8899), Email ID, or Phone Number.
     */
    suspend fun searchUsers(query: String): List<UserProfile> = withContext(Dispatchers.IO) {
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) return@withContext emptyList()

        // 1. Local memory search
        val localMatches = _knownTravelers.value.filter { u ->
            u.safeProfileCode.equals(cleanQuery, ignoreCase = true) ||
                    u.profileCode.equals(cleanQuery, ignoreCase = true) ||
                    u.email.equals(cleanQuery, ignoreCase = true) ||
                    u.mobileNumber.contains(cleanQuery) ||
                    u.displayName.contains(cleanQuery, ignoreCase = true)
        }.toMutableList()

        // 2. Remote Firestore query if connected
        try {
            firestore?.let { db ->
                val snapshotByCode = db.collection("users")
                    .whereEqualTo("profileCode", cleanQuery)
                    .get().await()

                val snapshotByEmail = db.collection("users")
                    .whereEqualTo("email", cleanQuery.lowercase())
                    .get().await()

                val snapshotByPhone = db.collection("users")
                    .whereEqualTo("mobileNumber", cleanQuery)
                    .get().await()

                val allDocs = snapshotByCode.documents + snapshotByEmail.documents + snapshotByPhone.documents
                for (doc in allDocs) {
                    val uid = doc.id
                    val name = doc.getString("displayName") ?: "Traveler"
                    val email = doc.getString("email") ?: ""
                    val phone = doc.getString("mobileNumber") ?: ""
                    val code = doc.getString("profileCode") ?: "TTS-${uid.takeLast(4)}"
                    val photo = doc.getString("photoUrl") ?: ""
                    val bike = doc.getString("vehicleModel") ?: ""

                    if (localMatches.none { it.userId == uid }) {
                        localMatches.add(
                            UserProfile(
                                userId = uid,
                                displayName = name,
                                email = email,
                                mobileNumber = phone,
                                profileCode = code,
                                photoUrl = photo,
                                vehicleModel = bike
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        localMatches.distinctBy { it.userId }
    }

    /**
     * Get or register a traveler profile in the cache.
     */
    fun registerKnownTraveler(user: UserProfile) {
        val current = _knownTravelers.value.toMutableList()
        val existingIdx = current.indexOfFirst { it.userId == user.userId }
        if (existingIdx >= 0) {
            current[existingIdx] = user
        } else {
            current.add(user)
        }
        _knownTravelers.value = current
    }

    /**
     * Social Actions (Friend Requests, Follow, Block).
     */
    fun toggleFollowUser(currentProfile: UserProfile, targetUserId: String): UserProfile {
        val following = currentProfile.following.toMutableList()
        if (following.contains(targetUserId)) {
            following.remove(targetUserId)
        } else {
            following.add(targetUserId)
        }
        return currentProfile.copy(following = following)
    }

    fun sendFriendRequest(currentProfile: UserProfile, targetUserId: String): UserProfile {
        val sent = currentProfile.friendRequestsSent.toMutableList()
        if (!sent.contains(targetUserId)) {
            sent.add(targetUserId)
            NotificationRepository.addNotification(
                com.ridesync.data.model.AppNotification(
                    title = "📩 Friend Request Sent",
                    message = "Friend request sent!",
                    type = com.ridesync.data.model.NotificationType.FRIEND_REQUEST,
                    senderUserId = currentProfile.userId,
                    senderName = currentProfile.displayName
                )
            )
        }
        return currentProfile.copy(friendRequestsSent = sent)
    }

    fun acceptFriendRequest(currentProfile: UserProfile, targetUserId: String): UserProfile {
        val recv = currentProfile.friendRequestsReceived.toMutableList()
        recv.remove(targetUserId)
        val friends = currentProfile.friends.toMutableList()
        if (!friends.contains(targetUserId)) {
            friends.add(targetUserId)
            NotificationRepository.addNotification(
                com.ridesync.data.model.AppNotification(
                    title = "🎉 Friend Request Accepted",
                    message = "You are now connected as friends!",
                    type = com.ridesync.data.model.NotificationType.FRIEND_ACCEPTED,
                    senderUserId = currentProfile.userId,
                    senderName = currentProfile.displayName
                )
            )
        }
        return currentProfile.copy(friendRequestsReceived = recv, friends = friends)
    }

    fun declineFriendRequest(currentProfile: UserProfile, targetUserId: String): UserProfile {
        val recv = currentProfile.friendRequestsReceived.toMutableList()
        recv.remove(targetUserId)
        return currentProfile.copy(friendRequestsReceived = recv)
    }

    fun removeFriend(currentProfile: UserProfile, targetUserId: String): UserProfile {
        val friends = currentProfile.friends.toMutableList()
        friends.remove(targetUserId)
        return currentProfile.copy(friends = friends)
    }

    fun toggleBlockUser(currentProfile: UserProfile, targetUserId: String): UserProfile {
        val blocked = currentProfile.blockedUsers.toMutableList()
        if (blocked.contains(targetUserId)) {
            blocked.remove(targetUserId)
        } else {
            blocked.add(targetUserId)
        }
        return currentProfile.copy(blockedUsers = blocked)
    }

    // ==========================================
    // 1-ON-1 CHAT MESSAGING SYSTEM
    // ==========================================

    fun getChatId(userId1: String, userId2: String): String {
        return if (userId1 < userId2) "${userId1}_$userId2" else "${userId2}_$userId1"
    }

    suspend fun sendMessage(
        senderId: String,
        senderName: String,
        senderPhotoUrl: String,
        receiverId: String,
        text: String
    ) = withContext(Dispatchers.IO) {
        val chatId = getChatId(senderId, receiverId)
        val msg = ChatMessage(
            messageId = java.util.UUID.randomUUID().toString(),
            senderId = senderId,
            senderName = senderName,
            senderPhotoUrl = senderPhotoUrl,
            receiverId = receiverId,
            text = text,
            timestamp = System.currentTimeMillis()
        )

        // Local State Update
        val map = _chatMessagesMap.value.toMutableMap()
        val currentList = map[chatId]?.toMutableList() ?: mutableListOf()
        currentList.add(msg)
        map[chatId] = currentList
        _chatMessagesMap.value = map

        // Save locally
        saveLocalChatHistory()

        // Push real-time notification
        NotificationRepository.addNotification(
            com.ridesync.data.model.AppNotification(
                title = if (receiverId.startsWith("trip_group_")) "💬 Convoy Alert" else "💬 New Message",
                message = "$senderName: $text",
                type = com.ridesync.data.model.NotificationType.NEW_MESSAGE,
                senderUserId = senderId,
                senderName = senderName,
                senderPhotoUrl = senderPhotoUrl
            )
        )

        // Sync with Firestore if available
        try {
            firestore?.collection("chats")?.document(chatId)?.collection("messages")
                ?.document(msg.messageId)?.set(msg)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveLocalChatHistory() {
        try {
            val jsonObj = JSONObject()
            _chatMessagesMap.value.forEach { (chatId, list) ->
                val arr = JSONArray()
                list.forEach { m ->
                    arr.put(JSONObject().apply {
                        put("messageId", m.messageId)
                        put("senderId", m.senderId)
                        put("senderName", m.senderName)
                        put("senderPhotoUrl", m.senderPhotoUrl)
                        put("receiverId", m.receiverId)
                        put("text", m.text)
                        put("timestamp", m.timestamp)
                    })
                }
                jsonObj.put(chatId, arr)
            }
            prefs?.edit()?.putString("chats_json", jsonObj.toString())?.apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadLocalChatHistory() {
        try {
            val jsonStr = prefs?.getString("chats_json", null) ?: return
            val jsonObj = JSONObject(jsonStr)
            val map = mutableMapOf<String, List<ChatMessage>>()
            val keys = jsonObj.keys()
            while (keys.hasNext()) {
                val chatId = keys.next()
                val arr = jsonObj.getJSONArray(chatId)
                val list = mutableListOf<ChatMessage>()
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    list.add(
                        ChatMessage(
                            messageId = o.optString("messageId"),
                            senderId = o.optString("senderId"),
                            senderName = o.optString("senderName"),
                            senderPhotoUrl = o.optString("senderPhotoUrl"),
                            receiverId = o.optString("receiverId"),
                            text = o.optString("text"),
                            timestamp = o.optLong("timestamp")
                        )
                    )
                }
                map[chatId] = list
            }
            _chatMessagesMap.value = map
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
