package de.drachenfels.gcontrl.ui

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.drachenfels.gcontrl.R
import de.drachenfels.gcontrl.mqtt.DoorState
import de.drachenfels.gcontrl.mqtt.MQTTService
import de.drachenfels.gcontrl.ui.theme.GContrlTheme
import de.drachenfels.gcontrl.utils.AndroidLogger

private val logger = AndroidLogger()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onNavigateToSettings: () -> Unit,
    mqttService: MQTTService,
    isLocationAutomationEnabled: Boolean = false,
    currentDistance: Float = 0f,
    triggerDistance: Float = 100f,
    modifier: Modifier = Modifier
) {
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
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Location Automation Status (if enabled)
            if (isLocationAutomationEnabled) {
                LocationAutomationStatus(
                    doorState = doorState,
                    currentDistance = currentDistance,
                    triggerDistance = triggerDistance,
                    modifier = Modifier.padding(16.dp)
                )
            }

            // Door status
            DoorStatus(doorState)

            // Control buttons with spacer for better distribution
            Spacer(modifier = Modifier.weight(1f))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                GarageButton(
                    text = "Open",
                    onClick = { mqttService.openDoor() },
                    color = MaterialTheme.colorScheme.primary,
                    connectionState = connectionState
                )

                GarageButton(
                    text = "Stop",
                    onClick = { mqttService.stopDoor() },
                    color = MaterialTheme.colorScheme.secondary,
                    connectionState = connectionState
                )

                GarageButton(
                    text = "Close",
                    onClick = { mqttService.closeDoor() },
                    color = MaterialTheme.colorScheme.tertiary,
                    connectionState = connectionState
                )
            }

            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun ConnectionStatusIcon(connectionState: MQTTService.ConnectionState) {
    IconButton(onClick = { /* Optional: Show connection details */ }) {
        Icon(
            imageVector = if (connectionState is MQTTService.ConnectionState.Connected)
                Icons.Default.CloudDone
            else
                Icons.Default.CloudOff,
            contentDescription = if (connectionState is MQTTService.ConnectionState.Connected)
                "Connected"
            else
                "Disconnected",
            tint = if (connectionState is MQTTService.ConnectionState.Connected)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.error
        )
    }
}

@Composable
private fun LocationAutomationStatus(
    doorState: DoorState,
    currentDistance: Float,
    triggerDistance: Float,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Door Status Icon
        DoorStatus(
            state = doorState,
            size = 40.dp,
            showText = false
        )

        // Distance Slider
        Slider(
            value = currentDistance,
            onValueChange = { /* Read only */ },
            valueRange = 0f..triggerDistance,
            enabled = false,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp)
        )

        // Car Icon
        Icon(
            imageVector = Icons.Default.DirectionsCar,
            contentDescription = null,
            modifier = Modifier.size(40.dp),
            tint = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun DoorStatus(
    state: DoorState,
    size: Dp = 360.dp,
    showText: Boolean = true
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(vertical = if(showText) 16.dp else 4.dp)
    ) {
        Icon(
            painter = painterResource(
                id = when (state) {
                    DoorState.OPEN -> R.drawable.ic_garage_open
                    else -> R.drawable.ic_garage_closed
                }
            ),
            contentDescription = null,
            modifier = Modifier.size(size),
            tint = MaterialTheme.colorScheme.primary
        )

        if (showText) {
            Text(
                text = when (state) {
                    DoorState.OPEN -> "Open"
                    DoorState.CLOSED -> "Closed"
                    DoorState.OPENING -> "Opening..."
                    DoorState.CLOSING -> "Closing..."
                    DoorState.STOPPED -> "Stopped"
                    DoorState.UNKNOWN -> "Unknown"
                },
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

@Composable
private fun GarageButton(
    text: String,
    onClick: () -> Unit,
    color: Color,
    connectionState: MQTTService.ConnectionState
) {
    val context = LocalContext.current
    Button(
        onClick = {
            when (connectionState) {
                is MQTTService.ConnectionState.Connected -> onClick()
                else -> Toast.makeText(
                    context,
                    "Not connected to garage control",
                    Toast.LENGTH_SHORT
                ).show()
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = color
        )
    ) {
        Text(text, style = MaterialTheme.typography.headlineMedium)
    }
}

@Preview(
    showBackground = true,
    showSystemUi = true,
    device = "spec:width=411dp,height=891dp,dpi=420,isRound=false,chinSize=0dp,orientation=portrait"
)
@Composable
fun MainScreenPreview() {
    val context = LocalContext.current
    GContrlTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            MainScreen(
                onNavigateToSettings = { },
                mqttService = MQTTService(context),
                isLocationAutomationEnabled = true, // Show location automation in preview
                currentDistance = 50f,
                triggerDistance = 100f
            )
        }
    }
}