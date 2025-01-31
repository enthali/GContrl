package de.drachenfels.gcontrl.ui.settings

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import de.drachenfels.gcontrl.LocationAutomationSettings
import de.drachenfels.gcontrl.mqtt.MQTTService
import de.drachenfels.gcontrl.ui.settings.components.*
import de.drachenfels.gcontrl.utils.AndroidLogger
import de.drachenfels.gcontrl.utils.LogConfig
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.StateFlow
import java.lang.Thread.sleep

// TODO: consider moveing to dataStore
// Settings keys
private const val PREFS_NAME = "GContrlPrefs"

// MQTT configuration
private const val MQTT_TIMEOUT = 5000L  // 5 seconds timeout
private const val KEY_MQTT_SERVER = "mqtt_server"
private const val KEY_MQTT_USERNAME = "mqtt_username"
private const val KEY_MQTT_PASSWORD = "mqtt_password"
private const val KEY_CONFIG_VALID = "mqtt_config_valid"

// Location automation configuration
private const val KEY_LOCATION_AUTOMATION_ENABLED = "location_automation_enabled"
private const val KEY_GARAGE_LAT = "garage_latitude"
private const val KEY_GARAGE_LON = "garage_longitude"
private const val KEY_TRIGGER_DISTANCE = "trigger_distance"

private val logger = AndroidLogger()

// feature to be implemented in future release
private const val ENABLE_LOCATION_FEATURES = true  // Will be enabled in future release

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    mqttService: MQTTService,
    onNavigateBack: () -> Unit,
    updateLocationAutomationSettings: (LocationAutomationSettings) -> Unit,
    locationAutomationSettings: StateFlow<LocationAutomationSettings>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // MQTT state
    var mqttServer by remember { mutableStateOf(prefs.getString(KEY_MQTT_SERVER, "") ?: "") }
    var mqttUser by remember { mutableStateOf(prefs.getString(KEY_MQTT_USERNAME, "") ?: "") }
    var mqttPassword by remember { mutableStateOf(prefs.getString(KEY_MQTT_PASSWORD, "") ?: "") }
    var isTestingConnection by remember { mutableStateOf(false) }

    val currentSettings by locationAutomationSettings.collectAsState()

    // Location automation state
    var locationAutomationEnabled by remember {
        mutableStateOf(currentSettings.isLocationAutomationEnabled)
    }

    var garageLocation by remember {
        mutableStateOf(
            if (prefs.contains(KEY_GARAGE_LAT) && prefs.contains(KEY_GARAGE_LON)) {
                Pair(
                    prefs.getFloat(KEY_GARAGE_LAT, 0f).toDouble(),
                    prefs.getFloat(KEY_GARAGE_LON, 0f).toDouble()
                )
            } else null
        )
    }
    var triggerDistance by remember {
        mutableStateOf(currentSettings.triggerDistance)
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // MQTT Configuration Section
            MqttConfigurationSection(
                mqttServer = mqttServer,
                onMqttServerChange = { mqttServer = it },
                mqttUser = mqttUser,
                onMqttUserChange = { mqttUser = it },
                mqttPassword = mqttPassword,
                onMqttPasswordChange = { mqttPassword = it },
                isTestingConnection = isTestingConnection,
                onSaveAndTest = {
                    scope.launch {
                        mqttService.disconnect()
                        sleep(500)
                        isTestingConnection = true

                        val connectionJob = launch {
                            try {
                                prefs.edit()
                                    .putString(KEY_MQTT_SERVER, mqttServer)
                                    .putString(KEY_MQTT_USERNAME, mqttUser)
                                    .putString(KEY_MQTT_PASSWORD, mqttPassword)
                                    .putBoolean(KEY_CONFIG_VALID, false)
                                    .apply()

                                val connected = mqttService.connect()
                                if (connected) {
                                    prefs.edit().putBoolean(KEY_CONFIG_VALID, true).apply()
                                    Toast.makeText(context, "Connection successful", Toast.LENGTH_SHORT).show()
                                    onNavigateBack()
                                } else {
                                    Toast.makeText(context, "Connection failed - could not establish connection", Toast.LENGTH_LONG).show()
                                }
                            } catch (e: Exception) {
                                logger.e(LogConfig.TAG_SETTINGS, "Connection test failed", e)
                                Toast.makeText(context, "Connection failed: ${e.message}", Toast.LENGTH_LONG).show()
                            } finally {
                                isTestingConnection = false
                            }
                        }

                        delay(MQTT_TIMEOUT)
                        if (connectionJob.isActive) {
                            connectionJob.cancel()
                            withContext(Dispatchers.Main) {
                                isTestingConnection = false
                                Toast.makeText(context, "Connection timeout - please try again", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // feature to be implemented in future release
            if (ENABLE_LOCATION_FEATURES) {
                // Location Automation Section
                LocationAutomationSection(
                    isEnabled = locationAutomationEnabled,
                    onEnabledChange = { enabled ->
                        locationAutomationEnabled = enabled
                        updateLocationAutomationSettings(
                            currentSettings.copy(isLocationAutomationEnabled = enabled)
                        )
                    },
                    location = garageLocation,
                    onSetCurrentLocation = {
                        // For testing: Set to Munich coordinates
                        val munichLocation = Pair(48.1351, 11.5820)
                        garageLocation = munichLocation
                        prefs.edit()
                            .putFloat(KEY_GARAGE_LAT, munichLocation.first.toFloat())
                            .putFloat(KEY_GARAGE_LON, munichLocation.second.toFloat())
                            .apply()
                        Toast.makeText(context, "Location set to Munich", Toast.LENGTH_SHORT).show()
                    },
                    triggerDistance = triggerDistance,
                    onTriggerDistanceChange = { distance ->
                        triggerDistance = distance
                        updateLocationAutomationSettings(
                            currentSettings.copy(triggerDistance = distance)
                        )
                    }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
            VersionInfoSection()
        }
    }
}
