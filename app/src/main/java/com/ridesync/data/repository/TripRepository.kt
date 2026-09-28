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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

object TripRepository {
    private const val TAG = "TripRepository"
    private const val PREFS_NAME = "ridesync_trips_prefs"
    private const val KEY_TRIPS_JSON = "saved_trips_json"

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
    }

    @Synchronized
    fun getAllTrips(): List<SavedTrip> {
        if (_tripsFlow.value.isEmpty()) {
            loadTripsFromStorage()
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
    }

    @Synchronized
    fun deleteTrip(tripId: String) {
        val currentList = getAllTrips().toMutableList()
        val removed = currentList.removeAll { it.tripId == tripId }
        if (removed) {
            updateAndPersistList(currentList)
        }
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
                activeRidersCount = (trip.activeRidersCount - 1).coerceAtLeast(0)
            )
            currentList[index] = updatedTrip
            updateAndPersistList(currentList)
        }
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
            if (!jsonStr.isNullOrBlank()) {
                val jsonArray = JSONArray(jsonStr)
                val loadedList = mutableListOf<SavedTrip>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    deserializeTrip(obj)?.let { loadedList.add(it) }
                }
                if (loadedList.isNotEmpty()) {
                    _tripsFlow.value = loadedList
                    return
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading trips from storage: ${e.message}", e)
        }

        // Seed with default initial starter trips if storage is empty
        val starterTrips = getStarterTrips()
        updateAndPersistList(starterTrips)
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

            val lobbyCode = obj.optString("lobbyCode", "RRS-1001")
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
                            emergencyContact = rObj.optString("emergencyContact", "+91 98765 43210")
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
        return listOf(
            SavedTrip(
                tripId = "TRIP-ONGOING-01",
                plannerId = "user_host",
                title = "Hyderabad to Srisailam Dam Ghat Run",
                originName = "Attapur, Hyderabad",
                destinationName = "Srisailam Dam Viewpoint",
                startLatLng = LatLng(17.3753, 78.4344),
                destLatLng = LatLng(16.0748, 78.8687),
                waypoints = listOf("Kadthal Sagar Highway Stop"),
                waypointLatLngs = listOf(LatLng(17.0854, 78.5891)),
                distanceKm = 159.0,
                durationMinutes = 214,
                role = ConvoyRole.LEAD,
                category = TripCategory.ONGOING,
                lobbyCode = "RRS-9921",
                activeRidersCount = 4,
                avgSpeedKmh = 72,
                completedKm = 42.5,
                joinedRiders = listOf(
                    JoinedRiderProfile("r1", "Ahmed (You)", "Royal Enfield Meteor 350", ConvoyRole.LEAD, "Riding in Convoy", "Lead Navigator", "+91 86868 71994"),
                    JoinedRiderProfile("r2", "Rahul Sharma", "KTM Duke 390", ConvoyRole.SWEEP, "Sweep Guard Active", "Safety Marshal", "+91 98765 43210"),
                    JoinedRiderProfile("r3", "Vikram Singh", "BMW R 1250 GS", ConvoyRole.MEMBER, "In Pack", "Road Captain", "+91 91234 56789"),
                    JoinedRiderProfile("r4", "Priya Nair", "Kawasaki Ninja 650", ConvoyRole.MEMBER, "In Pack", "Pro Tourer", "+91 99887 76655")
                ),
                routeSegments = listOf(
                    TripRouteSegment(
                        segmentId = "seg_srisailam_day1",
                        segmentName = "Day 1: Hyderabad to Dindi Reservoir",
                        googleMapsUrl = "https://www.google.com/maps/dir/17.3753,78.4344/17.0854,78.5891/16.5700,78.9600",
                        originName = "Attapur, Hyderabad",
                        destinationName = "Dindi Reservoir Viewpoint",
                        encodedPolyline = "_p~iF~ps|U_ulLnnqC_mqNvxq`@",
                        distanceKm = 102.0,
                        estimatedDurationMinutes = 135,
                        waypointsCount = 1,
                        waypoints = listOf("Kadthal Sagar Highway Stop"),
                        orderIndex = 0
                    ),
                    TripRouteSegment(
                        segmentId = "seg_srisailam_day2",
                        segmentName = "Day 2: Dindi to Srisailam Dam Ghats",
                        googleMapsUrl = "https://www.google.com/maps/dir/16.5700,78.9600/16.3500,78.9100/16.0748,78.8687",
                        originName = "Dindi Reservoir",
                        destinationName = "Srisailam Dam Viewpoint",
                        encodedPolyline = "_p~iF~ps|U_ulLnnqC_mqNvxq`@",
                        distanceKm = 57.0,
                        estimatedDurationMinutes = 79,
                        waypointsCount = 1,
                        waypoints = listOf("Dindi Ghats Viewpoint"),
                        orderIndex = 1
                    )
                )
            ),
            SavedTrip(
                tripId = "TRIP-UPCOMING-01",
                plannerId = "user_host",
                title = "Bangalore Weekend Highway Ride",
                originName = "Bengaluru City Center",
                destinationName = "Nandi Hills Peak",
                startLatLng = LatLng(12.9716, 77.5946),
                destLatLng = LatLng(13.3702, 77.6835),
                waypoints = listOf("Devanahalli Toll Plaza"),
                waypointLatLngs = listOf(LatLng(13.2483, 77.7126)),
                distanceKm = 62.0,
                durationMinutes = 80,
                role = ConvoyRole.LEAD,
                category = TripCategory.UPCOMING,
                lobbyCode = "RRS-4482",
                scheduledDate = "Tomorrow, 06:00 AM",
                joinedRiders = listOf(
                    JoinedRiderProfile("r1", "Ahmed (You)", "Royal Enfield Meteor 350", ConvoyRole.LEAD, "Confirmed & Ready", "Lead Navigator", "+91 86868 71994"),
                    JoinedRiderProfile("r5", "Karan Verma", "Triumph Tiger 900", ConvoyRole.MEMBER, "Confirmed & Ready", "Pro Cruiser", "+91 97654 32109"),
                    JoinedRiderProfile("r6", "Ananya Roy", "RE Interceptor 650", ConvoyRole.SWEEP, "Confirmed & Ready", "Safety Marshal", "+91 98123 45678")
                )
            ),
            SavedTrip(
                tripId = "TRIP-UPCOMING-02",
                plannerId = "user_friend",
                title = "Goa Coastal Highway Express",
                originName = "Marine Drive, Mumbai",
                destinationName = "Calangute Beach, Goa",
                startLatLng = LatLng(18.9438, 72.8234),
                destLatLng = LatLng(15.5438, 73.7554),
                waypoints = listOf("Ratnagiri Coastal Stop"),
                waypointLatLngs = listOf(LatLng(16.9902, 73.3120)),
                distanceKm = 580.0,
                durationMinutes = 645,
                role = ConvoyRole.SWEEP,
                category = TripCategory.UPCOMING,
                lobbyCode = "RRS-7719",
                scheduledDate = "Oct 12, 2026 • 05:30 AM",
                joinedRiders = listOf(
                    JoinedRiderProfile("r1", "Ahmed (You)", "Royal Enfield Meteor 350", ConvoyRole.SWEEP, "Confirmed & Ready", "Lead Navigator", "+91 86868 71994"),
                    JoinedRiderProfile("r7", "Suresh Kumar", "Harley Davidson Street 750", ConvoyRole.LEAD, "Confirmed & Ready", "Highway Captain", "+91 96543 21098"),
                    JoinedRiderProfile("r8", "Sneha Patel", "Honda CB350 RS", ConvoyRole.MEMBER, "Confirmed & Ready", "Pro Tourer", "+91 95432 10987"),
                    JoinedRiderProfile("r9", "Rajesh Rao", "RE Himalayan 450", ConvoyRole.MEMBER, "Confirmed & Ready", "Adventure Specialist", "+91 94321 09876")
                ),
                routeSegments = listOf(
                    TripRouteSegment(
                        segmentId = "seg_goa_day1",
                        segmentName = "Day 1: Mumbai to Ratnagiri Coastal Leg",
                        googleMapsUrl = "https://www.google.com/maps/dir/18.9438,72.8234/18.6414,72.8722/17.5323,73.5186/16.9902,73.3120",
                        originName = "Marine Drive, Mumbai",
                        destinationName = "Ratnagiri Coastal Stop",
                        encodedPolyline = "_p~iF~ps|U_ulLnnqC_mqNvxq`@",
                        distanceKm = 330.0,
                        estimatedDurationMinutes = 360,
                        waypointsCount = 2,
                        waypoints = listOf("Alibaug Coastal Stop", "Chiplun River Viewpoint"),
                        orderIndex = 0
                    ),
                    TripRouteSegment(
                        segmentId = "seg_goa_day2",
                        segmentName = "Day 2: Ratnagiri to Calangute Goa Final Run",
                        googleMapsUrl = "https://www.google.com/maps/dir/16.9902,73.3120/16.0558,73.4687/15.5438,73.7554",
                        originName = "Ratnagiri Coastal Stop",
                        destinationName = "Calangute Beach, Goa",
                        encodedPolyline = "_p~iF~ps|U_ulLnnqC_mqNvxq`@",
                        distanceKm = 250.0,
                        estimatedDurationMinutes = 285,
                        waypointsCount = 1,
                        waypoints = listOf("Malvan Fort Stop"),
                        orderIndex = 1
                    )
                )
            ),
            SavedTrip(
                tripId = "TRIP-COMPLETED-01",
                plannerId = "user_host",
                title = "Attapur to Nagarjuna Sagar Dam Run",
                originName = "Attapur, Hyderabad",
                destinationName = "Nagarjuna Sagar Dam",
                startLatLng = LatLng(17.3753, 78.4344),
                destLatLng = LatLng(16.5772, 79.3125),
                waypoints = listOf("Ibrahimpatnam Sagar Rd Stop", "Devarakonda Fort Stop"),
                waypointLatLngs = listOf(LatLng(17.1856, 78.6473), LatLng(16.6978, 78.9281)),
                distanceKm = 159.0,
                durationMinutes = 214,
                role = ConvoyRole.LEAD,
                category = TripCategory.COMPLETED,
                completedKm = 159.0,
                activeRidersCount = 5,
                avgSpeedKmh = 68,
                ratingStars = 5.0,
                scheduledDate = "Sep 10, 2026",
                joinedRiders = listOf(
                    JoinedRiderProfile("r1", "Ahmed (You)", "Royal Enfield Meteor 350", ConvoyRole.LEAD, "Completed Ride", "Lead Navigator", "+91 86868 71994"),
                    JoinedRiderProfile("r2", "Rahul Sharma", "KTM Duke 390", ConvoyRole.SWEEP, "Completed Ride", "Safety Marshal", "+91 98765 43210"),
                    JoinedRiderProfile("r3", "Vikram Singh", "BMW R 1250 GS", ConvoyRole.MEMBER, "Completed Ride", "Road Captain", "+91 91234 56789"),
                    JoinedRiderProfile("r4", "Priya Nair", "Kawasaki Ninja 650", ConvoyRole.MEMBER, "Completed Ride", "Pro Tourer", "+91 99887 76655"),
                    JoinedRiderProfile("r10", "Amit Joshi", "RE Classic 350", ConvoyRole.MEMBER, "Completed Ride", "Veteran Cruiser", "+91 93210 98765")
                )
            ),
            SavedTrip(
                tripId = "TRIP-COMPLETED-02",
                plannerId = "user_host",
                title = "Charminar City Night Patrol",
                originName = "Charminar, Old City",
                destinationName = "Hitech City, Hyderabad",
                startLatLng = LatLng(17.3616, 78.4747),
                destLatLng = LatLng(17.4435, 78.3772),
                waypoints = emptyList(),
                distanceKm = 42.0,
                durationMinutes = 75,
                role = ConvoyRole.MEMBER,
                category = TripCategory.COMPLETED,
                completedKm = 42.0,
                activeRidersCount = 8,
                avgSpeedKmh = 45,
                ratingStars = 4.9,
                scheduledDate = "Aug 28, 2026",
                joinedRiders = listOf(
                    JoinedRiderProfile("r1", "Ahmed (You)", "Royal Enfield Meteor 350", ConvoyRole.MEMBER, "Completed Ride", "Lead Navigator", "+91 86868 71994"),
                    JoinedRiderProfile("r11", "Sameer Khan", "Yamaha R3", ConvoyRole.LEAD, "Completed Ride", "Night Patrol Captain", "+91 92109 87654"),
                    JoinedRiderProfile("r12", "Zoya Siddiqui", "RE Hunter 350", ConvoyRole.SWEEP, "Completed Ride", "Safety Marshal", "+91 91098 76543")
                )
            ),
            SavedTrip(
                tripId = "TRIP-COMPLETED-03",
                plannerId = "user_host",
                title = "Ananthagiri Hills Monsoon Ride",
                originName = "Gachibowli, Hyderabad",
                destinationName = "Ananthagiri Hills, Vikarabad",
                startLatLng = LatLng(17.4401, 78.3489),
                destLatLng = LatLng(17.3114, 77.8631),
                waypoints = listOf("Vikarabad Viewpoint"),
                distanceKm = 85.0,
                durationMinutes = 130,
                role = ConvoyRole.LEAD,
                category = TripCategory.COMPLETED,
                completedKm = 85.0,
                activeRidersCount = 6,
                avgSpeedKmh = 58,
                ratingStars = 4.8,
                scheduledDate = "Aug 14, 2026",
                joinedRiders = listOf(
                    JoinedRiderProfile("r1", "Ahmed (You)", "Royal Enfield Meteor 350", ConvoyRole.LEAD, "Completed Ride", "Lead Navigator", "+91 86868 71994"),
                    JoinedRiderProfile("r13", "Manish Varma", "Dominar 400", ConvoyRole.SWEEP, "Completed Ride", "Hill Specialist", "+91 90987 65432"),
                    JoinedRiderProfile("r14", "Deepak Reddy", "KTM Adventure 390", ConvoyRole.MEMBER, "Completed Ride", "Pro Tourer", "+91 89876 54321")
                )
            )
        )
    }
}
