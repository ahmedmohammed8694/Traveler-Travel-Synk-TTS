package com.ridesync.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.android.gms.maps.model.LatLng
import com.ridesync.RideSyncApplication
import com.ridesync.data.model.ConvoyRole
import com.ridesync.data.model.JoinedRiderProfile
import com.ridesync.data.model.SavedTrip
import com.ridesync.data.model.TripCategory
import com.ridesync.data.model.TripRouteSegment
import com.ridesync.data.model.ItineraryDay
import com.ridesync.data.model.ItineraryStop
import com.ridesync.data.model.ItineraryStopStatus
import com.ridesync.data.model.ItineraryTripPlan
import com.ridesync.data.model.TripCreationMode
import com.ridesync.data.model.UserProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object TripRepository {
    private const val TAG = "TripRepository"
    private const val PREFS_NAME = "ridesync_trips_prefs"
    private const val KEY_TRIPS_JSON = "saved_trips_json"
    private const val CLOUDFLARE_EDGE_URL = "https://ahmedmohammed8694-riders-ride-sync.mdahmed08061994.workers.dev"

    private val _tripsFlow = MutableStateFlow<List<SavedTrip>>(emptyList())
    val tripsFlow: StateFlow<List<SavedTrip>> = _tripsFlow.asStateFlow()

    private val prefs: SharedPreferences? by lazy {
        try {
            RideSyncApplication.appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get SharedPreferences: ${e.message}")
            null
        }
    }

    init {
        loadTripsFromStorage()
        fetchOnlineTripsAsync()
    }

    private var isInitialized = false

    @Synchronized
    fun getAllTrips(): List<SavedTrip> {
        if (!isInitialized) {
            loadTripsFromStorage()
            isInitialized = true
        }
        return _tripsFlow.value
    }

    @Synchronized
    fun getTripById(tripId: String): SavedTrip? {
        return getAllTrips().firstOrNull { it.tripId == tripId }
    }

    @Synchronized
    fun saveTrip(trip: SavedTrip) {
        val currentList = getAllTrips().toMutableList()
        val existingIndex = currentList.indexOfFirst { it.tripId == trip.tripId }
        if (existingIndex >= 0) {
            currentList[existingIndex] = trip
        } else {
            // Add new trip at index 0 or after ongoing
            val firstNonOngoingIndex = currentList.indexOfFirst { it.category != TripCategory.ONGOING }
            if (firstNonOngoingIndex >= 0) {
                currentList.add(firstNonOngoingIndex, trip)
            } else {
                currentList.add(0, trip)
            }
        }
        updateAndPersistList(currentList)
        syncTripToCloudflareAsync(trip)
    }

    @Synchronized
    fun updateTrip(trip: SavedTrip) {
        val currentList = getAllTrips().toMutableList()
        val index = currentList.indexOfFirst { it.tripId == trip.tripId }
        if (index >= 0) {
            currentList[index] = trip
            updateAndPersistList(currentList)
        } else {
            saveTrip(trip)
        }
        syncTripToCloudflareAsync(trip)
    }

    @Synchronized
    fun deleteTrip(tripId: String) {
        val currentList = getAllTrips().toMutableList()
        val removed = currentList.removeAll { it.tripId == tripId }
        if (removed) {
            updateAndPersistList(currentList)
        }
        deleteTripFromCloudflareAsync(tripId)
    }

    @Synchronized
    fun leaveTrip(tripId: String, riderId: String) {
        val currentList = getAllTrips().toMutableList()
        val index = currentList.indexOfFirst { it.tripId == tripId }
        if (index >= 0) {
            val trip = currentList[index]
            val updatedRiders = trip.joinedRiders.filterNot { it.riderId == riderId || it.riderId == "r1" || it.riderId == "user_me" }
            val updatedTrip = trip.copy(
                joinedRiders = updatedRiders,
                activeRidersCount = updatedRiders.size
            )
            currentList[index] = updatedTrip
            updateAndPersistList(currentList)
        }
        leaveTripOnCloudflareAsync(tripId, riderId)
    }

    fun joinTripOnline(trip: SavedTrip, userProfile: UserProfile) {
        val activeVehicle = userProfile.vehicles.firstOrNull { it.id == userProfile.activeVehicleId }
            ?: userProfile.vehicles.firstOrNull()

        val riderProfile = JoinedRiderProfile(
            riderId = userProfile.userId.ifBlank { "user_me" },
            displayName = userProfile.displayName.ifBlank { "Rider" },
            bikeModel = activeVehicle?.fullDisplayName ?: userProfile.vehicleModel.ifBlank { "Bike" },
            role = ConvoyRole.MEMBER,
            status = "Joined & Confirmed",
            experienceBadge = "Convoy Rider",
            emergencyContact = userProfile.privacySettings.emergencyContactPhone
        )

        val updatedRiders = if (trip.joinedRiders.none { it.riderId == riderProfile.riderId || it.displayName == riderProfile.displayName }) {
            trip.joinedRiders + riderProfile
        } else {
            trip.joinedRiders
        }

        val updatedTrip = trip.copy(
            joinedRiders = updatedRiders,
            activeRidersCount = updatedRiders.size,
            category = if (trip.category == TripCategory.COMPLETED) TripCategory.UPCOMING else trip.category
        )

        saveTrip(updatedTrip)
        joinTripOnCloudflareAsync(updatedTrip.tripId, updatedTrip.lobbyCode, riderProfile)
    }

    @Synchronized
    fun setOngoingTrip(tripId: String) {
        val currentList = getAllTrips().toMutableList()
        val targetIndex = currentList.indexOfFirst { it.tripId == tripId }
        if (targetIndex >= 0) {
            // Move previous ongoing trips to upcoming
            for (i in currentList.indices) {
                if (currentList[i].category == TripCategory.ONGOING && currentList[i].tripId != tripId) {
                    currentList[i] = currentList[i].copy(category = TripCategory.UPCOMING)
                }
            }
            val target = currentList[targetIndex]
            currentList[targetIndex] = target.copy(category = TripCategory.ONGOING)
            updateAndPersistList(currentList)
        }
    }

    @Synchronized
    fun completeTrip(tripId: String) {
        val currentList = getAllTrips().toMutableList()
        val index = currentList.indexOfFirst { it.tripId == tripId }
        if (index >= 0) {
            val trip = currentList[index]
            currentList[index] = trip.copy(
                category = TripCategory.COMPLETED,
                completedKm = trip.distanceKm
            )
            updateAndPersistList(currentList)
        }
    }

    @Synchronized
    fun updateStopStatus(tripId: String, dayNumber: Int, stopId: String, newStatus: ItineraryStopStatus) {
        val currentList = getAllTrips().toMutableList()
        val index = currentList.indexOfFirst { it.tripId == tripId }
        if (index >= 0) {
            val trip = currentList[index]
            val plan = trip.itineraryPlan ?: return
            val updatedDays = plan.days.map { day ->
                if (day.dayNumber == dayNumber) {
                    val updatedStops = day.stops.map { stop ->
                        if (stop.stopId == stopId) {
                            stop.copy(status = newStatus)
                        } else stop
                    }
                    day.copy(stops = updatedStops)
                } else day
            }
            val updatedTrip = trip.copy(itineraryPlan = plan.copy(days = updatedDays))
            currentList[index] = updatedTrip
            updateAndPersistList(currentList)
            syncStopStatusToCloudflareAsync(tripId, dayNumber, stopId, newStatus)
        }
    }

    private fun updateAndPersistList(newList: List<SavedTrip>) {
        _tripsFlow.value = newList
        persistToStorage(newList)
    }

    private fun persistToStorage(trips: List<SavedTrip>) {
        try {
            val jsonArray = JSONArray()
            trips.forEach { trip ->
                jsonArray.put(serializeTrip(trip))
            }
            prefs?.edit()?.putString(KEY_TRIPS_JSON, jsonArray.toString())?.apply()
            Log.d(TAG, "Persisted ${trips.size} trips to SharedPreferences.")
        } catch (e: Exception) {
            Log.e(TAG, "Error persisting trips: ${e.message}", e)
        }
    }

    private fun loadTripsFromStorage() {
        try {
            val jsonStr = prefs?.getString(KEY_TRIPS_JSON, null)
            if (jsonStr != null) {
                val jsonArray = JSONArray(jsonStr)
                val loadedList = mutableListOf<SavedTrip>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    deserializeTrip(obj)?.let { loadedList.add(it) }
                }
                _tripsFlow.value = loadedList
                return
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading trips from storage: ${e.message}", e)
        }

        _tripsFlow.value = emptyList()
    }

    private fun serializeTrip(trip: SavedTrip): JSONObject {
        val obj = JSONObject()
        obj.put("tripId", trip.tripId)
        obj.put("plannerId", trip.plannerId)
        obj.put("title", trip.title)
        obj.put("originName", trip.originName)
        obj.put("destinationName", trip.destinationName)
        obj.put("startLat", trip.startLatLng.latitude)
        obj.put("startLng", trip.startLatLng.longitude)
        obj.put("destLat", trip.destLatLng.latitude)
        obj.put("destLng", trip.destLatLng.longitude)

        val waypointsArr = JSONArray()
        trip.waypoints.forEach { waypointsArr.put(it) }
        obj.put("waypoints", waypointsArr)

        val wpLatLngArr = JSONArray()
        trip.waypointLatLngs.forEach { wp ->
            val wpObj = JSONObject()
            wpObj.put("lat", wp.latitude)
            wpObj.put("lng", wp.longitude)
            wpLatLngArr.put(wpObj)
        }
        obj.put("waypointLatLngs", wpLatLngArr)

        obj.put("distanceKm", trip.distanceKm)
        obj.put("durationMinutes", trip.durationMinutes)
        obj.put("role", trip.role.name)
        obj.put("category", trip.category.name)
        obj.put("lobbyCode", trip.lobbyCode)
        obj.put("scheduledDate", trip.scheduledDate)
        obj.put("activeRidersCount", trip.activeRidersCount)
        obj.put("avgSpeedKmh", trip.avgSpeedKmh)
        obj.put("completedKm", trip.completedKm)
        obj.put("ratingStars", trip.ratingStars)
        obj.put("incidentsCount", trip.incidentsCount)
        obj.put("activeSegmentId", trip.activeSegmentId)

        // Joined Riders
        val ridersArr = JSONArray()
        trip.joinedRiders.forEach { r ->
            val rObj = JSONObject()
            rObj.put("riderId", r.riderId)
            rObj.put("displayName", r.displayName)
            rObj.put("bikeModel", r.bikeModel)
            rObj.put("role", r.role.name)
            rObj.put("status", r.status)
            rObj.put("experienceBadge", r.experienceBadge)
            rObj.put("emergencyContact", r.emergencyContact)
            ridersArr.put(rObj)
        }
        obj.put("joinedRiders", ridersArr)

        // Route Segments
        val segArr = JSONArray()
        trip.routeSegments.forEach { seg ->
            val sObj = JSONObject()
            sObj.put("segmentId", seg.segmentId)
            sObj.put("segmentName", seg.segmentName)
            sObj.put("googleMapsUrl", seg.googleMapsUrl)
            sObj.put("originName", seg.originName)
            sObj.put("destinationName", seg.destinationName)
            sObj.put("encodedPolyline", seg.encodedPolyline)
            sObj.put("distanceKm", seg.distanceKm)
            sObj.put("estimatedDurationMinutes", seg.estimatedDurationMinutes)
            sObj.put("waypointsCount", seg.waypointsCount)
            val segWpArr = JSONArray()
            seg.waypoints.forEach { segWpArr.put(it) }
            sObj.put("waypoints", segWpArr)
            sObj.put("orderIndex", seg.orderIndex)
            segArr.put(sObj)
        }
        obj.put("routeSegments", segArr)

        // Itinerary Plan
        trip.itineraryPlan?.let { plan ->
            obj.put("itineraryPlan", serializeItineraryPlan(plan))
        }

        return obj
    }

    private fun serializeItineraryPlan(plan: ItineraryTripPlan): JSONObject {
        val planObj = JSONObject()
        planObj.put("planId", plan.planId)
        planObj.put("creationMode", plan.creationMode.name)
        planObj.put("tripTitle", plan.tripTitle)
        planObj.put("totalDuration", plan.totalDuration)
        planObj.put("startDate", plan.startDate)
        planObj.put("endDate", plan.endDate)

        val daysArr = JSONArray()
        plan.days.forEach { day ->
            val dObj = JSONObject()
            dObj.put("dayNumber", day.dayNumber)
            dObj.put("dayTitle", day.dayTitle)
            dObj.put("date", day.date)

            val stopsArr = JSONArray()
            day.stops.forEach { stop ->
                val stObj = JSONObject()
                stObj.put("stopId", stop.stopId)
                stObj.put("stopName", stop.stopName)
                stObj.put("activityDescription", stop.activityDescription)
                stObj.put("estimatedVisitTime", stop.estimatedVisitTime)
                stObj.put("rawLocationText", stop.rawLocationText)
                stObj.put("latitude", stop.latitude)
                stObj.put("longitude", stop.longitude)
                stObj.put("status", stop.status.name)
                stObj.put("orderIndex", stop.orderIndex)
                stObj.put("googleMapsUrl", stop.googleMapsUrl)
                stopsArr.put(stObj)
            }
            dObj.put("stops", stopsArr)
            daysArr.put(dObj)
        }
        planObj.put("days", daysArr)
        return planObj
    }

    private fun deserializeTrip(obj: JSONObject): SavedTrip? {
        return try {
            val tripId = obj.optString("tripId", "TRIP-${System.currentTimeMillis()}")
            val plannerId = obj.optString("plannerId", "user_host")
            val title = obj.optString("title", "Convoy Ride")
            val originName = obj.optString("originName", "Origin")
            val destinationName = obj.optString("destinationName", "Destination")
            val startLat = obj.optDouble("startLat", 17.3753)
            val startLng = obj.optDouble("startLng", 78.4344)
            val destLat = obj.optDouble("destLat", 16.5772)
            val destLng = obj.optDouble("destLng", 79.3125)

            val waypoints = mutableListOf<String>()
            val wpArr = obj.optJSONArray("waypoints")
            if (wpArr != null) {
                for (i in 0 until wpArr.length()) {
                    waypoints.add(wpArr.getString(i))
                }
            }

            val waypointLatLngs = mutableListOf<LatLng>()
            val wpLatLngArr = obj.optJSONArray("waypointLatLngs")
            if (wpLatLngArr != null) {
                for (i in 0 until wpLatLngArr.length()) {
                    val wpObj = wpLatLngArr.getJSONObject(i)
                    waypointLatLngs.add(LatLng(wpObj.optDouble("lat", 0.0), wpObj.optDouble("lng", 0.0)))
                }
            }

            val distanceKm = obj.optDouble("distanceKm", 100.0)
            val durationMinutes = obj.optInt("durationMinutes", 120)
            val roleStr = obj.optString("role", ConvoyRole.LEAD.name)
            val role = try { ConvoyRole.valueOf(roleStr) } catch (_: Exception) { ConvoyRole.LEAD }

            val catStr = obj.optString("category", TripCategory.UPCOMING.name)
            val category = try { TripCategory.valueOf(catStr) } catch (_: Exception) { TripCategory.UPCOMING }

            val lobbyCode = obj.optString("lobbyCode", "")
            val scheduledDate = obj.optString("scheduledDate", "")
            val activeRidersCount = obj.optInt("activeRidersCount", 1)
            val avgSpeedKmh = obj.optInt("avgSpeedKmh", 60)
            val completedKm = obj.optDouble("completedKm", 0.0)
            val ratingStars = obj.optDouble("ratingStars", 5.0)
            val incidentsCount = obj.optInt("incidentsCount", 0)
            val activeSegmentId = obj.optString("activeSegmentId", "")

            // Itinerary Plan
            val itineraryPlan = obj.optJSONObject("itineraryPlan")?.let { deserializeItineraryPlan(it) }

            // Joined Riders
            val joinedRiders = mutableListOf<JoinedRiderProfile>()
            val rArr = obj.optJSONArray("joinedRiders")
            if (rArr != null) {
                for (i in 0 until rArr.length()) {
                    val rObj = rArr.getJSONObject(i)
                    val rRoleStr = rObj.optString("role", ConvoyRole.MEMBER.name)
                    val rRole = try { ConvoyRole.valueOf(rRoleStr) } catch (_: Exception) { ConvoyRole.MEMBER }
                    joinedRiders.add(
                        JoinedRiderProfile(
                            riderId = rObj.optString("riderId", "r$i"),
                            displayName = rObj.optString("displayName", "Rider"),
                            bikeModel = rObj.optString("bikeModel", "Motorcycle"),
                            role = rRole,
                            status = rObj.optString("status", "Confirmed & Ready"),
                            experienceBadge = rObj.optString("experienceBadge", "Pro Tourer"),
                            emergencyContact = rObj.optString("emergencyContact", "")
                        )
                    )
                }
            }

            // Route Segments
            val routeSegments = mutableListOf<TripRouteSegment>()
            val segArr = obj.optJSONArray("routeSegments")
            if (segArr != null) {
                for (i in 0 until segArr.length()) {
                    val sObj = segArr.getJSONObject(i)
                    val segWps = mutableListOf<String>()
                    val sWpArr = sObj.optJSONArray("waypoints")
                    if (sWpArr != null) {
                        for (j in 0 until sWpArr.length()) {
                            segWps.add(sWpArr.getString(j))
                        }
                    }
                    routeSegments.add(
                        TripRouteSegment(
                            segmentId = sObj.optString("segmentId", "seg_$i"),
                            segmentName = sObj.optString("segmentName", "Day ${i + 1} Route"),
                            googleMapsUrl = sObj.optString("googleMapsUrl", ""),
                            originName = sObj.optString("originName", ""),
                            destinationName = sObj.optString("destinationName", ""),
                            encodedPolyline = sObj.optString("encodedPolyline", ""),
                            distanceKm = sObj.optDouble("distanceKm", 0.0),
                            estimatedDurationMinutes = sObj.optInt("estimatedDurationMinutes", 0),
                            waypointsCount = sObj.optInt("waypointsCount", segWps.size),
                            waypoints = segWps,
                            orderIndex = sObj.optInt("orderIndex", i)
                        )
                    )
                }
            }

            SavedTrip(
                tripId = tripId,
                plannerId = plannerId,
                title = title,
                originName = originName,
                destinationName = destinationName,
                startLatLng = LatLng(startLat, startLng),
                destLatLng = LatLng(destLat, destLng),
                waypoints = waypoints,
                waypointLatLngs = waypointLatLngs,
                distanceKm = distanceKm,
                durationMinutes = durationMinutes,
                role = role,
                category = category,
                lobbyCode = lobbyCode,
                scheduledDate = scheduledDate,
                activeRidersCount = activeRidersCount,
                avgSpeedKmh = avgSpeedKmh,
                completedKm = completedKm,
                ratingStars = ratingStars,
                incidentsCount = incidentsCount,
                joinedRiders = joinedRiders,
                routeSegments = routeSegments,
                activeSegmentId = activeSegmentId,
                itineraryPlan = itineraryPlan
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to deserialize trip: ${e.message}", e)
            null
        }
    }

    private fun deserializeItineraryPlan(obj: JSONObject): ItineraryTripPlan? {
        return try {
            val planId = obj.optString("planId", "plan_${System.currentTimeMillis()}")
            val modeStr = obj.optString("creationMode", TripCreationMode.MANUAL_SEARCH.name)
            val creationMode = try { TripCreationMode.valueOf(modeStr) } catch (_: Exception) { TripCreationMode.MANUAL_SEARCH }
            val tripTitle = obj.optString("tripTitle", "Itinerary Plan")
            val totalDuration = obj.optString("totalDuration", "")
            val startDate = obj.optString("startDate", "")
            val endDate = obj.optString("endDate", "")

            val days = mutableListOf<ItineraryDay>()
            val daysArr = obj.optJSONArray("days")
            if (daysArr != null) {
                for (i in 0 until daysArr.length()) {
                    val dObj = daysArr.getJSONObject(i)
                    val dayNum = dObj.optInt("dayNumber", i + 1)
                    val dayTitle = dObj.optString("dayTitle", "Day $dayNum")
                    val date = dObj.optString("date", "")

                    val stops = mutableListOf<ItineraryStop>()
                    val stopsArr = dObj.optJSONArray("stops")
                    if (stopsArr != null) {
                        for (j in 0 until stopsArr.length()) {
                            val stObj = stopsArr.getJSONObject(j)
                            val statusStr = stObj.optString("status", ItineraryStopStatus.PENDING.name)
                            val status = try { ItineraryStopStatus.valueOf(statusStr) } catch (_: Exception) { ItineraryStopStatus.PENDING }
                            stops.add(
                                ItineraryStop(
                                    stopId = stObj.optString("stopId", "stop_${i}_$j"),
                                    stopName = stObj.optString("stopName", "Stop ${j + 1}"),
                                    activityDescription = stObj.optString("activityDescription", ""),
                                    estimatedVisitTime = stObj.optString("estimatedVisitTime", ""),
                                    rawLocationText = stObj.optString("rawLocationText", ""),
                                    latitude = stObj.optDouble("latitude", 0.0),
                                    longitude = stObj.optDouble("longitude", 0.0),
                                    status = status,
                                    orderIndex = stObj.optInt("orderIndex", j),
                                    googleMapsUrl = stObj.optString("googleMapsUrl", "")
                                )
                            )
                        }
                    }
                    days.add(ItineraryDay(dayNumber = dayNum, dayTitle = dayTitle, date = date, stops = stops))
                }
            }
            ItineraryTripPlan(planId = planId, creationMode = creationMode, tripTitle = tripTitle, totalDuration = totalDuration, startDate = startDate, endDate = endDate, days = days)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to deserialize itinerary plan: ${e.message}", e)
            null
        }
    }

    private fun getStarterTrips(): List<SavedTrip> {
        return emptyList()
    }

    @Synchronized
    fun resetStorage() {
        try {
            prefs?.edit()?.clear()?.apply()
            _tripsFlow.value = emptyList()
            isInitialized = true
            Log.i(TAG, "Successfully reset and cleared all stored trip database entries.")

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val url = URL("$CLOUDFLARE_EDGE_URL/api/trip/reset")
                    val conn = url.openConnection() as HttpURLConnection
                    conn.requestMethod = "POST"
                    conn.connectTimeout = 5000
                    conn.readTimeout = 5000
                    Log.d(TAG, "Cloudflare D1 database reset code: ${conn.responseCode}")
                } catch (e: Exception) {
                    Log.w(TAG, "Cloudflare D1 reset note: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to reset trip storage: ${e.message}", e)
        }
    }

    fun fetchOnlineTripsAsync(onComplete: ((List<SavedTrip>) -> Unit)? = null) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("$CLOUDFLARE_EDGE_URL/api/trips/public")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.connectTimeout = 6000
                conn.readTimeout = 6000

                if (conn.responseCode in 200..299) {
                    val text = conn.inputStream.bufferedReader().readText()
                    val json = JSONObject(text)
                    val tripsArr = json.optJSONArray("trips")
                    val fetched = mutableListOf<SavedTrip>()
                    if (tripsArr != null) {
                        for (i in 0 until tripsArr.length()) {
                            val obj = tripsArr.getJSONObject(i)
                            deserializeTrip(obj)?.let { fetched.add(it) }
                        }
                    }
                    if (fetched.isNotEmpty()) {
                        val merged = (fetched + _tripsFlow.value).distinctBy { it.tripId }
                        updateAndPersistList(merged)
                        onComplete?.invoke(merged)
                        return@launch
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Cloudflare fetch trips note: ${e.message}")
            }
            onComplete?.invoke(_tripsFlow.value)
        }
    }

    suspend fun fetchTripByLobbyCode(code: String): SavedTrip? {
        return kotlinx.coroutines.withContext(Dispatchers.IO) {
            val cleanCode = code.trim().uppercase()

            // 1. Query Cloudflare Edge & D1 Database first
            try {
                val url = URL("$CLOUDFLARE_EDGE_URL/api/trip/by-code?code=$cleanCode")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.connectTimeout = 6000
                conn.readTimeout = 6000

                if (conn.responseCode in 200..299) {
                    val text = conn.inputStream.bufferedReader().readText()
                    val json = JSONObject(text)
                    val tripObj = json.optJSONObject("trip")
                    if (tripObj != null) {
                        val parsed = deserializeTrip(tripObj)
                        if (parsed != null && parsed.title.isNotBlank()) {
                            saveTrip(parsed)
                            return@withContext parsed
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error looking up trip code $cleanCode on Cloudflare", e)
            }

            // 2. Fallback to local cache
            getAllTrips().firstOrNull { it.lobbyCode.equals(cleanCode, ignoreCase = true) }
        }
    }

    private fun syncTripToCloudflareAsync(trip: SavedTrip) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("$CLOUDFLARE_EDGE_URL/api/trip/create")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.connectTimeout = 6000
                conn.readTimeout = 6000
                conn.doOutput = true

                val payload = serializeTrip(trip).toString()
                OutputStreamWriter(conn.outputStream).use { it.write(payload) }
                val code = conn.responseCode
                Log.d(TAG, "Cloudflare D1 Trip Sync Response: $code")
            } catch (e: Exception) {
                Log.w(TAG, "Cloudflare D1 Trip Sync Note: ${e.message}")
            }
        }
    }

    private fun joinTripOnCloudflareAsync(tripId: String, lobbyCode: String, riderProfile: JoinedRiderProfile) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("$CLOUDFLARE_EDGE_URL/api/trip/join")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.connectTimeout = 6000
                conn.readTimeout = 6000
                conn.doOutput = true

                val rObj = JSONObject().apply {
                    put("riderId", riderProfile.riderId)
                    put("displayName", riderProfile.displayName)
                    put("bikeModel", riderProfile.bikeModel)
                    put("role", riderProfile.role.name)
                    put("status", riderProfile.status)
                    put("experienceBadge", riderProfile.experienceBadge)
                    put("emergencyContact", riderProfile.emergencyContact)
                }

                val payload = JSONObject().apply {
                    put("tripId", tripId)
                    put("lobbyCode", lobbyCode)
                    put("riderProfile", rObj)
                }.toString()

                OutputStreamWriter(conn.outputStream).use { it.write(payload) }
                Log.d(TAG, "Cloudflare Join Trip Response: ${conn.responseCode}")
            } catch (e: Exception) {
                Log.w(TAG, "Cloudflare Join Trip Note: ${e.message}")
            }
        }
    }

    private fun leaveTripOnCloudflareAsync(tripId: String, riderId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("$CLOUDFLARE_EDGE_URL/api/trip/leave")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.connectTimeout = 6000
                conn.readTimeout = 6000
                conn.doOutput = true

                val payload = JSONObject().apply {
                    put("tripId", tripId)
                    put("riderId", riderId)
                }.toString()

                OutputStreamWriter(conn.outputStream).use { it.write(payload) }
                Log.d(TAG, "Cloudflare Leave Trip Response: ${conn.responseCode}")
            } catch (e: Exception) {
                Log.w(TAG, "Cloudflare Leave Trip Note: ${e.message}")
            }
        }
    }

    private fun deleteTripFromCloudflareAsync(tripId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("$CLOUDFLARE_EDGE_URL/api/trip/delete")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.connectTimeout = 6000
                conn.readTimeout = 6000
                conn.doOutput = true

                val payload = JSONObject().apply {
                    put("tripId", tripId)
                }.toString()

                OutputStreamWriter(conn.outputStream).use { it.write(payload) }
                Log.d(TAG, "Cloudflare Delete Trip Response: ${conn.responseCode}")
            } catch (e: Exception) {
                Log.w(TAG, "Cloudflare Delete Trip Note: ${e.message}")
            }
        }
    }

    private fun syncStopStatusToCloudflareAsync(tripId: String, dayNumber: Int, stopId: String, newStatus: ItineraryStopStatus) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("$CLOUDFLARE_EDGE_URL/api/trip/stop/status")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                conn.doOutput = true

                val payload = JSONObject().apply {
                    put("tripId", tripId)
                    put("dayNumber", dayNumber)
                    put("stopId", stopId)
                    put("status", newStatus.name)
                }.toString()

                OutputStreamWriter(conn.outputStream).use { it.write(payload) }
                val code = conn.responseCode
                Log.d(TAG, "Cloudflare D1 Stop Status Sync Response: $code")
            } catch (e: Exception) {
                Log.w(TAG, "Cloudflare D1 Stop Status Sync Note: ${e.message}")
            }
        }
    }
}
