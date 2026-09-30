package com.ridesync.engine

import com.google.android.gms.maps.model.LatLng
import com.ridesync.data.model.ConvoyMember
import com.ridesync.data.model.ConvoyRole
import com.ridesync.data.model.RiderLocationPing
import kotlin.math.*

enum class RadarRelativePosition {
    AHEAD,          // Front of your current convoy position
    BEHIND,         // Behind your current convoy position
    LEVEL_WITH_YOU  // Within 50m of your position
}

data class ConvoyRadarRiderInfo(
    val riderId: String,
    val displayName: String,
    val photoUrl: String = "",
    val vehicleModel: String = "",
    val phoneNumber: String = "",
    val relativePosition: RadarRelativePosition,
    val distanceMeters: Double,
    val formattedDistance: String,
    val speedKmh: Float,
    val batteryPercent: Int = 100,
    val latLng: LatLng,
    val role: ConvoyRole = ConvoyRole.MEMBER,
    val relativeOffsetKm: Double = 0.0
)

/**
 * Real-Time Convoy Radar Positioning Engine.
 * Computes exact along-track and line-of-sight spatial relationships between riders,
 * determining whether group partners are ahead (+X.X km), behind (-X.X km), or level.
 */
object ConvoyRadarEngine {

    private const val EARTH_RADIUS_METERS = 6371000.0

    /**
     * Compute relative positioning and distance gaps for all active riders relative to the user's location.
     */
    fun calculateRelativePositions(
        myLocation: LatLng,
        myBearing: Float,
        riders: Map<String, RiderLocationPing>,
        members: Map<String, ConvoyMember>,
        myUserId: String = ""
    ): List<ConvoyRadarRiderInfo> {
        val result = mutableListOf<ConvoyRadarRiderInfo>()

        val combinedRiders = riders.toMutableMap()
        for ((mId, member) in members) {
            if (!combinedRiders.containsKey(mId)) {
                combinedRiders[mId] = RiderLocationPing(
                    latitude = myLocation.latitude,
                    longitude = myLocation.longitude,
                    speedKmh = 0f,
                    bearing = myBearing,
                    timestamp = System.currentTimeMillis()
                )
            }
        }

        for ((riderId, ping) in combinedRiders) {
            if (riderId == myUserId) continue

            val riderLatLng = LatLng(ping.latitude, ping.longitude)
            val distanceMeters = computeHaversineDistance(myLocation, riderLatLng)
            val bearingToRider = computeBearing(myLocation, riderLatLng)

            // Calculate angle difference relative to user's heading
            val angleDiff = (bearingToRider - myBearing + 540) % 360 - 180

            val relativePos: RadarRelativePosition
            val distanceKm = distanceMeters / 1000.0

            if (distanceMeters <= 50.0) {
                relativePos = RadarRelativePosition.LEVEL_WITH_YOU
            } else if (abs(angleDiff) <= 90.0) {
                relativePos = RadarRelativePosition.AHEAD
            } else {
                relativePos = RadarRelativePosition.BEHIND
            }

            val formattedDist = when (relativePos) {
                RadarRelativePosition.AHEAD -> {
                    if (distanceMeters < 1000) "+${distanceMeters.roundToInt()} M" else "+${"%.1f".format(distanceKm)} KM"
                }
                RadarRelativePosition.BEHIND -> {
                    if (distanceMeters < 1000) "-${distanceMeters.roundToInt()} M" else "-${"%.1f".format(distanceKm)} KM"
                }
                RadarRelativePosition.LEVEL_WITH_YOU -> {
                    "LEVEL (${distanceMeters.roundToInt()} M)"
                }
            }

            val member = members[riderId]
            val signedOffset = if (relativePos == RadarRelativePosition.AHEAD) distanceKm else -distanceKm

            result.add(
                ConvoyRadarRiderInfo(
                    riderId = riderId,
                    displayName = member?.displayName ?: "Rider ${riderId.takeLast(4)}",
                    photoUrl = member?.photoUrl ?: "",
                    vehicleModel = member?.vehicleModel ?: "Motorcycle",
                    phoneNumber = "",
                    relativePosition = relativePos,
                    distanceMeters = distanceMeters,
                    formattedDistance = formattedDist,
                    speedKmh = ping.speedKmh,
                    batteryPercent = member?.batteryPercent ?: 100,
                    latLng = riderLatLng,
                    role = member?.role ?: ConvoyRole.MEMBER,
                    relativeOffsetKm = signedOffset
                )
            )
        }

        // Sort descending: Ahead (furthest first), then Level, then Behind (closest first, furthest last)
        return result.sortedByDescending { it.relativeOffsetKm }
    }

    fun computeHaversineDistance(p1: LatLng, p2: LatLng): Double {
        val lat1Rad = Math.toRadians(p1.latitude)
        val lat2Rad = Math.toRadians(p2.latitude)
        val deltaLat = Math.toRadians(p2.latitude - p1.latitude)
        val deltaLng = Math.toRadians(p2.longitude - p1.longitude)

        val a = sin(deltaLat / 2).pow(2.0) +
                cos(lat1Rad) * cos(lat2Rad) * sin(deltaLng / 2).pow(2.0)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return EARTH_RADIUS_METERS * c
    }

    fun computeBearing(p1: LatLng, p2: LatLng): Double {
        val lat1Rad = Math.toRadians(p1.latitude)
        val lat2Rad = Math.toRadians(p2.latitude)
        val deltaLng = Math.toRadians(p2.longitude - p1.longitude)

        val y = sin(deltaLng) * cos(lat2Rad)
        val x = cos(lat1Rad) * sin(lat2Rad) - sin(lat1Rad) * cos(lat2Rad) * cos(deltaLng)

        val bearing = Math.toDegrees(atan2(y, x))
        return (bearing + 360) % 360
    }
}
