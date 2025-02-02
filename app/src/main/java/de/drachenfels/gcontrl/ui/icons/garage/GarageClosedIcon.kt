package de.drachenfels.gcontrl.ui.icons.garage

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.drachenfels.gcontrl.ui.theme.GContrlTheme

@Composable
fun GarageClosedIcon(
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    Box(modifier = modifier) {
        Canvas(modifier = modifier.matchParentSize()) {
            val width = size.width
            val scale = width / 60f  // Viewbox scaling

            // Garagentor-Linien
            for (y in listOf(25f, 33f, 41f)) {
                drawLine(
                    color = primary,
                    start = Offset(14f * scale, y * scale),
                    end = Offset(46f * scale, y * scale),
                    strokeWidth = 6f * scale,
                    cap = StrokeCap.Round
                )
            }
        }
        GarageOutline(modifier = modifier.matchParentSize())
    }
}

@Preview
@Composable
fun PreviewGarageClosedIcon() {
    GContrlTheme {
        GarageClosedIcon(
            modifier = Modifier.size(64.dp)
        )
    }
}