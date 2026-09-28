package com.ridesync.engine

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder
import java.util.regex.Pattern

data class ParsedRouteQuery(
    val originQuery: String? = null,
    val destinationQuery: String? = null,
    val waypoints: List<String> = emptyList(),
    val originLat: Double? = null,
    val originLng: Double? = null,
    val destLat: Double? = null,
    val destLng: Double? = null,
    val waypointCoordinates: List<Pair<Double, Double>> = emptyList(),
    val allStops: List<String> = emptyList()
) {
    val hasDirectCoordinates: Boolean
        get() = (originLat != null && originLng != null) || (destLat != null && destLng != null)

    val hasEndpoints: Boolean
        get() = (originQuery != null || originLat != null) && (destinationQuery != null || destLat != null)

    val totalStopsCount: Int
        get() = (if (originQuery != null || originLat != null) 1 else 0) +
                waypoints.size +
                (if (destinationQuery != null || destLat != null) 1 else 0)
}

object GoogleMapsUrlParser {

    private val LAT_LNG_PATTERN = Pattern.compile("(-?\\d+\\.\\d+)[,\\s]+(-?\\d+\\.\\d+)")
    private val AT_COORDS_PATTERN = Pattern.compile("@(-?\\d+\\.\\d+),(-?\\d+\\.\\d+)")
    // In Google Maps protobuf data: !1d<lng>!2d<lat> (1d is Longitude, 2d is Latitude)
    private val PROTOBUF_LNG_LAT_PATTERN = Pattern.compile("!1d(-?\\d+\\.\\d+)!2d(-?\\d+\\.\\d+)")
    // In some cases: !2d<lat>!1d<lng>
    private val PROTOBUF_LAT_LNG_PATTERN = Pattern.compile("!2d(-?\\d+\\.\\d+)!1d(-?\\d+\\.\\d+)")

    fun parseUrl(rawUrl: String): ParsedRouteQuery? {
        val cleanUrl = rawUrl.trim()
        if (cleanUrl.isBlank()) return null

        try {
            // Extract protobuf coordinates from data=!4m... if present
            val protobufCoords = extractProtobufCoordinates(cleanUrl)

            // Case 1: Direction Path format -> /maps/dir/Origin/Stop1/Stop2/.../Destination
            if (cleanUrl.contains("/dir/")) {
                val dirIndex = cleanUrl.indexOf("/dir/")
                val pathAfterDir = cleanUrl.substring(dirIndex + 5)
                
                // Separate path components before query params, data=!4m..., or @lat,lng
                val mainPath = pathAfterDir.split("?")[0]
                    .split("/data=")[0]
                    .split("/@")[0]

                val rawParts = mainPath.split("/")
                    .map { URLDecoder.decode(it, "UTF-8").trim() }
                    .filter { it.isNotBlank() && !it.startsWith("@") && !it.startsWith("data=") }

                if (rawParts.size >= 2) {
                    // In Google Maps /dir/, first element is Origin, last is Destination, and all intermediate are Waypoints!
                    val originPart = cleanLocationName(rawParts.first())
                    val destPart = cleanLocationName(rawParts.last())
                    val waypoints = if (rawParts.size > 2) {
                        rawParts.subList(1, rawParts.size - 1).map { cleanLocationName(it) }
                    } else emptyList()

                    val originCoords = parseLatLng(originPart) ?: protobufCoords.firstOrNull()
                    val destCoords = parseLatLng(destPart) ?: if (protobufCoords.size >= 2) protobufCoords.lastOrNull() else null

                    val waypointCoords = waypoints.mapIndexedNotNull { idx, wp ->
                        parseLatLng(wp) ?: if (protobufCoords.size > idx + 1 && idx + 1 < protobufCoords.size - 1) {
                            protobufCoords[idx + 1]
                        } else null
                    }

                    val allStopsList = mutableListOf<String>()
                    allStopsList.add(originPart)
                    allStopsList.addAll(waypoints)
                    allStopsList.add(destPart)

                    return ParsedRouteQuery(
                        originQuery = if (originCoords == null || !originPart.matches(Regex("^-?\\d+\\.\\d+.*"))) originPart else null,
                        destinationQuery = if (destCoords == null || !destPart.matches(Regex("^-?\\d+\\.\\d+.*"))) destPart else null,
                        waypoints = waypoints,
                        originLat = originCoords?.first,
                        originLng = originCoords?.second,
                        destLat = destCoords?.first,
                        destLng = destCoords?.second,
                        waypointCoordinates = waypointCoords,
                        allStops = allStopsList
                    )
                } else if (rawParts.size == 1 && protobufCoords.size >= 2) {
                    // Path has single or incomplete part, but protobuf has full route stops
                    val originCoords = protobufCoords.first()
                    val destCoords = protobufCoords.last()
                    val wpCoords = protobufCoords.subList(1, protobufCoords.size - 1)
                    val waypoints = wpCoords.mapIndexed { idx, pair -> "Stop ${idx + 1} (${"%.4f".format(pair.first)}, ${"%.4f".format(pair.second)})" }

                    return ParsedRouteQuery(
                        originQuery = cleanLocationName(rawParts[0]),
                        destinationQuery = "Destination (${"%.4f".format(destCoords.first)}, ${"%.4f".format(destCoords.second)})",
                        waypoints = waypoints,
                        originLat = originCoords.first,
                        originLng = originCoords.second,
                        destLat = destCoords.first,
                        destLng = destCoords.second,
                        waypointCoordinates = wpCoords,
                        allStops = listOf(cleanLocationName(rawParts[0])) + waypoints
                    )
                }
            }

            // Case 2: Query Params format -> /dir/?api=1&origin=...&destination=...&waypoints=...
            if (cleanUrl.contains("origin=") || cleanUrl.contains("destination=") || cleanUrl.contains("waypoints=") || cleanUrl.contains("saddr=") || cleanUrl.contains("daddr=")) {
                val decoded = URLDecoder.decode(cleanUrl, "UTF-8")
                val origin = extractQueryParam(decoded, "origin") ?: extractQueryParam(decoded, "saddr")
                var dest = extractQueryParam(decoded, "destination")
                val daddr = extractQueryParam(decoded, "daddr")

                val waypointsList = mutableListOf<String>()
                val waypointsParam = extractQueryParam(decoded, "waypoints")
                if (!waypointsParam.isNullOrBlank()) {
                    val splitWps = waypointsParam.split(Regex("[|;\\n]|%7C")).map { cleanLocationName(it) }.filter { it.isNotBlank() }
                    waypointsList.addAll(splitWps)
                }

                // Handle legacy Google Maps daddr=stop1+to:stop2+to:destination
                if (daddr != null) {
                    if (daddr.contains("+to:") || daddr.contains(" to:")) {
                        val daddrParts = daddr.split(Regex("\\+?to:")).map { cleanLocationName(it) }.filter { it.isNotBlank() }
                        if (daddrParts.isNotEmpty()) {
                            dest = daddrParts.last()
                            if (daddrParts.size > 1) {
                                waypointsList.addAll(0, daddrParts.subList(0, daddrParts.size - 1))
                            }
                        }
                    } else if (dest.isNullOrBlank()) {
                        dest = cleanLocationName(daddr)
                    }
                }

                val originClean = origin?.let { cleanLocationName(it) }
                val destClean = dest?.let { cleanLocationName(it) }

                val originCoords = originClean?.let { parseLatLng(it) } ?: protobufCoords.firstOrNull()
                val destCoords = destClean?.let { parseLatLng(it) } ?: if (protobufCoords.size >= 2) protobufCoords.lastOrNull() else null
                val waypointCoords = waypointsList.mapNotNull { parseLatLng(it) }

                val allStopsList = mutableListOf<String>()
                if (!originClean.isNullOrBlank()) allStopsList.add(originClean)
                allStopsList.addAll(waypointsList)
                if (!destClean.isNullOrBlank()) allStopsList.add(destClean)

                if (originClean != null || destClean != null || protobufCoords.isNotEmpty()) {
                    return ParsedRouteQuery(
                        originQuery = if (originCoords == null || (originClean != null && !originClean.matches(Regex("^-?\\d+\\.\\d+.*")))) originClean else null,
                        destinationQuery = if (destCoords == null || (destClean != null && !destClean.matches(Regex("^-?\\d+\\.\\d+.*")))) destClean else null,
                        waypoints = waypointsList,
                        originLat = originCoords?.first,
                        originLng = originCoords?.second,
                        destLat = destCoords?.first,
                        destLng = destCoords?.second,
                        waypointCoordinates = waypointCoords,
                        allStops = allStopsList
                    )
                }
            }

            // Case 3: Place or Single Search Location format -> /place/18.5204,73.8567 or /place/Name or ?q=...
            if (cleanUrl.contains("/place/") || cleanUrl.contains("?q=") || cleanUrl.contains("&q=")) {
                val decoded = URLDecoder.decode(cleanUrl, "UTF-8")
                var target = if (decoded.contains("/place/")) {
                    val idx = decoded.indexOf("/place/")
                    decoded.substring(idx + 7).split("/")[0].split("?")[0].trim()
                } else {
                    extractQueryParam(decoded, "q")?.trim() ?: ""
                }

                val cleanTarget = cleanLocationName(target)
                val coords = parseLatLng(cleanTarget) ?: protobufCoords.firstOrNull()
                if (coords != null) {
                    return ParsedRouteQuery(
                        destinationQuery = if (cleanTarget.isNotBlank() && !cleanTarget.matches(Regex("^-?\\d+\\.\\d+.*"))) cleanTarget else null,
                        destLat = coords.first,
                        destLng = coords.second,
                        allStops = listOf(cleanTarget)
                    )
                }

                // Check @lat,lng in URL
                val atMatcher = AT_COORDS_PATTERN.matcher(cleanUrl)
                if (atMatcher.find()) {
                    val lat = atMatcher.group(1)?.toDoubleOrNull()
                    val lng = atMatcher.group(2)?.toDoubleOrNull()
                    if (lat != null && lng != null) {
                        return ParsedRouteQuery(
                            destinationQuery = if (cleanTarget.isNotBlank()) cleanTarget else null,
                            destLat = lat,
                            destLng = lng,
                            allStops = listOf(cleanTarget)
                        )
                    }
                }

                if (cleanTarget.isNotBlank()) {
                    return ParsedRouteQuery(
                        destinationQuery = cleanTarget,
                        allStops = listOf(cleanTarget)
                    )
                }
            }

            // Case 4: General coordinate fallback inside URL string or protobuf data
            if (protobufCoords.size >= 2) {
                val orig = protobufCoords.first()
                val dst = protobufCoords.last()
                val wp = protobufCoords.subList(1, protobufCoords.size - 1)
                return ParsedRouteQuery(
                    originLat = orig.first,
                    originLng = orig.second,
                    destLat = dst.first,
                    destLng = dst.second,
                    waypointCoordinates = wp,
                    waypoints = wp.mapIndexed { i, p -> "Stop ${i + 1} (${"%.4f".format(p.first)}, ${"%.4f".format(p.second)})" }
                )
            }

            val generalMatcher = LAT_LNG_PATTERN.matcher(cleanUrl)
            if (generalMatcher.find()) {
                val lat = generalMatcher.group(1)?.toDoubleOrNull()
                val lng = generalMatcher.group(2)?.toDoubleOrNull()
                if (lat != null && lng != null && lat in -90.0..90.0 && lng in -180.0..180.0) {
                    return ParsedRouteQuery(
                        destLat = lat,
                        destLng = lng,
                        allStops = listOf("${"%.4f".format(lat)}, ${"%.4f".format(lng)}")
                    )
                }
            }

        } catch (e: Exception) {
            // Return null on parsing failure
        }
        return null
    }

    /**
     * Extracts sequential GPS coordinates from Google Maps protobuf data strings (e.g. data=!4m...!1d78.4344!2d17.3753)
     */
    private fun extractProtobufCoordinates(url: String): List<Pair<Double, Double>> {
        val list = mutableListOf<Pair<Double, Double>>()
        try {
            // Google Maps standard protobuf: !1d<lng>!2d<lat>
            val matcher1 = PROTOBUF_LNG_LAT_PATTERN.matcher(url)
            while (matcher1.find()) {
                val lng = matcher1.group(1)?.toDoubleOrNull()
                val lat = matcher1.group(2)?.toDoubleOrNull()
                if (lat != null && lng != null && lat in -90.0..90.0 && lng in -180.0..180.0) {
                    list.add(Pair(lat, lng))
                }
            }

            if (list.isEmpty()) {
                // Fallback: !2d<lat>!1d<lng>
                val matcher2 = PROTOBUF_LAT_LNG_PATTERN.matcher(url)
                while (matcher2.find()) {
                    val lat = matcher2.group(1)?.toDoubleOrNull()
                    val lng = matcher2.group(2)?.toDoubleOrNull()
                    if (lat != null && lng != null && lat in -90.0..90.0 && lng in -180.0..180.0) {
                        list.add(Pair(lat, lng))
                    }
                }
            }
        } catch (_: Exception) {}
        return list
    }

    suspend fun resolveShortLink(shortUrl: String): String = withContext(Dispatchers.IO) {
        try {
            val url = URL(shortUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 6000
            connection.readTimeout = 6000
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Android; Mobile)")
            connection.connect()

            val redirectUrl = connection.getHeaderField("Location")
            if (!redirectUrl.isNullOrBlank()) {
                return@withContext redirectUrl
            }
        } catch (e: Exception) {
            // Ignore and return original
        }
        return@withContext shortUrl
    }

    fun parseLatLng(str: String): Pair<Double, Double>? {
        val cleanStr = str.replace("+", " ").trim()
        val matcher = LAT_LNG_PATTERN.matcher(cleanStr)
        if (matcher.find()) {
            val lat = matcher.group(1)?.toDoubleOrNull()
            val lng = matcher.group(2)?.toDoubleOrNull()
            if (lat != null && lng != null && lat in -90.0..90.0 && lng in -180.0..180.0) {
                return Pair(lat, lng)
            }
        }
        return null
    }

    private fun cleanLocationName(raw: String): String {
        return raw.replace("+", " ")
            .replace(Regex("@.*"), "")
            .replace(Regex("data=.*"), "")
            .trim()
    }

    private fun extractQueryParam(url: String, key: String): String? {
        val prefix = "$key="
        val idx = url.indexOf(prefix)
        if (idx == -1) return null
        val start = idx + prefix.length
        val end = url.indexOf("&", start).takeIf { it != -1 } ?: url.length
        val raw = url.substring(start, end)
        return raw.replace("+", " ").trim().takeIf { it.isNotBlank() }
    }
}
