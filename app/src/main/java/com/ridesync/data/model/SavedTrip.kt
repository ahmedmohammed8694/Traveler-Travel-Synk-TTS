package com.ridesync.data.model

import com.google.android.gms.maps.model.LatLng

enum class TripCategory {
    ONGOING,
    UPCOMING,
    COMPLETED
}

data class JoinedRiderProfile(
    val riderId: String,
    val displayName: String,
    val bikeModel: String,
    val role: ConvoyRole,
    val status: String = "Confirmed & Ready",
    val experienceBadge: String = "Pro Tourer",
    val emergencyContact: String = ""
)

data class SavedTrip(
    val tripId: String,
    val plannerId: String = "user_host",
    val title: String,
    val originName: String,
    val destinationName: String,
    val startLatLng: LatLng,
    val destLatLng: LatLng,
    val waypoints: List<String> = emptyList(),
    val waypointLatLngs: List<LatLng> = emptyList(),
    val distanceKm: Double,
    val durationMinutes: Int,
    val role: ConvoyRole = ConvoyRole.LEAD,
    val category: TripCategory = TripCategory.UPCOMING,
    val lobbyCode: String = "",
    val scheduledDate: String = "",
    val activeRidersCount: Int = 1,
    val avgSpeedKmh: Int = 0,
    val completedKm: Double = 0.0,
    val ratingStars: Double = 5.0,
    val incidentsCount: Int = 0,
    val joinedRiders: List<JoinedRiderProfile> = emptyList(),
    val routeSegments: List<TripRouteSegment> = emptyList(),
    val activeSegmentId: String = "",
    val itineraryPlan: ItineraryTripPlan? = null
)

