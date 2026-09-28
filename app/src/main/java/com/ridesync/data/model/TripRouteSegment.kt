package com.ridesync.data.model

import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class TripRouteSegment(
    val segmentId: String = "",
    val segmentName: String = "Day 1 Route",
    val googleMapsUrl: String = "",
    val originName: String = "",
    val destinationName: String = "",
    val encodedPolyline: String = "",
    val distanceKm: Double = 0.0,
    val estimatedDurationMinutes: Int = 0,
    val waypointsCount: Int = 0,
    val waypoints: List<String> = emptyList(),
    val orderIndex: Int = 0
)
