package de.drachenfels.gcontrl.ui.icons.garage

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.drachenfels.gcontrl.ui.theme.GContrlTheme

@Composable
fun GarageOpeningIcon(
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary

    // Animation für die Öffnungsbewegung
    val infiniteTransition = rememberInfiniteTransition(label = "opening")
    val doorProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000),
            repeatMode = RepeatMode.Restart
        ),
        label = "door"
    )

    Box(modifier = modifier) {
        Canvas(modifier = modifier.matchParentSize()) {
            val width = size.width
            val scale = width / 60f

            // Basis Y-Positionen der Linien
            val basePositions = listOf(25f, 33f, 41f)

            // Bewegung nach oben: von Originalposition bis 20 Einheiten höher
            val offset = doorProgress * 20f

            // Zeichne die bewegten Linien
            for (baseY in basePositions) {
                val y = baseY - offset
                if (y >= 22f) { // Nur zeichnen, wenn die Linie noch sichtbar sein soll
                    drawLine(
                        color = primary,
                        start = Offset(14f * scale, y * scale),
                        end = Offset(46f * scale, y * scale),
                        strokeWidth = 6f * scale,
                        cap = StrokeCap.Round
                    )
                }
            }
        }
        GarageOutline(modifier = modifier.matchParentSize())
    }
}

@Composable
fun GarageClosingIcon(
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary

    // Animation für die Schließbewegung
    val infiniteTransition = rememberInfiniteTransition(label = "closing")
    val doorProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000),
            repeatMode = RepeatMode.Restart
        ),
        label = "door"
    )

    Box(modifier = modifier) {
        Canvas(modifier = modifier.matchParentSize()) {
            val width = size.width
            val scale = width / 60f

            // Basis Y-Positionen der Linien
            val basePositions = listOf(25f, 33f, 41f)

            // Bewegung nach unten: von 20 Einheiten höher bis Originalposition
            val offset = (1f - doorProgress) * 20f

            // Zeichne die bewegten Linien
            for (baseY in basePositions) {
                val y = baseY - offset
                if (y >= 22f) { // Nur zeichnen, wenn die Linie im sichtbaren Bereich ist
                    drawLine(
                        color = primary,
                        start = Offset(14f * scale, y * scale),
                        end = Offset(46f * scale, y * scale),
                        strokeWidth = 6f * scale,
                        cap = StrokeCap.Round
                    )
                }
            }
        }
        GarageOutline(modifier = modifier.matchParentSize())
    }
}

@Preview
@Composable
fun PreviewGarageOpeningIcon() {
    GContrlTheme {
        GarageOpeningIcon(
            modifier = Modifier.size(64.dp)
        )
    }
}

@Preview
@Composable
fun PreviewGarageClosingIcon() {
    GContrlTheme {
        GarageClosingIcon(
            modifier = Modifier.size(64.dp)
        )
    }
}