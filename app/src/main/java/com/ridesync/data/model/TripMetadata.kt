package com.ridesync.data.model

import com.google.firebase.firestore.IgnoreExtraProperties

enum class RiderStatus {
    RIDING,
    STOPPED,
    DELAYED,
    SOS
}

enum class ConvoyRole {
    LEAD,
    SWEEP,
    MEMBER
}

enum class StopReason {
    FUEL,
    FOOD,
    BREAKDOWN,
    REST
}

@IgnoreExtraProperties
data class ConvoyMember(
    val userId: String = "",
    val displayName: String = "",
    val photoUrl: String = "",
    val vehicleModel: String = "",
    val role: ConvoyRole = ConvoyRole.MEMBER,
    val status: RiderStatus = RiderStatus.RIDING,
    val alongTrackProgressMeters: Double = 0.0,
    val distanceBehindLeadMeters: Double = 0.0,
    val isRouteDeviated: Boolean = false,
    val batteryPercent: Int = 100,
    val networkType: String = "4G",
    val lastSeenTimestamp: Long = System.currentTimeMillis()
)

@IgnoreExtraProperties
data class StopEvent(
    val stopId: String = "",
    val tripId: String = "",
    val riderId: String = "",
    val riderName: String = "",
    val reason: StopReason = StopReason.REST,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@IgnoreExtraProperties
data class TripMetadata(
    val tripId: String = "",
    val tripName: String = "",
    val encodedPolyline: String = "",
    val leadUserId: String = "",
    val sweepUserId: String = "",
    val status: String = "ACTIVE",
    val createdTimestamp: Long = System.currentTimeMillis(),
    val members: Map<String, ConvoyMember> = emptyMap(),
    val routeSegments: List<TripRouteSegment> = emptyList(),
    val activeSegmentId: String = ""
)
