package com.ridesync.ui.trip

import android.location.Geocoder
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.*
import com.ridesync.data.model.ConvoyRole
import com.ridesync.data.model.JoinedRiderProfile
import com.ridesync.data.model.SavedTrip
import com.ridesync.data.model.TripCategory
import com.ridesync.data.model.TripRouteSegment
import com.ridesync.data.model.UserProfile
import com.ridesync.data.model.ItineraryTripPlan
import com.ridesync.data.model.ItineraryDay
import com.ridesync.data.model.ItineraryStop
import com.ridesync.data.model.ItineraryStopStatus
import com.ridesync.data.model.TripCreationMode
import com.ridesync.data.repository.DirectionsRepository
import com.ridesync.data.repository.TripRepository
import com.ridesync.engine.GoogleMapsUrlParser
import com.ridesync.engine.LiveLocationEngine
import com.ridesync.engine.ItineraryParserEngine
import com.ridesync.ui.permissions.PermissionsOnboardingDialog
import com.ridesync.ui.itinerary.ItineraryTrackerScreen
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale


data class PlaceSearchResult(
    val title: String,
    val address: String,
    val latLng: LatLng,
    val category: String = "Location"
)

val POPULAR_MAP_LOCATIONS = listOf(
    // Hyderabad & Telangana Route Locations
    PlaceSearchResult("Attapur, Hyderabad", "Attapur Ring Rd, Rajendranagar, Hyderabad, Telangana 500048, India", LatLng(17.3753, 78.4344), "Hyderabad Origin"),
    PlaceSearchResult("Nagarjuna Sagar Dam", "Vijayapuri North, Nalgonda / Palnadu, Telangana/AP, India", LatLng(16.5772, 79.3125), "Dam & Scenic Route"),
    PlaceSearchResult("Devarakonda, Nalgonda", "Hyderabad - Nagarjuna Sagar Rd (NH565), Telangana 508248, India", LatLng(16.6978, 78.9281), "Highway Stop / Fort"),
    PlaceSearchResult("Ibrahimpatnam, Sagar Rd", "Sagar Rd (NH565), Ibrahimpatnam, Telangana 501506, India", LatLng(17.1856, 78.6473), "Highway Convoy Stop"),
    PlaceSearchResult("Gachibowli, Hyderabad", "Financial District, Gachibowli, Hyderabad, Telangana 500032, India", LatLng(17.4401, 78.3489), "Hyderabad IT Hub"),
    PlaceSearchResult("Hitech City, Hyderabad", "Cyber Towers, Hitech City, Hyderabad, Telangana 500081, India", LatLng(17.4435, 78.3772), "Hyderabad Tech Center"),
    PlaceSearchResult("Banjara Hills, Hyderabad", "Road No. 1, Banjara Hills, Hyderabad, Telangana 500034, India", LatLng(17.4156, 78.4347), "Hyderabad City"),
    PlaceSearchResult("Charminar, Old City", "Char Kaman, Ghansi Bazaar, Hyderabad, Telangana 500002, India", LatLng(17.3616, 78.4747), "Historic Landmark"),
    PlaceSearchResult("Secunderabad Junction", "Station Road, Secunderabad, Telangana 500003, India", LatLng(17.4344, 78.5013), "Transit Hub"),
    PlaceSearchResult("Bangalore Palace", "Vasanth Nagar, Bengaluru, Karnataka 560052, India", LatLng(12.9988, 77.5921), "Bengaluru Landmark"),
    PlaceSearchResult("Marine Drive, Mumbai", "Netaji Subhash Chandra Bose Road, Mumbai, Maharashtra 400020, India", LatLng(18.9438, 72.8234), "Scenic Coastal Road"),
    PlaceSearchResult("India Gate, New Delhi", "Rajpath, India Gate, New Delhi, Delhi 110001, India", LatLng(28.6129, 77.2295), "National Monument"),
    PlaceSearchResult("Calangute Beach, Goa", "Calangute, North Goa, Goa 403516, India", LatLng(15.5438, 73.7554), "Coastal Paradise"),
    // Global Destinations
    PlaceSearchResult("San Francisco, CA", "Market St & Embarcadero, SF, CA", LatLng(37.7749, -122.4194), "City Center"),
    PlaceSearchResult("Lake Tahoe, CA", "Emerald Bay Rd, Lake Tahoe, CA", LatLng(39.0968, -120.0324), "Mountain Destination"),
    PlaceSearchResult("Yosemite National Park", "Yosemite Valley, CA 95389", LatLng(37.8651, -119.5383), "National Park")
)

@Composable
fun TripDatePickerField(
    value: String,
    onDateSelected: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val calendar = java.util.Calendar.getInstance()

    val datePickerDialog = android.app.DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val months = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
            val formatted = String.format("%02d %s %04d", dayOfMonth, months[month.coerceIn(0, 11)], year)
            onDateSelected(formatted)
        },
        calendar.get(java.util.Calendar.YEAR),
        calendar.get(java.util.Calendar.MONTH),
        calendar.get(java.util.Calendar.DAY_OF_MONTH)
    )

    OutlinedTextField(
        value = value,
        onValueChange = {},
        readOnly = true,
        label = { Text(label) },
        placeholder = { Text("Select date from calendar") },
        leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null, tint = Color(0xFFD97706)) },
        trailingIcon = {
            IconButton(onClick = { datePickerDialog.show() }) {
                Icon(Icons.Default.CalendarToday, contentDescription = "Open Calendar", tint = Color(0xFF0052CC))
            }
        },
        singleLine = true,
        modifier = modifier
            .fillMaxWidth()
            .clickable { datePickerDialog.show() },
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color(0xFFFFFFFF),
            unfocusedContainerColor = Color(0xFFFFFFFF),
            focusedTextColor = Color(0xFF0F172A),
            unfocusedTextColor = Color(0xFF0F172A),
            focusedLabelColor = Color(0xFFD97706),
            unfocusedLabelColor = Color(0xFF64748B),
            focusedPlaceholderColor = Color(0xFF64748B),
            unfocusedPlaceholderColor = Color(0xFF64748B),
            cursorColor = Color(0xFF0052CC),
            focusedBorderColor = Color(0xFF0052CC),
            unfocusedBorderColor = Color(0xFFCBD5E1)
        )
    )
}

@Composable
fun TripCreationScreen(
    onStartTripClick: (tripTitle: String, role: ConvoyRole, origin: String, destination: String, waypoints: List<String>, routePolyline: List<LatLng>) -> Unit,
    onShareLobbyClick: (lobbyCode: String) -> Unit,
    userProfile: UserProfile = UserProfile(userId = "user_me", displayName = "Ahmed (You)"),
    initialTab: Int = 0,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var selectedScreenTab by remember(initialTab) { mutableIntStateOf(initialTab) } // 0: Plan & Route Builder, 1: Saved Trips & History

    // Core Route Builder States
    var tripTitle by remember { mutableStateOf("") }
    var tripStartDate by remember { mutableStateOf("15 Oct 2026") }
    var tripEndDate by remember { mutableStateOf("18 Oct 2026") }
    var origin by remember { mutableStateOf("") }
    var destination by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf(ConvoyRole.LEAD) }

    // Trip Vehicle selection (Defaults to user's Default Vehicle, but can be changed per trip)
    val userDefaultVehicle = remember(userProfile) {
        userProfile.vehicles.firstOrNull { it.id == userProfile.activeVehicleId }
            ?: userProfile.vehicles.firstOrNull()
    }
    var selectedTripVehicle by remember(userProfile) { mutableStateOf(userDefaultVehicle) }

    // Google Maps Coordinates State
    var startLatLng by remember { mutableStateOf(LatLng(17.3753, 78.4344)) }
    var destLatLng by remember { mutableStateOf(LatLng(17.4435, 78.3772)) }
    var mapPinSelectionMode by remember { mutableStateOf("START") }

    // Highway Stops / Waypoints
    val waypointNames = remember { mutableStateListOf<String>() }
    val waypointLatLngs = remember { mutableStateListOf<LatLng>() }

    // Google Maps Search Dialog State
    var searchTargetField by remember { mutableStateOf<String?>(null) } // "START", "DEST", or "STOP"
    var searchQuery by remember { mutableStateOf("") }
    var generatedLobbyCode by remember { mutableStateOf("TTS${(1000..9999).random()}") }


    // Real Google Maps Road Polyline State
    var activeRoutePolyline by remember { mutableStateOf<List<LatLng>>(emptyList()) }
    var estimatedDistanceKm by remember { mutableDoubleStateOf(0.0) }
    var estimatedDurationMin by remember { mutableIntStateOf(0) }
    var isFetchingRoute by remember { mutableStateOf(false) }

    // Persistent Saved Trips Flow from TripRepository (Never lost on tab switch or restart!)
    val savedTripsList by TripRepository.tripsFlow.collectAsState()

    // Dialog & Management States
    var selectedTripForDetails by remember { mutableStateOf<SavedTrip?>(null) }
    var tripToEdit by remember { mutableStateOf<SavedTrip?>(null) }
    var tripToDelete by remember { mutableStateOf<SavedTrip?>(null) }
    var tripToExit by remember { mutableStateOf<SavedTrip?>(null) }
    var pendingTripForDaySelection by remember { mutableStateOf<SavedTrip?>(null) }

    // Multi-Day / Multi-Segment Route Links State (Google Maps URL Import)
    val multiDaySegments = remember { mutableStateListOf<TripRouteSegment>() }
    var showAddSegmentDialog by remember { mutableStateOf(false) }
    var showCreationOptionsModal by remember { mutableStateOf(false) }
    var showDayByDayLinksDialog by remember { mutableStateOf(false) }
    var currentItineraryPlan by remember { mutableStateOf<ItineraryTripPlan?>(null) }
    var currentCreationMode by remember { mutableStateOf(TripCreationMode.MANUAL_SEARCH) }
    var showPermissionsDialog by remember { mutableStateOf(false) }
    var isParsingItineraryDocument by remember { mutableStateOf(false) }
    var itineraryUploadProgressText by remember { mutableStateOf("") }
    var viewingItineraryTripId by remember { mutableStateOf<String?>(null) }
    var showShareQrDialogForCreation by remember { mutableStateOf(false) }

    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    isParsingItineraryDocument = true
                    itineraryUploadProgressText = "Validating document size (<=10MB)..."

                    val fileSize = withContext(Dispatchers.IO) {
                        try {
                            context.contentResolver.openFileDescriptor(uri, "r")?.use { it.statSize } ?: 0L
                        } catch (_: Exception) { 0L }
                    }

                    if (fileSize > 10 * 1024 * 1024) {
                        Toast.makeText(context, "File exceeds 10MB limit. Please upload a smaller itinerary.", Toast.LENGTH_LONG).show()
                        isParsingItineraryDocument = false
                        return@launch
                    }

                    itineraryUploadProgressText = "Reading document stream..."
                    val mimeType = context.contentResolver.getType(uri) ?: "text/plain"
                    val rawText = withContext(Dispatchers.IO) {
                        try {
                            context.contentResolver.openInputStream(uri)?.use { stream ->
                                ItineraryParserEngine.extractFromDocumentStream(stream, mimeType)
                            } ?: ""
                        } catch (_: Exception) { "" }
                    }

                    if (rawText.isBlank()) {
                        Toast.makeText(context, "Could not extract text from document.", Toast.LENGTH_SHORT).show()
                        isParsingItineraryDocument = false
                        return@launch
                    }

                    itineraryUploadProgressText = "AI Extracting Multi-Day Schedule & Stops..."
                    val parsedPlan = ItineraryParserEngine.parseTextToTripPlan(rawText, tripTitle)
                    
                    itineraryUploadProgressText = "Resolving Coordinates & Mapping Corridors..."
                    val resolvedDays = withContext(Dispatchers.IO) {
                        parsedPlan.days.map { day ->
                            val resolvedStops = day.stops.mapIndexed { sIdx, stop ->
                                var lat = stop.latitude
                                var lng = stop.longitude
                                if (lat == 0.0 && lng == 0.0) {
                                    val res = DirectionsRepository.resolveLocationNameToLatLng(stop.stopName.ifBlank { stop.rawLocationText }, null)
                                    if (res.latitude != 17.3753 || res.longitude != 78.4344 || stop.stopName.contains("Attapur", ignoreCase = true)) {
                                        lat = res.latitude
                                        lng = res.longitude
                                    } else {
                                        try {
                                            val geocoder = Geocoder(context, Locale.getDefault())
                                            val addrs = geocoder.getFromLocationName(stop.stopName.ifBlank { stop.rawLocationText }, 1)
                                            if (!addrs.isNullOrEmpty()) {
                                                lat = addrs[0].latitude
                                                lng = addrs[0].longitude
                                            }
                                        } catch (_: Exception) {}
                                    }
                                }
                                stop.copy(latitude = lat, longitude = lng, orderIndex = sIdx)
                            }
                            day.copy(stops = resolvedStops)
                        }
                    }

                    val resolvedPlan = parsedPlan.copy(creationMode = TripCreationMode.DOCUMENT, days = resolvedDays)
                    currentItineraryPlan = resolvedPlan
                    currentCreationMode = TripCreationMode.DOCUMENT

                    if (parsedPlan.tripTitle.isNotBlank()) {
                        tripTitle = parsedPlan.tripTitle
                    }

                    // Populate multi-day segments & sync main builder fields
                    multiDaySegments.clear()
                    waypointNames.clear()
                    waypointLatLngs.clear()

                    val allStops = resolvedDays.flatMap { it.stops }
                    if (allStops.isNotEmpty()) {
                        val firstStop = allStops.first()
                        val lastStop = allStops.last()
                        origin = firstStop.stopName
                        if (firstStop.latitude != 0.0) startLatLng = LatLng(firstStop.latitude, firstStop.longitude)
                        destination = lastStop.stopName
                        if (lastStop.latitude != 0.0) destLatLng = LatLng(lastStop.latitude, lastStop.longitude)

                        val middleStops = if (allStops.size > 2) allStops.subList(1, allStops.size - 1) else emptyList()
                        middleStops.forEach { st ->
                            waypointNames.add(st.stopName)
                            waypointLatLngs.add(LatLng(st.latitude, st.longitude))
                        }
                    }

                    resolvedDays.forEachIndexed { dayIdx, day ->
                        val dayStops = day.stops
                        val originStop = dayStops.firstOrNull()?.stopName ?: origin
                        val destStop = dayStops.lastOrNull()?.stopName ?: destination
                        val waypoints = if (dayStops.size > 2) {
                            dayStops.subList(1, dayStops.size - 1).map { it.stopName }
                        } else emptyList()

                        val seg = TripRouteSegment(
                            segmentId = "seg_${System.currentTimeMillis()}_$dayIdx",
                            segmentName = day.dayTitle,
                            googleMapsUrl = dayStops.firstOrNull { it.googleMapsUrl.isNotBlank() }?.googleMapsUrl ?: "",
                            originName = originStop,
                            destinationName = destStop,
                            waypoints = waypoints,
                            waypointsCount = waypoints.size,
                            orderIndex = dayIdx
                        )
                        multiDaySegments.add(seg)
                    }

                    Toast.makeText(context, "Extracted ${resolvedDays.size} Days & ${resolvedDays.sumOf { it.stops.size }} Stops!", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(context, "Itinerary parsing error: ${e.message}", Toast.LENGTH_SHORT).show()
                } finally {
                    isParsingItineraryDocument = false
                }
            }
        }
    }


    // Fetch Real Google Maps Directions Road Route & Calculate Accurate Distance
    LaunchedEffect(startLatLng, destLatLng, waypointLatLngs.toList()) {
        isFetchingRoute = true
        try {
            val routeDetails = DirectionsRepository.getDirectionsRoute(
                origin = startLatLng,
                destination = destLatLng,
                waypoints = waypointLatLngs.toList()
            )
            activeRoutePolyline = routeDetails.polylinePoints
            val calcKm = DirectionsRepository.calculateRoadDistanceKm(
                startLatLng, destLatLng, waypointLatLngs.toList(), activeRoutePolyline
            )
            estimatedDistanceKm = if (routeDetails.distanceKm > 0) routeDetails.distanceKm else calcKm
            estimatedDurationMin = if (routeDetails.durationMinutes > 0) routeDetails.durationMinutes else (calcKm * 1.35).toInt().coerceAtLeast(10)
        } catch (e: Exception) {
            e.printStackTrace()
            val calcKm = DirectionsRepository.calculateRoadDistanceKm(
                startLatLng, destLatLng, waypointLatLngs.toList(), activeRoutePolyline
            )
            estimatedDistanceKm = calcKm
            estimatedDurationMin = (calcKm * 1.35).toInt().coerceAtLeast(10)
        } finally {
            isFetchingRoute = false
        }
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(startLatLng, 8f)
    }

    var mainGoogleMapsUrlInput by remember { mutableStateOf("") }
    var isImportingRouteLink by remember { mutableStateOf(false) }

    suspend fun resolveLocation(locStr: String, fallback: LatLng): LatLng {
        val coords = GoogleMapsUrlParser.parseLatLng(locStr)
        if (coords != null) return LatLng(coords.first, coords.second)
        return withContext(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                val addrs = geocoder.getFromLocationName(locStr, 1)
                if (!addrs.isNullOrEmpty()) {
                    LatLng(addrs[0].latitude, addrs[0].longitude)
                } else fallback
            } catch (_: Exception) {
                fallback
            }
        }
    }

    fun importRouteFromGoogleMapsUrl(rawUrl: String) {
        if (rawUrl.isBlank()) return
        coroutineScope.launch {
            isImportingRouteLink = true
            try {
                var effectiveUrl = rawUrl.trim()
                if (effectiveUrl.contains("goo.gl") || effectiveUrl.contains("maps.app")) {
                    effectiveUrl = GoogleMapsUrlParser.resolveShortLink(effectiveUrl)
                }
                val parsed = GoogleMapsUrlParser.parseUrl(effectiveUrl)
                if (parsed != null) {
                    var newOriginName = origin
                    var newStartLatLng = startLatLng
                    if (!parsed.originQuery.isNullOrBlank()) {
                        newOriginName = parsed.originQuery
                        newStartLatLng = resolveLocation(parsed.originQuery, startLatLng)
                    } else if (parsed.originLat != null && parsed.originLng != null) {
                        newStartLatLng = LatLng(parsed.originLat, parsed.originLng)
                        newOriginName = "${"%.4f".format(parsed.originLat)}, ${"%.4f".format(parsed.originLng)}"
                    }

                    var newDestName = destination
                    var newDestLatLng = destLatLng
                    if (!parsed.destinationQuery.isNullOrBlank()) {
                        newDestName = parsed.destinationQuery
                        newDestLatLng = resolveLocation(parsed.destinationQuery, destLatLng)
                    } else if (parsed.destLat != null && parsed.destLng != null) {
                        newDestLatLng = LatLng(parsed.destLat, parsed.destLng)
                        newDestName = "${"%.4f".format(parsed.destLat)}, ${"%.4f".format(parsed.destLng)}"
                    }

                    origin = newOriginName
                    startLatLng = newStartLatLng
                    destination = newDestName
                    destLatLng = newDestLatLng

                    // Intermediate Stops
                    waypointNames.clear()
                    waypointLatLngs.clear()

                    val parsedWaypoints = parsed.waypoints
                    waypointNames.addAll(parsedWaypoints)

                    val resolvedWps = parsedWaypoints.mapIndexed { idx, wpStr ->
                        val defaultFallback = LatLng(
                            newStartLatLng.latitude + (newDestLatLng.latitude - newStartLatLng.latitude) * ((idx + 1.0) / (parsedWaypoints.size + 1.0)),
                            newStartLatLng.longitude + (newDestLatLng.longitude - newStartLatLng.longitude) * ((idx + 1.0) / (parsedWaypoints.size + 1.0))
                        )
                        resolveLocation(wpStr, defaultFallback)
                    }
                    waypointLatLngs.addAll(resolvedWps)

                    // Strictly preserve user-entered custom title! Only set auto title if blank.
                    if (tripTitle.trim().isEmpty()) {
                        val cleanOrig = newOriginName.split(",").firstOrNull()?.trim() ?: newOriginName
                        val cleanDest = newDestName.split(",").firstOrNull()?.trim() ?: newDestName
                        tripTitle = "$cleanOrig to $cleanDest Ride"
                    }

                    // Fetch road route and calculate accurate distance KM immediately
                    val routeDetails = DirectionsRepository.getDirectionsRoute(
                        origin = newStartLatLng,
                        destination = newDestLatLng,
                        waypoints = resolvedWps
                    )
                    activeRoutePolyline = routeDetails.polylinePoints
                    val calcKm = DirectionsRepository.calculateRoadDistanceKm(
                        newStartLatLng, newDestLatLng, resolvedWps, activeRoutePolyline
                    )
                    estimatedDistanceKm = if (routeDetails.distanceKm > 0) routeDetails.distanceKm else calcKm
                    estimatedDurationMin = if (routeDetails.durationMinutes > 0) routeDetails.durationMinutes else (calcKm * 1.35).toInt().coerceAtLeast(10)

                    // Animate camera to fit route bounds
                    coroutineScope.launch {
                        kotlinx.coroutines.delay(350)
                        if (activeRoutePolyline.isNotEmpty()) {
                            val boundsBuilder = LatLngBounds.builder()
                            boundsBuilder.include(newStartLatLng)
                            boundsBuilder.include(newDestLatLng)
                            resolvedWps.forEach { boundsBuilder.include(it) }
                            activeRoutePolyline.forEach { boundsBuilder.include(it) }
                            cameraPositionState.animate(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 70))
                        } else {
                            cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(newStartLatLng, 9f))
                        }
                    }

                    Toast.makeText(
                        context,
                        "Imported Google Maps route! Distance: ${"%.1f".format(estimatedDistanceKm)} KM (${parsedWaypoints.size} stops)",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(context, "Could not parse Google Maps URL. Check format.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error importing link: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isImportingRouteLink = false
            }
        }
    }

    fun syncAndRecalculateManualRoute() {
        coroutineScope.launch {
            isFetchingRoute = true
            try {
                val originParam = resolveLocation(origin, startLatLng)
                val destParam = resolveLocation(destination, destLatLng)
                startLatLng = originParam
                destLatLng = destParam

                val updatedWps = waypointNames.mapIndexed { idx, name ->
                    val defaultFallback = LatLng(
                        originParam.latitude + (destParam.latitude - originParam.latitude) * ((idx + 1.0) / (waypointNames.size + 1.0)),
                        originParam.longitude + (destParam.longitude - originParam.longitude) * ((idx + 1.0) / (waypointNames.size + 1.0))
                    )
                    resolveLocation(name, defaultFallback)
                }

                waypointLatLngs.clear()
                waypointLatLngs.addAll(updatedWps)

                // If trip title was blank, generate clean title
                if (tripTitle.trim().isEmpty()) {
                    val cleanOrig = origin.split(",").firstOrNull()?.trim() ?: origin
                    val cleanDest = destination.split(",").firstOrNull()?.trim() ?: destination
                    tripTitle = "$cleanOrig to $cleanDest Ride"
                }

                val routeDetails = DirectionsRepository.getDirectionsRoute(
                    origin = originParam,
                    destination = destParam,
                    waypoints = updatedWps
                )
                activeRoutePolyline = routeDetails.polylinePoints
                val calcKm = DirectionsRepository.calculateRoadDistanceKm(
                    originParam, destParam, updatedWps, activeRoutePolyline
                )
                estimatedDistanceKm = if (routeDetails.distanceKm > 0) routeDetails.distanceKm else calcKm
                estimatedDurationMin = if (routeDetails.durationMinutes > 0) routeDetails.durationMinutes else (calcKm * 1.35).toInt().coerceAtLeast(10)

                if (activeRoutePolyline.isNotEmpty()) {
                    val boundsBuilder = LatLngBounds.builder()
                    boundsBuilder.include(originParam)
                    boundsBuilder.include(destParam)
                    updatedWps.forEach { boundsBuilder.include(it) }
                    activeRoutePolyline.forEach { boundsBuilder.include(it) }
                    cameraPositionState.animate(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 70))
                }

                Toast.makeText(
                    context,
                    "Synced route across ${2 + updatedWps.size} stops (${"%.1f".format(estimatedDistanceKm)} km)",
                    Toast.LENGTH_SHORT
                ).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Route sync error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isFetchingRoute = false
            }
        }
    }

    val backgroundColor = Color(0xFFF1F5F9)
    val cardBg = Color(0xFFFFFFFF)
    val accentColor = Color(0xFF0052CC)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        // High-Contrast Rally Instrument Graphic Background Pattern
        com.ridesync.ui.theme.RallyGridGraphicBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Screen Header
            Text(
                text = "Create New Trip",
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF0F172A)
            )
            Text(
                text = "Build multi-day itinerary routes, import map links & setup convoy",
                fontSize = 14.sp,
                color = Color(0xFF475569),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // UNIFIED SINGLE CREATE TRIP & ROUTE BUILDER FORM
            // 1. Convoy Role Selection
            Text(
                text = "Select Convoy Role",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A),
                modifier = Modifier.padding(bottom = 10.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                RoleChip(
                    role = ConvoyRole.LEAD,
                    title = "Lead Rider",
                    icon = Icons.Default.DirectionsBike,
                    isSelected = selectedRole == ConvoyRole.LEAD,
                    accentColor = accentColor,
                    modifier = Modifier.weight(1f),
                    onClick = { selectedRole = ConvoyRole.LEAD }
                )
                RoleChip(
                    role = ConvoyRole.SWEEP,
                    title = "Sweep Safety",
                    icon = Icons.Default.Shield,
                    isSelected = selectedRole == ConvoyRole.SWEEP,
                    accentColor = accentColor,
                    modifier = Modifier.weight(1f),
                    onClick = { selectedRole = ConvoyRole.SWEEP }
                )
                RoleChip(
                    role = ConvoyRole.MEMBER,
                    title = "Pack Rider",
                    icon = Icons.Default.CheckCircle,
                    isSelected = selectedRole == ConvoyRole.MEMBER,
                    accentColor = accentColor,
                    modifier = Modifier.weight(1f),
                    onClick = { selectedRole = ConvoyRole.MEMBER }
                )
            }

            // Role Detailed Explanation Box
            Surface(
                color = when (selectedRole) {
                    ConvoyRole.LEAD -> Color(0xFFFEF3C7)
                    ConvoyRole.SWEEP -> Color(0xFFE0E7FF)
                    ConvoyRole.MEMBER -> Color(0xFFDCFCE7)
                },
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    when (selectedRole) {
                        ConvoyRole.LEAD -> Color(0xFFD97706)
                        ConvoyRole.SWEEP -> Color(0xFF0052CC)
                        ConvoyRole.MEMBER -> Color(0xFF16A34A)
                    }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text(
                        text = when (selectedRole) {
                            ConvoyRole.LEAD -> "👑"
                            ConvoyRole.SWEEP -> "🛡️"
                            ConvoyRole.MEMBER -> "🏍️"
                        },
                        fontSize = 20.sp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = when (selectedRole) {
                                ConvoyRole.LEAD -> "LEAD RIDER (#1 Front Position)"
                                ConvoyRole.SWEEP -> "SWEEP SAFETY RIDER (Rear Guard Anchor)"
                                ConvoyRole.MEMBER -> "PACK RIDER (Middle Formation Member)"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = when (selectedRole) {
                                ConvoyRole.LEAD -> Color(0xFF92400E)
                                ConvoyRole.SWEEP -> Color(0xFF1E40AF)
                                ConvoyRole.MEMBER -> Color(0xFF166534)
                            }
                        )
                        Text(
                            text = when (selectedRole) {
                                ConvoyRole.LEAD -> "Navigates the route, sets safe cruising pace, signals hazards/turns, and commands front position #1 on the map."
                                ConvoyRole.SWEEP -> "Positioned at the very rear (last position) to protect the pack, assist with mechanical breakdowns/emergencies, and ensure no rider is left behind."
                                ConvoyRole.MEMBER -> "Rides in middle convoy formation, maintains staggered distance, and relays turn/hazard signals between Lead and Sweep."
                            },
                            fontSize = 11.sp,
                            color = Color(0xFF334155),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // 2. Vehicle Selection for Trip
            var showVehicleDropdown by remember { mutableStateOf(false) }

            Surface(
                color = Color(0xFFFFFFFF),
                shape = RoundedCornerShape(14.dp),
                shadowElevation = 2.dp,
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFCBD5E1)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = selectedTripVehicle?.vehicleTypeEnum?.iconEmoji ?: "🏍️",
                                fontSize = 20.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Trip Vehicle",
                                        fontSize = 12.sp,
                                        color = Color(0xFF64748B)
                                    )
                                    if (selectedTripVehicle?.id == userProfile.activeVehicleId) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            color = Color(0xFFEFF6FF),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = "★ DEFAULT",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Black,
                                                color = Color(0xFF0052CC),
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = selectedTripVehicle?.fullDisplayName ?: userProfile.vehicleModel.ifBlank { "Royal Enfield Meteor 350" },
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                            }
                        }

                        if (userProfile.vehicles.size > 1) {
                            TextButton(onClick = { showVehicleDropdown = !showVehicleDropdown }) {
                                Text(
                                    text = if (showVehicleDropdown) "Close" else "Change",
                                    color = Color(0xFF0052CC),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            Text(
                                text = "${selectedTripVehicle?.estimatedRangeKm ?: 350.0} km range",
                                color = Color(0xFF059669),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (showVehicleDropdown && userProfile.vehicles.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Divider(color = Color(0xFFE2E8F0))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Select vehicle for this trip:",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B),
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        userProfile.vehicles.forEach { v ->
                            val isSelected = selectedTripVehicle?.id == v.id
                            Surface(
                                onClick = {
                                    selectedTripVehicle = v
                                    showVehicleDropdown = false
                                },
                                color = if (isSelected) Color(0xFFEFF6FF) else Color.Transparent,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${v.vehicleTypeEnum.iconEmoji} ${v.fullDisplayName}",
                                        color = if (isSelected) Color(0xFF0052CC) else Color(0xFF0F172A),
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                    if (v.id == userProfile.activeVehicleId) {
                                        Text("★ Default", fontSize = 10.sp, color = Color(0xFFD97706), fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. Main Trip Information (Title & Dates)
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFFFF)),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFCBD5E1)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Trip Essentials",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        modifier = Modifier.padding(bottom = 10.dp)
                    )

                    OutlinedTextField(
                        value = tripTitle,
                        onValueChange = { tripTitle = it },
                        label = { Text("Trip Title") },
                        placeholder = { Text("Enter your custom trip title") },
                        leadingIcon = { Icon(Icons.Default.Route, contentDescription = null, tint = accentColor) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFFFFFFF),
                            unfocusedContainerColor = Color(0xFFFFFFFF),
                            focusedTextColor = Color(0xFF0F172A),
                            unfocusedTextColor = Color(0xFF0F172A),
                            focusedLabelColor = accentColor,
                            unfocusedLabelColor = Color(0xFF64748B),
                            focusedPlaceholderColor = Color(0xFF64748B),
                            unfocusedPlaceholderColor = Color(0xFF64748B),
                            cursorColor = accentColor,
                            focusedBorderColor = accentColor,
                            unfocusedBorderColor = Color(0xFFCBD5E1)
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        TripDatePickerField(
                            value = tripStartDate,
                            onDateSelected = { tripStartDate = it },
                            label = "Trip Start Date",
                            modifier = Modifier.weight(1f)
                        )
                        TripDatePickerField(
                            value = tripEndDate,
                            onDateSelected = { tripEndDate = it },
                            label = "Trip End Date",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 4. Unified Route Creation Card (Google Maps Link Paste OR Origin/Destination Search)
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFFFF)),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFCBD5E1)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Route Creation Options",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = "Paste a Google Maps link OR search Start location, Destination & Stops below",
                        fontSize = 12.sp,
                        color = Color(0xFF475569),
                        modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                    )

                    // OPTION A: Google Maps Link Paste Box
                    OutlinedTextField(
                        value = mainGoogleMapsUrlInput,
                        onValueChange = { url ->
                            mainGoogleMapsUrlInput = url
                            if (url.isNotBlank() && (url.contains("maps.app") || url.contains("/dir/"))) {
                                importRouteFromGoogleMapsUrl(url)
                            }
                        },
                        label = { Text("Paste Google Maps Link (Auto-Extracts Route)") },
                        placeholder = { Text("https://maps.app.goo.gl/... or /dir/...") },
                        leadingIcon = { Icon(Icons.Default.AddLink, contentDescription = null, tint = Color(0xFF0052CC)) },
                        trailingIcon = {
                            if (isImportingRouteLink) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color(0xFF0052CC), strokeWidth = 2.dp)
                            } else if (mainGoogleMapsUrlInput.isNotBlank()) {
                                IconButton(onClick = { importRouteFromGoogleMapsUrl(mainGoogleMapsUrlInput) }) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Import", tint = Color(0xFF0052CC))
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFFFFFFF),
                            unfocusedContainerColor = Color(0xFFFFFFFF),
                            focusedTextColor = Color(0xFF0F172A),
                            unfocusedTextColor = Color(0xFF0F172A),
                            focusedLabelColor = Color(0xFF0052CC),
                            unfocusedLabelColor = Color(0xFF64748B),
                            focusedPlaceholderColor = Color(0xFF64748B),
                            unfocusedPlaceholderColor = Color(0xFF64748B),
                            cursorColor = Color(0xFF0052CC),
                            focusedBorderColor = Color(0xFF0052CC),
                            unfocusedBorderColor = Color(0xFFCBD5E1)
                        )
                    )

                    HorizontalDivider(color = Color(0xFFE2E8F0))
                    Spacer(modifier = Modifier.height(14.dp))

                    // OPTION B: Origin, Destination & Stops Search
                    Text(
                        text = "Or Search Start, Destination & Stops",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        modifier = Modifier.padding(bottom = 10.dp)
                    )

                    // Origin Input
                    OutlinedTextField(
                        value = origin,
                        onValueChange = { origin = it },
                        label = { Text("Start Location (Origin)") },
                        leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFF059669)) },
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        searchTargetField = "START"
                                        searchQuery = ""
                                    }
                                ) {
                                    Icon(Icons.Default.Search, contentDescription = "Search Google Maps", tint = Color(0xFF0052CC))
                                }
                                IconButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            val phoneGps = LiveLocationEngine.getCurrentPhoneLocation(context, forceFresh = true)
                                            if (phoneGps != null) {
                                                startLatLng = phoneGps
                                                origin = "Current Phone GPS (${"%.4f".format(phoneGps.latitude)}, ${"%.4f".format(phoneGps.longitude)})"
                                                cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(startLatLng, 14f))
                                                Toast.makeText(context, "Start location set to live GPS!", Toast.LENGTH_SHORT).show()
                                            } else {
                                                startLatLng = LatLng(17.3753, 78.4344)
                                                origin = "Current GPS (17.3753, 78.4344)"
                                                cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(startLatLng, 12f))
                                                Toast.makeText(context, "Fetching GPS location...", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.MyLocation, contentDescription = "Use GPS", tint = Color(0xFF059669))
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFFFFFFF),
                            unfocusedContainerColor = Color(0xFFFFFFFF),
                            focusedTextColor = Color(0xFF0F172A),
                            unfocusedTextColor = Color(0xFF0F172A),
                            focusedLabelColor = Color(0xFF059669),
                            unfocusedLabelColor = Color(0xFF64748B),
                            focusedPlaceholderColor = Color(0xFF64748B),
                            unfocusedPlaceholderColor = Color(0xFF64748B),
                            cursorColor = accentColor,
                            focusedBorderColor = Color(0xFF059669),
                            unfocusedBorderColor = Color(0xFFCBD5E1)
                        )
                    )

                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                    ) {
                        TextButton(
                            onClick = {
                                val tempName = origin
                                origin = destination
                                destination = tempName

                                val tempLatLng = startLatLng
                                startLatLng = destLatLng
                                destLatLng = tempLatLng
                            }
                        ) {
                            Icon(Icons.Default.SwapVert, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Swap Start & Destination", color = accentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Destination Input
                    OutlinedTextField(
                        value = destination,
                        onValueChange = { destination = it },
                        label = { Text("Destination Location") },
                        leadingIcon = { Icon(Icons.Default.Flag, contentDescription = null, tint = Color(0xFFDC2626)) },
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    searchTargetField = "DEST"
                                    searchQuery = ""
                                }
                            ) {
                                Icon(Icons.Default.Search, contentDescription = "Search Google Maps", tint = Color(0xFF0052CC))
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFFFFFFF),
                            unfocusedContainerColor = Color(0xFFFFFFFF),
                            focusedTextColor = Color(0xFF0F172A),
                            unfocusedTextColor = Color(0xFF0F172A),
                            focusedLabelColor = Color(0xFFDC2626),
                            unfocusedLabelColor = Color(0xFF64748B),
                            focusedPlaceholderColor = Color(0xFF64748B),
                            unfocusedPlaceholderColor = Color(0xFF64748B),
                            cursorColor = accentColor,
                            focusedBorderColor = Color(0xFFDC2626),
                            unfocusedBorderColor = Color(0xFFCBD5E1)
                        )
                    )

                    Button(
                        onClick = { syncAndRecalculateManualRoute() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEFF6FF), contentColor = Color(0xFF0052CC)),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF0052CC)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                    ) {
                        Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Sync Locations & Update Route on Map", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            // 5. Multi-Day Day Routes Section ("+ Add Day Route" direct option)
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFFFF)),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFCBD5E1)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Multi-Day Day Routes (${multiDaySegments.size})",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "Add day-by-day legs using search or map links for multi-day trips",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                        }

                        Button(
                            onClick = { showAddSegmentDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEFF6FF), contentColor = Color(0xFF0052CC)),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF0052CC))
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("+ Add Day Route", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (multiDaySegments.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        multiDaySegments.forEachIndexed { index, segment ->
                            Surface(
                                color = Color(0xFFF8FAFC),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(12.dp)
                                ) {
                                    Surface(
                                        color = Color(0xFF0052CC),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.size(26.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text("${index + 1}", color = Color.White, fontWeight = FontWeight.Black, fontSize = 12.sp)
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(segment.segmentName.ifBlank { "Day ${index + 1}" }, color = Color(0xFF0F172A), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                        Text("${segment.originName} ➔ ${segment.destinationName}", color = Color(0xFF475569), fontSize = 12.sp)
                                        if (segment.waypoints.isNotEmpty()) {
                                            Text("📍 Stops (${segment.waypoints.size}): ${segment.waypoints.joinToString(" ➔ ")}", color = Color(0xFF0052CC), fontSize = 11.sp)
                                        }
                                        Text("📏 ${"%.1f".format(segment.distanceKm)} KM • ⏱️ ${segment.estimatedDurationMinutes / 60}h ${segment.estimatedDurationMinutes % 60}m", color = Color(0xFFD97706), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    IconButton(
                                        onClick = {
                                            if (index < multiDaySegments.size) {
                                                multiDaySegments.removeAt(index)
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 6. Interactive Google Map Route Preview Card & Distance Readout
            Text(
                text = "Interactive Google Map Route Preview",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A),
                modifier = Modifier.padding(bottom = 10.dp)
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFFFF)),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFCBD5E1)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = mapPinSelectionMode == "START",
                            onClick = { mapPinSelectionMode = "START" },
                            label = { Text("📍 Set Start", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF059669),
                                selectedLabelColor = Color.White,
                                containerColor = Color(0xFFF8FAFC),
                                labelColor = Color(0xFF475569)
                            )
                        )

                        FilterChip(
                            selected = mapPinSelectionMode == "DEST",
                            onClick = { mapPinSelectionMode = "DEST" },
                            label = { Text("🏁 Set Dest", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFDC2626),
                                selectedLabelColor = Color.White,
                                containerColor = Color(0xFFF8FAFC),
                                labelColor = Color(0xFF475569)
                            )
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(startLatLng, 8f))
                                }
                            }
                        ) {
                            Icon(Icons.Default.Map, contentDescription = "Fit Route", tint = accentColor)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(12.dp))
                    ) {
                        GoogleMap(
                            modifier = Modifier.fillMaxSize(),
                            cameraPositionState = cameraPositionState,
                            uiSettings = MapUiSettings(
                                zoomControlsEnabled = true,
                                compassEnabled = true,
                                myLocationButtonEnabled = false,
                                mapToolbarEnabled = true
                            ),
                            properties = MapProperties(isTrafficEnabled = false),
                            onMapClick = { clickedLatLng ->
                                when (mapPinSelectionMode) {
                                    "START" -> {
                                        startLatLng = clickedLatLng
                                        origin = "Map Pin (${"%.4f".format(clickedLatLng.latitude)}, ${"%.4f".format(clickedLatLng.longitude)})"
                                        Toast.makeText(context, "Start location set to map pin!", Toast.LENGTH_SHORT).show()
                                    }
                                    "DEST" -> {
                                        destLatLng = clickedLatLng
                                        destination = "Map Pin (${"%.4f".format(clickedLatLng.latitude)}, ${"%.4f".format(clickedLatLng.longitude)})"
                                        Toast.makeText(context, "Destination set to map pin!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        ) {
                            Marker(
                                state = rememberMarkerState(position = startLatLng),
                                title = "Start: $origin",
                                snippet = "Convoy Origin",
                                icon = com.google.android.gms.maps.model.BitmapDescriptorFactory.defaultMarker(
                                    com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_GREEN
                                )
                            )

                            Marker(
                                state = rememberMarkerState(position = destLatLng),
                                title = "Destination: $destination",
                                snippet = "Convoy Target",
                                icon = com.google.android.gms.maps.model.BitmapDescriptorFactory.defaultMarker(
                                    com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_RED
                                )
                            )

                            waypointLatLngs.forEachIndexed { index, wpLatLng ->
                                val stopTitle = if (index < waypointNames.size) waypointNames[index] else "Stop ${index + 1}"
                                Marker(
                                    state = rememberMarkerState(position = wpLatLng),
                                    title = "Stop ${index + 1}: $stopTitle",
                                    snippet = "Highway Rest / Regroup Point",
                                    icon = com.google.android.gms.maps.model.BitmapDescriptorFactory.defaultMarker(
                                        com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_AZURE
                                    )
                                )
                            }

                            if (activeRoutePolyline.isNotEmpty()) {
                                Polyline(
                                    points = activeRoutePolyline,
                                    color = Color(0xFF0052CC),
                                    width = 12f
                                )
                            }
                        }

                        if (isFetchingRoute) {
                            Surface(
                                color = Color.White.copy(alpha = 0.9f),
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF0052CC)),
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(top = 10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(14.dp), color = accentColor, strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Calculating Road Route...", color = Color(0xFF0F172A), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val displayDist = if (estimatedDistanceKm > 0) estimatedDistanceKm else DirectionsRepository.calculateRoadDistanceKm(startLatLng, destLatLng, waypointLatLngs.toList(), activeRoutePolyline)
                        Text(
                            text = "📏 Route: ${"%.1f".format(displayDist)} KM",
                            color = Color(0xFF0F172A),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "⏱️ Est. Time: ${estimatedDurationMin / 60}h ${estimatedDurationMin % 60}m",
                            color = accentColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // 7. Intermediate Highway Stops Section
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
            ) {
                Text(
                    text = "Intermediate Stops (${waypointNames.size})",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )

                Button(
                    onClick = {
                        searchTargetField = "STOP"
                        searchQuery = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEFF6FF), contentColor = Color(0xFF0052CC)),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF0052CC))
                ) {
                    Icon(Icons.Default.AddLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("+ Add Stop", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFFFF)),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFCBD5E1)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (waypointNames.isEmpty()) {
                        Text(
                            text = "No intermediate stops added yet. Tap '+ Add Stop' above to search places on Google Maps.",
                            color = Color(0xFF64748B),
                            fontSize = 13.sp,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        waypointNames.forEachIndexed { index, wpName ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                                    .background(Color(0xFFF8FAFC), RoundedCornerShape(10.dp))
                                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Surface(
                                    color = Color(0xFFEFF6FF),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "STOP ${index + 1}",
                                        color = Color(0xFF0052CC),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = wpName,
                                    color = Color(0xFF0F172A),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = {
                                        if (index < waypointNames.size && index < waypointLatLngs.size) {
                                            waypointNames.removeAt(index)
                                            waypointLatLngs.removeAt(index)
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete Stop", tint = Color(0xFFDC2626), modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Permissions & Policy Setup Button
            OutlinedButton(
                onClick = { showPermissionsDialog = true },
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF059669)),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFFECFDF5), contentColor = Color(0xFF059669)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "🛡️ Device Permissions & Policy Setup",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Single Prominent "Save Trip" Button
            Button(
                onClick = {
                    val finalTitle = tripTitle.ifBlank {
                        if (origin.isNotBlank() && destination.isNotBlank()) "$origin to $destination Ride"
                        else "Multi-Day Convoy Tour"
                    }
                    val freshLobbyCode = "TTS${(1000..9999).random()}"

                    val finalPlan = currentItineraryPlan ?: run {
                        val dayStops = mutableListOf<ItineraryStop>()
                        dayStops.add(
                            ItineraryStop(
                                stopId = "stop_0",
                                stopName = origin.ifBlank { "Start Point" },
                                activityDescription = "Starting Point (Origin)",
                                latitude = startLatLng.latitude,
                                longitude = startLatLng.longitude,
                                status = ItineraryStopStatus.PENDING,
                                orderIndex = 0
                            )
                        )
                        waypointNames.forEachIndexed { idx, wpName ->
                            val latLng = waypointLatLngs.getOrNull(idx) ?: LatLng(0.0, 0.0)
                            dayStops.add(
                                ItineraryStop(
                                    stopId = "stop_${idx + 1}",
                                    stopName = wpName,
                                    activityDescription = "Intermediate Stop / Route Point",
                                    latitude = latLng.latitude,
                                    longitude = latLng.longitude,
                                    status = ItineraryStopStatus.PENDING,
                                    orderIndex = idx + 1
                                )
                            )
                        }
                        dayStops.add(
                            ItineraryStop(
                                stopId = "stop_${dayStops.size}",
                                stopName = destination.ifBlank { "Destination" },
                                activityDescription = "Ending Point (Destination)",
                                latitude = destLatLng.latitude,
                                longitude = destLatLng.longitude,
                                status = ItineraryStopStatus.PENDING,
                                orderIndex = dayStops.size
                            )
                        )
                        ItineraryTripPlan(
                            planId = "plan_${System.currentTimeMillis()}",
                            creationMode = currentCreationMode,
                            tripTitle = finalTitle,
                            startDate = tripStartDate,
                            totalDuration = if (multiDaySegments.isNotEmpty()) "${multiDaySegments.size} Days"
                                            else if (estimatedDurationMin > 0) "${estimatedDurationMin / 60}h ${estimatedDurationMin % 60}m"
                                            else "1 Day",
                            days = if (multiDaySegments.isNotEmpty()) {
                                multiDaySegments.mapIndexed { idx, seg ->
                                    ItineraryDay(
                                        dayNumber = idx + 1,
                                        dayTitle = seg.segmentName.ifBlank { "Day ${idx + 1}" },
                                        date = tripStartDate,
                                        stops = listOf(
                                            ItineraryStop(stopId = "stop_${idx}_0", stopName = seg.originName.ifBlank { "Start" }, orderIndex = 0)
                                        ) + seg.waypoints.mapIndexed { wIdx, wp ->
                                            ItineraryStop(stopId = "stop_${idx}_${wIdx + 1}", stopName = wp, orderIndex = wIdx + 1)
                                        } + listOf(
                                            ItineraryStop(stopId = "stop_${idx}_${seg.waypoints.size + 1}", stopName = seg.destinationName.ifBlank { "End" }, orderIndex = seg.waypoints.size + 1)
                                        )
                                    )
                                }
                            } else {
                                listOf(
                                    ItineraryDay(
                                        dayNumber = 1,
                                        dayTitle = "Day 1: ${origin.ifBlank { "Start" }} to ${destination.ifBlank { "Destination" }}",
                                        date = tripStartDate,
                                        stops = dayStops
                                    )
                                )
                            }
                        )
                    }

                    val formattedDateRange = if (tripStartDate.isNotBlank() && tripEndDate.isNotBlank() && tripStartDate != tripEndDate) {
                        "$tripStartDate - $tripEndDate"
                    } else tripStartDate.ifBlank { "Planned Upcoming Ride" }

                    val calcDist = DirectionsRepository.calculateRoadDistanceKm(
                        startLatLng, destLatLng, waypointLatLngs.toList(), activeRoutePolyline
                    )
                    val finalDist = if (multiDaySegments.isNotEmpty()) {
                        multiDaySegments.sumOf { it.distanceKm }.let { if (it > 0) it else calcDist }
                    } else {
                        if (estimatedDistanceKm > 0) estimatedDistanceKm else calcDist
                    }
                    val finalDur = if (multiDaySegments.isNotEmpty()) {
                        multiDaySegments.sumOf { it.estimatedDurationMinutes }.let { if (it > 0) it else (finalDist * 1.35).toInt() }
                    } else {
                        if (estimatedDurationMin > 0) estimatedDurationMin else (finalDist * 1.35).toInt()
                    }

                    val newSavedTrip = SavedTrip(
                        tripId = "TRIP-SAVED-${System.currentTimeMillis()}",
                        plannerId = userProfile.userId.ifBlank { "user_host" },
                        title = finalTitle,
                        originName = origin.ifBlank { "Start Point" },
                        destinationName = destination.ifBlank { "Destination" },
                        startLatLng = startLatLng,
                        destLatLng = destLatLng,
                        waypoints = waypointNames.toList(),
                        waypointLatLngs = waypointLatLngs.toList(),
                        distanceKm = finalDist,
                        durationMinutes = finalDur,
                        role = selectedRole,
                        category = TripCategory.UPCOMING,
                        lobbyCode = freshLobbyCode,
                        scheduledDate = formattedDateRange,
                        joinedRiders = listOf(
                            JoinedRiderProfile(
                                riderId = userProfile.userId.ifBlank { "rider_me" },
                                displayName = "${userProfile.displayName.ifBlank { "Rider" }} (Host)",
                                bikeModel = selectedTripVehicle?.fullDisplayName ?: userProfile.displayVehicleModel,
                                role = selectedRole,
                                status = "Lead Navigator",
                                experienceBadge = "Trip Host",
                                emergencyContact = userProfile.privacySettings.emergencyContactPhone
                            )
                        ),
                        routeSegments = multiDaySegments.toList(),
                        itineraryPlan = finalPlan
                    )
                    TripRepository.saveTrip(newSavedTrip)
                    Toast.makeText(context, "Trip '$finalTitle' saved! Code: $freshLobbyCode", Toast.LENGTH_LONG).show()
                    selectedScreenTab = 1
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(bottom = 8.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color(0xFF0052CC), Color(0xFF003399))
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = Color.White),
                elevation = ButtonDefaults.buttonElevation(6.dp)
            ) {
                Icon(Icons.Default.Bookmark, contentDescription = null, modifier = Modifier.size(20.dp), tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save Trip", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color.White)
            }
        }

        var geocoderResults by remember { mutableStateOf<List<PlaceSearchResult>>(emptyList()) }
        var isGeocoding by remember { mutableStateOf(false) }

        // Live Google Maps Geocoder Search
        LaunchedEffect(searchQuery) {
            if (searchQuery.trim().length >= 2) {
                isGeocoding = true
                try {
                    val results = withContext(Dispatchers.IO) {
                        val geocoder = Geocoder(context, Locale.getDefault())
                        val addresses = geocoder.getFromLocationName(searchQuery, 5)
                        addresses?.map { addr ->
                            val feature = addr.featureName ?: addr.locality ?: searchQuery
                            val fullAddress = (0..addr.maxAddressLineIndex)
                                .map { addr.getAddressLine(it) }
                                .joinToString(", ")
                                .ifBlank { "${addr.locality ?: ""}, ${addr.adminArea ?: ""}, ${addr.countryName ?: ""}" }

                            PlaceSearchResult(
                                title = if (feature.isNotBlank() && !fullAddress.startsWith(feature)) "$feature, ${addr.locality ?: addr.adminArea ?: ""}" else fullAddress,
                                address = fullAddress,
                                latLng = LatLng(addr.latitude, addr.longitude),
                                category = addr.countryName ?: "Google Maps Location"
                            )
                        } ?: emptyList()
                    }
                    geocoderResults = results
                } catch (e: Exception) {
                    geocoderResults = emptyList()
                } finally {
                    isGeocoding = false
                }
            } else {
                geocoderResults = emptyList()
                isGeocoding = false
            }
        }

        // Google Maps Location Search Dialog
        if (searchTargetField != null) {
            val targetLabel = when (searchTargetField) {
                "START" -> "Start Location (Origin)"
                "DEST" -> "Destination Location"
                else -> "Intermediate Stop Location"
            }

            val filteredPresetResults = remember(searchQuery) {
                if (searchQuery.isBlank()) {
                    POPULAR_MAP_LOCATIONS
                } else {
                    POPULAR_MAP_LOCATIONS.filter {
                        it.title.contains(searchQuery, ignoreCase = true) ||
                                it.address.contains(searchQuery, ignoreCase = true) ||
                                it.category.contains(searchQuery, ignoreCase = true)
                    }
                }
            }

            val combinedResults = remember(geocoderResults, filteredPresetResults) {
                (geocoderResults + filteredPresetResults).distinctBy { "${it.latLng.latitude},${it.latLng.longitude}" }
            }

            AlertDialog(
                onDismissRequest = { searchTargetField = null },
                containerColor = Color(0xF0090D16),
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier.border(
                    width = 1.5.dp,
                    brush = Brush.horizontalGradient(listOf(Color(0xFF38BDF8), Color(0xFF00E5FF), Color(0xFF0284C7))),
                    shape = RoundedCornerShape(22.dp)
                ),
                title = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Search $targetLabel",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp
                            )
                        }
                        Text(
                            text = "Search Google Maps places, cities, landmarks, or highway stops",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search location on Google Maps...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF38BDF8)) },
                            trailingIcon = {
                                if (isGeocoding) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = accentColor, strokeWidth = 2.dp)
                                } else if (searchQuery.isNotBlank()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color(0xFF94A3B8))
                                    }
                                }
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF0F172A),
                                unfocusedContainerColor = Color(0xFF0F172A),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedLabelColor = Color(0xFF38BDF8),
                                unfocusedLabelColor = Color(0xFF94A3B8),
                                focusedPlaceholderColor = Color(0xFF64748B),
                                unfocusedPlaceholderColor = Color(0xFF64748B),
                                cursorColor = Color(0xFF00F0FF),
                                focusedBorderColor = Color(0xFF38BDF8),
                                unfocusedBorderColor = Color(0xFF334155)
                            )
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 300.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            if (combinedResults.isEmpty() && !isGeocoding) {
                                Text(
                                    text = if (searchQuery.isBlank()) "Type to search any city or place." else "No places found for '$searchQuery'. Tap button below to use text.",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(vertical = 12.dp)
                                )
                            }

                            combinedResults.forEach { item ->
                                Surface(
                                    color = Color(0xFF1E293B),
                                    shape = RoundedCornerShape(10.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clickable {
                                            when (searchTargetField) {
                                                "START" -> {
                                                    origin = item.title
                                                    startLatLng = item.latLng
                                                    coroutineScope.launch {
                                                        cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(item.latLng, 12f))
                                                    }
                                                }
                                                "DEST" -> {
                                                    destination = item.title
                                                    destLatLng = item.latLng
                                                    coroutineScope.launch {
                                                        cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(item.latLng, 12f))
                                                    }
                                                }
                                                "STOP" -> {
                                                    waypointNames.add(item.title)
                                                    waypointLatLngs.add(item.latLng)
                                                }
                                            }
                                            searchTargetField = null
                                        }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(10.dp)
                                    ) {
                                        Icon(Icons.Default.Place, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(text = item.title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                            Text(text = item.address, color = Color(0xFF94A3B8), fontSize = 11.sp, maxLines = 1)
                                        }
                                    }
                                }
                            }

                            if (searchQuery.isNotBlank() && combinedResults.none { it.title.equals(searchQuery, ignoreCase = true) }) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Surface(
                                    color = Color(0xFF0F172A),
                                    shape = RoundedCornerShape(10.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            coroutineScope.launch {
                                                var resolvedLatLng: LatLng? = null
                                                try {
                                                    val addrs = withContext(Dispatchers.IO) {
                                                        Geocoder(context, Locale.getDefault()).getFromLocationName(searchQuery, 1)
                                                    }
                                                    if (!addrs.isNullOrEmpty()) {
                                                        resolvedLatLng = LatLng(addrs[0].latitude, addrs[0].longitude)
                                                    }
                                                } catch (_: Exception) {}

                                                val finalLatLng = resolvedLatLng
                                                    ?: if (searchQuery.contains("hyderabad", ignoreCase = true) || searchQuery.contains("attapur", ignoreCase = true)) {
                                                        LatLng(17.3753, 78.4344)
                                                    } else {
                                                        LatLng(startLatLng.latitude + 0.05, startLatLng.longitude + 0.05)
                                                    }

                                                when (searchTargetField) {
                                                    "START" -> {
                                                        origin = searchQuery
                                                        startLatLng = finalLatLng
                                                        cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(finalLatLng, 12f))
                                                    }
                                                    "DEST" -> {
                                                        destination = searchQuery
                                                        destLatLng = finalLatLng
                                                        cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(finalLatLng, 12f))
                                                    }
                                                    "STOP" -> {
                                                        waypointNames.add(searchQuery)
                                                        waypointLatLngs.add(finalLatLng)
                                                    }
                                                }
                                                searchTargetField = null
                                            }
                                        }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(10.dp)
                                    ) {
                                        Icon(Icons.Default.AddLocation, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text("Find '$searchQuery' on Google Maps", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { searchTargetField = null }) {
                        Text("Cancel", color = Color(0xFF94A3B8))
                    }
                }
            )
        }

        // Trip Full Details & Itinerary Modal Dialog
        selectedTripForDetails?.let { trip ->
            TripFullDetailsDialog(
                trip = trip,
                currentUserId = userProfile.userId.ifBlank { "user_me" },
                onDismiss = { selectedTripForDetails = null },
                onLoadInPlanner = {
                    tripTitle = trip.title
                    tripStartDate = trip.scheduledDate.ifBlank { "15 Oct 2026" }
                    origin = trip.originName
                    destination = trip.destinationName
                    startLatLng = trip.startLatLng
                    destLatLng = trip.destLatLng
                    waypointNames.clear()
                    waypointNames.addAll(trip.waypoints)
                    waypointLatLngs.clear()
                    waypointLatLngs.addAll(trip.waypointLatLngs)
                    selectedRole = trip.role
                    multiDaySegments.clear()
                    multiDaySegments.addAll(trip.routeSegments)
                    selectedScreenTab = 0
                    selectedTripForDetails = null
                    Toast.makeText(context, "Loaded '${trip.title}' into Route Planner!", Toast.LENGTH_SHORT).show()
                },
                onLaunchTrip = {
                    TripRepository.setOngoingTrip(trip.tripId)
                    if (trip.routeSegments.isNotEmpty()) {
                        pendingTripForDaySelection = trip
                    } else {
                        val currentPolyline = if (trip.waypointLatLngs.isNotEmpty()) listOf(trip.startLatLng) + trip.waypointLatLngs + listOf(trip.destLatLng) else listOf(trip.startLatLng, trip.destLatLng)
                        onStartTripClick(trip.title, trip.role, trip.originName, trip.destinationName, trip.waypoints, currentPolyline)
                    }
                    selectedTripForDetails = null
                },
                onEditTrip = {
                    tripToEdit = trip
                    selectedTripForDetails = null
                },
                onDeleteTrip = {
                    selectedTripForDetails = null
                    tripToDelete = trip
                },
                onExitTrip = {
                    selectedTripForDetails = null
                    tripToExit = trip
                },
                onViewItinerary = {
                    viewingItineraryTripId = trip.tripId
                    selectedTripForDetails = null
                },
                onLaunchSegment = { segment ->
                    val segOriginName = segment.originName.ifBlank { trip.originName }
                    val segDestName = segment.destinationName.ifBlank { trip.destinationName }
                    val origPt = DirectionsRepository.resolveLocationNameToLatLng(segOriginName, trip.startLatLng)
                    val destPt = DirectionsRepository.resolveLocationNameToLatLng(segDestName, trip.destLatLng)

                    val segmentPolyline = if (segment.encodedPolyline.isNotBlank() && segment.encodedPolyline.length > 50) {
                        try {
                            com.google.maps.android.PolyUtil.decode(segment.encodedPolyline)
                        } catch (_: Exception) {
                            listOf(origPt, destPt)
                        }
                    } else {
                        listOf(origPt, destPt)
                    }
                    TripRepository.setOngoingTrip(trip.tripId)
                    onStartTripClick(
                        segment.segmentName.ifBlank { trip.title },
                        trip.role,
                        segOriginName,
                        segDestName,
                        segment.waypoints,
                        segmentPolyline
                    )
                    selectedTripForDetails = null
                }
            )
        }

        // Multi-Day Route Selection Modal (Choose specific Day or Overall Master Map)
        pendingTripForDaySelection?.let { trip ->
            val masterPolyline = if (trip.title == tripTitle && activeRoutePolyline.isNotEmpty()) {
                activeRoutePolyline
            } else if (trip.waypointLatLngs.isNotEmpty()) {
                listOf(trip.startLatLng) + trip.waypointLatLngs + listOf(trip.destLatLng)
            } else {
                listOf(trip.startLatLng, trip.destLatLng)
            }

            SelectDayRouteDialog(
                trip = trip,
                masterPolyline = masterPolyline,
                onDismiss = { pendingTripForDaySelection = null },
                onSelectRoute = { title, role, orig, dest, stops, polyline ->
                    pendingTripForDaySelection = null
                    onStartTripClick(title, role, orig, dest, stops, polyline)
                }
            )
        }

        // Edit Trip Dialog (Full Trip or One Day Route)
        if (tripToEdit != null) {
            EditTripRouteDialog(
                trip = tripToEdit!!,
                onDismiss = { tripToEdit = null },
                onTripUpdated = { updatedTrip ->
                    if (selectedTripForDetails?.tripId == updatedTrip.tripId) {
                        selectedTripForDetails = updatedTrip
                    }
                    tripToEdit = null
                }
            )
        }

        // Share QR Dialog during Creation
        if (showShareQrDialogForCreation) {
            ShareTripQrDialog(
                tripTitle = tripTitle.ifBlank { "New Motorcycle Trip" },
                lobbyCode = generatedLobbyCode,
                startDate = tripStartDate,
                routeDescription = "$origin to $destination",
                onDismiss = { showShareQrDialogForCreation = false }
            )
        }

        // Delete Confirmation Dialog
        if (tripToDelete != null) {
            val trip = tripToDelete!!
            AlertDialog(
                onDismissRequest = { tripToDelete = null },
                containerColor = Color(0xFF0F172A),
                shape = RoundedCornerShape(16.dp),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFEF4444))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Delete Trip?", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                },
                text = {
                    Text(
                        "Are you sure you want to delete '${trip.title}'? This action cannot be undone.",
                        color = Color(0xFF94A3B8),
                        fontSize = 14.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            TripRepository.deleteTrip(trip.tripId)
                            if (selectedTripForDetails?.tripId == trip.tripId) {
                                selectedTripForDetails = null
                            }
                            tripToDelete = null
                            Toast.makeText(context, "Trip '${trip.title}' deleted.", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444), contentColor = Color.White)
                    ) {
                        Text("Delete", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { tripToDelete = null }) {
                        Text("Cancel", color = Color(0xFF94A3B8))
                    }
                }
            )
        }

        // Exit / Leave Trip Confirmation Dialog
        if (tripToExit != null) {
            val trip = tripToExit!!
            AlertDialog(
                onDismissRequest = { tripToExit = null },
                containerColor = Color(0xFF0F172A),
                shape = RoundedCornerShape(16.dp),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ExitToApp, contentDescription = null, tint = Color(0xFFF59E0B))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Exit Trip?", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                },
                text = {
                    Text(
                        "Are you sure you want to leave '${trip.title}'? You will be removed from the joined riders roster.",
                        color = Color(0xFF94A3B8),
                        fontSize = 14.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val currentRiderId = userProfile.userId.ifBlank { "r1" }
                            TripRepository.leaveTrip(trip.tripId, currentRiderId)
                            if (selectedTripForDetails?.tripId == trip.tripId) {
                                selectedTripForDetails = null
                            }
                            tripToExit = null
                            Toast.makeText(context, "You have left the trip: ${trip.title}", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B), contentColor = Color.Black)
                    ) {
                        Text("Exit Trip", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { tripToExit = null }) {
                        Text("Cancel", color = Color(0xFF94A3B8))
                    }
                }
            )
        }

        if (showAddSegmentDialog) {
            AddRouteSegmentDialog(
                dayNumber = multiDaySegments.size + 1,
                onDismiss = { showAddSegmentDialog = false },
                onSegmentAdded = { newSegment ->
                    multiDaySegments.add(newSegment)
                    showAddSegmentDialog = false
                    Toast.makeText(context, "Added '${newSegment.segmentName}' to trip!", Toast.LENGTH_SHORT).show()
                }
            )
        }

        if (showCreationOptionsModal) {
            TripCreationOptionsModal(
                onSelectUploadDocument = {
                    showCreationOptionsModal = false
                    currentCreationMode = TripCreationMode.DOCUMENT
                    documentPickerLauncher.launch("*/*")
                },
                onSelectMapLinks = {
                    showCreationOptionsModal = false
                    currentCreationMode = TripCreationMode.MAP_LINKS
                    showDayByDayLinksDialog = true
                },
                onSelectManualSearch = {
                    showCreationOptionsModal = false
                    currentCreationMode = TripCreationMode.MANUAL_SEARCH
                    selectedScreenTab = 0
                    Toast.makeText(context, "Direct In-App Builder active. Search Start & Destination below!", Toast.LENGTH_SHORT).show()
                },
                onDismiss = { showCreationOptionsModal = false }
            )
        }

        if (showDayByDayLinksDialog) {
            DayByDayMapLinksDialog(
                initialTripTitle = tripTitle,
                onDismiss = { showDayByDayLinksDialog = false },
                onItineraryCreated = { title, segments, plan ->
                    tripTitle = title
                    multiDaySegments.clear()
                    multiDaySegments.addAll(segments)
                    currentItineraryPlan = plan
                    currentCreationMode = TripCreationMode.MAP_LINKS
                    showDayByDayLinksDialog = false
                    Toast.makeText(context, "Configured ${segments.size} days with ${plan.days.sumOf { it.stops.size }} stops!", Toast.LENGTH_LONG).show()
                }
            )
        }

        if (showPermissionsDialog) {
            PermissionsOnboardingDialog(
                onAllGranted = { showPermissionsDialog = false },
                onDismiss = { showPermissionsDialog = false }
            )
        }

        if (isParsingItineraryDocument) {
            AlertDialog(
                onDismissRequest = {},
                containerColor = Color(0xFF0F172A),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(color = Color(0xFF00E5FF), modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("AI Parsing Itinerary...", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Text(itineraryUploadProgressText, color = Color(0xFF94A3B8), fontSize = 13.sp)
                },
                confirmButton = {}
            )
        }

        viewingItineraryTripId?.let { tId ->
            Dialog(onDismissRequest = { viewingItineraryTripId = null }, properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)) {
                ItineraryTrackerScreen(
                    tripId = tId,
                    onNavigateBack = { viewingItineraryTripId = null },
                    onLaunchCockpit = { trip ->
                        viewingItineraryTripId = null
                        val currentPolyline = if (trip.waypointLatLngs.isNotEmpty()) listOf(trip.startLatLng) + trip.waypointLatLngs + listOf(trip.destLatLng) else listOf(trip.startLatLng, trip.destLatLng)
                        onStartTripClick(trip.title, trip.role, trip.originName, trip.destinationName, trip.waypoints, currentPolyline)
                    }
                )
            }
        }
    }
}


@Composable
private fun CreationMethodTabChip(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) Color(0xFFEFF6FF) else Color(0xFFFFFFFF),
        shadowElevation = if (isSelected) 4.dp else 1.dp,
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) Color(0xFF0052CC) else Color(0xFFCBD5E1)
        ),
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp)
        ) {
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) Color(0xFF0052CC) else Color(0xFF0F172A),
                textAlign = TextAlign.Center,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) Color(0xFF0284C7) else Color(0xFF64748B),
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun RoleChip(
    role: ConvoyRole,
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) Color(0xFFEFF6FF) else Color(0xFFFFFFFF),
        shadowElevation = if (isSelected) 4.dp else 1.dp,
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) Color(0xFF0052CC) else Color(0xFFCBD5E1)
        ),
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) Color(0xFF0052CC) else Color(0xFF475569),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) Color(0xFF0052CC) else Color(0xFF0F172A),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun StatBox(
    title: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(0xFFF8FAFC),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp)
        ) {
            Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color(0xFF0052CC))
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = title, fontSize = 10.sp, color = Color(0xFF475569), textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun TripDetailsWithRidersDialog(
    trip: SavedTrip,
    currentUserId: String = "user_me",
    onDismiss: () -> Unit,
    onLoadInPlanner: () -> Unit,
    onLaunchTrip: () -> Unit,
    onEditTrip: () -> Unit,
    onDeleteTrip: () -> Unit,
    onExitTrip: () -> Unit,
    onViewItinerary: (() -> Unit)? = null,
    onLaunchSegment: ((TripRouteSegment) -> Unit)? = null
) {
    val context = LocalContext.current
    val accentColor = Color(0xFFF59E0B)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F172A),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, Color(0xFF06B6D4), RoundedCornerShape(20.dp)),
        title = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Surface(
                            color = Color(0xFF06B6D4).copy(alpha = 0.2f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                Icons.Default.Group,
                                contentDescription = null,
                                tint = Color(0xFF06B6D4),
                                modifier = Modifier
                                    .padding(8.dp)
                                    .size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Trip Details & Riders",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp
                            )
                            Text(
                                text = "Lobby Code: ${trip.lobbyCode}",
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Category Badge
                    val (catText, catBg, catTextColor) = when (trip.category) {
                        TripCategory.ONGOING -> Triple("LIVE CONVOY", Color(0xFF22C55E).copy(alpha = 0.2f), Color(0xFF22C55E))
                        TripCategory.UPCOMING -> Triple("UPCOMING", accentColor.copy(alpha = 0.2f), accentColor)
                        TripCategory.COMPLETED -> Triple("COMPLETED", Color(0xFF38BDF8).copy(alpha = 0.2f), Color(0xFF38BDF8))
                    }
                    Surface(color = catBg, shape = RoundedCornerShape(8.dp)) {
                        Text(
                            text = catText,
                            color = catTextColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Trip Header Summary Card
                Surface(
                    color = Color(0xFF1E293B),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = trip.title,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Place, contentDescription = null, tint = Color(0xFF22C55E), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${trip.originName} ➔ ${trip.destinationName}",
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp
                            )
                        }

                        if (trip.waypoints.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AddLocation, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Stops: ${trip.waypoints.joinToString(", ")}",
                                    color = Color(0xFF38BDF8),
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "📏 Distance: ${trip.distanceKm.toInt()} KM",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "⏱️ Duration: ${trip.durationMinutes / 60}h ${trip.durationMinutes % 60}m",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "📅 ${trip.scheduledDate.ifBlank { "Active" }}",
                                color = accentColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Planner Host Actions Bar inside summary
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = onEditTrip,
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Edit Trip", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = onExitTrip,
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFF59E0B)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.ExitToApp, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Exit Trip", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = onDeleteTrip,
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Delete", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Day-wise Routes & Google Maps Itinerary
                if (trip.routeSegments.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    ) {
                        Text(
                            text = "DAY-WISE ROUTES (${trip.routeSegments.size} LEGS)",
                            color = Color(0xFF38BDF8),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "Tap Open Route to navigate",
                            color = Color(0xFF64748B),
                            fontSize = 11.sp
                        )
                    }

                    trip.routeSegments.forEachIndexed { index, segment ->
                        Surface(
                            color = Color(0xFF020617),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Surface(
                                            color = accentColor,
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier.size(26.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = "${index + 1}",
                                                    color = Color.Black,
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 12.sp
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = segment.segmentName.ifBlank { "Day ${index + 1} Route" },
                                                color = Color.White,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "${segment.originName.ifBlank { "Origin" }} ➔ ${segment.destinationName.ifBlank { "Destination" }}",
                                                color = Color(0xFF94A3B8),
                                                fontSize = 12.sp
                                            )
                                            if (segment.waypoints.isNotEmpty()) {
                                                Text(
                                                    text = "📍 Stops (${segment.waypoints.size}): ${segment.waypoints.joinToString(" ➔ ")}",
                                                    color = Color(0xFF38BDF8),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                    }

                                    // Open Route Button
                                    Button(
                                        onClick = {
                                            onLaunchSegment?.invoke(segment)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.Black),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Open Route", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "📏 ${"%.1f".format(segment.distanceKm)} KM  •  ⏱️ ${segment.estimatedDurationMinutes / 60}h ${segment.estimatedDurationMinutes % 60}m",
                                        color = Color(0xFF38BDF8),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    if (segment.googleMapsUrl.isNotBlank()) {
                                        Text(
                                            text = "🔗 Google Maps Link",
                                            color = Color(0xFF94A3B8),
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Joined Riders Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                ) {
                    Text(
                        text = "JOINED RIDERS & PROFILES (${trip.joinedRiders.size})",
                        color = Color(0xFF38BDF8),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "Tap phone to call rider",
                        color = Color(0xFF64748B),
                        fontSize = 11.sp
                    )
                }

                // Rider Profiles List
                if (trip.joinedRiders.isEmpty()) {
                    Text(
                        text = "No other riders joined this trip yet.",
                        color = Color(0xFF94A3B8),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    trip.joinedRiders.forEach { rider ->
                        Surface(
                            color = Color(0xFF020617),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    // Rider Avatar with Role Indicator Badge
                                    Box(contentAlignment = Alignment.BottomEnd) {
                                        Surface(
                                            color = when (rider.role) {
                                                ConvoyRole.LEAD -> accentColor.copy(alpha = 0.2f)
                                                ConvoyRole.SWEEP -> Color(0xFF38BDF8).copy(alpha = 0.2f)
                                                ConvoyRole.MEMBER -> Color(0xFF22C55E).copy(alpha = 0.2f)
                                            },
                                            shape = RoundedCornerShape(20.dp),
                                            border = androidx.compose.foundation.BorderStroke(
                                                1.5.dp,
                                                when (rider.role) {
                                                    ConvoyRole.LEAD -> accentColor
                                                    ConvoyRole.SWEEP -> Color(0xFF38BDF8)
                                                    ConvoyRole.MEMBER -> Color(0xFF22C55E)
                                                }
                                            ),
                                            modifier = Modifier.size(44.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = rider.displayName.take(1).uppercase(),
                                                    color = Color.White,
                                                    fontSize = 18.sp,
                                                    fontWeight = FontWeight.Black
                                                )
                                            }
                                        }

                                        // Role Icon Overlay
                                        Surface(
                                            color = when (rider.role) {
                                                ConvoyRole.LEAD -> accentColor
                                                ConvoyRole.SWEEP -> Color(0xFF38BDF8)
                                                ConvoyRole.MEMBER -> Color(0xFF22C55E)
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.size(16.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                val iconSymbol = when (rider.role) {
                                                    ConvoyRole.LEAD -> "👑"
                                                    ConvoyRole.SWEEP -> "🛡️"
                                                    ConvoyRole.MEMBER -> "🏍️"
                                                }
                                                Text(iconSymbol, fontSize = 9.sp)
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    // Rider Details (Name, Bike Model, Rank Badge)
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = rider.displayName,
                                                color = Color.White,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                color = when (rider.role) {
                                                    ConvoyRole.LEAD -> accentColor.copy(alpha = 0.2f)
                                                    ConvoyRole.SWEEP -> Color(0xFF38BDF8).copy(alpha = 0.2f)
                                                    ConvoyRole.MEMBER -> Color(0xFF22C55E).copy(alpha = 0.2f)
                                                },
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = rider.role.name,
                                                    color = when (rider.role) {
                                                        ConvoyRole.LEAD -> accentColor
                                                        ConvoyRole.SWEEP -> Color(0xFF38BDF8)
                                                        ConvoyRole.MEMBER -> Color(0xFF22C55E)
                                                    },
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Black,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.DirectionsBike, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = rider.bikeModel,
                                                color = Color(0xFF94A3B8),
                                                fontSize = 12.sp
                                            )
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Shield, contentDescription = null, tint = accentColor, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "${rider.experienceBadge} • ${rider.status}",
                                                color = Color(0xFF38BDF8),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Rider Emergency Contact Bar
                                Surface(
                                    color = Color(0xFF1E293B),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            Toast.makeText(context, "Calling emergency contact for ${rider.displayName}: ${rider.emergencyContact}", Toast.LENGTH_SHORT).show()
                                        }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("📞 Contact / Emergency:", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(rider.emergencyContact, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Text("Call ➔", color = Color(0xFF22C55E), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                onViewItinerary?.let { viewItin ->
                    Button(
                        onClick = viewItin,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7), contentColor = Color.White)
                    ) {
                        Icon(Icons.Default.FormatListNumbered, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Itinerary", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                OutlinedButton(
                    onClick = onLoadInPlanner,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8))
                ) {
                    Text("Planner", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onLaunchTrip,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.Black)
                ) {
                    Text("Launch", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = Color(0xFF94A3B8))
            }
        }
    )
}

@Composable
fun EditTripDialog(
    trip: SavedTrip,
    onDismiss: () -> Unit,
    onSave: (SavedTrip) -> Unit
) {
    var editTitle by remember { mutableStateOf(trip.title) }
    var editOrigin by remember { mutableStateOf(trip.originName) }
    var editDestination by remember { mutableStateOf(trip.destinationName) }
    var editDate by remember { mutableStateOf(trip.scheduledDate) }
    var editRole by remember { mutableStateOf(trip.role) }
    var editCategory by remember { mutableStateOf(trip.category) }
    val editWaypoints = remember { mutableStateListOf<String>().apply { addAll(trip.waypoints) } }
    var newStopInput by remember { mutableStateOf("") }

    val accentColor = Color(0xFFF59E0B)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F172A),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, Color(0xFF38BDF8), RoundedCornerShape(20.dp)),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Edit, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Edit Trip Details",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Trip Title
                OutlinedTextField(
                    value = editTitle,
                    onValueChange = { editTitle = it },
                    label = { Text("Trip Title") },
                    leadingIcon = { Icon(Icons.Default.Route, contentDescription = null, tint = accentColor) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF0F172A),
                        unfocusedContainerColor = Color(0xFF0F172A),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedLabelColor = accentColor,
                        unfocusedLabelColor = Color(0xFF94A3B8),
                        focusedPlaceholderColor = Color(0xFF64748B),
                        unfocusedPlaceholderColor = Color(0xFF64748B),
                        cursorColor = Color(0xFF00F0FF),
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = Color(0xFF334155)
                    )
                )

                // Origin
                OutlinedTextField(
                    value = editOrigin,
                    onValueChange = { editOrigin = it },
                    label = { Text("Start Location (Origin)") },
                    leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFF22C55E)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF0F172A),
                        unfocusedContainerColor = Color(0xFF0F172A),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedLabelColor = Color(0xFF22C55E),
                        unfocusedLabelColor = Color(0xFF94A3B8),
                        focusedPlaceholderColor = Color(0xFF64748B),
                        unfocusedPlaceholderColor = Color(0xFF64748B),
                        cursorColor = Color(0xFF00F0FF),
                        focusedBorderColor = Color(0xFF22C55E),
                        unfocusedBorderColor = Color(0xFF334155)
                    )
                )

                // Destination
                OutlinedTextField(
                    value = editDestination,
                    onValueChange = { editDestination = it },
                    label = { Text("Destination Location") },
                    leadingIcon = { Icon(Icons.Default.Flag, contentDescription = null, tint = Color(0xFFEF4444)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF0F172A),
                        unfocusedContainerColor = Color(0xFF0F172A),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedLabelColor = Color(0xFFEF4444),
                        unfocusedLabelColor = Color(0xFF94A3B8),
                        focusedPlaceholderColor = Color(0xFF64748B),
                        unfocusedPlaceholderColor = Color(0xFF64748B),
                        cursorColor = Color(0xFF00F0FF),
                        focusedBorderColor = Color(0xFFEF4444),
                        unfocusedBorderColor = Color(0xFF334155)
                    )
                )

                // Scheduled Date / Time
                OutlinedTextField(
                    value = editDate,
                    onValueChange = { editDate = it },
                    label = { Text("Date & Time / Schedule") },
                    leadingIcon = { Icon(Icons.Default.Schedule, contentDescription = null, tint = Color(0xFF38BDF8)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF0F172A),
                        unfocusedContainerColor = Color(0xFF0F172A),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedLabelColor = Color(0xFF38BDF8),
                        unfocusedLabelColor = Color(0xFF94A3B8),
                        focusedPlaceholderColor = Color(0xFF64748B),
                        unfocusedPlaceholderColor = Color(0xFF64748B),
                        cursorColor = Color(0xFF00F0FF),
                        focusedBorderColor = Color(0xFF38BDF8),
                        unfocusedBorderColor = Color(0xFF334155)
                    )
                )

                // Category Selection Chips
                Text("Trip Status", color = Color(0xFF94A3B8), fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = editCategory == TripCategory.UPCOMING,
                        onClick = { editCategory = TripCategory.UPCOMING },
                        label = { Text("Upcoming", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = accentColor.copy(alpha = 0.3f),
                            selectedLabelColor = accentColor
                        )
                    )
                    FilterChip(
                        selected = editCategory == TripCategory.ONGOING,
                        onClick = { editCategory = TripCategory.ONGOING },
                        label = { Text("Live Ongoing", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF22C55E).copy(alpha = 0.3f),
                            selectedLabelColor = Color(0xFF22C55E)
                        )
                    )
                    FilterChip(
                        selected = editCategory == TripCategory.COMPLETED,
                        onClick = { editCategory = TripCategory.COMPLETED },
                        label = { Text("Completed", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF38BDF8).copy(alpha = 0.3f),
                            selectedLabelColor = Color(0xFF38BDF8)
                        )
                    )
                }

                // Intermediate Stops Section
                Text("Intermediate Stops (${editWaypoints.size})", color = Color(0xFF94A3B8), fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 6.dp))
                editWaypoints.forEachIndexed { idx, stopName ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .background(Color(0xFF1E293B), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Text("${idx + 1}. $stopName", color = Color.White, fontSize = 12.sp, modifier = Modifier.weight(1f))
                        IconButton(
                            onClick = { editWaypoints.removeAt(idx) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Stop", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    OutlinedTextField(
                        value = newStopInput,
                        onValueChange = { newStopInput = it },
                        placeholder = { Text("Add stop name...", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF0F172A),
                            unfocusedContainerColor = Color(0xFF0F172A),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedLabelColor = Color(0xFF38BDF8),
                            unfocusedLabelColor = Color(0xFF94A3B8),
                            focusedPlaceholderColor = Color(0xFF64748B),
                            unfocusedPlaceholderColor = Color(0xFF64748B),
                            cursorColor = Color(0xFF00F0FF),
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedBorderColor = Color(0xFF334155)
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (newStopInput.isNotBlank()) {
                                editWaypoints.add(newStopInput.trim())
                                newStopInput = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8), contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Add", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val updated = trip.copy(
                        title = editTitle.ifBlank { trip.title },
                        originName = editOrigin.ifBlank { trip.originName },
                        destinationName = editDestination.ifBlank { trip.destinationName },
                        scheduledDate = editDate,
                        role = editRole,
                        category = editCategory,
                        waypoints = editWaypoints.toList()
                    )
                    onSave(updated)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E), contentColor = Color.Black)
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Save Changes", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF94A3B8))
            }
        }
    )
}

@Composable
fun SelectDayRouteDialog(
    trip: SavedTrip,
    masterPolyline: List<LatLng>,
    onDismiss: () -> Unit,
    onSelectRoute: (title: String, role: ConvoyRole, origin: String, destination: String, waypoints: List<String>, routePolyline: List<LatLng>) -> Unit
) {
    val accentColor = Color(0xFF0052CC)
    val goldColor = Color(0xFFD97706)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFFFFFFFF),
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, Color(0xFF0052CC), RoundedCornerShape(22.dp)),
        title = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.AltRoute,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Select Route Map",
                        color = Color(0xFF0F172A),
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = trip.title,
                    color = accentColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "This trip has ${trip.routeSegments.size} multi-day itinerary stages. Choose which route you want to open in the Convoy HUD Map:",
                    color = Color(0xFF475569),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Option 1: OVERALL MASTER ROUTE (Full Itinerary Corridor)
                Surface(
                    color = Color(0xFFFFFBEB),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFF59E0B)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = Color(0xFFFEF3C7),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "🌐 OVERALL",
                                        color = goldColor,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Full Master Itinerary",
                                    color = Color(0xFF0F172A),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "${trip.originName} ➔ ${trip.destinationName}",
                            color = Color(0xFF1E293B),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        if (trip.waypoints.isNotEmpty()) {
                            Text(
                                text = "📍 All Waypoints (${trip.waypoints.size}): ${trip.waypoints.joinToString(" ➔ ")}",
                                color = Color(0xFF0052CC),
                                fontSize = 11.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "📏 ~${trip.distanceKm.toInt()} KM Total • ⏱️ ${trip.durationMinutes / 60}h ${trip.durationMinutes % 60}m",
                            color = goldColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                onSelectRoute(
                                    trip.title,
                                    trip.role,
                                    trip.originName,
                                    trip.destinationName,
                                    trip.waypoints,
                                    masterPolyline
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B), contentColor = Color.Black),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Launch Overall Master Map", fontWeight = FontWeight.Black, fontSize = 13.sp)
                        }
                    }
                }

                // Section 2: DAY-BY-DAY ROUTES
                Text(
                    text = "📅 DAY-BY-DAY ROUTES (${trip.routeSegments.size} Days)",
                    color = accentColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                trip.routeSegments.forEachIndexed { index, segment ->
                    Surface(
                        color = Color(0xFFF8FAFC),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        color = accentColor,
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "${index + 1}",
                                                color = Color.White,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = segment.segmentName.ifBlank { "Day ${index + 1} Route" },
                                        color = Color(0xFF0F172A),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${segment.originName.ifBlank { trip.originName }} ➔ ${segment.destinationName.ifBlank { trip.destinationName }}",
                                color = Color(0xFF475569),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )

                            if (segment.waypoints.isNotEmpty()) {
                                Text(
                                    text = "📍 Stops: ${segment.waypoints.joinToString(" ➔ ")}",
                                    color = Color(0xFF0052CC),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "📏 ${"%.1f".format(segment.distanceKm)} KM • ⏱️ ${segment.estimatedDurationMinutes / 60}h ${segment.estimatedDurationMinutes % 60}m",
                                color = goldColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    val segOriginName = segment.originName.ifBlank { trip.originName }
                                    val segDestName = segment.destinationName.ifBlank { trip.destinationName }
                                    val origPt = DirectionsRepository.resolveLocationNameToLatLng(segOriginName, trip.startLatLng)
                                    val destPt = DirectionsRepository.resolveLocationNameToLatLng(segDestName, trip.destLatLng)

                                    val segPolyline = if (segment.encodedPolyline.isNotBlank() && segment.encodedPolyline.length > 50) {
                                        try {
                                            DirectionsRepository.decodePolyline(segment.encodedPolyline)
                                        } catch (_: Exception) {
                                            listOf(origPt, destPt)
                                        }
                                    } else {
                                        listOf(origPt, destPt)
                                    }
                                    onSelectRoute(
                                        "${trip.title} - ${segment.segmentName.ifBlank { "Day ${index + 1}" }}",
                                        trip.role,
                                        segOriginName,
                                        segDestName,
                                        segment.waypoints,
                                        segPolyline
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0052CC), contentColor = Color.White),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Launch Day ${index + 1} Map", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
            }
        }
    )
}

/**
 * Dedicated Trip History & Saved Rides Page.
 * Displays Saved Trips, Ongoing Convoy, Completed Tours & Lifetime Stats.
 */
/**
 * Dedicated Trip History & Saved Rides Dashboard Screen.
 * Displays all trips user joined, created, upcoming, and completed with full details & stats.
 */
@Composable
fun SavedTripsHistoryScreen(
    onStartTripClick: (tripTitle: String, role: ConvoyRole, origin: String, destination: String, waypoints: List<String>, routePolyline: List<LatLng>) -> Unit,
    onShareLobbyClick: (lobbyCode: String) -> Unit,
    userProfile: UserProfile = UserProfile(userId = "user_me", displayName = "Ahmed (You)"),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val savedTripsList by TripRepository.tripsFlow.collectAsState()

    var activeCategoryFilter by remember { mutableStateOf("ALL") } // ALL, CREATED, JOINED, UPCOMING, COMPLETED
    var selectedTripForDetails by remember { mutableStateOf<SavedTrip?>(null) }
    var tripToEdit by remember { mutableStateOf<SavedTrip?>(null) }
    var tripToDelete by remember { mutableStateOf<SavedTrip?>(null) }
    var pendingTripForDaySelection by remember { mutableStateOf<SavedTrip?>(null) }

    val accentColor = Color(0xFF00E5FF)
    val cardBg = Color(0xF0090D16)

    val filteredTrips = remember(savedTripsList, activeCategoryFilter, userProfile.userId) {
        when (activeCategoryFilter) {
            "CREATED" -> savedTripsList.filter { it.plannerId == userProfile.userId || it.plannerId == "user_me" || it.plannerId == "user_host" }
            "JOINED" -> savedTripsList.filter { trip -> trip.joinedRiders.any { it.riderId == userProfile.userId || it.riderId == "user_me" } }
            "UPCOMING" -> savedTripsList.filter { it.category == TripCategory.UPCOMING }
            "COMPLETED" -> savedTripsList.filter { it.category == TripCategory.COMPLETED }
            else -> savedTripsList
        }
    }

    val totalDistCompleted = remember(savedTripsList) {
        savedTripsList.filter { it.category == TripCategory.COMPLETED || it.category == TripCategory.ONGOING }
            .sumOf { if (it.category == TripCategory.ONGOING) it.completedKm else it.distanceKm }
    }
    val completedCount = remember(savedTripsList) { savedTripsList.count { it.category == TripCategory.COMPLETED } }
    val upcomingCount = remember(savedTripsList) { savedTripsList.count { it.category == TripCategory.UPCOMING } }
    val ongoingTrip = remember(savedTripsList) { savedTripsList.firstOrNull { it.category == TripCategory.ONGOING } }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF1F5F9))
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // 1. Screen Header
        Text(
            text = "Trip History & Saved Rides",
            fontSize = 26.sp,
            fontWeight = FontWeight.Black,
            color = Color(0xFF0F172A)
        )
        Text(
            text = "View all trips you created, joined convoys, upcoming, and completed tours",
            fontSize = 13.sp,
            color = Color(0xFF475569),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // 2. Rider Lifetime Stats Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFFFF)),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    1.5.dp,
                    Brush.horizontalGradient(listOf(Color(0xFF0052CC), Color(0xFF3B82F6))),
                    RoundedCornerShape(18.dp)
                )
                .padding(bottom = 16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "🏆 Rider Activity & Tour Stats",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatBox(title = "Total Distance", value = "${totalDistCompleted.toInt()} KM", accentColor = Color(0xFF0052CC), modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.width(8.dp))
                    StatBox(title = "Completed Rides", value = "$completedCount Rides", accentColor = Color(0xFF16A34A), modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.width(8.dp))
                    StatBox(title = "Upcoming Saved", value = "$upcomingCount Trips", accentColor = Color(0xFF0052CC), modifier = Modifier.weight(1f))
                }
            }
        }

        // 3. Category Filter Chips Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(
                "ALL" to "All (${savedTripsList.size})",
                "CREATED" to "Created",
                "JOINED" to "Joined",
                "UPCOMING" to "Upcoming",
                "COMPLETED" to "Completed"
            ).forEach { (catKey, catLabel) ->
                val isSelected = activeCategoryFilter == catKey
                Surface(
                    onClick = { activeCategoryFilter = catKey },
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = if (isSelected) Color(0xFF0052CC) else Color(0xFFFFFFFF),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) Color(0xFF0052CC) else Color(0xFFCBD5E1)),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = catLabel,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                        color = if (isSelected) Color.White else Color(0xFF0F172A),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp),
                        maxLines = 1
                    )
                }
            }
        }

        // 4. Live Ongoing Ride Banner (If active)
        if (ongoingTrip != null && (activeCategoryFilter == "ALL" || activeCategoryFilter == "UPCOMING")) {
            Text(
                text = "⚡ Live Ongoing Ride (In Progress)",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF22C55E),
                modifier = Modifier.padding(bottom = 10.dp)
            )

            TripSummaryCard(
                trip = ongoingTrip,
                onViewDetails = { selectedTripForDetails = ongoingTrip },
                onLaunchTrip = {
                    TripRepository.setOngoingTrip(ongoingTrip.tripId)
                    onStartTripClick(
                        ongoingTrip.title,
                        ongoingTrip.role,
                        ongoingTrip.originName,
                        ongoingTrip.destinationName,
                        ongoingTrip.waypoints,
                        if (ongoingTrip.waypointLatLngs.isNotEmpty()) listOf(ongoingTrip.startLatLng) + ongoingTrip.waypointLatLngs + listOf(ongoingTrip.destLatLng) else listOf(ongoingTrip.startLatLng, ongoingTrip.destLatLng)
                    )
                },
                onEditTrip = { tripToEdit = ongoingTrip },
                onDeleteTrip = { tripToDelete = ongoingTrip },
                modifier = Modifier.padding(bottom = 20.dp)
            )
        }

        // 5. Filtered Trips List
        Text(
            text = "Trip History Records (${filteredTrips.size})",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0F172A),
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (filteredTrips.isEmpty()) {
            Surface(
                color = Color(0xFFFFFFFF),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.BookmarkBorder, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(36.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No trips found in '$activeCategoryFilter' filter.",
                        color = Color(0xFF0F172A),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Create a new ride or join a convoy using a lobby code.",
                        color = Color(0xFF64748B),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        } else {
            filteredTrips.forEach { trip ->
                TripSummaryCard(
                    trip = trip,
                    onViewDetails = { selectedTripForDetails = trip },
                    onLaunchTrip = {
                        TripRepository.setOngoingTrip(trip.tripId)
                        if (trip.routeSegments.isNotEmpty()) {
                            pendingTripForDaySelection = trip
                        } else {
                            onStartTripClick(
                                trip.title,
                                trip.role,
                                trip.originName,
                                trip.destinationName,
                                trip.waypoints,
                                if (trip.waypointLatLngs.isNotEmpty()) listOf(trip.startLatLng) + trip.waypointLatLngs + listOf(trip.destLatLng) else listOf(trip.startLatLng, trip.destLatLng)
                            )
                        }
                    },
                    onEditTrip = { tripToEdit = trip },
                    onDeleteTrip = { tripToDelete = trip },
                    modifier = Modifier.padding(bottom = 14.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }

    // Full Trip Details Dialog
    if (selectedTripForDetails != null) {
        val trip = selectedTripForDetails!!
        AlertDialog(
            onDismissRequest = { selectedTripForDetails = null },
            containerColor = Color(0xFFFFFFFF),
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier.border(
                width = 1.5.dp,
                brush = Brush.horizontalGradient(listOf(Color(0xFF0052CC), Color(0xFF3B82F6))),
                shape = RoundedCornerShape(22.dp)
            ),
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "🗺️", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = trip.title,
                            color = Color(0xFF0F172A),
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            maxLines = 1
                        )
                    }
                    IconButton(onClick = { selectedTripForDetails = null }) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF64748B))
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(text = "Scheduled: ${trip.scheduledDate} • Category: ${trip.category.name}", color = Color(0xFF0052CC), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(10.dp))

                    Text(text = "📍 Route Details:", color = Color(0xFF0F172A), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(text = "• Origin: ${trip.originName}", color = Color(0xFF475569), fontSize = 13.sp)
                    Text(text = "• Destination: ${trip.destinationName}", color = Color(0xFF475569), fontSize = 13.sp)
                    Text(text = "• Distance: ${trip.distanceKm.toInt()} km", color = Color(0xFF475569), fontSize = 13.sp)
                    if (trip.waypoints.isNotEmpty()) {
                        Text(text = "• Waypoints: ${trip.waypoints.joinToString(", ")}", color = Color(0xFFD97706), fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = Color(0xFFE2E8F0))
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(text = "👥 Joined Convoy Members (${trip.joinedRiders.size}):", color = Color(0xFF0F172A), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    trip.joinedRiders.forEach { rider ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Text(text = "🏍️", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(text = rider.displayName, color = Color(0xFF0F172A), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text(text = "${rider.bikeModel} • ${rider.role.name}", color = Color(0xFF64748B), fontSize = 11.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = "🔑 Lobby Join Code: ${trip.lobbyCode}", color = Color(0xFF0052CC), fontSize = 13.sp, fontWeight = FontWeight.Black)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val tr = selectedTripForDetails!!
                        selectedTripForDetails = null
                        TripRepository.setOngoingTrip(tr.tripId)
                        onStartTripClick(
                            tr.title,
                            tr.role,
                            tr.originName,
                            tr.destinationName,
                            tr.waypoints,
                            if (tr.waypointLatLngs.isNotEmpty()) listOf(tr.startLatLng) + tr.waypointLatLngs + listOf(tr.destLatLng) else listOf(tr.startLatLng, tr.destLatLng)
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0052CC), contentColor = Color.White),
                    shape = androidx.compose.foundation.shape.CircleShape
                ) {
                    Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("START TRIP NAVIGATION", fontWeight = FontWeight.Black, fontSize = 12.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedTripForDetails = null }) {
                    Text("Close", color = Color(0xFF64748B))
                }
            }
        )
    }

    // Edit Trip Dialog
    if (tripToEdit != null) {
        EditTripRouteDialog(
            trip = tripToEdit!!,
            onDismiss = { tripToEdit = null },
            onTripUpdated = { updated ->
                TripRepository.saveTrip(updated)
                tripToEdit = null
                Toast.makeText(context, "Updated trip '${updated.title}'!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Delete Trip Confirmation Dialog
    if (tripToDelete != null) {
        AlertDialog(
            onDismissRequest = { tripToDelete = null },
            containerColor = Color(0xFFFFFFFF),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(20.dp)),
            title = { Text("Delete Trip?", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to remove '${tripToDelete?.title}' from your saved trips history?", color = Color(0xFF475569)) },
            confirmButton = {
                Button(
                    onClick = {
                        val t = tripToDelete!!
                        tripToDelete = null
                        TripRepository.deleteTrip(t.tripId)
                        Toast.makeText(context, "Deleted '${t.title}'", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444), contentColor = Color.White)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { tripToDelete = null }) { Text("Cancel", color = Color(0xFF64748B)) }
            }
        )
    }

    // Multi-Day Day Route Selector Dialog
    if (pendingTripForDaySelection != null) {
        val trip = pendingTripForDaySelection!!
        SelectDayRouteDialog(
            trip = trip,
            masterPolyline = if (trip.waypointLatLngs.isNotEmpty()) listOf(trip.startLatLng) + trip.waypointLatLngs + listOf(trip.destLatLng) else listOf(trip.startLatLng, trip.destLatLng),
            onDismiss = { pendingTripForDaySelection = null },
            onSelectRoute = { title, role, orig, dest, waypoints, segPolyline ->
                pendingTripForDaySelection = null
                TripRepository.setOngoingTrip(trip.tripId)
                onStartTripClick(
                    title,
                    role,
                    orig,
                    dest,
                    waypoints,
                    segPolyline
                )
            }
        )
    }
}

