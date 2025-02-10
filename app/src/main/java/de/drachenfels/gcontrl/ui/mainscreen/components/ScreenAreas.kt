package de.drachenfels.gcontrl.ui.mainscreen.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import de.drachenfels.gcontrl.LocationAutomationSettings
import de.drachenfels.gcontrl.services.DoorState

@Composable
fun IconArea(
    doorState: DoorState,
    locationAutomationSettings: LocationAutomationSettings,
    currentDistance: Float,
    onDoorClick: (DoorState) -> Unit,
    modifier: Modifier = Modifier
) {
    var boxSize by remember { mutableStateOf(IntSize.Zero) }
    val childSize: Dp

    if (boxSize.width > boxSize.height) {
        childSize = with(LocalDensity.current) { boxSize.height.toDp() }
    } else {
        childSize = with(LocalDensity.current) { boxSize.height.toDp() * 0.5f }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .onSizeChanged { boxSize = it },
        contentAlignment = Alignment.Center
    ) {
        if (locationAutomationSettings.isLocationAutomationEnabled) {
            LocationAutomationStatus(
                doorState = doorState,
                currentDistance = currentDistance,
                triggerDistance = locationAutomationSettings.triggerDistance.toFloat(),
                onClick = { onDoorClick(doorState) },
                modifier = Modifier.padding(16.dp)
            )
        } else {
            DoorStatus(
                state = doorState,
                size = childSize,
                onClick = { onDoorClick(doorState) }
            )
        }
    }
}

@Composable
fun ControlButtonArea(
    onOpenClick: () -> Unit,
    onStopClick: () -> Unit,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.Bottom)
    ) {
        // open Button
        Button(
            onClick = onOpenClick,
            shape = RoundedCornerShape(24.dp),
            modifier = modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.primary)
        ) {
            Text("Open",style = MaterialTheme.typography.headlineMedium)
        }

        // stop Button
        Button(
            onClick = onStopClick,
            shape = RoundedCornerShape(24.dp),
            modifier = modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.secondary)
        ) {
            Text("Stop", style = MaterialTheme.typography.headlineMedium)
        }

        // open Button
        Button(
            onClick = onCloseClick,
            shape = RoundedCornerShape(24.dp),
            modifier = modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.tertiary)
        ) {
            Text("Close", style = MaterialTheme.typography.headlineMedium)
        }
    }
}



@Preview(showBackground = true, widthDp = 400)
@Composable
fun IconAreaPreview() {
    IconArea(
        doorState = DoorState.CLOSED,
        locationAutomationSettings = LocationAutomationSettings(
            isLocationAutomationEnabled = false,
            triggerDistance = 100
        ),
        currentDistance = 0f,
        onDoorClick = {}
    )
}

@Preview(showBackground = true, widthDp = 400)
@Composable
fun IconAreaWithLocationPreview() {
    IconArea(
        doorState = DoorState.CLOSED,
        locationAutomationSettings = LocationAutomationSettings(
            isLocationAutomationEnabled = true,
            triggerDistance = 100
        ),
        currentDistance = 50f,
        onDoorClick = {}
    )
}

@Preview(showBackground = true, widthDp = 400)
@Composable
fun ControlButtonAreaPreview() {
    ControlButtonArea(
        onOpenClick = {},
        onStopClick = {},
        onCloseClick = {}
    )
}

