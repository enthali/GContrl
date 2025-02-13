package de.drachenfels.gcontrl

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import de.drachenfels.gcontrl.services.GaragePilotService
import de.drachenfels.gcontrl.services.LocationDataRepository
import de.drachenfels.gcontrl.services.MQTTService
import de.drachenfels.gcontrl.ui.mainscreen.MainScreen
import de.drachenfels.gcontrl.ui.settings.SettingsScreen
import de.drachenfels.gcontrl.ui.theme.GContrlTheme
import de.drachenfels.gcontrl.utils.AndroidLogger
import de.drachenfels.gcontrl.utils.LogConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class LocationAutomationSettings(
    val isLocationAutomationEnabled: Boolean = false,
    val triggerDistance: Int = 100
)

// Settings keys
private const val PREFS_NAME = "GContrlPrefs"
private const val KEY_LOCATION_AUTOMATION_ENABLED = "location_automation_enabled"
private const val KEY_TRIGGER_DISTANCE = "trigger_distance"

class MainActivity : ComponentActivity() {
    private val logger = AndroidLogger()
    private lateinit var prefs: SharedPreferences
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var requestNotificationPermissionLauncher: ActivityResultLauncher<String>

    private val _locationAutomationSettings = MutableStateFlow(LocationAutomationSettings())
    val locationAutomationSettings: StateFlow<LocationAutomationSettings> = _locationAutomationSettings.asStateFlow()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        logger.d(LogConfig.TAG_MAIN, "onCreate - Initializing app")
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        _locationAutomationSettings.value = LocationAutomationSettings(
            isLocationAutomationEnabled = prefs.getBoolean(KEY_LOCATION_AUTOMATION_ENABLED, false),
            triggerDistance = prefs.getInt(KEY_TRIGGER_DISTANCE, 100)
        )

        requestNotificationPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                logger.d(LogConfig.TAG_MAIN, "Notification permission granted")
                startGaragePilotService()
            } else {
                logger.d(LogConfig.TAG_MAIN, "Notification permission denied")
            }
        }

        requestPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
                val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
                val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

                if (fineLocationGranted && coarseLocationGranted) {
                    logger.d(LogConfig.TAG_LOCATION, "Location permissions granted by user")
                    startLocationAutomationService()
                } else {
                    logger.d(LogConfig.TAG_LOCATION, "Location permissions denied by user")
                }
            }

        requestLocationPermissions()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermission()
        } else {
            startGaragePilotService()
        }

        enableEdgeToEdge()
        setContent {
            GContrlApp(::updateLocationAutomationSettings, locationAutomationSettings, LocationDataRepository)
        }
        logger.d(LogConfig.TAG_MAIN, "onCreate - App initialized")

        startLocationAutomationService()
    }

    private fun requestNotificationPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED -> {
                logger.d(LogConfig.TAG_MAIN, "Notification permission already granted")
                startGaragePilotService()
            }
            else -> {
                logger.d(LogConfig.TAG_MAIN, "Requesting notification permission")
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    fun updateLocationAutomationSettings(newSettings: LocationAutomationSettings) {
        _locationAutomationSettings.value = newSettings
        with(prefs.edit()) {
            putBoolean(KEY_LOCATION_AUTOMATION_ENABLED, newSettings.isLocationAutomationEnabled)
            putInt(KEY_TRIGGER_DISTANCE, newSettings.triggerDistance)
            apply()
        }
    }

    override fun onPause() {
        super.onPause()
        logger.d(LogConfig.TAG_MAIN, "onPause - App going to background")
    }

    override fun onResume() {
        super.onResume()
        logger.d(LogConfig.TAG_MAIN, "onResume - App coming to foreground")
        if (!GaragePilotService.isRunning()) {
            startGaragePilotService()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        logger.d(LogConfig.TAG_MAIN, "onDestroy - Cleaning up")
        stopLocationAutomationService()
    }

    private fun requestLocationPermissions() {
        val fineLocationPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        val coarseLocationPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        if (fineLocationPermission != PackageManager.PERMISSION_GRANTED ||
            coarseLocationPermission != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else {
            logger.d(LogConfig.TAG_LOCATION, "Location permissions already granted")
        }
    }

    private fun startLocationAutomationService() {
        logger.d(LogConfig.TAG_MAIN, "Starting LocationAutomationService")
        val serviceIntent = Intent(this, LocationAutomationService::class.java)
        startService(serviceIntent)
    }

    private fun stopLocationAutomationService() {
        logger.d(LogConfig.TAG_MAIN, "Stopping LocationAutomationService")
        val serviceIntent = Intent(this, LocationAutomationService::class.java)
        stopService(serviceIntent)
    }

    private fun startGaragePilotService() {
        logger.d(LogConfig.TAG_MAIN, "Starting GaragePilotService")
        val serviceIntent = Intent(this, GaragePilotService::class.java)
        startForegroundService(serviceIntent)
    }

    private fun stopGaragePilotService() {
        logger.d(LogConfig.TAG_MAIN, "Stopping GaragePilotService")
        val serviceIntent = Intent(this, GaragePilotService::class.java)
        stopService(serviceIntent)
    }
}

@Composable
fun GContrlApp(
    updateLocationAutomationSettings: (LocationAutomationSettings) -> Unit,
    locationAutomationSettings: StateFlow<LocationAutomationSettings>,
    locationDataRepository: LocationDataRepository
){
    val logger = AndroidLogger()
    var showSettings by remember { mutableStateOf(false) }
    val mqttService = MQTTService.getInstance()
    val context = LocalContext.current

    // Collect location data for speed check
    val locationData by locationDataRepository.locationUpdates.collectAsState()

    // Effect to handle speed-based navigation
    if (locationData?.speed ?: 0f > 3f && showSettings) {
        logger.d(LogConfig.TAG_MAIN, "Speed exceeds 3 km/h, returning to main screen")
        showSettings = false
    }

    GContrlTheme {
        if (showSettings) {
            SettingsScreen(
                mqttService = mqttService,
                onNavigateBack = {
                    logger.d(LogConfig.TAG_MAIN, "User requested navigation: Settings -> Main")
                    showSettings = false
                },
                updateLocationAutomationSettings = updateLocationAutomationSettings,
                locationAutomationSettings = locationAutomationSettings,
                locationDataRepository = locationDataRepository
            )
        } else {
            MainScreen(
                mqttService = mqttService,
                onNavigateToSettings = {
                    val speed = locationData?.speed ?: 0f
                    if (speed <= 3f) {
                        logger.d(LogConfig.TAG_MAIN, "User requested navigation: Main -> Settings")
                        showSettings = true
                    }
                },
                locationAutomationSettingsFlow = locationAutomationSettings
            )
        }
    }
}