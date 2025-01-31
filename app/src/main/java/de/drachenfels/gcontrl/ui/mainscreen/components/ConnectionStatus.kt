package de.drachenfels.gcontrl.ui.mainscreen.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import de.drachenfels.gcontrl.services.MQTTService

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

@Preview(showBackground = true)
@Composable
fun ConnectionStatusConnectedPreview() {
    MaterialTheme {
        ConnectionStatusIcon(
            connectionState = MQTTService.ConnectionState.Connected
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ConnectionStatusDisconnectedPreview() {
    MaterialTheme {
        ConnectionStatusIcon(
            connectionState = MQTTService.ConnectionState.Disconnected
        )
    }
}