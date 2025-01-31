package de.drachenfels.gcontrl.ui.mainscreen.components

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.drachenfels.gcontrl.services.MQTTService

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

@Preview(showBackground = true)
@Composable
fun GarageButtonOpenPreview() {
    MaterialTheme {
        GarageButton(
            text = "Open",
            onClick = {},
            connectionState = MQTTService.ConnectionState.Connected
        )
    }
}

@Preview(showBackground = true)
@Composable
fun GarageButtonClosePreview() {
    MaterialTheme {
        GarageButton(
            text = "Close",
            onClick = {},
            connectionState = MQTTService.ConnectionState.Connected
        )
    }
}

@Preview(showBackground = true)
@Composable
fun GarageButtonDisconnectedPreview() {
    MaterialTheme {
        GarageButton(
            text = "Open",
            onClick = {},
            connectionState = MQTTService.ConnectionState.Disconnected
        )
    }
}