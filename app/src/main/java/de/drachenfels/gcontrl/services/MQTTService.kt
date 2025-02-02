package de.drachenfels.gcontrl.services

import android.content.Context
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client
import de.drachenfels.gcontrl.utils.AndroidLogger
import de.drachenfels.gcontrl.utils.LogConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.coroutines.Continuation
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

class MQTTService private constructor () {
    private val logger = AndroidLogger()
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState

    private val _doorState = MutableStateFlow<DoorState>(DoorState.UNKNOWN)
    val doorState: StateFlow<DoorState> = _doorState

    private var client: Mqtt5AsyncClient? = null
    private var connectContinuation: Continuation<Boolean>? = null

    //TODO: investigate sporadic disconnects
    companion object {
        @Volatile
        private var instance: MQTTService? = null

        fun getInstance(): MQTTService {
            return instance ?: synchronized(this) {
                instance ?: MQTTService().also { instance = it }
            }
        }
    }

    private fun buildMqttClient(server: String): Mqtt5AsyncClient {
        val clientId = UUID.randomUUID().toString()
        logger.d(LogConfig.TAG_MQTT, "Building MQTT client with ID: $clientId")
        logger.d(LogConfig.TAG_MQTT, "Server: $server")

        return Mqtt5Client.builder()
            .identifier(clientId)
            .serverHost(server)
            .serverPort(MQTT_PORT)
            .sslConfig()
            .applySslConfig()
            .automaticReconnectWithDefaultConfig()
            .addConnectedListener {
                logger.d(LogConfig.TAG_MQTT, """Connected listener triggered for client:
Client ID: $clientId""".trimMargin())
                _connectionState.value = ConnectionState.Connected
                subscribeToState()
                connectContinuation?.resume(true)
                connectContinuation = null
            }
            .addDisconnectedListener {
                logger.d(LogConfig.TAG_MQTT, """Disconnected listener triggered:
Client ID: $clientId""".trimMargin())
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
                    // Status nach erfolgreichem Subscribe anfragen
                    logger.d(LogConfig.TAG_MQTT, "Requesting initial door state")
                    requestStatus()
                }
            }
    }

    suspend fun connect(server: String, username: String, password: String): Boolean = suspendCoroutine { continuation ->
        try {
            logger.d(LogConfig.TAG_MQTT, "Starting connect sequence")
            connectContinuation = continuation

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
            client = buildMqttClient(server)
            logger.d(LogConfig.TAG_MQTT, "Created new MQTT client")

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
                        connectContinuation?.resume(false)
                        connectContinuation = null
                    } else {
                        logger.d(LogConfig.TAG_MQTT, "Connect acknowledged successfully")
                        // Connected event will be handled by the ConnectedListener
                    }
                }
        } catch (e: Exception) {
            logger.e(LogConfig.TAG_MQTT, "Exception during connect", e)
            _connectionState.value = ConnectionState.Error(e.message ?: "Unknown error during connect")
            continuation.resumeWithException(e)
            connectContinuation = null
        }
    }

    fun disconnect() {
        try {
            logger.d(LogConfig.TAG_MQTT, "Starting disconnect")
            connectContinuation = null  // Clear any pending connection continuation
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