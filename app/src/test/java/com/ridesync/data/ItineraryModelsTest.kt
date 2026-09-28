package com.ridesync.data

import com.google.android.gms.maps.model.LatLng
import com.ridesync.data.model.ItineraryDay
import com.ridesync.data.model.ItineraryStop
import com.ridesync.data.model.ItineraryStopStatus
import com.ridesync.data.model.ItineraryTripPlan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ItineraryModelsTest {

    @Test
    fun testItineraryStopDefaultStatusIsPending() {
        val stop = ItineraryStop(
            stopId = "stop-01",
            stopName = "Charminar Monument",
            activityDescription = "Morning photoshoot & Irani chai",
            estimatedVisitTime = "08:00 AM - 09:00 AM",
            rawLocationText = "Charminar, Old City, Hyderabad",
            latitude = 17.3616,
            longitude = 78.4747,
            orderIndex = 0
        )

        assertEquals("stop-01", stop.stopId)
        assertEquals(ItineraryStopStatus.PENDING, stop.status)
        assertEquals(17.3616, stop.latitude, 0.0001)
        assertEquals(78.4747, stop.longitude, 0.0001)
    }

    @Test
    fun testItineraryDayStopsManagement() {
        val stop1 = ItineraryStop(
            stopId = "stop-01",
            stopName = "Attapur Start Point",
            activityDescription = "Assemble & briefing",
            estimatedVisitTime = "06:00 AM",
            rawLocationText = "Attapur, Hyderabad",
            latitude = 17.3753,
            longitude = 78.4344,
            status = ItineraryStopStatus.COMPLETED,
            orderIndex = 0
        )

        val stop2 = ItineraryStop(
            stopId = "stop-02",
            stopName = "Kadthal Highway Stop",
            activityDescription = "Breakfast break",
            estimatedVisitTime = "07:30 AM",
            rawLocationText = "Kadthal Sagar Highway",
            latitude = 17.0854,
            longitude = 78.5891,
            status = ItineraryStopStatus.PENDING,
            orderIndex = 1
        )

        val stop3 = ItineraryStop(
            stopId = "stop-03",
            stopName = "Devarakonda Fort Bypassed",
            activityDescription = "Quick view",
            estimatedVisitTime = "09:30 AM",
            rawLocationText = "Devarakonda",
            latitude = 16.6978,
            longitude = 78.9281,
            status = ItineraryStopStatus.SKIPPED,
            orderIndex = 2
        )

        val day1 = ItineraryDay(
            dayNumber = 1,
            dayTitle = "Day 1: Hyderabad to Srisailam",
            date = "Oct 12, 2026",
            stops = listOf(stop1, stop2, stop3)
        )

        assertEquals(1, day1.dayNumber)
        assertEquals(3, day1.stops.size)
        assertEquals(ItineraryStopStatus.COMPLETED, day1.stops[0].status)
        assertEquals(ItineraryStopStatus.PENDING, day1.stops[1].status)
        assertEquals(ItineraryStopStatus.SKIPPED, day1.stops[2].status)

        // Active routing stops (only PENDING or COMPLETED)
        val activeStops = day1.stops.filter { it.status == ItineraryStopStatus.PENDING || it.status == ItineraryStopStatus.COMPLETED }
        assertEquals(2, activeStops.size)
    }

    @Test
    fun testItineraryTripPlanCreation() {
        val day1 = ItineraryDay(
            dayNumber = 1,
            dayTitle = "Day 1: Coastline Cruising",
            date = "Oct 15, 2026",
            stops = listOf(
                ItineraryStop("s1", "Marine Drive", "Assembly", "06:00 AM", "Marine Drive, Mumbai", 18.9438, 72.8234, ItineraryStopStatus.PENDING, 0)
            )
        )

        val day2 = ItineraryDay(
            dayNumber = 2,
            dayTitle = "Day 2: Coastal Ghats & Beaches",
            date = "Oct 16, 2026",
            stops = listOf(
                ItineraryStop("s2", "Calangute Beach", "Final Destination", "04:00 PM", "Calangute, Goa", 15.5438, 73.7554, ItineraryStopStatus.PENDING, 0)
            )
        )

        val plan = ItineraryTripPlan(
            planId = "plan_goa_01",
            tripTitle = "Mumbai to Goa 2-Day Coastal Express",
            totalDuration = "2 Days • 580 KM",
            startDate = "Oct 15, 2026",
            endDate = "Oct 16, 2026",
            days = listOf(day1, day2)
        )

        assertEquals("plan_goa_01", plan.planId)
        assertEquals(2, plan.days.size)
        assertEquals(1, plan.days[0].stops.size)
        assertEquals(1, plan.days[1].stops.size)
    }
}
