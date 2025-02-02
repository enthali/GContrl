package de.drachenfels.gcontrl.ui.icons.garage

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.drachenfels.gcontrl.ui.theme.GContrlTheme

// TODO: refactor from xml resources to composeable für garage door visualistaion
@Composable
fun GarageOutline(
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val scale = width / 60f  // Viewbox scaling


        // Haus-Umriss
        val outlinePath = Path().apply {
            moveTo(5f * scale, 19f * scale)
            lineTo(30f * scale, 5f * scale)
            lineTo(55f * scale, 19f * scale)
            lineTo(55f * scale, 50f * scale)
            lineTo(5f * scale, 50f * scale)
            close()
        }
        drawPath(
            path = outlinePath,
            color = primary,
            style = Stroke(width = 8f * scale, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

@Preview
@Composable
fun PreviewGarageOutlineIcon() {
    GContrlTheme {
        GarageOutline(
            modifier = Modifier.size(64.dp),
        )
    }
}      