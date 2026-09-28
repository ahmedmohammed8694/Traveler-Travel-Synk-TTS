package com.ridesync.data

import com.google.android.gms.maps.model.LatLng
import com.ridesync.data.model.ConvoyRole
import com.ridesync.data.model.JoinedRiderProfile
import com.ridesync.data.model.SavedTrip
import com.ridesync.data.model.TripCategory
import com.ridesync.data.model.TripRouteSegment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TripModelTest {

    @Test
    fun testSavedTripCreationAndDefaultFields() {
        val trip = SavedTrip(
            tripId = "TRIP-TEST-01",
            plannerId = "user_123",
            title = "Monsoon Ghat Ride",
            originName = "Mumbai",
            destinationName = "Goa",
            startLatLng = LatLng(18.9438, 72.8234),
            destLatLng = LatLng(15.5438, 73.7554),
            waypoints = listOf("Ratnagiri", "Malvan"),
            waypointLatLngs = listOf(LatLng(16.9902, 73.3120), LatLng(16.0558, 73.4687)),
            distanceKm = 580.0,
            durationMinutes = 645,
            role = ConvoyRole.LEAD,
            category = TripCategory.UPCOMING
        )

        assertEquals("TRIP-TEST-01", trip.tripId)
        assertEquals("user_123", trip.plannerId)
        assertEquals("Monsoon Ghat Ride", trip.title)
        assertEquals(2, trip.waypoints.size)
        assertEquals(TripCategory.UPCOMING, trip.category)
    }

    @Test
    fun testJoinedRiderExitLogic() {
        val rider1 = JoinedRiderProfile("r1", "Ahmed", "RE Meteor 350", ConvoyRole.LEAD)
        val rider2 = JoinedRiderProfile("r2", "Rahul", "KTM Duke 390", ConvoyRole.SWEEP)
        val rider3 = JoinedRiderProfile("r3", "Vikram", "BMW R 1250 GS", ConvoyRole.MEMBER)

        val trip = SavedTrip(
            tripId = "TRIP-EXIT-01",
            title = "Hyderabad Highway Run",
            originName = "Attapur",
            destinationName = "Nagarjuna Sagar",
            startLatLng = LatLng(17.3753, 78.4344),
            destLatLng = LatLng(16.5772, 79.3125),
            distanceKm = 159.0,
            durationMinutes = 214,
            activeRidersCount = 3,
            joinedRiders = listOf(rider1, rider2, rider3)
        )

        // Simulate rider 3 exiting
        val updatedRiders = trip.joinedRiders.filterNot { it.riderId == "r3" }
        val updatedTrip = trip.copy(
            joinedRiders = updatedRiders,
            activeRidersCount = (trip.activeRidersCount - 1).coerceAtLeast(0)
        )

        assertEquals(2, updatedTrip.joinedRiders.size)
        assertEquals(2, updatedTrip.activeRidersCount)
        assertTrue(updatedTrip.joinedRiders.none { it.riderId == "r3" })
    }

    @Test
    fun testTripRouteSegmentPreservation() {
        val segment = TripRouteSegment(
            segmentId = "seg_01",
            segmentName = "Day 1: Coastal Leg",
            googleMapsUrl = "https://www.google.com/maps/dir/Mumbai/Ratnagiri",
            originName = "Mumbai",
            destinationName = "Ratnagiri",
            distanceKm = 330.0,
            estimatedDurationMinutes = 360,
            waypoints = listOf("Alibaug")
        )

        val trip = SavedTrip(
            tripId = "TRIP-SEG-01",
            title = "Multi-Day Coastline Tour",
            originName = "Mumbai",
            destinationName = "Goa",
            startLatLng = LatLng(18.9438, 72.8234),
            destLatLng = LatLng(15.5438, 73.7554),
            distanceKm = 580.0,
            durationMinutes = 645,
            routeSegments = listOf(segment)
        )

        assertEquals(1, trip.routeSegments.size)
        assertEquals("Day 1: Coastal Leg", trip.routeSegments[0].segmentName)
        assertEquals("Alibaug", trip.routeSegments[0].waypoints[0])
    }
}
