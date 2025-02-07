package de.drachenfels.gcontrl.ui.mainscreen

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import de.drachenfels.gcontrl.LocationAutomationSettings
import de.drachenfels.gcontrl.services.DoorState
import de.drachenfels.gcontrl.services.LocationDataRepository
import de.drachenfels.gcontrl.services.MQTTService
import de.drachenfels.gcontrl.ui.mainscreen.components.ConnectionStatusIcon
import de.drachenfels.gcontrl.ui.mainscreen.components.ControlButtonArea
import de.drachenfels.gcontrl.ui.mainscreen.components.IconArea
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
    val configuration = LocalConfiguration.current

    val locationData by LocationDataRepository.locationUpdates.collectAsState()
    val currentSpeed = locationData?.speed ?: 0f
    val showSettings = currentSpeed <= 3f
    val currentDistance = locationData?.distanceToGarage?.toFloat() ?: 1000f

    fun handleDoorClick(currentState: DoorState) {
        when (currentState) {
            DoorState.OPEN -> mqttService.closeDoor()
            DoorState.CLOSED -> mqttService.openDoor()
            DoorState.OPENING, DoorState.CLOSING -> mqttService.stopDoor()
            DoorState.STOPPED -> mqttService.openDoor()
            else -> {} // Handle UNKNOWN state
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("GaragePilot") },
                navigationIcon = {
                    ConnectionStatusIcon(connectionState)
                },
                actions = {
                    if (showSettings) {
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
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
            if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                // Landscape layout
                Row(modifier = Modifier.fillMaxSize()) {
                    IconArea(
                        doorState = doorState,
                        locationAutomationSettings = locationAutomationSettings,
                        currentDistance = currentDistance,
                        onDoorClick = { handleDoorClick(it) },
                        modifier = Modifier.weight(0.67f)
                    )
                    ControlButtonArea(
                        connectionState = connectionState,
                        onOpenClick = { mqttService.openDoor() },
                        onStopClick = { mqttService.stopDoor() },
                        onCloseClick = { mqttService.closeDoor() },
                        modifier = Modifier.weight(0.33f)
                    )
                }
            } else {
                // Portrait layout
                Column(modifier = Modifier.fillMaxSize()) {
                    IconArea(
                        doorState = doorState,
                        locationAutomationSettings = locationAutomationSettings,
                        currentDistance = currentDistance,
                        onDoorClick = { handleDoorClick(it) },
                        modifier = Modifier.weight(0.67f)
                    )
                    ControlButtonArea(
                        connectionState = connectionState,
                        onOpenClick = { mqttService.openDoor() },
                        onStopClick = { mqttService.stopDoor() },
                        onCloseClick = { mqttService.closeDoor() },
                        modifier = Modifier.weight(0.33f)
                    )
                }
            }
        }
    }
}

// Preview code unverändert
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
                    mqttService = MQTTService.getInstance(),
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
                    mqttService = MQTTService.getInstance(),
                    locationAutomationSettingsFlow = locationAutomationSettingsFlow.asStateFlow()
                )
            }
        }
    }
}