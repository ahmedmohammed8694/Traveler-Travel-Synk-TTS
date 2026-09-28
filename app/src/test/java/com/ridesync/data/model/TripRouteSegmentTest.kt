package com.ridesync.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class TripRouteSegmentTest {
    @Test
    fun testSegmentCreationAndDefaults() {
        val segment = TripRouteSegment(
            segmentId = "seg_1",
            segmentName = "Day 1: Coast Highway",
            googleMapsUrl = "https://maps.google.com/dir/19.0760,72.8777/18.5204,73.8567",
            originName = "Mumbai",
            destinationName = "Pune",
            encodedPolyline = "_p~iF~ps|U_ulLnnqC_mqNvxq`@",
            distanceKm = 148.5,
            estimatedDurationMinutes = 180,
            waypointsCount = 2,
            orderIndex = 0
        )
        assertEquals("seg_1", segment.segmentId)
        assertEquals("Day 1: Coast Highway", segment.segmentName)
        assertEquals(148.5, segment.distanceKm, 0.01)
        assertEquals(180, segment.estimatedDurationMinutes)
        assertEquals("Mumbai", segment.originName)
        assertEquals("Pune", segment.destinationName)
    }

    @Test
    fun testTripMetadataWithMultipleSegments() {
        val segment1 = TripRouteSegment(
            segmentId = "seg_1",
            segmentName = "Day 1: Coast Highway",
            distanceKm = 140.0
        )
        val segment2 = TripRouteSegment(
            segmentId = "seg_2",
            segmentName = "Day 2: Mountain Pass",
            distanceKm = 210.0
        )
        val trip = TripMetadata(
            tripId = "trip_100",
            tripName = "Western Ghats Odyssey",
            routeSegments = listOf(segment1, segment2),
            activeSegmentId = "seg_1"
        )
        assertNotNull(trip)
        assertEquals(2, trip.routeSegments.size)
        assertEquals("Day 1: Coast Highway", trip.routeSegments[0].segmentName)
        assertEquals("Day 2: Mountain Pass", trip.routeSegments[1].segmentName)
        assertEquals("seg_1", trip.activeSegmentId)
    }
}
