package com.ridesync.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.google.firebase.firestore.FirebaseFirestore
import com.ridesync.RideSyncApplication
import com.ridesync.data.model.UserProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL

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
        _knownTravelers.value = emptyList()
    }

    /**
     * Search Travelers by Profile Code (e.g. TTSP8694), Email ID, Phone Number, or Display Name.
     * Searches local cache, Supabase Postgres Database, Cloudflare Edge, and Firestore.
     * Returns empty list if no user is found (no fake data creation).
     */
    suspend fun searchUsers(query: String): List<UserProfile> = withContext(Dispatchers.IO) {
        val rawClean = query.trim()
        if (rawClean.isBlank()) return@withContext emptyList()

        val cleanQuery = rawClean.lowercase()
        val digitsOnly = rawClean.filter { it.isDigit() }
        val resultsList = mutableListOf<UserProfile>()

        // Helper function for comprehensive multi-field matching
        fun matchesUserQuery(user: UserProfile): Boolean {
            val userEmail = user.email.trim().lowercase()
            val userPhoneDigits = user.mobileNumber.filter { it.isDigit() }
            val userCode = user.safeProfileCode.trim().uppercase()
            val userCodeDigits = userCode.filter { it.isDigit() }
            val userRawCode = user.profileCode.trim().uppercase()
            val userRawCodeDigits = userRawCode.filter { it.isDigit() }
            val userName = user.displayName.trim().lowercase()
            val userId = user.userId.trim().lowercase()

            // 1. Email match (substring / exact)
            if (userEmail.isNotBlank() && userEmail.contains(cleanQuery)) return true

            // 2. Display Name match
            if (userName.isNotBlank() && userName.contains(cleanQuery)) return true

            // 3. User ID match
            if (userId.isNotBlank() && userId.contains(cleanQuery)) return true

            // 4. Mobile Number match (raw or digits)
            if (user.mobileNumber.isNotBlank() && user.mobileNumber.contains(rawClean)) return true
            if (digitsOnly.isNotBlank() && userPhoneDigits.isNotBlank() && userPhoneDigits.contains(digitsOnly)) return true

            // 5. Profile Code match (formatted e.g. TTSP8694 or raw)
            if (userCode.contains(cleanQuery.uppercase()) || userRawCode.contains(cleanQuery.uppercase())) return true

            // 6. Profile Code Digits match (e.g. 8694 matches TTSP8694)
            if (digitsOnly.isNotBlank() && digitsOnly.length >= 2) {
                if (userCodeDigits.contains(digitsOnly) || userRawCodeDigits.contains(digitsOnly)) return true
            }

            return false
        }

        // 1. Local memory search (known travelers & current active user)
        _knownTravelers.value.forEach { traveler ->
            if (matchesUserQuery(traveler)) {
                if (resultsList.none { it.userId == traveler.userId }) {
                    resultsList.add(traveler)
                }
            }
        }

        // 2. Remote Supabase REST API Database query
        try {
            val url = URL("https://oktfyxdrvscmifomtlkp.supabase.co/rest/v1/users?select=*")
            val conn = url.openConnection() as java.net.HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("apikey", "sb_publishable_dF8gDIF6Ahw4wWPRfYOH7Q_AaJrBrO8")
            conn.setRequestProperty("Authorization", "Bearer sb_publishable_dF8gDIF6Ahw4wWPRfYOH7Q_AaJrBrO8")
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            if (conn.responseCode in 200..299) {
                val responseText = conn.inputStream.bufferedReader().readText()
                val jsonArr = JSONArray(responseText)
                for (i in 0 until jsonArr.length()) {
                    val obj = jsonArr.getJSONObject(i)
                    val uid = obj.optString("uid", obj.optString("user_id", ""))
                    val name = obj.optString("display_name", obj.optString("displayName", "Traveler"))
                    val email = obj.optString("email", "")
                    val phone = obj.optString("mobile_number", obj.optString("mobileNumber", ""))
                    val code = obj.optString("profile_code", obj.optString("profileCode", "TTSP${uid.takeLast(4)}"))
                    val photo = obj.optString("photo_url", obj.optString("photoUrl", ""))
                    val bike = obj.optString("vehicle_model", obj.optString("vehicleModel", ""))
                    
                    if (uid.isNotBlank()) {
                        val user = UserProfile(
                            userId = uid,
                            displayName = name,
                            email = email,
                            mobileNumber = phone,
                            profileCode = code,
                            photoUrl = photo,
                            vehicleModel = bike
                        )
                        if (matchesUserQuery(user)) {
                            registerKnownTraveler(user)
                            if (resultsList.none { it.userId == uid }) {
                                resultsList.add(user)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 3. Remote Cloudflare Edge API query
        try {
            val encodedQuery = java.net.URLEncoder.encode(rawClean, "UTF-8")
            val cfUrlStr = "https://ahmedmohammed8694-riders-ride-sync.mdahmed08061994.workers.dev/api/auth/profile?email=$encodedQuery"
            val url = URL(cfUrlStr)
            val conn = url.openConnection() as java.net.HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            if (conn.responseCode in 200..299) {
                val responseText = conn.inputStream.bufferedReader().readText()
                val json = JSONObject(responseText)
                val prof = json.optJSONObject("profile")
                if (prof != null) {
                    val uid = prof.optString("userId", "")
                    val name = prof.optString("displayName", "Traveler")
                    val email = prof.optString("email", "")
                    val phone = prof.optString("mobileNumber", "")
                    val code = prof.optString("profileCode", "TTSP${uid.takeLast(4)}")
                    val photo = prof.optString("photoUrl", "")
                    val bike = prof.optString("vehicleModel", "")
                    if (uid.isNotBlank()) {
                        val user = UserProfile(
                            userId = uid,
                            displayName = name,
                            email = email,
                            mobileNumber = phone,
                            profileCode = code,
                            photoUrl = photo,
                            vehicleModel = bike
                        )
                        if (matchesUserQuery(user)) {
                            registerKnownTraveler(user)
                            if (resultsList.none { it.userId == uid }) {
                                resultsList.add(user)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 4. Remote Firestore query if connected
        try {
            firestore?.let { db ->
                val snapshotAll = db.collection("users").get().await()
                for (doc in snapshotAll.documents) {
                    val uid = doc.id
                    val name = doc.getString("displayName") ?: "Traveler"
                    val email = doc.getString("email") ?: ""
                    val phone = doc.getString("mobileNumber") ?: ""
                    val code = doc.getString("profileCode") ?: "TTSP${uid.takeLast(4)}"
                    val photo = doc.getString("photoUrl") ?: ""
                    val bike = doc.getString("vehicleModel") ?: ""

                    val user = UserProfile(
                        userId = uid,
                        displayName = name,
                        email = email,
                        mobileNumber = phone,
                        profileCode = code,
                        photoUrl = photo,
                        vehicleModel = bike
                    )
                    if (matchesUserQuery(user)) {
                        registerKnownTraveler(user)
                        if (resultsList.none { it.userId == uid }) {
                            resultsList.add(user)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        resultsList.distinctBy { it.userId }
    }

    private fun parseDisplayNameFromQuery(query: String): String {
        val handle = if (query.contains("@")) query.substringBefore("@") else query
        val clean = handle.replace(Regex("[0-9_.]"), " ").trim()
        return if (clean.isNotBlank()) {
            clean.split(" ").filter { it.isNotBlank() }.joinToString(" ") { it.lowercase().replaceFirstChar { char -> char.uppercase() } }
        } else {
            "Traveler ($handle)"
        }
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
     * Fetch traveler profile details by list of User IDs from local cache & Firestore.
     */
    suspend fun fetchProfilesByIds(userIds: List<String>): List<UserProfile> = withContext(Dispatchers.IO) {
        if (userIds.isEmpty()) return@withContext emptyList()
        val missingIds = userIds.filter { id -> _knownTravelers.value.none { it.userId == id } }
        if (missingIds.isNotEmpty()) {
            try {
                firestore?.let { db ->
                    for (id in missingIds) {
                        val doc = db.collection("users").document(id).get().await()
                        if (doc.exists()) {
                            val name = doc.getString("displayName") ?: "Traveler"
                            val email = doc.getString("email") ?: ""
                            val phone = doc.getString("mobileNumber") ?: ""
                            val code = doc.getString("profileCode") ?: "TTSP${id.takeLast(4)}"
                            val photo = doc.getString("photoUrl") ?: ""
                            val bike = doc.getString("vehicleModel") ?: ""
                            val friends = (doc.get("friends") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
                            val followers = (doc.get("followers") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
                            val following = (doc.get("following") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
                            val reqSent = (doc.get("friendRequestsSent") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
                            val reqRecv = (doc.get("friendRequestsReceived") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()

                            val loadedUser = UserProfile(
                                userId = id,
                                displayName = name,
                                email = email,
                                mobileNumber = phone,
                                profileCode = code,
                                photoUrl = photo,
                                vehicleModel = bike,
                                friends = friends,
                                followers = followers,
                                following = following,
                                friendRequestsSent = reqSent,
                                friendRequestsReceived = reqRecv
                            )
                            registerKnownTraveler(loadedUser)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        _knownTravelers.value.filter { userIds.contains(it.userId) }
    }

    /**
     * Sync user profile changes to local cache and Firestore.
     */
    suspend fun syncProfileToRemote(user: UserProfile) = withContext(Dispatchers.IO) {
        registerKnownTraveler(user)
        try {
            firestore?.collection("users")?.document(user.userId)?.set(user)?.await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Social Actions (Friend Requests, Follow, Block).
     * Handles bi-directional updates:
     * - When A follows B: A's following gets B, B's followers gets A.
     * - When A unfollows B: A's following loses B, B's followers loses A.
     */
    fun toggleFollowUser(currentProfile: UserProfile, targetUserId: String): UserProfile {
        val following = currentProfile.following.toMutableList()
        val isFollowing = following.contains(targetUserId)
        if (isFollowing) {
            following.remove(targetUserId)
        } else {
            following.add(targetUserId)
        }
        val updatedCurrent = currentProfile.copy(following = following)
        registerKnownTraveler(updatedCurrent)

        // Asynchronously update target user's followers list & remote DB
        CoroutineScope(Dispatchers.IO).launch {
            syncProfileToRemote(updatedCurrent)
            val targetUser = _knownTravelers.value.firstOrNull { it.userId == targetUserId }
                ?: fetchProfilesByIds(listOf(targetUserId)).firstOrNull()
            if (targetUser != null) {
                val targetFollowers = targetUser.followers.toMutableList()
                if (isFollowing) {
                    targetFollowers.remove(currentProfile.userId)
                } else {
                    if (!targetFollowers.contains(currentProfile.userId)) {
                        targetFollowers.add(currentProfile.userId)
                    }
                }
                val updatedTarget = targetUser.copy(followers = targetFollowers)
                syncProfileToRemote(updatedTarget)
            }
        }

        return updatedCurrent
    }

    fun sendFriendRequest(currentProfile: UserProfile, targetUserId: String): UserProfile {
        val sent = currentProfile.friendRequestsSent.toMutableList()
        if (!sent.contains(targetUserId)) {
            sent.add(targetUserId)
        }
        val updatedCurrent = currentProfile.copy(friendRequestsSent = sent)
        registerKnownTraveler(updatedCurrent)

        CoroutineScope(Dispatchers.IO).launch {
            syncProfileToRemote(updatedCurrent)
            val targetUser = _knownTravelers.value.firstOrNull { it.userId == targetUserId }
                ?: fetchProfilesByIds(listOf(targetUserId)).firstOrNull()
            if (targetUser != null) {
                val targetRecv = targetUser.friendRequestsReceived.toMutableList()
                if (!targetRecv.contains(currentProfile.userId)) {
                    targetRecv.add(currentProfile.userId)
                }
                val updatedTarget = targetUser.copy(friendRequestsReceived = targetRecv)
                syncProfileToRemote(updatedTarget)
            }

            NotificationRepository.addNotification(
                com.ridesync.data.model.AppNotification(
                    title = "📩 Friend Request Received",
                    message = "${currentProfile.displayName} sent you a friend request!",
                    type = com.ridesync.data.model.NotificationType.FRIEND_REQUEST,
                    senderUserId = currentProfile.userId,
                    senderName = currentProfile.displayName,
                    senderPhotoUrl = currentProfile.photoUrl
                )
            )
        }

        return updatedCurrent
    }

    fun acceptFriendRequest(currentProfile: UserProfile, targetUserId: String): UserProfile {
        val recv = currentProfile.friendRequestsReceived.toMutableList()
        recv.remove(targetUserId)
        val friends = currentProfile.friends.toMutableList()
        if (!friends.contains(targetUserId)) {
            friends.add(targetUserId)
        }
        val updatedCurrent = currentProfile.copy(friendRequestsReceived = recv, friends = friends)
        registerKnownTraveler(updatedCurrent)

        CoroutineScope(Dispatchers.IO).launch {
            syncProfileToRemote(updatedCurrent)
            val targetUser = _knownTravelers.value.firstOrNull { it.userId == targetUserId }
                ?: fetchProfilesByIds(listOf(targetUserId)).firstOrNull()
            if (targetUser != null) {
                val targetSent = targetUser.friendRequestsSent.toMutableList()
                targetSent.remove(currentProfile.userId)
                val targetFriends = targetUser.friends.toMutableList()
                if (!targetFriends.contains(currentProfile.userId)) {
                    targetFriends.add(currentProfile.userId)
                }
                val updatedTarget = targetUser.copy(friendRequestsSent = targetSent, friends = targetFriends)
                syncProfileToRemote(updatedTarget)
            }

            NotificationRepository.addNotification(
                com.ridesync.data.model.AppNotification(
                    title = "🎉 Friend Request Accepted",
                    message = "${currentProfile.displayName} accepted your friend request! You are now connected as friends.",
                    type = com.ridesync.data.model.NotificationType.FRIEND_ACCEPTED,
                    senderUserId = currentProfile.userId,
                    senderName = currentProfile.displayName,
                    senderPhotoUrl = currentProfile.photoUrl
                )
            )
        }

        return updatedCurrent
    }

    fun declineFriendRequest(currentProfile: UserProfile, targetUserId: String): UserProfile {
        val recv = currentProfile.friendRequestsReceived.toMutableList()
        recv.remove(targetUserId)
        val updatedCurrent = currentProfile.copy(friendRequestsReceived = recv)
        registerKnownTraveler(updatedCurrent)

        CoroutineScope(Dispatchers.IO).launch {
            syncProfileToRemote(updatedCurrent)
            val targetUser = _knownTravelers.value.firstOrNull { it.userId == targetUserId }
                ?: fetchProfilesByIds(listOf(targetUserId)).firstOrNull()
            if (targetUser != null) {
                val targetSent = targetUser.friendRequestsSent.toMutableList()
                targetSent.remove(currentProfile.userId)
                val updatedTarget = targetUser.copy(friendRequestsSent = targetSent)
                syncProfileToRemote(updatedTarget)
            }
        }

        return updatedCurrent
    }

    fun removeFriend(currentProfile: UserProfile, targetUserId: String): UserProfile {
        val friends = currentProfile.friends.toMutableList()
        friends.remove(targetUserId)
        val updatedCurrent = currentProfile.copy(friends = friends)
        registerKnownTraveler(updatedCurrent)

        CoroutineScope(Dispatchers.IO).launch {
            syncProfileToRemote(updatedCurrent)
            val targetUser = _knownTravelers.value.firstOrNull { it.userId == targetUserId }
                ?: fetchProfilesByIds(listOf(targetUserId)).firstOrNull()
            if (targetUser != null) {
                val targetFriends = targetUser.friends.toMutableList()
                targetFriends.remove(currentProfile.userId)
                val updatedTarget = targetUser.copy(friends = targetFriends)
                syncProfileToRemote(updatedTarget)
            }
        }

        return updatedCurrent
    }

    fun toggleBlockUser(currentProfile: UserProfile, targetUserId: String): UserProfile {
        val blocked = currentProfile.blockedUsers.toMutableList()
        if (blocked.contains(targetUserId)) {
            blocked.remove(targetUserId)
        } else {
            blocked.add(targetUserId)
        }
        val updatedCurrent = currentProfile.copy(blockedUsers = blocked)
        registerKnownTraveler(updatedCurrent)
        return updatedCurrent
    }

    // ==========================================
    // 1-ON-1 CHAT MESSAGING SYSTEM
    // ==========================================

    private val activeChatListeners = mutableMapOf<String, com.google.firebase.firestore.ListenerRegistration>()

    fun startListeningToChat(chatId: String) {
        if (activeChatListeners.containsKey(chatId)) return
        try {
            val listener = firestore?.collection("chats")?.document(chatId)
                ?.collection("messages")
                ?.orderBy("timestamp", com.google.firebase.firestore.Query.Direction.ASCENDING)
                ?.addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener
                    val list = mutableListOf<ChatMessage>()
                    for (doc in snapshot.documents) {
                        val msgId = doc.getString("messageId") ?: doc.id
                        val sId = doc.getString("senderId") ?: ""
                        val sName = doc.getString("senderName") ?: "Traveler"
                        val sPhoto = doc.getString("senderPhotoUrl") ?: ""
                        val rId = doc.getString("receiverId") ?: ""
                        val text = doc.getString("text") ?: ""
                        val ts = doc.getLong("timestamp") ?: System.currentTimeMillis()

                        if (text.isNotBlank()) {
                            list.add(ChatMessage(msgId, sId, sName, sPhoto, rId, text, ts))
                        }
                    }
                    if (list.isNotEmpty()) {
                        val map = _chatMessagesMap.value.toMutableMap()
                        map[chatId] = list
                        _chatMessagesMap.value = map
                        saveLocalChatHistory()
                    }
                }
            if (listener != null) {
                activeChatListeners[chatId] = listener
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

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
