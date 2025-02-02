package de.drachenfels.gcontrl.ui.icons.garage

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.drachenfels.gcontrl.ui.icons.misc.QuestionMarkIcon
import de.drachenfels.gcontrl.ui.theme.GContrlTheme

@Composable
fun GarageUnknownIcon(
    modifier: Modifier = Modifier,
    primary: Color = MaterialTheme.colorScheme.primary,
    background: Color = MaterialTheme.colorScheme.background
) {
    Box(modifier = modifier) {
        GarageOutline(modifier = modifier.matchParentSize())
        QuestionMarkIcon(modifier = modifier.matchParentSize())
    }
}


@Preview
@Composable
private fun PreviewGarageUnknownIcon() {
    GContrlTheme {
        GarageUnknownIcon(
            modifier = Modifier.size(64.dp)
        )
    }
}