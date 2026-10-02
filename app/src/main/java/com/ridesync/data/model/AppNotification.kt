package com.ridesync.data.model

enum class NotificationType {
    FRIEND_REQUEST,
    FRIEND_ACCEPTED,
    NEW_MESSAGE,
    CONVOY_MEMBER_JOINED,
    TRIP_UPDATE,
    SOS_ALERT
}

data class AppNotification(
    val id: String = "notif_${System.currentTimeMillis()}",
    val title: String = "",
    val message: String = "",
    val type: NotificationType = NotificationType.TRIP_UPDATE,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val senderUserId: String = "",
    val senderName: String = "",
    val senderPhotoUrl: String = "",
    val relatedTripId: String = ""
)
