package de.drachenfels.gcontrl.ui.mainscreen

import android.content.Context
import android.content.ContextWrapper
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import de.drachenfels.gcontrl.LocationAutomationSettings
import de.drachenfels.gcontrl.mqtt.MQTTService
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.drachenfels.gcontrl.ui.mainscreen.components.*
import de.drachenfels.gcontrl.ui.theme.GContrlTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onNavigateToSettings: () -> Unit,
    mqttService: MQTTService,
    locationAutomationSettingsFlow: StateFlow<LocationAutomationSettings>,
    modifier: Modifier = Modifier
) {
    val locationAutomationSettings by locationAutomationSettingsFlow.collectAsState()
    val doorState by mqttService.doorState.collectAsState()
    val connectionState by mqttService.connectionState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("GaragePilot") },
                navigationIcon = {
                    ConnectionStatusIcon(connectionState)
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (locationAutomationSettings.isLocationAutomationEnabled) {
                    LocationAutomationStatus(
                        doorState = doorState,
                        currentDistance = 50f, // TODO: Implementiere echte Distanz
                        triggerDistance = locationAutomationSettings.triggerDistance.toFloat(),
                        modifier = Modifier.padding(16.dp)
                    )
                } else {
                    DoorStatus(doorState)
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.Bottom)
            ) {
                GarageButton(
                    text = "Open",
                    onClick = { mqttService.openDoor() },
                    connectionState = connectionState
                )

                GarageButton(
                    text = "Stop",
                    onClick = { mqttService.stopDoor() },
                    connectionState = connectionState
                )

                GarageButton(
                    text = "Close",
                    onClick = { mqttService.closeDoor() },
                    connectionState = connectionState,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
        }
    }
}


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