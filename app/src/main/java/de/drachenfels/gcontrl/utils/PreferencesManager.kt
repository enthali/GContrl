package de.drachenfels.gcontrl.utils

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PreferencesManager private constructor(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val logger = AndroidLogger()
    
    // MQTT Settings
    private val _mqttSettings = MutableStateFlow(MqttSettings())
    val mqttSettings: StateFlow<MqttSettings> = _mqttSettings.asStateFlow()
    
    // Location Settings
    private val _locationSettings = MutableStateFlow(LocationSettings())
    val locationSettings: StateFlow<LocationSettings> = _locationSettings.asStateFlow()
    
    init {
        loadSettings()
        logger.d(LogConfig.TAG_SETTINGS, "PreferencesManager initialized")
    }
    
    private fun loadSettings() {
        _mqttSettings.value = MqttSettings(
            server = prefs.getString(KEY_MQTT_SERVER, "") ?: "",
            username = prefs.getString(KEY_MQTT_USERNAME, "") ?: "",
            password = prefs.getString(KEY_MQTT_PASSWORD, "") ?: "",
            isConfigValid = prefs.getBoolean(KEY_CONFIG_VALID, false)
        )
        
        _locationSettings.value = LocationSettings(
            isEnabled = prefs.getBoolean(KEY_LOCATION_AUTOMATION_ENABLED, false),
            garageLat = prefs.getFloat(KEY_GARAGE_LAT, 0f).toDouble(),
            garageLon = prefs.getFloat(KEY_GARAGE_LON, 0f).toDouble(),
            triggerDistance = prefs.getInt(KEY_TRIGGER_DISTANCE, 100)
        )
        
        logger.d(LogConfig.TAG_SETTINGS, "Settings loaded from SharedPreferences")
    }
    
    fun updateMqttSettings(settings: MqttSettings) {
        prefs.edit().apply {
            putString(KEY_MQTT_SERVER, settings.server)
            putString(KEY_MQTT_USERNAME, settings.username)
            putString(KEY_MQTT_PASSWORD, settings.password)
            putBoolean(KEY_CONFIG_VALID, settings.isConfigValid)
            apply()
        }
        _mqttSettings.value = settings
        logger.d(LogConfig.TAG_SETTINGS, "MQTT settings updated")
    }
    
    fun updateLocationSettings(settings: LocationSettings) {
        prefs.edit().apply {
            putBoolean(KEY_LOCATION_AUTOMATION_ENABLED, settings.isEnabled)
            putFloat(KEY_GARAGE_LAT, settings.garageLat.toFloat())
            putFloat(KEY_GARAGE_LON, settings.garageLon.toFloat())
            putInt(KEY_TRIGGER_DISTANCE, settings.triggerDistance)
            apply()
        }
        _locationSettings.value = settings
        logger.d(LogConfig.TAG_SETTINGS, "Location settings updated")
    }
    
    companion object {
        private const val PREFS_NAME = "GContrlPrefs"
        
        // MQTT Keys
        private const val KEY_MQTT_SERVER = "mqtt_server"
        private const val KEY_MQTT_USERNAME = "mqtt_username"
        private const val KEY_MQTT_PASSWORD = "mqtt_password"
        private const val KEY_CONFIG_VALID = "mqtt_config_valid"
        
        // Location Keys
        private const val KEY_LOCATION_AUTOMATION_ENABLED = "location_automation_enabled"
        private const val KEY_GARAGE_LAT = "garage_latitude"
        private const val KEY_GARAGE_LON = "garage_longitude"
        private const val KEY_TRIGGER_DISTANCE = "trigger_distance"
        
        @Volatile
        private var instance: PreferencesManager? = null
        
        fun getInstance(context: Context): PreferencesManager {
            return instance ?: synchronized(this) {
                instance ?: PreferencesManager(context).also { instance = it }
            }
        }
    }
}

data class MqttSettings(
    val server: String = "",
    val username: String = "",
    val password: String = "",
    val isConfigValid: Boolean = false
)

data class LocationSettings(
    val isEnabled: Boolean = false,
    val garageLat: Double = 0.0,
    val garageLon: Double = 0.0,
    val triggerDistance: Int = 100
)