package com.ridesync.data.model

/**
 * Status of an individual milestone/stop on an itinerary route.
 */
enum class ItineraryStopStatus {
    PENDING,    // Active upcoming stop (default)
    COMPLETED,  // Visited and checked off by user
    SKIPPED,    // Bypassed on this journey (excluded from dynamic route)
    IGNORED     // Struck through / not relevant (excluded from dynamic route)
}

/**
 * Data model for a single stop/activity along an itinerary day.
 */
data class ItineraryStop(
    val stopId: String,
    val stopName: String,
    val activityDescription: String = "",
    val estimatedVisitTime: String = "",
    val rawLocationText: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val status: ItineraryStopStatus = ItineraryStopStatus.PENDING,
    val orderIndex: Int = 0,
    val googleMapsUrl: String = ""
)

/**
 * Data model representing a single day's route and stops.
 */
data class ItineraryDay(
    val dayNumber: Int,
    val dayTitle: String,
    val date: String = "",
    val stops: List<ItineraryStop> = emptyList()
)

/**
 * Creation workflow used to establish the itinerary.
 */
enum class TripCreationMode {
    DOCUMENT,       // Uploaded itinerary file (.pdf, .docx, .txt, .png, .jpg)
    MAP_LINKS,      // Day-by-day pasted Google Maps links
    MANUAL_SEARCH   // In-app direct Origin & Destination Places search
}

/**
 * Complete structured trip plan containing multi-day breakdowns.
 */
data class ItineraryTripPlan(
    val planId: String,
    val creationMode: TripCreationMode = TripCreationMode.MANUAL_SEARCH,
    val tripTitle: String,
    val totalDuration: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val days: List<ItineraryDay> = emptyList()
)

