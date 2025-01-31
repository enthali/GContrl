package de.drachenfels.gcontrl.ui.mainscreen

import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import de.drachenfels.gcontrl.LocationAutomationSettings
import de.drachenfels.gcontrl.mqtt.MQTTService
import de.drachenfels.gcontrl.ui.theme.GContrlTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PreviewContextWrapper(
    base: Context,
    val locationAutomationSettingsFlow: StateFlow<LocationAutomationSettings>
) : ContextWrapper(base)

@Preview(
    name = "Without Location Automation",
    showBackground = true,
    showSystemUi = true,
    device = "spec:width=411dp,height=891dp,dpi=420,isRound=false,chinSize=0dp,orientation=portrait"
)
@Composable
fun MainScreenPreviewWithoutLocation() {
    val locationAutomationSettingsFlow =
        remember { MutableStateFlow(LocationAutomationSettings(isLocationAutomationEnabled = false)) }
    // Verwende PreviewContextWrapper
    CompositionLocalProvider(
        LocalContext provides PreviewContextWrapper(
            LocalContext.current,
            locationAutomationSettingsFlow.asStateFlow()
        )
    ) {
        GContrlTheme {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                MainScreen(
                    onNavigateToSettings = { },
                    mqttService = MQTTService(LocalContext.current),
                    locationAutomationSettingsFlow = locationAutomationSettingsFlow.asStateFlow()
                )
            }
        }
    }
}

@Preview(
    name = "With Location Automation",
    showBackground = true,
    showSystemUi = true,
    device = "spec:width=411dp,height=891dp,dpi=420,isRound=false,chinSize=0dp,orientation=portrait"
)
@Composable
fun MainScreenPreviewWithLocation() {
    val locationAutomationSettingsFlow =
        remember { MutableStateFlow(LocationAutomationSettings(isLocationAutomationEnabled = true)) }
    // Verwende PreviewContextWrapper
    CompositionLocalProvider(
        LocalContext provides PreviewContextWrapper(
            LocalContext.current,
            locationAutomationSettingsFlow.asStateFlow()
        )
    ) {
        GContrlTheme {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                MainScreen(
                    onNavigateToSettings = { },
                    mqttService = MQTTService(LocalContext.current),
                    locationAutomationSettingsFlow = locationAutomationSettingsFlow.asStateFlow()
                )
            }
        }
    }
}