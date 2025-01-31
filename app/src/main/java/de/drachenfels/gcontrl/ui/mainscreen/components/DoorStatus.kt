package de.drachenfels.gcontrl.ui.mainscreen.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.drachenfels.gcontrl.R
import de.drachenfels.gcontrl.mqtt.DoorState

@Composable
fun DoorStatus(
    state: DoorState,
    size: Dp = Dp.Unspecified,
    showText: Boolean = true
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val iconSize = if (size != Dp.Unspecified) size else (screenWidth * 0.50f)

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

@Preview(
    name = "Door Status - Open",
    showBackground = true
)
@Composable
fun DoorStatusPreview() {
    MaterialTheme {
        DoorStatus(state = DoorState.OPEN)
    }
}

@Preview(
    name = "Door Status - Closed",
    showBackground = true
)
@Composable
fun DoorStatusClosedPreview() {
    MaterialTheme {
        DoorStatus(state = DoorState.CLOSED)
    }
}