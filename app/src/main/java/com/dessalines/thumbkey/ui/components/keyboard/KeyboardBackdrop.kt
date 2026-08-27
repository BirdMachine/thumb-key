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

/** A positioned color stop in a keyboard-space gradient. */
data class KeyboardGradientStop(
    val position: Float,
    val color: Color,
)

/**
 * Describes a keyboard gradient without tying the renderer to a fixed number of colors.
 * Explicit stop positions make the same model usable by a future draggable-stop editor.
 */
data class KeyboardBackdrop(
    val stops: List<KeyboardGradientStop>,
    val angleDegrees: Float = 135f,
) {
    constructor(
        colors: List<Color>,
        angleDegrees: Float = 135f,
    ) : this(
        stops =
            when (colors.size) {
                0 -> {
                    emptyList()
                }

                1 -> {
                    listOf(KeyboardGradientStop(0f, colors.first()))
                }

                else -> {
                    colors.mapIndexed { index, color ->
                        KeyboardGradientStop(index.toFloat() / (colors.size - 1).toFloat(), color)
                    }
                }
            },
        angleDegrees = angleDegrees,
    )
}

/** Birdie's original four-stop rainbow. Keep this preset exactly as-is. */
val BIRDIE_RAINBOW_BACKDROP =
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

/** Backwards-compatible name used by the first experimental builds. */
val VIOLENTLY_GARISH_BACKDROP = BIRDIE_RAINBOW_BACKDROP

/** A cyclic sinebow sampled densely enough to stay silky across a keyboard. */
val SINEBOW_BACKDROP =
    KeyboardBackdrop(
        colors =
            listOf(
                Color(0xFF00BFBF),
                Color(0xFF11EE80),
                Color(0xFF40FF40),
                Color(0xFF7FEE11),
                Color(0xFFBFBF00),
                Color(0xFFEE8011),
                Color(0xFFFF4040),
                Color(0xFFEE117F),
                Color(0xFFBF00BF),
                Color(0xFF8011EE),
                Color(0xFF4040FF),
                Color(0xFF117FEE),
                Color(0xFF00BFBF),
            ),
        angleDegrees = 0f,
    )

/** Initial shared black/grey key surface; deliberately editable as positioned stops. */
val BIRDIE_KEY_GRADIENT =
    KeyboardBackdrop(
        stops =
            listOf(
                KeyboardGradientStop(0f, Color(0xFF090A0F)),
                KeyboardGradientStop(0.34f, Color(0xFF242631)),
                KeyboardGradientStop(0.67f, Color(0xFF555866)),
                KeyboardGradientStop(1f, Color(0xFF151720)),
            ),
        angleDegrees = 22f,
    )

private fun KeyboardBackdrop.sortedStops(): List<KeyboardGradientStop> =
    stops
        .map { it.copy(position = it.position.coerceIn(0f, 1f)) }
        .sortedBy { it.position }

private fun gradientEndpoints(
    width: Float,
    height: Float,
    angleDegrees: Float,
): Pair<Offset, Offset> {
    val radians = angleDegrees * (PI / 180.0)
    val direction = Offset(cos(radians).toFloat(), sin(radians).toFloat())
    val center = Offset(width / 2f, height / 2f)
    val halfSpan =
        (abs(direction.x) * width / 2f) +
            (abs(direction.y) * height / 2f)
    return Pair(center - (direction * halfSpan), center + (direction * halfSpan))
}

private fun KeyboardBackdrop.brush(
    width: Float,
    height: Float,
    offset: Offset = Offset.Zero,
): Brush? {
    val orderedStops = sortedStops()
    if (orderedStops.isEmpty()) return null
    if (orderedStops.size == 1) {
        return Brush.linearGradient(listOf(orderedStops.first().color, orderedStops.first().color))
    }

    val (globalStart, globalEnd) = gradientEndpoints(width, height, angleDegrees)
    val localStart = globalStart - offset
    val localEnd = globalEnd - offset
    val colorStops = orderedStops.map { it.position to it.color }.toTypedArray()
    return Brush.linearGradient(
        colorStops = colorStops,
        start = localStart,
        end = localEnd,
    )
}

/** Paint a gradient across this composable's complete bounds. */
fun Modifier.keyboardGradientBackground(backdrop: KeyboardBackdrop): Modifier {
    val orderedStops = backdrop.sortedStops()
    if (orderedStops.isEmpty()) return this
    if (orderedStops.size == 1) return background(orderedStops.first().color)

    return drawWithCache {
        val brush = backdrop.brush(size.width, size.height)
        onDrawBehind {
            brush?.let { drawRect(brush = it) }
        }
    }
}

/**
 * Paint only this key's slice of a larger keyboard-space gradient.
 * Every key receives the same virtual canvas and a different offset, so the keys read as
 * cut-outs from one continuous sheet while the gaps remain available to the backdrop below.
 */
fun Modifier.keyboardGradientSlice(
    backdrop: KeyboardBackdrop,
    canvasWidth: Float,
    canvasHeight: Float,
    offsetX: Float,
    offsetY: Float,
): Modifier =
    drawWithCache {
        val brush =
            backdrop.brush(
                width = canvasWidth.coerceAtLeast(size.width),
                height = canvasHeight.coerceAtLeast(size.height),
                offset = Offset(offsetX, offsetY),
            )
        onDrawBehind {
            brush?.let { drawRect(brush = it) }
        }
    }
