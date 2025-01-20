package de.drachenfels.gcontrl

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import de.drachenfels.gcontrl.ui.theme.GaragePilotTheme
import de.drachenfels.gcontrl.ui.MainScreen
import de.drachenfels.gcontrl.ui.SettingsScreen
import de.drachenfels.gcontrl.utils.LogConfig
import de.drachenfels.gcontrl.utils.AndroidLogger

class MainActivity : ComponentActivity() {
    private val logger = AndroidLogger()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        logger.d(LogConfig.TAG_MAIN, "onCreate")
        
        enableEdgeToEdge()
        setContent {
            var showSettings by remember { mutableStateOf(false) }

            GaragePilotTheme {
                if (showSettings) {
                    SettingsScreen(
                        onNavigateBack = {
                            showSettings = false
                            logger.d(LogConfig.TAG_MAIN, "Navigating back to main")
                        }
                    )
                } else {
                    MainScreen(
                        onNavigateToSettings = {
                            showSettings = true
                            logger.d(LogConfig.TAG_MAIN, "Navigating to settings")
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        logger.d(LogConfig.TAG_MAIN, "onResume")
    }

    override fun onPause() {
        super.onPause()
        logger.d(LogConfig.TAG_MAIN, "onPause")
    }
}