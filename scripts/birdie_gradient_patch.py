from pathlib import Path

BACKDROP = r'''package com.dessalines.thumbkey.ui.components.keyboard

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
                0 -> emptyList()
                1 -> listOf(KeyboardGradientStop(0f, colors.first()))
                else -> colors.mapIndexed { index, color ->
                    KeyboardGradientStop(index.toFloat() / (colors.size - 1).toFloat(), color)
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
'''

ORIGINAL_WORKFLOW = '''name: Android build

on:
  pull_request:
    branches:
      - main
  push:
    branches:
      - "birdie/**"

jobs:
  build:
    runs-on: ubuntu-latest
    permissions:
      contents: read

    steps:
      - name: Check out repository
        uses: actions/checkout@v6

      - name: Set up JDK
        uses: actions/setup-java@v5
        with:
          distribution: temurin
          java-version: "21"
          cache: gradle

      - name: Set up Gradle
        uses: gradle/actions/setup-gradle@v6

      - name: Check Kotlin formatting
        run: ./gradlew lintKotlin

      - name: Android lint
        run: ./gradlew lint

      - name: Build debug APK
        run: ./gradlew assembleDebug

      - name: Upload debug APK
        uses: actions/upload-artifact@v6
        with:
          name: keywi-debug
          path: app/build/outputs/apk/debug/*.apk
'''


def require_replace(text: str, old: str, new: str, label: str, *, last: bool = False) -> str:
    if old not in text:
        raise SystemExit(f'{label} anchor not found')
    if not last:
        return text.replace(old, new, 1)
    index = text.rfind(old)
    return text[:index] + text[index:].replace(old, new, 1)


Path('app/src/main/java/com/dessalines/thumbkey/ui/components/keyboard/KeyboardBackdrop.kt').write_text(BACKDROP)

screen_path = Path('app/src/main/java/com/dessalines/thumbkey/ui/components/keyboard/KeyboardScreen.kt')
s = screen_path.read_text()
if 'import com.dessalines.thumbkey.BuildConfig\n' not in s:
    s = require_replace(
        s,
        'import com.dessalines.thumbkey.IMEService\n',
        'import com.dessalines.thumbkey.BuildConfig\nimport com.dessalines.thumbkey.IMEService\n',
        'BuildConfig import',
    )
s = require_replace(
    s,
    '''                modifier =
                    Modifier
                        .then(if (drawBackdrop) Modifier.background(backdropColor) else (Modifier))''',
    '''                modifier =
                    Modifier
                        .fillMaxWidth()
                        .then(if (drawBackdrop) Modifier.background(backdropColor) else (Modifier))''',
    'keyboard alignment',
)
s = require_replace(
    s,
    '''        val drawKeyboard = @Composable { alignment: Alignment, drawBackdrop: Boolean, positionPadding: Int ->
            val modifierPositionPadding =''',
    '''        val gradientCanvasWidth =
            keyboard.arr.maxOfOrNull { row ->
                row.sumOf { key -> (key.widthMultiplier * keyWidth).toDouble() }.toFloat()
            } ?: keyWidth
        val gradientCanvasHeight = keyboard.arr.size * keyHeight
        val keyGradient = if (BuildConfig.DEBUG) BIRDIE_KEY_GRADIENT else null

        val drawKeyboard = @Composable { alignment: Alignment, drawBackdrop: Boolean, positionPadding: Int ->
            val modifierPositionPadding =''',
    'drawKeyboard',
)
s = require_replace(
    s,
    '''                            row.forEachIndexed { j, key ->
                                Column {''',
    '''                            row.forEachIndexed { j, key ->
                                val gradientOffsetX =
                                    row.take(j).sumOf { previousKey ->
                                        (previousKey.widthMultiplier * keyWidth).toDouble()
                                    }.toFloat()
                                val gradientOffsetY = i * keyHeight
                                Column {''',
    'row key',
)
s = require_replace(
    s,
    '''                                        slideHoldEnabled = slideHoldEnabled,
                                    )''',
    '''                                        slideHoldEnabled = slideHoldEnabled,
                                        keyGradient = keyGradient,
                                        keyGradientCanvasWidth = gradientCanvasWidth,
                                        keyGradientCanvasHeight = gradientCanvasHeight,
                                        keyGradientOffsetX = gradientOffsetX,
                                        keyGradientOffsetY = gradientOffsetY,
                                    )''',
    'main KeyboardKey call',
    last=True,
)
screen_path.write_text(s)

key_path = Path('app/src/main/java/com/dessalines/thumbkey/ui/components/keyboard/KeyboardKey.kt')
k = key_path.read_text()
k = require_replace(
    k,
    '''    counterclockwiseDragAction: CircularDragAction,
    slideHoldEnabled: Boolean,
) {''',
    '''    counterclockwiseDragAction: CircularDragAction,
    slideHoldEnabled: Boolean,
    keyGradient: KeyboardBackdrop? = null,
    keyGradientCanvasWidth: Float = 0f,
    keyGradientCanvasHeight: Float = 0f,
    keyGradientOffsetX: Float = 0f,
    keyGradientOffsetY: Float = 0f,
) {''',
    'KeyboardKey signature',
)
k = require_replace(
    k,
    '''            ).background(color = backgroundColor)
            // Note: pointerInput has a delay when switching keyboards, so you must use this''',
    '''            ).then(
                if (!(isDragged.value || isPressed) && keyGradient != null) {
                    Modifier.keyboardGradientSlice(
                        backdrop = keyGradient,
                        canvasWidth = keyGradientCanvasWidth,
                        canvasHeight = keyGradientCanvasHeight,
                        offsetX = keyGradientOffsetX,
                        offsetY = keyGradientOffsetY,
                    )
                } else {
                    Modifier.background(color = backgroundColor)
                },
            )
            // Note: pointerInput has a delay when switching keyboards, so you must use this''',
    'KeyboardKey background',
)
key_path.write_text(k)

Path('.github/workflows/android-build.yml').write_text(ORIGINAL_WORKFLOW)
for temporary in (
    Path('.github/workflows/birdie-gradient-patch.yml'),
    Path('scripts/birdie_gradient_patch.py'),
):
    if temporary.exists():
        temporary.unlink()
