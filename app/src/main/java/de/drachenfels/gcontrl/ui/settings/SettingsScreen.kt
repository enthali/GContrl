package de.drachenfels.gcontrl.ui.settings

import android.content.Context
import android.content.ContextWrapper
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.drachenfels.gcontrl.LocationAutomationSettings
import de.drachenfels.gcontrl.services.LocationAutomationManager
import de.drachenfels.gcontrl.services.MqttManager
import de.drachenfels.gcontrl.ui.settings.components.*
import de.drachenfels.gcontrl.ui.theme.GContrlTheme
import de.drachenfels.gcontrl.utils.AndroidLogger
import de.drachenfels.gcontrl.utils.LogConfig
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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
private const val ENABLE_LOCATION_FEATURES = true

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    mqttManager: MqttManager,
    onNavigateBack: () -> Unit,
    updateLocationAutomationSettings: (LocationAutomationSettings) -> Unit,
    locationAutomationSettings: StateFlow<LocationAutomationSettings>,
    locationManager: LocationAutomationManager,
    modifier: Modifier = Modifier
){
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

    // Collect location updates
    var currentLocation by remember { mutableStateOf<Pair<Double, Double>?>(null) }

    LaunchedEffect(key1 = locationManager) {
        logger.d(LogConfig.TAG_SETTINGS, "Starting location collection")
        locationManager.locationData.collect { locationData ->
            locationData?.let {
                logger.d(LogConfig.TAG_SETTINGS, "Collected location: ${it.latitude}, ${it.longitude}")
                currentLocation = Pair(it.latitude, it.longitude)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                        mqttManager.disconnect()
                        delay (200)
                        isTestingConnection = true

                        val connectionJob = launch {
                            try {
                                // Zuerst Einstellungen speichern
                                prefs.edit()
                                    .putString(KEY_MQTT_SERVER, mqttServer)
                                    .putString(KEY_MQTT_USERNAME, mqttUser)
                                    .putString(KEY_MQTT_PASSWORD, mqttPassword)
                                    .putBoolean(KEY_CONFIG_VALID, true)
                                    .apply()

                                // Verbindung testen
                                val connected = mqttManager.connect()
                                if (connected) {
                                    Toast.makeText(context, "Connection successful", Toast.LENGTH_SHORT).show()
                                    onNavigateBack()
                                } else {
                                    // if the test failed set to false.
                                    Toast.makeText(context, "Connection failed - could not establish connection", Toast.LENGTH_LONG).show()
                                    prefs.edit().putBoolean(KEY_CONFIG_VALID, false).apply()
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
                        currentLocation?.let { location ->
                            garageLocation = location
                            prefs.edit()
                                .putFloat(KEY_GARAGE_LAT, location.first.toFloat())
                                .putFloat(KEY_GARAGE_LON, location.second.toFloat())
                                .apply()
                            Toast.makeText(context, "Location set to current location", Toast.LENGTH_SHORT).show()
                        } ?: run {
                            Toast.makeText(context, "Current location not available", Toast.LENGTH_SHORT).show()
                        }
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

// Preview Context Wrapper
class SettingsPreviewContextWrapper(
    base: Context,
    val locationAutomationSettingsFlow: StateFlow<LocationAutomationSettings>
) : ContextWrapper(base)

@Preview(
    name = "Settings Screen",
    showBackground = true,
    showSystemUi = true,
    device = "spec:width=411dp,height=891dp,dpi=420,isRound=false,chinSize=0dp,orientation=portrait"
)
@Composable
fun SettingsScreenPreview() {
    val context = LocalContext.current
    GContrlTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            SettingsScreen(
                mqttManager = MqttManager.getInstance(context),
                onNavigateBack = { },
                updateLocationAutomationSettings = { },
                locationAutomationSettings = remember {
                    MutableStateFlow(LocationAutomationSettings())
                }.asStateFlow(),
                locationManager = LocationAutomationManager.getInstance()
            )
        }
    }
}