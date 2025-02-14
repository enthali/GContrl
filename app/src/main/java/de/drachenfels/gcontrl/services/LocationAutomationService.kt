package de.drachenfels.gcontrl

import android.Manifest
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.os.IBinder
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import de.drachenfels.gcontrl.services.LocationData
import de.drachenfels.gcontrl.services.LocationDataRepository
import de.drachenfels.gcontrl.services.MqttManager
import de.drachenfels.gcontrl.utils.AndroidLogger
import de.drachenfels.gcontrl.utils.LogConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class LocationAutomationService : Service() {
    private val logger = AndroidLogger()
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private lateinit var prefs: SharedPreferences
    private val locationDataRepository = LocationDataRepository

    // Settings keys
    private  val PREFS_NAME = "GContrlPrefs"
    private  val KEY_LOCATION_AUTOMATION_ENABLED = "location_automation_enabled"
    private  val KEY_TRIGGER_DISTANCE = "trigger_distance"
    private  val KEY_HOME_LATITUDE = "garage_latitude"
    private  val KEY_HOME_LONGITUDE = "garage_longitude"

    // Add new properties for state tracking
    private var lastKnownDistance: Double? = null
    private var lastCommandTime: Long = 0
    private val COMMAND_COOLDOWN = 60000L // 1 minute cooldown between commands

    override fun onCreate() {
        super.onCreate()
        logger.d(LogConfig.TAG_LOCATION, "LocationAutomationService created")
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createLocationRequest()
        createLocationCallback()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        logger.d(LogConfig.TAG_LOCATION, "LocationAutomationService started")
        startLocationUpdates()
        return START_STICKY
    }

    private fun createLocationRequest() {
        locationRequest = LocationRequest.Builder(2000L)
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
            .build()
    }

    // TODO: Implmenent preferences manager possibly using a stateflow to distribute changes to the settings...
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

                    // Emit location update to SharedFlow with distance
                    CoroutineScope(Dispatchers.IO).launch {
                        locationDataRepository.emitLocation(
                            LocationData(
                                latitude = location.latitude,
                                longitude = location.longitude,
                                speed = location.speed,
                                distanceToGarage = distance
                            )
                        )
                    }
                    checkLocation(location)
                }
            }
        }
    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            null
        )
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
                        MqttManager.getInstance().openDoor()
                    }
                    lastCommandTime = currentTime
                }
                // Trigger when crossing from inside to outside
                lastDistance < triggerDistance && currentDistance >= triggerDistance -> {
                    logger.d(LogConfig.TAG_LOCATION, "Crossed trigger distance outward, closing garage")
                    CoroutineScope(Dispatchers.IO).launch {
                        MqttManager.getInstance().closeDoor()
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

    override fun onBind(intent: Intent): IBinder? {
        return null
    }
}