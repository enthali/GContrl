package de.drachenfels.gcontrl.services

import android.content.Context
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client
import de.drachenfels.gcontrl.utils.AndroidLogger
import de.drachenfels.gcontrl.utils.LogConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.UUID
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

private const val PREFS_NAME = "GContrlPrefs"
private const val KEY_MQTT_SERVER = "mqtt_server"
private const val KEY_MQTT_USERNAME = "mqtt_username"
private const val KEY_MQTT_PASSWORD = "mqtt_password"
private const val KEY_CONFIG_VALID = "mqtt_config_valid"
private const val MQTT_PORT = 8883  // Standard MQTT TLS port

// MQTT Topics und Commands
private const val TOPIC_STATE = "garage/state"      // ESPHome publishes door state here
private const val TOPIC_COMMAND = "garage/command"  // App publishes commands here

private const val COMMAND_OPEN = "open"
private const val COMMAND_CLOSE = "close"
private const val COMMAND_STOP = "stop"
private const val COMMAND_REQUEST_STATUS = "request_status"

class MqttManager private constructor (private val context: Context) {
    private val logger = AndroidLogger()
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState

    private val _doorState = MutableStateFlow<DoorState>(DoorState.UNKNOWN)
    val doorState: StateFlow<DoorState> = _doorState

    private var client: Mqtt5AsyncClient? = null

    companion object {
        @Volatile
        private var instance: MqttManager? = null

        fun getInstance(context: Context): MqttManager {
            return instance ?: synchronized(this) {
                instance ?: MqttManager(context.applicationContext).also { instance = it }
            }
        }
    }
    private fun buildMqttClient(server: String): Mqtt5AsyncClient {
        val clientId = UUID.randomUUID().toString()
        logger.d(LogConfig.TAG_MQTT, "Building MQTT client with ID: $clientId")

        return Mqtt5Client.builder()
            .identifier(clientId)
            .serverHost(server)
            .serverPort(MQTT_PORT)
            .sslConfig()
            .applySslConfig()
            .automaticReconnectWithDefaultConfig()
            .addConnectedListener {
                logger.d(LogConfig.TAG_MQTT, "Connected")
                _connectionState.value = ConnectionState.Connected
                subscribeToState()
            }
 /* do not delete
            .addDisconnectedListener { event ->
                logger.d(LogConfig.TAG_MQTT, """Disconnected listener triggered:
Client ID: $clientId
Available properties: ${event::class.java.methods.joinToString { it.name }}""".trimMargin())
*/

            .addDisconnectedListener { event ->
                logger.d(LogConfig.TAG_MQTT, """Disconnected listener triggered:
Client ID: $clientId
Source: ${event.source}
Client Config: ${event.clientConfig}""".trimMargin())
                // We do get an automated disconnect when the Client autoreconnects
                // _connectionState.value = ConnectionState.Disconnected
                // door state should only be set from actual MQTT messages
                // _doorState.value = DoorState.UNKNOWN
            }
            .buildAsync()
    }

    private fun subscribeToState() {
        logger.d(LogConfig.TAG_MQTT, "Subscribing to state topic")
        client?.subscribeWith()
            ?.topicFilter(TOPIC_STATE)
            ?.callback { publish ->
                val message = String(publish.payloadAsBytes)
                logger.d(LogConfig.TAG_MQTT, "Received state: $message")
                try {
                    _doorState.value = DoorState.valueOf(message)
                } catch (e: IllegalArgumentException) {
                    _doorState.value = DoorState.UNKNOWN
                }
            }
            ?.send()
            ?.whenComplete { subAck, throwable ->
                if (throwable != null) {
                    logger.e(LogConfig.TAG_MQTT, "Subscribe failed", throwable)
                    _connectionState.value = ConnectionState.Error(throwable.message ?: "Subscribe failed")
                } else {
                    logger.d(LogConfig.TAG_MQTT, "Subscribe successful")
                    // Status nach erfolgreichem Subscribe anfragen
                    logger.d(LogConfig.TAG_MQTT, "Requesting initial door state")
                    requestStatus()
                }
            }
    }

    suspend fun connect(): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_CONFIG_VALID, false)) {
            logger.d(LogConfig.TAG_MQTT, "MQTT config not valid")
            return false
        }

        val server = prefs.getString(KEY_MQTT_SERVER, "") ?: ""
        val username = prefs.getString(KEY_MQTT_USERNAME, "") ?: ""
        val password = prefs.getString(KEY_MQTT_PASSWORD, "") ?: ""

        if (server.isEmpty()) {
            logger.d(LogConfig.TAG_MQTT, "MQTT server not configured")
            return false
        }

        return connectWithCredentials(server, username, password)
    }

    private suspend fun connectWithCredentials(
        server: String,
        username: String,
        password: String
    ): Boolean = suspendCancellableCoroutine { continuation ->
        try {
            // Clean up any existing client
            client?.disconnect()
            client = buildMqttClient(server)

            _connectionState.value = ConnectionState.Disconnected

            client?.connectWith()
                ?.keepAlive(30)
                ?.simpleAuth()
                ?.username(username)
                ?.password(password.toByteArray())
                ?.applySimpleAuth()
                ?.send()
                ?.whenComplete { _, throwable ->
                    if (throwable != null) {
                        logger.e(LogConfig.TAG_MQTT, "Connection failed", throwable)
                        _connectionState.value = ConnectionState.Error(throwable.message ?: "Connection failed")
                        if (!continuation.isCompleted) continuation.resume(false)
                    } else {
                        if (!continuation.isCompleted) continuation.resume(true)
                    }
                }

            continuation.invokeOnCancellation {
                logger.d(LogConfig.TAG_MQTT, "Connection cancelled")
                disconnect()
            }
        } catch (e: Exception) {
            logger.e(LogConfig.TAG_MQTT, "Exception during connect", e)
            _connectionState.value = ConnectionState.Error(e.message ?: "Unknown error during connect")
            if (!continuation.isCompleted) continuation.resumeWithException(e)
        }
    }

    fun disconnect() {
        try {
            logger.d(LogConfig.TAG_MQTT, "Starting disconnect")
            client?.disconnect()
                ?.whenComplete { _, throwable ->
                    if (throwable != null) {
                        logger.e(LogConfig.TAG_MQTT, "Disconnect failed", throwable)
                        _connectionState.value = ConnectionState.Error(throwable.message ?: "Disconnect failed")
                    } else {
                        logger.d(LogConfig.TAG_MQTT, "Disconnect successful")
                        client = null
                        _connectionState.value = ConnectionState.Disconnected
                        _doorState.value = DoorState.UNKNOWN
                    }
                }
        } catch (e: Exception) {
            logger.e(LogConfig.TAG_MQTT, "Exception during disconnect", e)
            _connectionState.value = ConnectionState.Error(e.message ?: "Unknown error during disconnect")
        }
    }

    fun requestStatus() {
        logger.d(LogConfig.TAG_MQTT, "Requesting door status")
        client?.publishWith()
            ?.topic(TOPIC_COMMAND)
            ?.payload(COMMAND_REQUEST_STATUS.toByteArray())
            ?.send()
            ?.whenComplete { pubAck, throwable ->
                if (throwable != null) {
                    logger.e(LogConfig.TAG_MQTT, "Failed to request status", throwable)
                    _connectionState.value = ConnectionState.Error(throwable.message ?: "Status request failed")
                } else {
                    logger.d(LogConfig.TAG_MQTT, "Status request sent successfully")
                }
            }
    }

    fun openDoor() {
        CoroutineScope(Dispatchers.IO).launch {
            logger.d(LogConfig.TAG_MQTT, "Publishing open command")
            client?.publishWith()
                ?.topic(TOPIC_COMMAND)
                ?.payload(COMMAND_OPEN.toByteArray())
                ?.send()
                ?.whenComplete { pubAck, throwable ->
                    if (throwable != null) {
                        logger.e(LogConfig.TAG_MQTT, "Failed to publish open command", throwable)
                        _connectionState.value =
                            ConnectionState.Error(throwable.message ?: "Publish failed")
                    } else {
                        logger.d(LogConfig.TAG_MQTT, "Open command published successfully")
                    }
                }
        }
    }

    fun closeDoor() {
        CoroutineScope(Dispatchers.IO).launch {
            logger.d(LogConfig.TAG_MQTT, "Publishing close command")
            client?.publishWith()
                ?.topic(TOPIC_COMMAND)
                ?.payload(COMMAND_CLOSE.toByteArray())
                ?.send()
                ?.whenComplete { pubAck, throwable ->
                    if (throwable != null) {
                        logger.e(LogConfig.TAG_MQTT, "Failed to publish close command", throwable)
                        _connectionState.value = ConnectionState.Error(throwable.message ?: "Publish failed")
                    } else {
                        logger.d(LogConfig.TAG_MQTT, "Close command published successfully")
                    }
                }
        }
    }

    fun stopDoor() {
        CoroutineScope(Dispatchers.IO).launch {
            logger.d(LogConfig.TAG_MQTT, "Publishing stop command")
            client?.publishWith()
                ?.topic(TOPIC_COMMAND)
                ?.payload(COMMAND_STOP.toByteArray())
                ?.send()
                ?.whenComplete { pubAck, throwable ->
                    if (throwable != null) {
                        logger.e(LogConfig.TAG_MQTT, "Failed to publish stop command", throwable)
                        _connectionState.value =
                            ConnectionState.Error(throwable.message ?: "Publish failed")
                    } else {
                        logger.d(LogConfig.TAG_MQTT, "Stop command published successfully")
                    }
                }
        }
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