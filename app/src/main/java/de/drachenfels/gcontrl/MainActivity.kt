package de.drachenfels.gcontrl

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import de.drachenfels.gcontrl.mqtt.MQTTService
import de.drachenfels.gcontrl.ui.MainScreen
import de.drachenfels.gcontrl.ui.SettingsScreen
import de.drachenfels.gcontrl.ui.theme.GContrlTheme
import de.drachenfels.gcontrl.utils.AndroidLogger
import de.drachenfels.gcontrl.utils.LogConfig
import kotlinx.coroutines.launch

// TODO: MQTTService should be converted to Foreground Service
class MainActivity : ComponentActivity() {
    private val logger = AndroidLogger()
    private lateinit var mqttService: MQTTService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        logger.d(LogConfig.TAG_MAIN, "onCreate")
        mqttService = MQTTService(this)
        
        enableEdgeToEdge()
        setContent {
            GContrlApp(mqttService)
        }
    }


    override fun onResume() {
        super.onResume()
        logger.d(LogConfig.TAG_MAIN, "onResume - attempting to connect")
        // Versuche beim Starten zu verbinden
        lifecycleScope.launch {
            val isConnected = mqttService.connect()
            if (isConnected) {
                // Handle successful connection, e.g., update UI
                logger.d(LogConfig.TAG_MAIN, "onResume - connected")
            } else {
                // Handle connection failure, e.g., show error message
                logger.d(LogConfig.TAG_MAIN, "onResume - connection failed")
            }
        }
    }

    override fun onPause() {
        super.onPause()
        mqttService.disconnect()
        logger.d(LogConfig.TAG_MAIN, "onPause")
    }

    override fun onDestroy() {
        super.onDestroy()
        mqttService.disconnect()
        logger.d(LogConfig.TAG_MAIN, "onDestroy")
    }
}

@Composable
fun GContrlApp(mqttService: MQTTService) {
    var showSettings by remember { mutableStateOf(false) }

    GContrlTheme {
        if (showSettings) {
            SettingsScreen(
                mqttService = mqttService,
                onNavigateBack = { showSettings = false }
            )
        } else {
            MainScreen(
                mqttService = mqttService,
                onNavigateToSettings = { showSettings = true }
            )
        }
    }
}