package de.drachenfels.gcontrl.ui.mainscreen

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import de.drachenfels.gcontrl.ui.mainscreen.components.*
import kotlinx.coroutines.flow.StateFlow

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
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter),
                horizontalAlignment = Alignment.CenterHorizontally
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
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
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