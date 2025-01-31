package de.drachenfels.gcontrl

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import de.drachenfels.gcontrl.mqtt.MQTTService
import de.drachenfels.gcontrl.ui.MainScreen
import de.drachenfels.gcontrl.ui.settings.SettingsScreen
import de.drachenfels.gcontrl.ui.theme.GContrlTheme
import de.drachenfels.gcontrl.utils.AndroidLogger
import de.drachenfels.gcontrl.utils.LogConfig

class MainActivity : ComponentActivity() {
    private val logger = AndroidLogger()
    private lateinit var mqttService: MQTTService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        logger.d(LogConfig.TAG_MAIN, "onCreate - Initializing app")
        mqttService = MQTTService(this)
        
        enableEdgeToEdge()
        setContent {
            GContrlApp(mqttService)
        }
        logger.d(LogConfig.TAG_MAIN, "onCreate - App initialized")
    }

    override fun onPause() {
        super.onPause()
        logger.d(LogConfig.TAG_MAIN, "onPause - App going to background")
    }

    override fun onResume() {
        super.onResume()
        logger.d(LogConfig.TAG_MAIN, "onResume - App coming to foreground")
    }

    override fun onDestroy() {
        super.onDestroy()
        logger.d(LogConfig.TAG_MAIN, "onDestroy - Cleaning up")
        mqttService.disconnect()
        logger.d(LogConfig.TAG_MAIN, "onDestroy - MQTT connection closed")
    }
}

@Composable
fun GContrlApp(mqttService: MQTTService) {
    val logger = AndroidLogger()
    var showSettings by remember { mutableStateOf(false) }

    // Effekt für Screen-Wechsel
    LaunchedEffect(showSettings) {
        if (showSettings) {
            logger.d(LogConfig.TAG_MAIN, "Screen transition: Main -> Settings, disconnecting MQTT")
            mqttService.disconnect()
        } else {
            logger.d(LogConfig.TAG_MAIN, "Screen transition: Settings -> Main, connecting MQTT")
            mqttService.connect()
        }
    }

    GContrlTheme {
        if (showSettings) {
            SettingsScreen(
                mqttService = mqttService,
                onNavigateBack = { 
                    logger.d(LogConfig.TAG_MAIN, "User requested navigation: Settings -> Main")
                    showSettings = false 
                }
            )
        } else {
            MainScreen(
                mqttService = mqttService,
                onNavigateToSettings = { 
                    logger.d(LogConfig.TAG_MAIN, "User requested navigation: Main -> Settings")
                    showSettings = true 
                }
            )
        }
    }
}