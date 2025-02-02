package de.drachenfels.gcontrl.ui.icons.garage

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.drachenfels.gcontrl.ui.theme.GContrlTheme

@Composable
fun GarageUnknownIcon(
    modifier: Modifier = Modifier
) {
    GarageOutline(modifier = modifier)

    val primary = MaterialTheme.colorScheme.primary
    val background = MaterialTheme.colorScheme.background

    Canvas(modifier = modifier) {
        scale(scale = 7f)
        {
            translate(
                left = size.width * 0.43f,
                top = size.height * 0.43f
            ) {// Fragezeichen zeichnen

                val questionMarkPath = Path().apply {
                    // Fragezeichen-Kurve
                    moveTo(11.07f, 12.85f)
                    cubicTo(
                        11.84f, 11.46f,
                        13.32f, 10.64f,
                        14.18f, 9.41f
                    )
                    cubicTo(
                        15.09f, 8.12f,
                        14.58f, 5.71f,
                        12f, 5.71f
                    )
                    cubicTo(
                        10.31f, 5.71f,
                        9.48f, 6.99f,
                        9.13f, 8.05f
                    )
                    lineTo(6.54f, 6.96f)
                    cubicTo(
                        7.25f, 4.83f,
                        9.18f, 3f,
                        11.99f, 3f
                    )
                    cubicTo(
                        14.34f, 3f,
                        15.95f, 4.07f,
                        16.77f, 5.41f
                    )
                    cubicTo(
                        17.47f, 6.56f,
                        17.88f, 8.71f,
                        16.8f, 10.31f
                    )
                    cubicTo(
                        15.6f, 12.08f,
                        14.45f, 12.62f,
                        13.83f, 13.76f
                    )
                    cubicTo(
                        13.58f, 14.22f,
                        13.48f, 14.52f,
                        13.48f, 16f
                    )
                    lineTo(10.59f, 16f)
                    cubicTo(
                        10.58f, 15.22f,
                        10.46f, 13.95f,
                        11.07f, 12.85f
                    )
                    close()

                    // Punkt unten
                    moveTo(14f, 20f)
                    cubicTo(
                        14f, 21.1f,
                        13.1f, 22f,
                        12f, 22f
                    )
                    cubicTo(
                        10.9f, 22f,
                        10f, 21.1f,
                        10f, 20f
                    )
                    cubicTo(
                        10f, 18.9f,
                        10.9f, 18f,
                        12f, 18f
                    )
                    cubicTo(
                        13.1f, 18f,
                        14f, 18.9f,
                        14f, 20f
                    )
                    close()
                }

                drawPath(
                    path = questionMarkPath,
                    color = background,
                    style = Stroke(
                        width = 4f,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )

                drawPath(
                    path = questionMarkPath,
                    color = primary,
                    style = Fill
                )

                drawPath(
                    path = questionMarkPath,
                    color = primary,
                    style = Stroke(
                        width = .5f,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
        }
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