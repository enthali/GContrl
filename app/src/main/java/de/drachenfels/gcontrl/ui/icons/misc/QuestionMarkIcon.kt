package de.drachenfels.gcontrl.ui.icons.misc

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
fun QuestionMarkIcon(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    backgroundColor: Color = MaterialTheme.colorScheme.background
) {
    Canvas(modifier = modifier) {

        scale(size.width / 25f, size.height / 25f) {
            translate(size.width / 2f, size.height / 2f) {
                val questionMarkPath = Path().apply {
                    // Fragezeichen-Kurve

                    moveTo(-0.93f, 0.85f)  // 11.07-12, 12.85-12
                    cubicTo(-0.16f, -0.54f, 1.32f, -1.36f, 2.18f, -2.59f)
                    cubicTo(3.09f, -3.88f, 2.58f, -6.29f, 0f, -6.29f)
                    cubicTo(-1.69f, -6.29f, -2.52f, -5.01f, -2.87f, -3.95f)
                    lineTo(-5.46f, -5.04f)
                    cubicTo(-4.75f, -7.17f, -2.82f, -9f, -0.01f, -9f)
                    cubicTo(2.34f, -9f, 3.95f, -7.93f, 4.77f, -6.59f)
                    cubicTo(5.47f, -5.44f, 5.88f, -3.29f, 4.8f, -1.69f)
                    cubicTo(3.6f, 0.08f, 2.45f, 0.62f, 1.83f, 1.76f)
                    cubicTo(1.58f, 2.22f, 1.48f, 2.52f, 1.48f, 4f)
                    lineTo(-1.41f, 4f)
                    cubicTo(-1.42f, 3.22f, -1.54f, 1.95f, -0.93f, 0.85f)
                    close()

                    // Punkt unten
                    moveTo(2f, 8f)
                    cubicTo(2f, 9.1f, 1.1f, 10f, 0f, 10f)
                    cubicTo(-1.1f, 10f, -2f, 9.1f, -2f, 8f)
                    cubicTo(-2f, 6.9f, -1.1f, 6f, 0f, 6f)
                    cubicTo(1.1f, 6f, 2f, 6.9f, 2f, 8f)
                    close()
                }

                // Outline (Hintergrund)
                drawPath(
                    path = questionMarkPath, color = backgroundColor, style = Stroke(
                        width = 4f, cap = StrokeCap.Round, join = StrokeJoin.Round
                    )
                )

                // Füllung
                drawPath(
                    path = questionMarkPath, color = color, style = Fill
                )

                // Rand
                drawPath(
                    path = questionMarkPath, color = color, style = Stroke(
                        width = .5f, cap = StrokeCap.Round, join = StrokeJoin.Round
                    )
                )
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
private fun PreviewQuestionMarkIcon() {
    GContrlTheme {
        QuestionMarkIcon(
            modifier = Modifier.size(64.dp)
        )
    }
}