package de.drachenfels.gcontrl.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
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
        
        if (!hasNotificationPermission()) {
            logger.e(LogConfig.TAG_NOTIFICATION, "Missing POST_NOTIFICATIONS permission!")
            stopSelf()
            return
        }

        createNotificationChannel()
        val notification = createNotification()
        logger.d(LogConfig.TAG_NOTIFICATION, "Starting foreground service with notification")
        startForeground(NOTIFICATION_ID, notification)
        
        mqttService = MQTTService.getInstance()
        startStateCollection()
        connectToMqtt()
    }

    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            logger.d(LogConfig.TAG_NOTIFICATION, "Notification permission check: $hasPermission")
            hasPermission
        } else {
            logger.d(LogConfig.TAG_NOTIFICATION, "Notification permission not required for this Android version")
            true
        }
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
        logger.d(LogConfig.TAG_NOTIFICATION, "Creating notification channel: $CHANNEL_ID")
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "GaragePilot Service Status"
            setShowBadge(false)
        }

        try {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            logger.d(LogConfig.TAG_NOTIFICATION, "Notification channel created successfully")
        } catch (e: Exception) {
            logger.e(LogConfig.TAG_NOTIFICATION, "Failed to create notification channel", e)
        }
    }

    private fun createNotification(): Notification {
        logger.d(LogConfig.TAG_NOTIFICATION, "Creating initial notification")
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("GaragePilot Active")
            .setContentText("Monitoring garage door status")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setSilent(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
            .also { logger.d(LogConfig.TAG_NOTIFICATION, "Initial notification created") }
    }

    private fun startStateCollection() {
        stateCollectorJob = serviceScope.launch {
            mqttService?.doorState?.collectLatest { state ->
                updateNotification(state)
            }
        }
    }

    private fun updateNotification(doorState: DoorState) {
        logger.d(LogConfig.TAG_NOTIFICATION, "Updating notification with door state: $doorState")
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("GaragePilot Active")
            .setContentText("Door Status: ${doorState.name}")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setSilent(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()

        try {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NOTIFICATION_ID, notification)
            logger.d(LogConfig.TAG_NOTIFICATION, "Notification updated successfully")
        } catch (e: Exception) {
            logger.e(LogConfig.TAG_NOTIFICATION, "Failed to update notification", e)
        }
    }
}