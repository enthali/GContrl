package de.drachenfels.gcontrl.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
    modifier: Modifier = Modifier
) {
    val doorState by mqttService.doorState.collectAsState()
    val connectionState by mqttService.connectionState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("GaragePilot") },
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
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Connection status
            Text(
                text = if (connectionState is MQTTService.ConnectionState.Connected) "Connected" else "Disconnected",
                color = if (connectionState is MQTTService.ConnectionState.Connected) 
                    MaterialTheme.colorScheme.primary 
                else MaterialTheme.colorScheme.error
            )

            // Door status
            DoorStatus(doorState)

            // Control buttons with spacer for better distribution
            Spacer(modifier = Modifier.weight(1f))

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

            Spacer(modifier = Modifier.weight(1f))
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

@Composable
private fun DoorStatus(state: DoorState) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .run {
                    val backgroundColor = when (state) {
                        DoorState.OPEN -> MaterialTheme.colorScheme.tertiaryContainer
                        DoorState.CLOSED -> MaterialTheme.colorScheme.primaryContainer
                        DoorState.OPENING, DoorState.CLOSING -> MaterialTheme.colorScheme.secondaryContainer
                        DoorState.STOPPED -> MaterialTheme.colorScheme.errorContainer
                        DoorState.UNKNOWN -> MaterialTheme.colorScheme.surfaceVariant
                    }
                    this.background(backgroundColor)
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = when (state) {
                    DoorState.OPEN -> "Door Open"
                    DoorState.CLOSED -> "Door Closed"
                    DoorState.OPENING -> "Door Opening..."
                    DoorState.CLOSING -> "Door Closing..."
                    DoorState.STOPPED -> "Door Stopped"
                    DoorState.UNKNOWN -> "Door State Unknown"
                },
                style = MaterialTheme.typography.headlineMedium,
                color = when (state) {
                    DoorState.OPEN -> MaterialTheme.colorScheme.onTertiaryContainer
                    DoorState.CLOSED -> MaterialTheme.colorScheme.onPrimaryContainer
                    DoorState.OPENING, DoorState.CLOSING -> MaterialTheme.colorScheme.onSecondaryContainer
                    DoorState.STOPPED -> MaterialTheme.colorScheme.onErrorContainer
                    DoorState.UNKNOWN -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
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
            // Einfache Preview-Version mit statischen Werten
            MainScreen(
                onNavigateToSettings = { },
                mqttService = MQTTService(context),
            )
        }
    }
}