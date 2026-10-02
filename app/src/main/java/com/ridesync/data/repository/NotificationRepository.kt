package com.ridesync.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.ridesync.RideSyncApplication
import com.ridesync.data.model.AppNotification
import com.ridesync.data.model.NotificationType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

/**
 * Real-Time Notifications Repository for TTS (Traveler Travel Synk).
 * Handles targeted push notifications between users via Firestore real-time listener & local cache.
 */
object NotificationRepository {

    private val firestore by lazy {
        try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            null
        }
    }

    private val prefs: SharedPreferences? by lazy {
        try {
            RideSyncApplication.appContext.getSharedPreferences("ridesync_notifications_prefs", Context.MODE_PRIVATE)
        } catch (e: Exception) {
            null
        }
    }

    private val _notifications = MutableStateFlow<List<AppNotification>>(emptyList())
    val notifications: StateFlow<List<AppNotification>> = _notifications.asStateFlow()

    private var activeListener: ListenerRegistration? = null
    private var activeUserId: String = ""

    init {
        loadLocalNotifications()
    }

    fun startListeningForUser(userId: String) {
        if (userId.isBlank()) return
        if (activeUserId == userId && activeListener != null) return

        activeUserId = userId
        loadLocalNotifications()

        try {
            activeListener?.remove()
            activeListener = firestore?.collection("notifications")
                ?.whereEqualTo("targetUserId", userId)
                ?.addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener
                    val list = mutableListOf<AppNotification>()
                    for (doc in snapshot.documents) {
                        val notif = parseNotificationDoc(doc)
                        if (notif != null) {
                            list.add(notif)
                        }
                    }
                    val sorted = list.sortedByDescending { it.timestamp }
                    _notifications.value = sorted
                    saveLocalNotifications(sorted)
                }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun pushNotification(notification: AppNotification) {
        // 1. If notification is targeted at current user (or global system alert), add locally
        if (notification.targetUserId.isBlank() || notification.targetUserId == activeUserId) {
            val current = _notifications.value.toMutableList()
            if (current.none { it.id == notification.id }) {
                current.add(0, notification)
                _notifications.value = current
                saveLocalNotifications(current)
            }
        }

        // 2. Push to Firestore for remote target recipient
        if (notification.targetUserId.isNotBlank()) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val map = hashMapOf(
                        "id" to notification.id,
                        "title" to notification.title,
                        "message" to notification.message,
                        "type" to notification.type.name,
                        "timestamp" to notification.timestamp,
                        "isRead" to notification.isRead,
                        "targetUserId" to notification.targetUserId,
                        "senderUserId" to notification.senderUserId,
                        "senderName" to notification.senderName,
                        "senderPhotoUrl" to notification.senderPhotoUrl,
                        "relatedTripId" to notification.relatedTripId
                    )
                    firestore?.collection("notifications")?.document(notification.id)?.set(map)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun addNotification(notification: AppNotification) {
        pushNotification(notification)
    }

    fun markAsRead(id: String) {
        _notifications.value = _notifications.value.map {
            if (it.id == id) it.copy(isRead = true) else it
        }
        saveLocalNotifications(_notifications.value)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                firestore?.collection("notifications")?.document(id)?.update("isRead", true)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun markAllAsRead() {
        val readList = _notifications.value.map { it.copy(isRead = true) }
        _notifications.value = readList
        saveLocalNotifications(readList)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                readList.forEach { notif ->
                    firestore?.collection("notifications")?.document(notif.id)?.update("isRead", true)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun clearAll() {
        _notifications.value = emptyList()
        saveLocalNotifications(emptyList())
    }

    private fun parseNotificationDoc(doc: com.google.firebase.firestore.DocumentSnapshot): AppNotification? {
        return try {
            val id = doc.getString("id") ?: doc.id
            val title = doc.getString("title") ?: ""
            val message = doc.getString("message") ?: ""
            val typeStr = doc.getString("type") ?: NotificationType.TRIP_UPDATE.name
            val type = try { NotificationType.valueOf(typeStr) } catch (e: Exception) { NotificationType.TRIP_UPDATE }
            val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
            val isRead = doc.getBoolean("isRead") ?: false
            val targetUserId = doc.getString("targetUserId") ?: ""
            val senderUserId = doc.getString("senderUserId") ?: ""
            val senderName = doc.getString("senderName") ?: ""
            val senderPhotoUrl = doc.getString("senderPhotoUrl") ?: ""
            val relatedTripId = doc.getString("relatedTripId") ?: ""

            AppNotification(
                id = id,
                title = title,
                message = message,
                type = type,
                timestamp = timestamp,
                isRead = isRead,
                targetUserId = targetUserId,
                senderUserId = senderUserId,
                senderName = senderName,
                senderPhotoUrl = senderPhotoUrl,
                relatedTripId = relatedTripId
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun saveLocalNotifications(list: List<AppNotification>) {
        if (activeUserId.isBlank()) return
        try {
            val arr = JSONArray()
            list.take(50).forEach { n ->
                arr.put(JSONObject().apply {
                    put("id", n.id)
                    put("title", n.title)
                    put("message", n.message)
                    put("type", n.type.name)
                    put("timestamp", n.timestamp)
                    put("isRead", n.isRead)
                    put("targetUserId", n.targetUserId)
                    put("senderUserId", n.senderUserId)
                    put("senderName", n.senderName)
                    put("senderPhotoUrl", n.senderPhotoUrl)
                    put("relatedTripId", n.relatedTripId)
                })
            }
            prefs?.edit()?.putString("notifs_$activeUserId", arr.toString())?.apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadLocalNotifications() {
        if (activeUserId.isBlank()) return
        try {
            val str = prefs?.getString("notifs_$activeUserId", null) ?: return
            val arr = JSONArray(str)
            val list = mutableListOf<AppNotification>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val typeStr = o.optString("type", NotificationType.TRIP_UPDATE.name)
                val type = try { NotificationType.valueOf(typeStr) } catch (e: Exception) { NotificationType.TRIP_UPDATE }
                list.add(
                    AppNotification(
                        id = o.optString("id"),
                        title = o.optString("title"),
                        message = o.optString("message"),
                        type = type,
                        timestamp = o.optLong("timestamp"),
                        isRead = o.optBoolean("isRead"),
                        targetUserId = o.optString("targetUserId"),
                        senderUserId = o.optString("senderUserId"),
                        senderName = o.optString("senderName"),
                        senderPhotoUrl = o.optString("senderPhotoUrl"),
                        relatedTripId = o.optString("relatedTripId")
                    )
                )
            }
            _notifications.value = list
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
