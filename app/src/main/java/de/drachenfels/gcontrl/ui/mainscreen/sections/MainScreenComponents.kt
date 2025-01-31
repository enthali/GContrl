package de.drachenfels.gcontrl.ui.mainscreen.sections

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.drachenfels.gcontrl.R
import de.drachenfels.gcontrl.mqtt.DoorState
import de.drachenfels.gcontrl.mqtt.MQTTService

@Composable
fun ConnectionStatusIcon(connectionState: MQTTService.ConnectionState) {
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
fun LocationAutomationStatus(
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
fun DoorStatus(
    state: DoorState,
    size: Dp = Dp.Unspecified,
    showText: Boolean = true
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val iconSize = if (size != Dp.Unspecified) size else (screenWidth * 0.55f)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(vertical = if (showText) 16.dp else 4.dp)
    ) {
        Icon(
            painter = painterResource(
                id = when (state) {
                    DoorState.OPEN -> R.drawable.ic_garage_open
                    else -> R.drawable.ic_garage_closed
                }
            ),
            contentDescription = null,
            modifier = Modifier.size(iconSize),
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
fun GarageButton(
    text: String,
    onClick: () -> Unit,
    connectionState: MQTTService.ConnectionState,
    modifier: Modifier = Modifier
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
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = when (text) {
                "Open" -> MaterialTheme.colorScheme.primary
                "Stop" -> MaterialTheme.colorScheme.secondary
                "Close" -> MaterialTheme.colorScheme.tertiary
                else -> MaterialTheme.colorScheme.primary
            }
        )
    ) {
        Text(text, style = MaterialTheme.typography.headlineMedium)
    }
}