package de.drachenfels.gcontrl.mqtt

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

private const val PREFS_NAME = "GContrlPrefs"
private const val KEY_MQTT_SERVER = "mqtt_server"
private const val KEY_MQTT_USERNAME = "mqtt_username"
private const val KEY_MQTT_PASSWORD = "mqtt_password"
private const val MQTT_WS_PORT = 8884  // WebSocket TLS port

// MQTT Topics und Commands
private const val TOPIC_STATE = "garage/state"      // ESPHome publishes door state here
private const val TOPIC_COMMAND = "garage/command"  // App publishes commands here

private const val COMMAND_OPEN = "open"
private const val COMMAND_CLOSE = "close"
private const val COMMAND_STOP = "stop"

class MQTTService(private val context: Context) {
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState
    
    private val _doorState = MutableStateFlow<DoorState>(DoorState.UNKNOWN)
    val doorState: StateFlow<DoorState> = _doorState

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun connect() {
        // TODO: Implementiere MQTT Verbindungsaufbau
        // - Konfiguration aus SharedPreferences lesen
        // - HiveMQ Client erstellen und konfigurieren
        // - Verbindung aufbauen
        // - Subscribe auf TOPIC_STATE für Door Status Updates
        // - Status Updates in _doorState publishen
    }

    fun disconnect() {
        // TODO: Implementiere sauberes Trennen der MQTT Verbindung
        // - Unsubscribe von TOPIC_STATE
        // - Client disconnecten
        // - States zurücksetzen
    }

    fun openDoor() {
        // TODO: Publish COMMAND_OPEN an TOPIC_COMMAND
        // ESPHome übernimmt die Logik ob das Kommando ausgeführt werden kann
    }

    fun closeDoor() {
        // TODO: Publish COMMAND_CLOSE an TOPIC_COMMAND
        // ESPHome übernimmt die Logik ob das Kommando ausgeführt werden kann
    }

    fun stopDoor() {
        // TODO: Publish COMMAND_STOP an TOPIC_COMMAND
        // ESPHome übernimmt die Logik ob das Kommando ausgeführt werden kann
    }

    sealed class ConnectionState {
        object Connected : ConnectionState()
        object Disconnected : ConnectionState()
        data class Error(val message: String) : ConnectionState()
    }
}

enum class DoorState {
    OPEN,           // Tor ist vollständig geöffnet
    CLOSED,         // Tor ist vollständig geschlossen
    OPENING,        // Tor fährt gerade hoch
    CLOSING,        // Tor fährt gerade runter
    STOPPED,        // Tor wurde während der Fahrt gestoppt
    UNKNOWN         // Initialzustand oder bei Verbindungsverlust
}