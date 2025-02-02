package de.drachenfels.gcontrl.ui.icons.garage

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.drachenfels.gcontrl.ui.theme.GContrlTheme

@Composable
fun GarageOpenIcon(
    modifier: Modifier = Modifier
) {

    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

    Canvas(modifier = modifier) {
        val width = size.width
        val scale = width / 60f  // Viewbox scaling

        // Innerer Bereich
        drawRect(
            color = surfaceVariant,
            alpha = 0.3f,
            topLeft = Offset(6f * scale, 20f * scale),
            size = Size(49f * scale, 30f * scale)
        )
    }
    GarageOutline(modifier = modifier)
}

@Preview
@Composable
fun PreviewGarageOpenIcon() {
    GContrlTheme {
        GarageOpenIcon(
            modifier = Modifier.size(64.dp)
        )
    }
}