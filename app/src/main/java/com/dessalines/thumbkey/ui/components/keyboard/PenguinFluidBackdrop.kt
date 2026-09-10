package com.dessalines.thumbkey.ui.components.keyboard

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlin.math.cos
import kotlin.math.sin

/**
 * Phase-one Penguin backdrop.
 *
 * This is deliberately NOT a Navier-Stokes solver yet. It is a cheap, continuously animated,
 * touch-reactive field used to prove that keyboard input and a reactive visual surface can coexist
 * without either system owning the other's gestures.
 *
 * The real fluid solver can replace this composable once the interaction seam is proven.
 */
@Composable
fun PenguinFluidBackdrop(
    touchX: Float,
    touchY: Float,
    touchEnergy: Float,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "penguin-fluid")
    val phase =
        transition.animateFloat(
            initialValue = 0f,
            targetValue = (Math.PI * 2.0).toFloat(),
            animationSpec = infiniteRepeatable(tween(durationMillis = 9000), RepeatMode.Restart),
            label = "penguin-fluid-phase",
        ).value

    Canvas(modifier = modifier) {
        val w = size.width.coerceAtLeast(1f)
        val h = size.height.coerceAtLeast(1f)
        val minSide = minOf(w, h)

        // Deep-water base. The moving radial fields below intentionally overlap and interfere.
        drawRect(
            brush =
                Brush.linearGradient(
                    colors =
                        listOf(
                            Color(0xFF071B34),
                            Color(0xFF17315F),
                            Color(0xFF24124F),
                        ),
                    start = Offset.Zero,
                    end = Offset(w, h),
                ),
        )

        val orbitA =
            Offset(
                x = w * (0.50f + 0.31f * cos(phase * 0.73f)),
                y = h * (0.48f + 0.35f * sin(phase * 0.91f)),
            )
        val orbitB =
            Offset(
                x = w * (0.52f + 0.39f * cos(phase * 0.47f + 2.1f)),
                y = h * (0.50f + 0.30f * sin(phase * 0.63f + 1.4f)),
            )
        val orbitC =
            Offset(
                x = w * (0.50f + 0.27f * cos(phase * 0.39f + 4.0f)),
                y = h * (0.52f + 0.42f * sin(phase * 0.52f + 3.2f)),
            )

        fun glow(center: Offset, radius: Float, inner: Color) {
            drawCircle(
                brush =
                    Brush.radialGradient(
                        colors = listOf(inner, inner.copy(alpha = 0f)),
                        center = center,
                        radius = radius,
                    ),
                radius = radius,
                center = center,
            )
        }

        glow(orbitA, minSide * 0.82f, Color(0x9959F3FF))
        glow(orbitB, minSide * 0.94f, Color(0x887C4DFF))
        glow(orbitC, minSide * 0.72f, Color(0x88FF3BC8))

        // Finger disturbance. Coordinates arrive normalized from the root IME view, so this layer
        // never participates in hit-testing and therefore cannot eat keyboard gestures.
        val finger = Offset(touchX.coerceIn(0f, 1f) * w, touchY.coerceIn(0f, 1f) * h)
        val energy = touchEnergy.coerceIn(0f, 1f)
        if (energy > 0.01f) {
            val pulse = 0.86f + 0.14f * sin(phase * 4f)
            glow(
                finger,
                minSide * (0.30f + energy * 0.55f) * pulse,
                Color(0xCCB8FFFF).copy(alpha = 0.35f + energy * 0.45f),
            )
            glow(
                finger + Offset(minSide * 0.08f * cos(phase * 3f), minSide * 0.08f * sin(phase * 3f)),
                minSide * (0.18f + energy * 0.28f),
                Color(0xAAFF4FD8).copy(alpha = 0.25f + energy * 0.35f),
            )
        }
    }
}
