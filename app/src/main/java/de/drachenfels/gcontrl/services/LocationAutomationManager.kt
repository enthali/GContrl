package de.drachenfels.gcontrl.services

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import de.drachenfels.gcontrl.utils.AndroidLogger
import de.drachenfels.gcontrl.utils.LogConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

// Battery optimization implemented through dynamic location updates

data class LocationData(
    val latitude: Double,
    val longitude: Double,
    val speed: Float,
    val distanceToGarage: Double? = null
)

class LocationAutomationManager private constructor() {
    private val logger = AndroidLogger()
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private lateinit var prefs: SharedPreferences
    private lateinit var context: Context
    
    // States
    private val _locationState = MutableStateFlow<LocationState>(LocationState.Inactive)
    val locationState: StateFlow<LocationState> = _locationState
    
    private val _locationData = MutableStateFlow<LocationData?>(null)
    val locationData: StateFlow<LocationData?> = _locationData

    // Settings keys
    private val PREFS_NAME = "GContrlPrefs"
    private val KEY_LOCATION_AUTOMATION_ENABLED = "location_automation_enabled"
    private val KEY_TRIGGER_DISTANCE = "trigger_distance"
    private val KEY_HOME_LATITUDE = "garage_latitude"
    private val KEY_HOME_LONGITUDE = "garage_longitude"

    // Location tracking constants
    private val NEAR_DISTANCE = 500.0 // meters
    private val FAST_UPDATE_INTERVAL = 2000L // 2 second
    private val SLOW_UPDATE_INTERVAL = 20000L // 20 seconds

    // State tracking
    private var lastKnownDistance: Double? = null
    private var lastCommandTime: Long = 0
    private val COMMAND_COOLDOWN = 60000L // 1 minute cooldown
    private var currentUpdateMode: UpdateMode = UpdateMode.UNKNOWN

    companion object {
        @Volatile
        private var instance: LocationAutomationManager? = null
        
        fun getInstance(): LocationAutomationManager {
            return instance ?: synchronized(this) {
                instance ?: LocationAutomationManager().also { instance = it }
            }
        }
    }

    fun initialize(context: Context) {
        this.context = context
        if (!::prefs.isInitialized) {
            prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
        if (!::fusedLocationClient.isInitialized) {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            // Initial setup with default values - will be updated based on distance
            createLocationRequest(FAST_UPDATE_INTERVAL, Priority.PRIORITY_HIGH_ACCURACY)
            createLocationCallback()
        }
    }

    private fun createLocationRequest(interval: Long, priority: Int) {
        locationRequest = LocationRequest.Builder(interval)
            .setPriority(priority)
            .build()
    }

    private fun createLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    logger.d(LogConfig.TAG_LOCATION, "Location update: ${location.latitude}, ${location.longitude}")

                    // Calculate distance if home location is set
                    val homeLatitude = prefs.getFloat(KEY_HOME_LATITUDE, 0.0f).toDouble()
                    val homeLongitude = prefs.getFloat(KEY_HOME_LONGITUDE, 0.0f).toDouble()
                    val distance = if (homeLatitude != 0.0 && homeLongitude != 0.0) {
                        val homeLocation = Location("").apply {
                            latitude = homeLatitude
                            longitude = homeLongitude
                        }
                        calculateDistance(location, homeLocation)
                    } else null

                    // Update location data
                    val locationData = LocationData(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        speed = location.speed,
                        distanceToGarage = distance
                    )
                    _locationData.value = locationData
                    
                    // Adjust location update strategy based on distance
                    distance?.let { adjustLocationUpdates(it) }
                    
                    checkLocation(location)
                }
            }
        }
    }

    private fun adjustLocationUpdates(distance: Double) {
        val newMode = if (distance < NEAR_DISTANCE) {
            UpdateMode.NEAR
        } else {
            UpdateMode.FAR
        }
        
        // Only update if the mode changed
        if (newMode != currentUpdateMode) {
            when (newMode) {
                UpdateMode.NEAR -> {
                    logger.d(LogConfig.TAG_LOCATION, "Switching to high accuracy mode (within 500m)")
                    updateLocationRequest(FAST_UPDATE_INTERVAL, Priority.PRIORITY_HIGH_ACCURACY)
                }
                UpdateMode.FAR -> {
                    logger.d(LogConfig.TAG_LOCATION, "Switching to balanced power mode (beyond 500m)")
                    //updateLocationRequest(SLOW_UPDATE_INTERVAL, Priority.PRIORITY_BALANCED_POWER_ACCURACY)
                    // TODO : find out why balanced power accuracy doesn't work in the background, for now we use high accuracy
                    updateLocationRequest(SLOW_UPDATE_INTERVAL, Priority.PRIORITY_HIGH_ACCURACY)
                }
                else -> { /* Do nothing for UNKNOWN */ }
            }
            currentUpdateMode = newMode
        }
    }

    private fun updateLocationRequest(interval: Long, priority: Int) {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        try {
            // Create new request with updated parameters
            createLocationRequest(interval, priority)

            // Update existing request without removing
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                null
            )

            logger.d(LogConfig.TAG_LOCATION, "Location updates adjusted: interval=$interval, priority=$priority")
        } catch (e: Exception) {
            logger.e(LogConfig.TAG_LOCATION, "Failed to update location request", e)
        }
    }

    fun startLocationTracking() {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            _locationState.value = LocationState.Error("Missing location permission")
            return
        }

        try {
            // Start with default high accuracy - will adjust based on distance
            createLocationRequest(FAST_UPDATE_INTERVAL, Priority.PRIORITY_HIGH_ACCURACY)
            currentUpdateMode = UpdateMode.UNKNOWN
            
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                null
            )
            _locationState.value = LocationState.Active
            logger.d(LogConfig.TAG_LOCATION, "Location tracking started")
        } catch (e: Exception) {
            _locationState.value = LocationState.Error("Failed to start location tracking: ${e.message}")
            logger.e(LogConfig.TAG_LOCATION, "Failed to start location tracking", e)
        }
    }

    fun stopLocationTracking() {
        try {
            fusedLocationClient.removeLocationUpdates(locationCallback)
            _locationState.value = LocationState.Inactive
            currentUpdateMode = UpdateMode.UNKNOWN
            logger.d(LogConfig.TAG_LOCATION, "Location tracking stopped")
        } catch (e: Exception) {
            _locationState.value = LocationState.Error("Failed to stop location tracking: ${e.message}")
            logger.e(LogConfig.TAG_LOCATION, "Failed to stop location tracking", e)
        }
    }

    private fun checkLocation(currentLocation: Location) {
        val isLocationAutomationEnabled = prefs.getBoolean(KEY_LOCATION_AUTOMATION_ENABLED, false)
        if (!isLocationAutomationEnabled) {
            logger.d(LogConfig.TAG_LOCATION, "Location Automation is disabled")
            return
        }

        val homeLatitude = prefs.getFloat(KEY_HOME_LATITUDE, 0.0f).toDouble()
        val homeLongitude = prefs.getFloat(KEY_HOME_LONGITUDE, 0.0f).toDouble()
        val triggerDistance = prefs.getInt(KEY_TRIGGER_DISTANCE, 100)

        if (homeLatitude == 0.0 && homeLongitude == 0.0) {
            logger.d(LogConfig.TAG_LOCATION, "Home location not set")
            return
        }

        val homeLocation = Location("").apply {
            latitude = homeLatitude
            longitude = homeLongitude
        }

        val currentDistance = calculateDistance(currentLocation, homeLocation)
        logger.d(LogConfig.TAG_LOCATION, "Distance to home: $currentDistance meters")

        // Check if enough time has passed since last command
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastCommandTime < COMMAND_COOLDOWN) {
            logger.d(LogConfig.TAG_LOCATION, "Command cooldown active, skipping check")
            return
        }

        // Only trigger if we have a previous distance to compare against
        lastKnownDistance?.let { lastDistance ->
            when {
                // Trigger when crossing from outside to inside
                lastDistance >= triggerDistance && currentDistance < triggerDistance -> {
                    logger.d(LogConfig.TAG_LOCATION, "Crossed trigger distance inward, opening garage")
                    CoroutineScope(Dispatchers.IO).launch {
                        MqttManager.getInstance(context).openDoor()
                    }
                    lastCommandTime = currentTime
                }
                // Trigger when crossing from inside to outside
                lastDistance < triggerDistance && currentDistance >= triggerDistance -> {
                    logger.d(LogConfig.TAG_LOCATION, "Crossed trigger distance outward, closing garage")
                    CoroutineScope(Dispatchers.IO).launch {
                        MqttManager.getInstance(context).closeDoor()
                    }
                    lastCommandTime = currentTime
                }
            }
        }

        lastKnownDistance = currentDistance
    }

    private fun calculateDistance(location1: Location, location2: Location): Double {
        val earthRadius = 6371000.0 // in meters

        val lat1 = Math.toRadians(location1.latitude)
        val lon1 = Math.toRadians(location1.longitude)
        val lat2 = Math.toRadians(location2.latitude)
        val lon2 = Math.toRadians(location2.longitude)

        val dLat = lat2 - lat1
        val dLon = lon2 - lon1

        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(lat1) * cos(lat2) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return earthRadius * c
    }

    sealed class LocationState {
        object Active : LocationState()
        object Inactive : LocationState()
        data class Error(val message: String) : LocationState()
    }
    
    private enum class UpdateMode {
        NEAR, FAR, UNKNOWN
    }
}