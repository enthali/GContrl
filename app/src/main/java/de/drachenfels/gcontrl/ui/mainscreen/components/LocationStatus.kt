package de.drachenfels.gcontrl.ui.mainscreen.components


import android.icu.text.DecimalFormat
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.drachenfels.gcontrl.services.DoorState

@Composable
fun LocationAutomationStatus(
    doorState: DoorState,
    currentDistance: Float,
    triggerDistance: Float,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val iconSize = screenWidth * 0.25f

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            DoorStatus(doorState, iconSize, false)

            Icon(
                imageVector = Icons.Default.DirectionsCar,
                contentDescription = null,
                modifier = Modifier.size(iconSize),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        
        Slider(
            value = triggerDistance,
            onValueChange = { /* Read only */ },
            valueRange = 0f..currentDistance,
            enabled = false,
            colors = SliderDefaults.colors(
                activeTrackColor = MaterialTheme.colorScheme.primary,
                disabledActiveTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant,
                disabledInactiveTrackColor = MaterialTheme.colorScheme.outlineVariant,
                thumbColor = MaterialTheme.colorScheme.primary,
                disabledThumbColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )

        Text(
            text = formatDistance(currentDistance),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

fun formatDistance(distance: Float): String {
    return if (distance < 900) {
        "${distance.toInt()}m"
    } else {
        val km = distance / 1000
        val df = DecimalFormat("#.#")
        "${df.format(km)}km"
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
            currentDistance = 1200f,
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