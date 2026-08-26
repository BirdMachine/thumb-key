package com.dessalines.thumbkey.ui.components.keyboard

import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlin.math.PI
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
 * The brush uses an effectively infinite line centred on the composable. Compose clips
 * the result to the element bounds, so this works without needing to know keyboard size.
 */
fun Modifier.keyboardGradientBackground(backdrop: KeyboardBackdrop): Modifier {
    if (backdrop.colors.isEmpty()) return this
    if (backdrop.colors.size == 1) return background(backdrop.colors.first())

    val radians = backdrop.angleDegrees * (PI / 180.0)
    val direction = Offset(cos(radians).toFloat(), sin(radians).toFloat())
    val extent = 10_000f

    return background(
        brush =
            Brush.linearGradient(
                colors = backdrop.colors,
                start = Offset(-direction.x * extent, -direction.y * extent),
                end = Offset(direction.x * extent, direction.y * extent),
            ),
    )
}
