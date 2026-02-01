package de.drachenfels.gcontrl.services

import android.content.Context
import com.hivemq.client.mqtt.lifecycle.MqttDisconnectSource
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client
import de.drachenfels.gcontrl.utils.AndroidLogger
import de.drachenfels.gcontrl.utils.LogConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.coroutines.resume
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

    private val _doorState = MutableStateFlow(DoorState.UNKNOWN)
    val doorState: StateFlow<DoorState> = _doorState

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

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


    private fun buildMqttClient(server: String): Mqtt5AsyncClient? {
        return try {
            val clientId = UUID.randomUUID().toString()
            logger.d(LogConfig.TAG_MQTT, "Building MQTT client with ID: $clientId for server: $server")

            Mqtt5Client.builder()
                .identifier(clientId)
                .serverHost(server)
                .serverPort(MQTT_PORT)
                .sslConfig()
                .applySslConfig()
                .addConnectedListener {
                    logger.d(LogConfig.TAG_MQTT, "Connected - ConnectionListener")
                    _connectionState.value = ConnectionState.Connected
                    subscribeToState()
                }
                .addDisconnectedListener { event ->
                    // Bestehende ausführliche Logging beibehalten für Diagnose
                    logger.d(LogConfig.TAG_MQTT, """Disconnected listener triggered: 
                                                Client ID: ${event.clientConfig.clientIdentifier}
                                                Source: ${event.source}
                                                Client Config: ${event.clientConfig} 
                                                Cause: ${event.cause?.message ?: "No cause provided"}
                                                State: ${event.clientConfig.state}
                                                Connection Details: ${event.clientConfig.connectionConfig}
                                                Current Thread: ${Thread.currentThread().name}
                                                Current connection state: ${_connectionState.value}
                                                Server Address: ${event.clientConfig.serverAddress}
                                                Server Port: ${event.clientConfig.serverPort}
                                                Client Config Methods: ${event.clientConfig::class.java.methods.joinToString { it.name }}
                                                Available properties: ${event::class.java.methods.joinToString { it.name }}
                                            """.trimMargin())

                    _connectionState.value = ConnectionState.Disconnected

                    when (event.source) {
                        MqttDisconnectSource.CLIENT, MqttDisconnectSource.SERVER -> {
                            scope.launch {
                                delay(2000)
                                logger.d(LogConfig.TAG_MQTT, "Attempting reconnect")
                                connect()
                            }
                        }
                        else -> {
                            logger.d(LogConfig.TAG_MQTT, "Server or other disconnect detected (${event.source}), cause: ${event.cause?.message ?: "unknown"}, no auto-reconnect")
                        }
                    }
                }
                .buildAsync()
        } catch (e: Exception) {
            logger.e(LogConfig.TAG_MQTT, "Failed to build MQTT client", e)
            null
        }
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
            ?.whenComplete { _, throwable ->
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
    ): Boolean = suspendCoroutine { continuation ->
        try {
            logger.d(LogConfig.TAG_MQTT, "Starting connection to server: $server with username: $username")
            _connectionState.value = ConnectionState.Connecting

            client = buildMqttClient(server)

            if (client == null) {
                logger.e(LogConfig.TAG_MQTT, "Failed to build MQTT client")
                _connectionState.value = ConnectionState.Error("Failed to build MQTT client")
                continuation.resume(false)
                return@suspendCoroutine
            }

            logger.d(LogConfig.TAG_MQTT, "Client built successfully, attempting to connect...")
            val connectFuture = client?.connectWith()
                ?.keepAlive(15)
                ?.simpleAuth()
                ?.username(username)
                ?.password(password.toByteArray())
                ?.applySimpleAuth()
                ?.send()

            if (connectFuture == null) {
                logger.e(LogConfig.TAG_MQTT, "Connect future is null")
                _connectionState.value = ConnectionState.Error("Connection setup failed")
                continuation.resume(false)
                return@suspendCoroutine
            }

            connectFuture.whenComplete { _, throwable ->
                if (throwable != null) {
                    logger.e(LogConfig.TAG_MQTT, "Connection failed", throwable)
                    _connectionState.value = ConnectionState.Error(throwable.message ?: "Connection failed")
                    continuation.resume(false)
                } else {
                    logger.d(LogConfig.TAG_MQTT, "Connection successful")
                    continuation.resume(true)
                }
            }
        } catch (e: Exception) {
            logger.e(LogConfig.TAG_MQTT, "Exception during connection", e)
            _connectionState.value = ConnectionState.Error(e.message ?: "Connection failed")
            continuation.resume(false)
        }
    }

    suspend fun disconnect() = suspendCoroutine { continuation ->
        try {
            logger.d(LogConfig.TAG_MQTT, "Starting disconnect")

            if (client == null) {
                logger.d(LogConfig.TAG_MQTT, "Client is already null, nothing to disconnect")
                continuation.resume(true)
                return@suspendCoroutine
            }

            val disconnectFuture = client?.disconnect()

            if (disconnectFuture == null) {
                logger.d(LogConfig.TAG_MQTT, "Disconnect future is null")
                client = null
                continuation.resume(true)
                return@suspendCoroutine
            }

            disconnectFuture.whenComplete { _, throwable ->
                if (throwable != null) {
                    logger.e(LogConfig.TAG_MQTT, "Disconnect failed", throwable)
                    client = null
                    continuation.resume(false)
                } else {
                    logger.d(LogConfig.TAG_MQTT, "Disconnect successful")
                    client = null
                    continuation.resume(true)
                }
            }
        } catch (e: Exception) {
            logger.e(LogConfig.TAG_MQTT, "Exception during disconnect", e)
            client = null
            continuation.resume(false)
        }
    }

    private fun publishCommand(command: String) {
        CoroutineScope(Dispatchers.IO).launch {
            logger.d(LogConfig.TAG_MQTT, "Publishing command: $command")
            client?.publishWith()
                ?.topic(TOPIC_COMMAND)
                ?.payload(command.toByteArray())
                ?.send()
                ?.whenComplete { _, throwable ->
                    if (throwable != null) {
                        logger.e(LogConfig.TAG_MQTT, "Failed to publish command: $command", throwable)
                        _connectionState.value = ConnectionState.Error(throwable.message ?: "Publish failed")
                    } else {
                        logger.d(LogConfig.TAG_MQTT, "Published command: $command")
                    }
                }
        }
    }

    // Öffentliche API bleibt gleich
    fun openDoor() = publishCommand(COMMAND_OPEN)
    fun closeDoor() = publishCommand(COMMAND_CLOSE)
    fun stopDoor() = publishCommand(COMMAND_STOP)
    fun requestStatus() = publishCommand(COMMAND_REQUEST_STATUS)

    sealed class ConnectionState {
        data object Connected : ConnectionState()
        data object Connecting : ConnectionState()
        data object Disconnected : ConnectionState()
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