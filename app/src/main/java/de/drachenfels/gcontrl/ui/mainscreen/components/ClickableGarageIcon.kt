package de.drachenfels.gcontrl.ui.mainscreen.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import de.drachenfels.gcontrl.services.DoorState
import de.drachenfels.gcontrl.ui.icons.garage.*

@Composable
fun ClickableGarageIcon(
    state: DoorState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.clickable(onClick = onClick)
    ) {
        when (state) {
            DoorState.OPEN -> GarageOpenIcon(modifier)
            DoorState.CLOSED -> GarageClosedIcon(modifier)
            DoorState.OPENING -> GarageOpeningIcon(modifier)
            DoorState.CLOSING -> GarageClosingIcon(modifier)
            DoorState.STOPPED -> GarageClosedIcon(modifier)
            else -> GarageUnknownIcon(modifier)
        }
    }
}