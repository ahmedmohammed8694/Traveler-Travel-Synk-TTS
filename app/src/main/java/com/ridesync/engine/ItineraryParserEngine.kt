package com.ridesync.engine

import com.ridesync.data.model.ItineraryDay
import com.ridesync.data.model.ItineraryStop
import com.ridesync.data.model.ItineraryStopStatus
import com.ridesync.data.model.ItineraryTripPlan
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.util.regex.Pattern

/**
 * Intelligent Document & Text Parsing Engine for Automated Trip Planning.
 * Extracts structured multi-day schedules, sequential stops, time estimates,
 * descriptions, and Google Maps location links from raw document streams and text.
 */
object ItineraryParserEngine {

    private val DAY_HEADER_REGEX = Pattern.compile("(?i)^\\s*(?:Day|Stage|Leg)\\s*(\\d+)[:\\-\\s]*(.*)$")
    private val TIME_REGEX = Pattern.compile("(?i)(\\b\\d{1,2}:\\d{2}\\s*(?:AM|PM|am|pm)?(?:\\s*-\\s*\\d{1,2}:\\d{2}\\s*(?:AM|PM|am|pm)?)?\\b)")
    private val MAPS_URL_REGEX = Pattern.compile("https?://(?:www\\.)?(?:google\\.com/maps|maps\\.google\\.com|maps\\.app\\.goo\\.gl|goo\\.gl/maps)[^\\s]+")

    /**
     * Read and extract raw text from an input stream based on MIME type.
     */
    fun extractFromDocumentStream(inputStream: InputStream, mimeType: String): String {
        return try {
            val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
            val sb = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                sb.append(line).append("\n")
            }
            sb.toString()
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    /**
     * Parse raw document text into structured ItineraryTripPlan.
     */
    fun parseTextToTripPlan(rawText: String, defaultTitle: String = "Automated Convoy Plan"): ItineraryTripPlan {
        val lines = rawText.lines().map { it.trim() }.filter { it.isNotBlank() }
        if (lines.isEmpty()) {
            return ItineraryTripPlan(
                planId = "plan_${System.currentTimeMillis()}",
                tripTitle = defaultTitle,
                days = listOf(ItineraryDay(dayNumber = 1, dayTitle = "Day 1", stops = emptyList()))
            )
        }

        var extractedTitle = ""
        var extractedDuration = ""
        var extractedDates = ""

        val daysList = mutableListOf<ItineraryDay>()
        var currentDayNumber = 1
        var currentDayTitle = "Day 1"
        var currentStops = mutableListOf<ItineraryStop>()

        for (line in lines) {
            // Check for Title keywords
            if (extractedTitle.isBlank() && (line.startsWith("TRIP PLAN:", ignoreCase = true) || line.startsWith("TRIP:", ignoreCase = true) || line.startsWith("TITLE:", ignoreCase = true))) {
                extractedTitle = line.substringAfter(":").trim()
                continue
            } else if (extractedTitle.isBlank() && !line.startsWith("Day", ignoreCase = true) && !line.startsWith("-") && !line.contains(":")) {
                extractedTitle = line
                continue
            }

            // Check for duration or dates
            if (line.startsWith("Duration:", ignoreCase = true)) {
                extractedDuration = line.substringAfter(":").trim()
                continue
            }
            if (line.startsWith("Dates:", ignoreCase = true) || line.startsWith("Date:", ignoreCase = true)) {
                extractedDates = line.substringAfter(":").trim()
                continue
            }

            // Check for Day headers
            val dayMatcher = DAY_HEADER_REGEX.matcher(line)
            if (dayMatcher.matches()) {
                if (currentStops.isNotEmpty()) {
                    daysList.add(ItineraryDay(dayNumber = currentDayNumber, dayTitle = currentDayTitle, stops = currentStops.toList()))
                    currentStops = mutableListOf()
                }
                currentDayNumber = dayMatcher.group(1)?.toIntOrNull() ?: (daysList.size + 1)
                val subtitle = dayMatcher.group(2)?.trim() ?: ""
                currentDayTitle = if (subtitle.isNotBlank()) "Day $currentDayNumber: $subtitle" else "Day $currentDayNumber"
                continue
            }

            // Parse Stop / Milestone line
            val stop = parseStopLine(line, currentStops.size)
            if (stop != null) {
                currentStops.add(stop)
            }
        }

        // Add last day
        if (currentStops.isNotEmpty() || daysList.isEmpty()) {
            daysList.add(ItineraryDay(dayNumber = currentDayNumber, dayTitle = currentDayTitle, stops = currentStops.toList()))
        }

        val finalTitle = if (extractedTitle.isNotBlank()) extractedTitle else defaultTitle

        return ItineraryTripPlan(
            planId = "plan_${System.currentTimeMillis()}",
            tripTitle = finalTitle,
            totalDuration = extractedDuration,
            startDate = extractedDates,
            days = daysList
        )
    }

    private fun parseStopLine(rawLine: String, orderIndex: Int): ItineraryStop? {
        var line = rawLine
        // Remove leading bullets or numbered lists like "1. ", "- ", "* "
        line = line.replace(Regex("^[-*•\\d.]+\\s*"), "").trim()
        if (line.isBlank()) return null

        // Extract Google Maps URL if present
        var mapsUrl = ""
        val urlMatcher = MAPS_URL_REGEX.matcher(line)
        if (urlMatcher.find()) {
            mapsUrl = urlMatcher.group()
            line = line.replace(mapsUrl, "").trim()
        }

        // Extract Time estimate if present
        var visitTime = ""
        val timeMatcher = TIME_REGEX.matcher(line)
        if (timeMatcher.find()) {
            visitTime = timeMatcher.group().trim()
            // Clean time prefix from line
            line = line.replaceFirst(visitTime, "").trim().removePrefix("-").removePrefix(":").trim()
        }

        // Extract Location from "(Location: ...)" if present
        var rawLocation = ""
        val locPattern = Pattern.compile("(?i)\\(Location:\\s*([^)]+)\\)")
        val locMatcher = locPattern.matcher(line)
        if (locMatcher.find()) {
            rawLocation = locMatcher.group(1)?.trim() ?: ""
            val fullMatch = locMatcher.group(0)
            if (fullMatch != null) {
                line = line.replace(fullMatch, "").trim()
            }
        }

        // Split Stop Name and Activity Description by '|' or ' - '
        val parts = if (line.contains("|")) {
            line.split("|", limit = 2)
        } else if (line.contains(" - ")) {
            line.split(" - ", limit = 2)
        } else {
            listOf(line)
        }

        var stopName = parts[0].trim().trimEnd('-', ':').trim()
        var activity = if (parts.size > 1) parts[1].trim() else ""

        // If activity is blank and stopName ends with (Activity)
        if (activity.isBlank()) {
            val activityParenMatcher = Pattern.compile("^(.*?)\\s*\\(([^)]+)\\)$").matcher(stopName)
            if (activityParenMatcher.matches()) {
                stopName = activityParenMatcher.group(1)?.trim() ?: stopName
                activity = activityParenMatcher.group(2)?.trim() ?: ""
            }
        }

        if (rawLocation.isBlank()) {
            rawLocation = stopName
        }

        // Extract coordinates if embedded in URL or raw text
        var lat = 0.0
        var lng = 0.0
        if (mapsUrl.isNotBlank()) {
            val coords = GoogleMapsUrlParser.parseLatLng(mapsUrl)
            if (coords != null) {
                lat = coords.first
                lng = coords.second
            }
        }

        return ItineraryStop(
            stopId = "stop_${System.currentTimeMillis()}_$orderIndex",
            stopName = if (stopName.isNotBlank()) stopName else "Milestone ${orderIndex + 1}",
            activityDescription = activity,
            estimatedVisitTime = visitTime,
            rawLocationText = rawLocation,
            latitude = lat,
            longitude = lng,
            status = ItineraryStopStatus.PENDING,
            orderIndex = orderIndex,
            googleMapsUrl = mapsUrl
        )
    }
}
