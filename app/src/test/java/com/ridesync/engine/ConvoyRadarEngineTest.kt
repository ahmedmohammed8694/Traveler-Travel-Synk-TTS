package com.ridesync.engine

import com.google.android.gms.maps.model.LatLng
import com.ridesync.data.model.ConvoyMember
import com.ridesync.data.model.ConvoyRole
import com.ridesync.data.model.RiderLocationPing
import com.ridesync.data.model.RiderStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConvoyRadarEngineTest {

    @Test
    fun testAheadAndBehindCalculation() {
        val myLoc = LatLng(17.3753, 78.4344) // Current user at Attapur
        val myBearing = 0f // Heading North (0 degrees)

        val aheadPing = RiderLocationPing(
            latitude = 17.3900, // Further North
            longitude = 78.4344,
            speedKmh = 75f,
            bearing = 0f
        )

        val behindPing = RiderLocationPing(
            latitude = 17.3600, // Further South
            longitude = 78.4344,
            speedKmh = 65f,
            bearing = 0f
        )

        val levelPing = RiderLocationPing(
            latitude = 17.37532, // 2-3 meters away
            longitude = 78.43442,
            speedKmh = 70f,
            bearing = 0f
        )

        val pings = mapOf(
            "user_lead" to aheadPing,
            "user_sweep" to behindPing,
            "user_buddy" to levelPing
        )

        val members = mapOf(
            "user_lead" to ConvoyMember(userId = "user_lead", displayName = "Vikram Lead", role = ConvoyRole.LEAD, status = RiderStatus.RIDING, vehicleModel = "BMW GS"),
            "user_sweep" to ConvoyMember(userId = "user_sweep", displayName = "Rahul Sweep", role = ConvoyRole.SWEEP, status = RiderStatus.RIDING, vehicleModel = "KTM Duke"),
            "user_buddy" to ConvoyMember(userId = "user_buddy", displayName = "Priya Member", role = ConvoyRole.MEMBER, status = RiderStatus.RIDING, vehicleModel = "Ninja 650")
        )

        val radarList = ConvoyRadarEngine.calculateRelativePositions(
            myLocation = myLoc,
            myBearing = myBearing,
            riders = pings,
            members = members,
            myUserId = "user_me"
        )

        assertEquals(3, radarList.size)

        // Ahead rider
        val leadRider = radarList.first { it.riderId == "user_lead" }
        assertEquals(RadarRelativePosition.AHEAD, leadRider.relativePosition)
        assertTrue(leadRider.formattedDistance.startsWith("+"))

        // Level rider
        val buddyRider = radarList.first { it.riderId == "user_buddy" }
        assertEquals(RadarRelativePosition.LEVEL_WITH_YOU, buddyRider.relativePosition)

        // Behind rider
        val sweepRider = radarList.first { it.riderId == "user_sweep" }
        assertEquals(RadarRelativePosition.BEHIND, sweepRider.relativePosition)
        assertTrue(sweepRider.formattedDistance.startsWith("-"))
    }
}
