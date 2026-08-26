package com.dessalines.thumbkey.ui.components.keyboard

import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Describes a keyboard backdrop without tying the renderer to a fixed number of colors.
 * Keeping the stops as a list lets the settings UI grow from two-color gradients to
 * gloriously excessive multi-stop gradients without changing the rendering API.
 */
data class KeyboardBackdrop(
    val colors: List<Color>,
    val angleDegrees: Float = 135f,
)

/** A deliberately loud proof-of-concept palette for the first BirdMachine build. */
val VIOLENTLY_GARISH_BACKDROP =
    KeyboardBackdrop(
        colors =
            listOf(
                Color(0xFFFF4FA3),
                Color(0xFFFFD84D),
                Color(0xFF58F5FF),
                Color(0xFF8C5CFF),
            ),
        angleDegrees = 135f,
    )

/**
 * Paint a linear gradient that can contain any number of color stops.
 *
 * The gradient is calculated from the composable's actual bounds, so every requested
 * angle spans the full keyboard instead of sampling only a small section of a huge brush.
 */
fun Modifier.keyboardGradientBackground(backdrop: KeyboardBackdrop): Modifier {
    if (backdrop.colors.isEmpty()) return this
    if (backdrop.colors.size == 1) return background(backdrop.colors.first())

    return drawWithCache {
        val radians = backdrop.angleDegrees * (PI / 180.0)
        val direction = Offset(cos(radians).toFloat(), sin(radians).toFloat())
        val center = Offset(size.width / 2f, size.height / 2f)
        val halfSpan =
            (abs(direction.x) * size.width / 2f) +
                (abs(direction.y) * size.height / 2f)
        val start = center - (direction * halfSpan)
        val end = center + (direction * halfSpan)
        val brush =
            Brush.linearGradient(
                colors = backdrop.colors,
                start = start,
                end = end,
            )

        onDrawBehind {
            drawRect(brush = brush)
        }
    }
}
