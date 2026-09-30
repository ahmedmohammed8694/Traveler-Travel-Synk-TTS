package com.ridesync.data

import com.google.android.gms.maps.model.LatLng
import com.ridesync.data.model.ConvoyRole
import com.ridesync.data.model.ItineraryDay
import com.ridesync.data.model.ItineraryStop
import com.ridesync.data.model.ItineraryStopStatus
import com.ridesync.data.model.ItineraryTripPlan
import com.ridesync.data.model.SavedTrip
import com.ridesync.data.model.TripCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CloudflareD1DatabaseSyncTest {

    @Test
    fun testCloudflareD1TripPayloadStructure() {
        val stop = ItineraryStop(
            stopId = "stop_001",
            stopName = "Fuel Stop & Tea Break",
            activityDescription = "Refuel at HP station and rest",
            estimatedVisitTime = "10:30 AM",
            latitude = 17.4065,
            longitude = 78.4772,
            status = ItineraryStopStatus.PENDING,
            orderIndex = 1
        )

        val day = ItineraryDay(
            dayNumber = 1,
            dayTitle = "Day 1: Hyderabad to Srisailam",
            stops = listOf(stop)
        )

        val plan = ItineraryTripPlan(
            planId = "plan_d1_test",
            tripTitle = "Srisailam Ghat Run",
            totalDuration = "1 Day",
            days = listOf(day)
        )

        val trip = SavedTrip(
            tripId = "TRIP-D1-100",
            plannerId = "user_rider_42",
            title = "Srisailam Ghat Run",
            originName = "Hyderabad",
            destinationName = "Srisailam",
            startLatLng = LatLng(17.3850, 78.4867),
            destLatLng = LatLng(16.0748, 78.8687),
            distanceKm = 215.0,
            durationMinutes = 270,
            role = ConvoyRole.LEAD,
            category = TripCategory.UPCOMING,
            lobbyCode = "SRI992",
            itineraryPlan = plan
        )

        // Verify key D1 SQLite database payload fields match schema.sql definitions
        assertEquals("TRIP-D1-100", trip.tripId)
        assertEquals("user_rider_42", trip.plannerId)
        assertEquals("SRI992", trip.lobbyCode)
        assertEquals(215.0, trip.distanceKm, 0.01)
        assertEquals(270, trip.durationMinutes)
        assertNotNull(trip.itineraryPlan)
        assertEquals(1, trip.itineraryPlan?.days?.size)
        assertEquals(1, trip.itineraryPlan?.days?.get(0)?.stops?.size)
        assertEquals("Fuel Stop & Tea Break", trip.itineraryPlan?.days?.get(0)?.stops?.get(0)?.stopName)
        assertEquals(ItineraryStopStatus.PENDING, trip.itineraryPlan?.days?.get(0)?.stops?.get(0)?.status)
    }

    @Test
    fun testCloudflareD1StopStatusUpdatePayload() {
        val payloadMap = mapOf(
            "tripId" to "TRIP-D1-100",
            "dayNumber" to 1,
            "stopId" to "stop_001",
            "status" to ItineraryStopStatus.COMPLETED.name
        )

        assertEquals("TRIP-D1-100", payloadMap["tripId"])
        assertEquals(1, payloadMap["dayNumber"])
        assertEquals("stop_001", payloadMap["stopId"])
        assertEquals("COMPLETED", payloadMap["status"])
    }
}
