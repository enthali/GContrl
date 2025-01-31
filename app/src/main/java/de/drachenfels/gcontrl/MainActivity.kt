package de.drachenfels.gcontrl

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import de.drachenfels.gcontrl.mqtt.MQTTService
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
    private lateinit var mqttService: MQTTService
    private lateinit var prefs: SharedPreferences

    private val _locationAutomationSettings = MutableStateFlow(LocationAutomationSettings())
    val locationAutomationSettings: StateFlow<LocationAutomationSettings> = _locationAutomationSettings.asStateFlow()

    fun updateLocationAutomationSettings(newSettings: LocationAutomationSettings) {
        _locationAutomationSettings.value = newSettings
        with(prefs.edit()) {
            putBoolean(KEY_LOCATION_AUTOMATION_ENABLED, newSettings.isLocationAutomationEnabled)
            putInt(KEY_TRIGGER_DISTANCE, newSettings.triggerDistance)
            apply()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        logger.d(LogConfig.TAG_MAIN, "onCreate - Initializing app")
        mqttService = MQTTService(this)
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        _locationAutomationSettings.value = LocationAutomationSettings(
            isLocationAutomationEnabled = prefs.getBoolean(KEY_LOCATION_AUTOMATION_ENABLED, false),
            triggerDistance = prefs.getInt(KEY_TRIGGER_DISTANCE, 100)
        )

        enableEdgeToEdge()
        setContent {
            GContrlApp(mqttService, ::updateLocationAutomationSettings, locationAutomationSettings)
        }
        logger.d(LogConfig.TAG_MAIN, "onCreate - App initialized")
    }

    override fun onPause() {
        super.onPause()
        logger.d(LogConfig.TAG_MAIN, "onPause - App going to background")
    }

    override fun onResume() {
        super.onResume()
        logger.d(LogConfig.TAG_MAIN, "onResume - App coming to foreground")
    }

    override fun onDestroy() {
        super.onDestroy()
        logger.d(LogConfig.TAG_MAIN, "onDestroy - Cleaning up")
        mqttService.disconnect()
        logger.d(LogConfig.TAG_MAIN, "onDestroy - MQTT connection closed")
    }
}

@Composable
fun GContrlApp(
    mqttService: MQTTService,
    updateLocationAutomationSettings: (LocationAutomationSettings) -> Unit,
    locationAutomationSettings: StateFlow<LocationAutomationSettings>
) {
    val logger = AndroidLogger()
    var showSettings by remember { mutableStateOf(false) }

    // Effekt für Screen-Wechsel
    LaunchedEffect(showSettings) {
        if (showSettings) {
            logger.d(LogConfig.TAG_MAIN, "Screen transition: Main -> Settings, disconnecting MQTT")
            mqttService.disconnect()
        } else {
            logger.d(LogConfig.TAG_MAIN, "Screen transition: Settings -> Main, connecting MQTT")
            mqttService.connect()
        }
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
                locationAutomationSettings = locationAutomationSettings
            )
        } else {
            MainScreen(
                mqttService = mqttService,
                onNavigateToSettings = {
                    logger.d(LogConfig.TAG_MAIN, "User requested navigation: Main -> Settings")
                    showSettings = true
                },
                locationAutomationSettingsFlow = locationAutomationSettings
            )
        }
    }
}