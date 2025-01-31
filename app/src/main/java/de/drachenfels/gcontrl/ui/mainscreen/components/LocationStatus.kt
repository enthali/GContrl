package de.drachenfels.gcontrl.ui.mainscreen.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.drachenfels.gcontrl.R
import de.drachenfels.gcontrl.mqtt.DoorState

@Composable
fun LocationAutomationStatus(
    doorState: DoorState,
    currentDistance: Float,
    triggerDistance: Float,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_garage_closed),
                contentDescription = null,
                modifier = Modifier.size(60.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Icon(
                imageVector = Icons.Default.DirectionsCar,
                contentDescription = null,
                modifier = Modifier.size(60.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        
        Slider(
            value = currentDistance,
            onValueChange = { /* Read only */ },
            valueRange = 0f..triggerDistance,
            enabled = false,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )
        
        Text(
            text = "${currentDistance.toInt()}/${triggerDistance.toInt()}m",
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Preview(
    name = "Location Status - Far",
    showBackground = true,
    widthDp = 400
)
@Composable
fun LocationAutomationStatusPreview() {
    MaterialTheme {
        LocationAutomationStatus(
            doorState = DoorState.CLOSED,
            currentDistance = 50f,
            triggerDistance = 100f
        )
    }
}

@Preview(
    name = "Location Status - Near",
    showBackground = true,
    widthDp = 400
)
@Composable
fun LocationAutomationStatusPreviewNear() {
    MaterialTheme {
        LocationAutomationStatus(
            doorState = DoorState.CLOSED,
            currentDistance = 10f,
            triggerDistance = 100f
        )
    }
}