package com.ridesync.engine

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import com.ridesync.RideSyncApplication
import com.ridesync.data.model.RiderLocationPing
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

object LiveLocationEngine {
    private const val TAG = "LiveLocationEngine"

    private val _liveLocationPing = MutableStateFlow<RiderLocationPing?>(null)
    val liveLocationPing: StateFlow<RiderLocationPing?> = _liveLocationPing.asStateFlow()

    private val _liveLatLng = MutableStateFlow<LatLng?>(null)
    val liveLatLng: StateFlow<LatLng?> = _liveLatLng.asStateFlow()

    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var locationCallback: LocationCallback? = null
    private var isListening = false

    fun hasLocationPermission(context: Context = RideSyncApplication.appContext): Boolean {
        val fine = ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    @SuppressLint("MissingPermission")
    fun startLiveLocationUpdates(context: Context = RideSyncApplication.appContext) {
        if (!hasLocationPermission(context)) {
            Log.w(TAG, "Location permission not granted. Cannot start LiveLocationEngine.")
            return
        }

        try {
            if (fusedLocationClient == null) {
                fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            }

            // Immediately attempt to fetch last known GPS location
            fusedLocationClient?.lastLocation?.addOnSuccessListener { location: Location? ->
                if (location != null) {
                    updateLocation(location)
                    Log.d(TAG, "Initial last known phone GPS acquired: ${location.latitude}, ${location.longitude}")
                }
            }

            if (!isListening) {
                val locationRequest = LocationRequest.Builder(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    1200L // update every 1.2 seconds for real-time mobile tracking
                ).apply {
                    setMinUpdateIntervalMillis(600L)
                    setMinUpdateDistanceMeters(0.5f) // update every half meter displacement
                    setWaitForAccurateLocation(false)
                }.build()

                locationCallback = object : LocationCallback() {
                    override fun onLocationResult(result: LocationResult) {
                        val loc = result.lastLocation ?: return
                        updateLocation(loc)
                    }

                    override fun onLocationAvailability(avail: LocationAvailability) {
                        Log.d(TAG, "Location availability: ${avail.isLocationAvailable}")
                    }
                }

                fusedLocationClient?.requestLocationUpdates(
                    locationRequest,
                    locationCallback!!,
                    Looper.getMainLooper()
                )

                isListening = true
                Log.d(TAG, "LiveLocationEngine started high-accuracy GPS tracking")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting location updates with FusedLocationClient, attempting LocationManager fallback: ${e.message}", e)
            startLocationManagerFallback(context)
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationManagerFallback(context: Context) {
        try {
            val locManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    updateLocation(location)
                }
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
            }

            if (locManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 0.5f, listener, Looper.getMainLooper())
            } else if (locManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1500L, 1f, listener, Looper.getMainLooper())
            }
            isListening = true
        } catch (e: Exception) {
            Log.e(TAG, "LocationManager fallback failed: ${e.message}")
        }
    }

    fun stopLiveLocationUpdates() {
        if (!isListening) return
        try {
            locationCallback?.let { fusedLocationClient?.removeLocationUpdates(it) }
            locationCallback = null
            isListening = false
            Log.d(TAG, "LiveLocationEngine stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping location updates: ${e.message}")
        }
    }

    suspend fun getCurrentPhoneLocation(context: Context = RideSyncApplication.appContext, forceFresh: Boolean = false): LatLng? {
        if (!hasLocationPermission(context)) return null

        if (!forceFresh && _liveLatLng.value != null) {
            return _liveLatLng.value
        }

        return withContext(Dispatchers.IO) {
            try {
                if (fusedLocationClient == null) {
                    fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
                }
                @SuppressLint("MissingPermission")
                val loc = fusedLocationClient?.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)?.await()
                    ?: fusedLocationClient?.lastLocation?.await()
                if (loc != null) {
                    withContext(Dispatchers.Main) { updateLocation(loc) }
                    LatLng(loc.latitude, loc.longitude)
                } else {
                    _liveLatLng.value
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get current phone location: ${e.message}")
                _liveLatLng.value
            }
        }
    }

    fun updateLocation(location: Location) {
        val speedKmh = location.speed * 3.6f
        val ping = RiderLocationPing(
            latitude = location.latitude,
            longitude = location.longitude,
            speedKmh = speedKmh,
            bearing = location.bearing,
            altitude = location.altitude,
            timestamp = System.currentTimeMillis()
        )
        _liveLocationPing.value = ping
        _liveLatLng.value = LatLng(location.latitude, location.longitude)
    }
}
