package de.drachenfels.gcontrl.mqtt

import android.content.Context
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client
import de.drachenfels.gcontrl.utils.AndroidLogger
import de.drachenfels.gcontrl.utils.LogConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

private const val PREFS_NAME = "GContrlPrefs"
private const val KEY_MQTT_SERVER = "mqtt_server"
private const val KEY_MQTT_USERNAME = "mqtt_username"
private const val KEY_MQTT_PASSWORD = "mqtt_password"
private const val MQTT_PORT = 8883  // Standard MQTT TLS port

// MQTT Topics und Commands
private const val TOPIC_STATE = "garage/state"      // ESPHome publishes door state here
private const val TOPIC_COMMAND = "garage/command"  // App publishes commands here

private const val COMMAND_OPEN = "open"
private const val COMMAND_CLOSE = "close"
private const val COMMAND_STOP = "stop"
private const val COMMAND_REQUEST_STATUS = "request_status"

// TODO: Convert to Android Foreground Service to maintain connection and support future geofencing features
class MQTTService(private val context: Context) {
    private val logger = AndroidLogger()
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState

    private val _doorState = MutableStateFlow<DoorState>(DoorState.UNKNOWN)
    val doorState: StateFlow<DoorState> = _doorState

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private var client: Mqtt5AsyncClient? = null

    private fun buildMqttClient(): Mqtt5AsyncClient {
        logger.d(LogConfig.TAG_MQTT, "Building MQTT client")
        val server = prefs.getString(KEY_MQTT_SERVER, "GaragePilot.com") ?: "GaragePilot.com"
        logger.d(LogConfig.TAG_MQTT, "Server: $server")

        return Mqtt5Client.builder()
            .identifier(UUID.randomUUID().toString())
            .serverHost(server)
            .serverPort(MQTT_PORT)
            .sslConfig()
            .applySslConfig()
            .automaticReconnectWithDefaultConfig()
            .addConnectedListener {
                logger.d(LogConfig.TAG_MQTT, "Connected listener triggered")
                _connectionState.value = ConnectionState.Connected
                subscribeToState()
            }
            .addDisconnectedListener {
                logger.d(LogConfig.TAG_MQTT, "Disconnected listener triggered")
                _connectionState.value = ConnectionState.Disconnected
                _doorState.value = DoorState.UNKNOWN
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
                }
            }
    }

    suspend fun connect(): Boolean = suspendCoroutine { continuation ->
        try {
            logger.d(LogConfig.TAG_MQTT, "Starting connect sequence")

            // Force new client creation
            if (client != null) {
                logger.d(LogConfig.TAG_MQTT, "Disposing old client")
                try {
                    client?.disconnect()
                } catch (e: Exception) {
                    logger.e(LogConfig.TAG_MQTT, "Error during old client disconnect", e)
                }
                client = null
            }

            // Create new client
            client = buildMqttClient()
            logger.d(LogConfig.TAG_MQTT, "Created new MQTT client")

            val username = prefs.getString(KEY_MQTT_USERNAME, "") ?: ""
            val password = prefs.getString(KEY_MQTT_PASSWORD, "") ?: ""

            _connectionState.value = ConnectionState.Disconnected

            client?.connectWith()
                ?.simpleAuth()
                ?.username(username)
                ?.password(password.toByteArray())
                ?.applySimpleAuth()
                ?.send()
                ?.whenComplete { connAck, throwable ->
                    if (throwable != null) {
                        logger.e(LogConfig.TAG_MQTT, "Connection failed", throwable)
                        _connectionState.value = ConnectionState.Error(throwable.message ?: "Connection failed")
                        continuation.resume(false)
                    } else {
                        logger.d(LogConfig.TAG_MQTT, "Connect acknowledged successfully")
                        // Wait for Connected status from ConnectedListener
                        object : Thread() {
                            override fun run() {
                                var attempts = 0
                                while (attempts < 50) {
                                    if (_connectionState.value is ConnectionState.Connected) {
                                        logger.d(LogConfig.TAG_MQTT, "Full connection established")
                                        continuation.resume(true)
                                        return
                                    }
                                    sleep(100)
                                    attempts++
                                }
                                logger.d(LogConfig.TAG_MQTT, "Connection timeout - state: ${_connectionState.value}")
                                continuation.resume(false)
                            }
                        }.start()
                    }
                }
        } catch (e: Exception) {
            logger.e(LogConfig.TAG_MQTT, "Exception during connect", e)
            _connectionState.value = ConnectionState.Error(e.message ?: "Unknown error during connect")
            continuation.resumeWithException(e)
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
        logger.d(LogConfig.TAG_MQTT, "Publishing open command")
        client?.publishWith()
            ?.topic(TOPIC_COMMAND)
            ?.payload(COMMAND_OPEN.toByteArray())
            ?.send()
            ?.whenComplete { pubAck, throwable ->
                if (throwable != null) {
                    logger.e(LogConfig.TAG_MQTT, "Failed to publish open command", throwable)
                    _connectionState.value = ConnectionState.Error(throwable.message ?: "Publish failed")
                } else {
                    logger.d(LogConfig.TAG_MQTT, "Open command published successfully")
                }
            }
    }

    fun closeDoor() {
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

    fun stopDoor() {
        logger.d(LogConfig.TAG_MQTT, "Publishing stop command")
        client?.publishWith()
            ?.topic(TOPIC_COMMAND)
            ?.payload(COMMAND_STOP.toByteArray())
            ?.send()
            ?.whenComplete { pubAck, throwable ->
                if (throwable != null) {
                    logger.e(LogConfig.TAG_MQTT, "Failed to publish stop command", throwable)
                    _connectionState.value = ConnectionState.Error(throwable.message ?: "Publish failed")
                } else {
                    logger.d(LogConfig.TAG_MQTT, "Stop command published successfully")
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