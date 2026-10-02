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
    val emergencyContact: String = "",
    val photoUrl: String = "",
    val profileCode: String = "",
    val email: String = "",
    val mobileNumber: String = ""
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
) {
    val formattedTripCode: String
        get() = if (lobbyCode.isNotBlank()) {
            val clean = lobbyCode.trim().uppercase()
            val digitsOnly = clean.replace(Regex("[^0-9]"), "")
            if (digitsOnly.length >= 4) {
                "TTSP${digitsOnly.takeLast(4)}"
            } else if (clean.startsWith("TTSP")) {
                clean
            } else if (clean.startsWith("TTS-")) {
                "TTSP${clean.removePrefix("TTS-")}"
            } else if (clean.startsWith("TTS")) {
                "TTSP${clean.removePrefix("TTS")}"
            } else {
                "TTSP$clean"
            }
        } else {
            val seed = Math.abs(tripId.hashCode() % 9000) + 1000
            "TTSP$seed"
        }
}

