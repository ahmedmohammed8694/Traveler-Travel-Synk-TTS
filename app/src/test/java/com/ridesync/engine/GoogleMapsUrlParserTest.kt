package com.ridesync.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GoogleMapsUrlParserTest {

    @Test
    fun testParseDirectCoordinatesUrl() {
        val url = "https://www.google.com/maps/dir/19.0760,72.8777/18.5204,73.8567"
        val parsed = GoogleMapsUrlParser.parseUrl(url)
        assertNotNull(parsed)
        assertEquals(19.0760, parsed!!.originLat!!, 0.001)
        assertEquals(72.8777, parsed.originLng!!, 0.001)
        assertEquals(18.5204, parsed.destLat!!, 0.001)
        assertEquals(73.8567, parsed.destLng!!, 0.001)
        assertEquals(0, parsed.waypoints.size)
    }

    @Test
    fun testParseMultiStopPathUrl() {
        // 4 stops: Attapur (Origin), Ibrahimpatnam (Stop 1), Devarakonda (Stop 2), Nagarjuna Sagar (Destination)
        val url = "https://www.google.com/maps/dir/Attapur,+Hyderabad/Ibrahimpatnam/Devarakonda/Nagarjuna+Sagar+Dam/@16.9806,78.4116,9z"
        val parsed = GoogleMapsUrlParser.parseUrl(url)
        assertNotNull(parsed)
        assertEquals("Attapur, Hyderabad", parsed!!.originQuery)
        assertEquals("Nagarjuna Sagar Dam", parsed.destinationQuery)
        assertEquals(2, parsed.waypoints.size)
        assertEquals("Ibrahimpatnam", parsed.waypoints[0])
        assertEquals("Devarakonda", parsed.waypoints[1])
        assertEquals(4, parsed.allStops.size)
    }

    @Test
    fun testParseMultiStopCoordinatesPathUrl() {
        val url = "https://www.google.com/maps/dir/17.3753,78.4344/17.1856,78.6473/16.6978,78.9281/16.5772,79.3125/@17.0,78.8,9z"
        val parsed = GoogleMapsUrlParser.parseUrl(url)
        assertNotNull(parsed)
        assertEquals(17.3753, parsed!!.originLat!!, 0.001)
        assertEquals(78.4344, parsed.originLng!!, 0.001)
        assertEquals(16.5772, parsed.destLat!!, 0.001)
        assertEquals(79.3125, parsed.destLng!!, 0.001)
        assertEquals(2, parsed.waypoints.size)
        assertEquals("17.1856,78.6473", parsed.waypoints[0])
        assertEquals("16.6978,78.9281", parsed.waypoints[1])
    }

    @Test
    fun testParseProtobufDataUrl() {
        val url = "https://www.google.com/maps/dir///@17.0,78.8,9z/data=!4m25!4m24!1m5!1m1!1s0x0!2m2!1d78.4344!2d17.3753!1m5!1m1!1s0x0!2m2!1d78.6473!2d17.1856!1m5!1m1!1s0x0!2m2!1d78.9281!2d16.6978!1m5!1m1!1s0x0!2m2!1d79.3125!2d16.5772"
        val parsed = GoogleMapsUrlParser.parseUrl(url)
        assertNotNull(parsed)
        assertEquals(17.3753, parsed!!.originLat!!, 0.001)
        assertEquals(78.4344, parsed.originLng!!, 0.001)
        assertEquals(16.5772, parsed.destLat!!, 0.001)
        assertEquals(79.3125, parsed.destLng!!, 0.001)
        assertEquals(2, parsed.waypoints.size)
    }

    @Test
    fun testParseQueryParamsUrl() {
        val url = "https://www.google.com/maps/dir/?api=1&origin=Mumbai&destination=Pune&waypoints=Lonavala%7CKhandala"
        val parsed = GoogleMapsUrlParser.parseUrl(url)
        assertNotNull(parsed)
        assertEquals("Mumbai", parsed!!.originQuery)
        assertEquals("Pune", parsed.destinationQuery)
        assertEquals(2, parsed.waypoints.size)
        assertTrue(parsed.waypoints.contains("Lonavala"))
        assertTrue(parsed.waypoints.contains("Khandala"))
    }

    @Test
    fun testParseLegacyDaddrUrl() {
        val url = "https://maps.google.com/?saddr=Mumbai&daddr=Lonavala+to:Khandala+to:Pune"
        val parsed = GoogleMapsUrlParser.parseUrl(url)
        assertNotNull(parsed)
        assertEquals("Mumbai", parsed!!.originQuery)
        assertEquals("Pune", parsed.destinationQuery)
        assertEquals(2, parsed.waypoints.size)
        assertEquals("Lonavala", parsed.waypoints[0])
        assertEquals("Khandala", parsed.waypoints[1])
    }

    @Test
    fun testParseCoordinatePlaceUrl() {
        val url = "https://www.google.com/maps/place/18.5204,73.8567/@18.5204,73.8567,15z"
        val parsed = GoogleMapsUrlParser.parseUrl(url)
        assertNotNull(parsed)
        assertEquals(18.5204, parsed!!.destLat!!, 0.001)
        assertEquals(73.8567, parsed.destLng!!, 0.001)
    }

    @Test
    fun testParseQueryLocationUrl() {
        val url = "https://maps.google.com/?q=Mahabaleshwar+Hill+Station"
        val parsed = GoogleMapsUrlParser.parseUrl(url)
        assertNotNull(parsed)
        assertEquals("Mahabaleshwar Hill Station", parsed!!.destinationQuery)
    }
}
