package com.ridesync.engine

import com.google.android.gms.maps.model.LatLng
import com.ridesync.data.model.*
import org.junit.Assert.*
import org.junit.Test

class UnifiedTripCreationFlowTest {

    @Test
    fun testOptionA_DocumentUploadCreation() {
        val documentText = """
            Weekend Konkan Coastal Ride
            Duration: 2 Days
            
            Day 1: Pune to Mahabaleshwar
            - Stop 1: Shirwal Food Plaza, Breakfast & regroup, 08:30 AM, https://maps.app.goo.gl/shirwal
            - Stop 2: Wai Ghat Viewpoint, Photo stop, 11:00 AM, https://maps.app.goo.gl/wai
            - Stop 3: Mahabaleshwar Market, Hotel check-in & dinner, 04:00 PM, https://maps.app.goo.gl/maha
            
            Day 2: Mahabaleshwar to Goa
            - Stop 1: Tapola Lake, Boating break, 09:00 AM, https://maps.app.goo.gl/tapola
            - Stop 2: Calangute Beach, Final destination, 06:00 PM, https://maps.app.goo.gl/goa
        """.trimIndent()

        val parsedPlan = ItineraryParserEngine.parseTextToTripPlan(documentText, "Weekend Konkan Coastal Ride")
        val unifiedPlan = parsedPlan.copy(creationMode = TripCreationMode.DOCUMENT)

        assertEquals(TripCreationMode.DOCUMENT, unifiedPlan.creationMode)
        assertEquals(2, unifiedPlan.days.size)
        assertEquals(3, unifiedPlan.days[0].stops.size)
        assertEquals(2, unifiedPlan.days[1].stops.size)
        assertTrue(unifiedPlan.days[0].stops.all { it.status == ItineraryStopStatus.PENDING })
    }

    @Test
    fun testOptionB_MapLinksCreation() {
        val days = listOf(
            ItineraryDay(
                dayNumber = 1,
                dayTitle = "Day 1: Hyderabad to Srisailam",
                stops = listOf(
                    ItineraryStop("s1_0", "Attapur Hyderabad", "Start Leg", latitude = 17.3753, longitude = 78.4344, status = ItineraryStopStatus.PENDING),
                    ItineraryStop("s1_1", "Dindi Reservoir Stop", "Milestone", latitude = 16.5821, longitude = 78.9102, status = ItineraryStopStatus.PENDING),
                    ItineraryStop("s1_2", "Srisailam Temple", "End Leg", latitude = 16.0748, longitude = 78.8682, status = ItineraryStopStatus.PENDING)
                )
            ),
            ItineraryDay(
                dayNumber = 2,
                dayTitle = "Day 2: Srisailam to Nagarjuna Sagar",
                stops = listOf(
                    ItineraryStop("s2_0", "Srisailam Temple", "Start Leg", latitude = 16.0748, longitude = 78.8682, status = ItineraryStopStatus.PENDING),
                    ItineraryStop("s2_1", "Nagarjuna Sagar Dam", "End Leg", latitude = 16.5772, longitude = 79.3125, status = ItineraryStopStatus.PENDING)
                )
            )
        )

        val plan = ItineraryTripPlan(
            planId = "plan_map_links_123",
            creationMode = TripCreationMode.MAP_LINKS,
            tripTitle = "Telangana Wilderness Tour",
            totalDuration = "2 Days Planned",
            days = days
        )

        assertEquals(TripCreationMode.MAP_LINKS, plan.creationMode)
        assertEquals(2, plan.days.size)
        assertEquals(5, plan.days.sumOf { it.stops.size })
    }

    @Test
    fun testOptionC_ManualSearchCreation() {
        val stops = listOf(
            ItineraryStop("stop_0", "Attapur, Hyderabad", "Origin", latitude = 17.3753, longitude = 78.4344, status = ItineraryStopStatus.PENDING, orderIndex = 0),
            ItineraryStop("stop_1", "Devarakonda Fort Stop", "Way-point", latitude = 16.6978, longitude = 78.9281, status = ItineraryStopStatus.PENDING, orderIndex = 1),
            ItineraryStop("stop_2", "Nagarjuna Sagar Dam", "Destination", latitude = 16.5772, longitude = 79.3125, status = ItineraryStopStatus.PENDING, orderIndex = 2)
        )

        val plan = ItineraryTripPlan(
            planId = "plan_search_456",
            creationMode = TripCreationMode.MANUAL_SEARCH,
            tripTitle = "Hyderabad to Sagar Run",
            totalDuration = "3h 34m",
            days = listOf(ItineraryDay(dayNumber = 1, dayTitle = "Day 1: Attapur to Sagar", stops = stops))
        )

        assertEquals(TripCreationMode.MANUAL_SEARCH, plan.creationMode)
        assertEquals(1, plan.days.size)
        assertEquals(3, plan.days[0].stops.size)
    }

    @Test
    fun testStopStatusDynamicFiltering() {
        val stops = listOf(
            ItineraryStop("s1", "Stop 1", status = ItineraryStopStatus.COMPLETED, latitude = 17.0, longitude = 78.0),
            ItineraryStop("s2", "Stop 2", status = ItineraryStopStatus.SKIPPED, latitude = 17.1, longitude = 78.1),
            ItineraryStop("s3", "Stop 3", status = ItineraryStopStatus.IGNORED, latitude = 17.2, longitude = 78.2),
            ItineraryStop("s4", "Stop 4", status = ItineraryStopStatus.PENDING, latitude = 17.3, longitude = 78.3)
        )

        val activeStops = stops.filter { it.status == ItineraryStopStatus.PENDING || it.status == ItineraryStopStatus.COMPLETED }
        val skippedOrIgnoredStops = stops.filter { it.status == ItineraryStopStatus.SKIPPED || it.status == ItineraryStopStatus.IGNORED }

        assertEquals(2, activeStops.size)
        assertEquals(listOf("Stop 1", "Stop 4"), activeStops.map { it.stopName })
        assertEquals(2, skippedOrIgnoredStops.size)
        assertEquals(listOf("Stop 2", "Stop 3"), skippedOrIgnoredStops.map { it.stopName })
    }
}
