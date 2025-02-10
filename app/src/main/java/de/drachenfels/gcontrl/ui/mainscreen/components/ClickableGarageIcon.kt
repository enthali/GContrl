package de.drachenfels.gcontrl.ui.mainscreen.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import de.drachenfels.gcontrl.services.DoorState
import de.drachenfels.gcontrl.ui.icons.garage.*

// TODO: Move string resources to strings.xml and provide German and English translations
@Composable
fun ClickableGarageIcon(
    state: DoorState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Bestimme die Beschreibung basierend auf dem Zustand
    val description = when (state) {
        DoorState.OPEN -> "Garagentor ist geöffnet"
        DoorState.CLOSED -> "Garagentor ist geschlossen"
        DoorState.OPENING -> "Garagentor öffnet sich"
        DoorState.CLOSING -> "Garagentor schließt sich"
        DoorState.STOPPED -> "Garagentor ist angehalten"
        else -> "Garagentor-Status unbekannt"
    }

    Box(
        modifier = modifier
            .semantics { 
                contentDescription = description
            }
            .clickable(onClick = onClick)
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