package com.ridesync.data.repository

import com.ridesync.data.model.AppNotification
import com.ridesync.data.model.NotificationType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-memory & Real-Time Notifications Repository for TTS (Traveler Travel Synk).
 * Manages notification alerts for:
 * 1. Friend requests & acceptances
 * 2. New direct messages & convoy chat updates
 * 3. New members joining trips
 * 4. Trip alerts (Rest stops, Fuel stops, Emergency SOS)
 */
object NotificationRepository {
    private val _notifications = MutableStateFlow<List<AppNotification>>(
        listOf(
            AppNotification(
                id = "notif_init_1",
                title = "🎉 Friend Request Accepted",
                message = "Alex Traveler accepted your friend request!",
                type = NotificationType.FRIEND_ACCEPTED,
                timestamp = System.currentTimeMillis() - 1000 * 60 * 15,
                isRead = false,
                senderUserId = "user_alex",
                senderName = "Alex Traveler"
            ),
            AppNotification(
                id = "notif_init_2",
                title = "💬 New Message",
                message = "Rohan: Hey, ready for the weekend ride?",
                type = NotificationType.NEW_MESSAGE,
                timestamp = System.currentTimeMillis() - 1000 * 60 * 45,
                isRead = false,
                senderUserId = "user_rohan",
                senderName = "Rohan Rider"
            ),
            AppNotification(
                id = "notif_init_3",
                title = "🚀 New Member Joined Convoy",
                message = "Vikram joined your trip 'Deccan Highway Cruise'!",
                type = NotificationType.CONVOY_MEMBER_JOINED,
                timestamp = System.currentTimeMillis() - 1000 * 60 * 120,
                isRead = true,
                senderUserId = "user_vikram",
                senderName = "Vikram",
                relatedTripId = "active_trip_101"
            )
        )
    )

    val notifications: StateFlow<List<AppNotification>> = _notifications.asStateFlow()

    fun addNotification(notification: AppNotification) {
        _notifications.value = listOf(notification) + _notifications.value
    }

    fun markAsRead(id: String) {
        _notifications.value = _notifications.value.map {
            if (it.id == id) it.copy(isRead = true) else it
        }
    }

    fun markAllAsRead() {
        _notifications.value = _notifications.value.map { it.copy(isRead = true) }
    }

    fun clearAll() {
        _notifications.value = emptyList()
    }
}
