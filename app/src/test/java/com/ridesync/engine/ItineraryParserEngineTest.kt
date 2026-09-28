package com.ridesync.engine

import com.ridesync.data.model.ItineraryStopStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ItineraryParserEngineTest {

    @Test
    fun testParseMultiDayItineraryText() {
        val sampleDoc = """
            TRIP PLAN: Hyderabad to Srisailam Dam Ghats Tour
            Duration: 2 Days • Total 350 KM
            Dates: Oct 15, 2026 to Oct 16, 2026

            Day 1: Hyderabad to Dindi Reservoir
            - 06:00 AM: Attapur Start Point | Assembly and safety briefing (Location: Attapur, Hyderabad)
            - 08:30 AM: Kadthal Highway Food Court | Breakfast & fuel stop (Location: Kadthal Sagar Highway)
            - 12:30 PM: Dindi Reservoir Viewpoint | Scenic photo stop & lunch https://www.google.com/maps/dir/17.3753,78.4344/16.5700,78.9600

            Day 2: Dindi to Srisailam Dam
            - 07:00 AM: Dindi Sunrise Point | Morning ride through dense ghat roads
            - 11:30 AM: Srisailam Dam Viewpoint | Final Destination & dam tour (Location: Srisailam, Andhra Pradesh)
        """.trimIndent()

        val plan = ItineraryParserEngine.parseTextToTripPlan(sampleDoc, "Default Trip")

        assertEquals("Hyderabad to Srisailam Dam Ghats Tour", plan.tripTitle)
        assertEquals(2, plan.days.size)

        // Day 1 Checks
        val day1 = plan.days[0]
        assertEquals(1, day1.dayNumber)
        assertEquals(3, day1.stops.size)
        assertEquals("Attapur Start Point", day1.stops[0].stopName)
        assertEquals("06:00 AM", day1.stops[0].estimatedVisitTime)
        assertEquals(ItineraryStopStatus.PENDING, day1.stops[0].status)

        assertEquals("Dindi Reservoir Viewpoint", day1.stops[2].stopName)
        assertTrue(day1.stops[2].googleMapsUrl.contains("google.com/maps"))

        // Day 2 Checks
        val day2 = plan.days[1]
        assertEquals(2, day2.dayNumber)
        assertEquals(2, day2.stops.size)
        assertEquals("Srisailam Dam Viewpoint", day2.stops[1].stopName)
    }

    @Test
    fun testParseSingleDayUnformattedText() {
        val rawText = """
            Nandi Hills Sunrise Ride
            1. 05:00 AM - Hebbal Flyover (Assemble)
            2. 06:30 AM - Nandi Hills Base Toll (Tea break)
            3. 07:15 AM - Nandi Hills Viewpoint Peak (Sunrise watch)
        """.trimIndent()

        val plan = ItineraryParserEngine.parseTextToTripPlan(rawText, "Nandi Hills Trip")

        assertEquals(1, plan.days.size)
        assertEquals(3, plan.days[0].stops.size)
        assertEquals("Hebbal Flyover", plan.days[0].stops[0].stopName)
        assertEquals("05:00 AM", plan.days[0].stops[0].estimatedVisitTime)
    }
}
