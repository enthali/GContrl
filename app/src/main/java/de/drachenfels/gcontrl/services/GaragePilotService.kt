package de.drachenfels.gcontrl.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import de.drachenfels.gcontrl.R
import de.drachenfels.gcontrl.utils.AndroidLogger
import de.drachenfels.gcontrl.utils.LogConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class GaragePilotService : Service() {
    private val logger = AndroidLogger()
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var mqttService: MQTTService? = null
    private var stateCollectorJob: Job? = null

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "GaragePilotService"
        private const val CHANNEL_NAME = "Garage Pilot"
        
        // Settings keys
        private const val PREFS_NAME = "GContrlPrefs"
        private const val KEY_MQTT_SERVER = "mqtt_server"
        private const val KEY_MQTT_USERNAME = "mqtt_username"
        private const val KEY_MQTT_PASSWORD = "mqtt_password"
        private const val KEY_CONFIG_VALID = "mqtt_config_valid"
    }

    override fun onCreate() {
        super.onCreate()
        logger.d(LogConfig.TAG_MAIN, "GaragePilotService: onCreate")
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        
        mqttService = MQTTService.getInstance()
        startStateCollection()
        connectToMqtt()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        logger.d(LogConfig.TAG_MAIN, "GaragePilotService: onStartCommand")
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        logger.d(LogConfig.TAG_MAIN, "GaragePilotService: onDestroy")
        stateCollectorJob?.cancel()
        mqttService?.disconnect()
        serviceScope.cancel()
    }

    private fun connectToMqtt() {
        val prefs = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isConfigValid = prefs.getBoolean(KEY_CONFIG_VALID, false)
        
        if (!isConfigValid) {
            logger.d(LogConfig.TAG_MAIN, "MQTT config not valid, skipping connection")
            return
        }

        val server = prefs.getString(KEY_MQTT_SERVER, "") ?: ""
        val username = prefs.getString(KEY_MQTT_USERNAME, "") ?: ""
        val password = prefs.getString(KEY_MQTT_PASSWORD, "") ?: ""

        if (server.isEmpty()) {
            logger.d(LogConfig.TAG_MAIN, "MQTT server not configured, skipping connection")
            return
        }

        serviceScope.launch {
            try {
                logger.d(LogConfig.TAG_MAIN, "Attempting MQTT connection")
                val connected = mqttService?.connect(server, username, password) ?: false
                if (connected) {
                    logger.d(LogConfig.TAG_MAIN, "MQTT connection established")
                } else {
                    logger.e(LogConfig.TAG_MAIN, "MQTT connection failed")
                }
            } catch (e: Exception) {
                logger.e(LogConfig.TAG_MAIN, "Error connecting to MQTT", e)
            }
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "GaragePilot Service Status"
            setShowBadge(false)
        }

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("GaragePilot Active")
            .setContentText("Monitoring garage door status")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setSilent(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    private fun startStateCollection() {
        stateCollectorJob = serviceScope.launch {
            mqttService?.doorState?.collectLatest { state ->
                updateNotification(state)
            }
        }
    }

    private fun updateNotification(doorState: DoorState) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("GaragePilot Active")
            .setContentText("Door Status: ${doorState.name}")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setSilent(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}