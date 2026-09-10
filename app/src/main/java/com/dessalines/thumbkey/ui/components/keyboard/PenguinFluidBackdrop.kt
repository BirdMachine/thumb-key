package com.dessalines.thumbkey.ui.components.keyboard

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.delay
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

private const val PENGUIN_GRID_W = 48
private const val PENGUIN_GRID_H = 24
private const val PENGUIN_PRESSURE_ITERATIONS = 12

/**
 * Tiny CPU Stable-Fluids-style field used as Penguin's first genuinely fluid simulation.
 *
 * The grid is deliberately low resolution. Velocity is advected, projected toward a
 * divergence-free field with a Jacobi pressure solve, then RGB dye is advected through the
 * resulting velocity field. The renderer scales the cells over the keyboard and lets the
 * gradients/glow hide the intentionally chunky simulation resolution.
 *
 * This is a correctness/feel milestone before moving the same passes to GPU textures/shaders.
 */
private class PenguinFluidGrid(
    private val width: Int = PENGUIN_GRID_W,
    private val height: Int = PENGUIN_GRID_H,
) {
    private val count = width * height

    private var velocityX = FloatArray(count)
    private var velocityY = FloatArray(count)
    private var velocityXPrev = FloatArray(count)
    private var velocityYPrev = FloatArray(count)

    private var dyeR = FloatArray(count)
    private var dyeG = FloatArray(count)
    private var dyeB = FloatArray(count)
    private var dyeRPrev = FloatArray(count)
    private var dyeGPrev = FloatArray(count)
    private var dyeBPrev = FloatArray(count)

    private val divergence = FloatArray(count)
    private var pressure = FloatArray(count)
    private var pressureNext = FloatArray(count)

    private fun index(x: Int, y: Int): Int = y * width + x

    private fun clampX(x: Int): Int = x.coerceIn(0, width - 1)

    private fun clampY(y: Int): Int = y.coerceIn(0, height - 1)

    private fun sample(field: FloatArray, x: Float, y: Float): Float {
        val clampedX = x.coerceIn(0f, (width - 1).toFloat())
        val clampedY = y.coerceIn(0f, (height - 1).toFloat())
        val x0 = floor(clampedX).toInt()
        val y0 = floor(clampedY).toInt()
        val x1 = min(x0 + 1, width - 1)
        val y1 = min(y0 + 1, height - 1)
        val tx = clampedX - x0
        val ty = clampedY - y0

        val a = field[index(x0, y0)] * (1f - tx) + field[index(x1, y0)] * tx
        val b = field[index(x0, y1)] * (1f - tx) + field[index(x1, y1)] * tx
        return a * (1f - ty) + b * ty
    }

    fun inject(
        normalizedX: Float,
        normalizedY: Float,
        normalizedVelocityX: Float,
        normalizedVelocityY: Float,
        energy: Float,
        huePhase: Int,
    ) {
        val centerX = normalizedX.coerceIn(0f, 1f) * (width - 1)
        val centerY = normalizedY.coerceIn(0f, 1f) * (height - 1)
        val radius = 3.2f + energy * 1.8f
        val radiusSq = radius * radius

        // Screen-widths/second -> grid-cells/second. Keep enough force for decisive wakes without
        // letting one flick launch the simulation numerically into orbit.
        val forceX = normalizedVelocityX.coerceIn(-4f, 4f) * width * 0.42f
        val forceY = normalizedVelocityY.coerceIn(-4f, 4f) * height * 0.42f

        val palette =
            when ((huePhase / 18) % 3) {
                0 -> floatArrayOf(0.20f, 1.00f, 1.00f) // cyan
                1 -> floatArrayOf(1.00f, 0.16f, 0.78f) // hot pink
                else -> floatArrayOf(0.55f, 0.30f, 1.00f) // violet
            }

        val minX = max(0, floor(centerX - radius).toInt())
        val maxX = min(width - 1, floor(centerX + radius).toInt())
        val minY = max(0, floor(centerY - radius).toInt())
        val maxY = min(height - 1, floor(centerY + radius).toInt())

        for (y in minY..maxY) {
            for (x in minX..maxX) {
                val dx = x - centerX
                val dy = y - centerY
                val distSq = dx * dx + dy * dy
                if (distSq > radiusSq) continue

                val falloff = (1f - distSq / radiusSq).coerceIn(0f, 1f)
                val i = index(x, y)
                velocityX[i] += forceX * falloff
                velocityY[i] += forceY * falloff

                val dyeAmount = (0.10f + energy * 0.20f) * falloff
                dyeR[i] = (dyeR[i] + palette[0] * dyeAmount).coerceAtMost(1.5f)
                dyeG[i] = (dyeG[i] + palette[1] * dyeAmount).coerceAtMost(1.5f)
                dyeB[i] = (dyeB[i] + palette[2] * dyeAmount).coerceAtMost(1.5f)
            }
        }
    }

    fun step(dtSeconds: Float) {
        val dt = dtSeconds.coerceIn(0.008f, 0.033f)
        advectVelocity(dt)
        projectVelocity()
        advectDye(dt)
        dissipate()
    }

    private fun advectVelocity(dt: Float) {
        velocityX.copyInto(velocityXPrev)
        velocityY.copyInto(velocityYPrev)

        for (y in 0 until height) {
            for (x in 0 until width) {
                val i = index(x, y)
                val backX = x - velocityXPrev[i] * dt
                val backY = y - velocityYPrev[i] * dt
                velocityX[i] = sample(velocityXPrev, backX, backY)
                velocityY[i] = sample(velocityYPrev, backX, backY)
            }
        }
    }

    private fun projectVelocity() {
        pressure.fill(0f)
        pressureNext.fill(0f)

        for (y in 0 until height) {
            for (x in 0 until width) {
                val left = index(clampX(x - 1), y)
                val right = index(clampX(x + 1), y)
                val up = index(x, clampY(y - 1))
                val down = index(x, clampY(y + 1))
                val i = index(x, y)
                divergence[i] =
                    -0.5f *
                        ((velocityX[right] - velocityX[left]) +
                            (velocityY[down] - velocityY[up]))
            }
        }

        repeat(PENGUIN_PRESSURE_ITERATIONS) {
            for (y in 0 until height) {
                for (x in 0 until width) {
                    val left = pressure[index(clampX(x - 1), y)]
                    val right = pressure[index(clampX(x + 1), y)]
                    val up = pressure[index(x, clampY(y - 1))]
                    val down = pressure[index(x, clampY(y + 1))]
                    pressureNext[index(x, y)] = (divergence[index(x, y)] + left + right + up + down) * 0.25f
                }
            }
            val swap = pressure
            pressure = pressureNext
            pressureNext = swap
        }

        for (y in 0 until height) {
            for (x in 0 until width) {
                val i = index(x, y)
                val left = pressure[index(clampX(x - 1), y)]
                val right = pressure[index(clampX(x + 1), y)]
                val up = pressure[index(x, clampY(y - 1))]
                val down = pressure[index(x, clampY(y + 1))]
                velocityX[i] -= 0.5f * (right - left)
                velocityY[i] -= 0.5f * (down - up)
            }
        }

        // No-through boundary: liquid can slide along the edge instead of escaping the keyboard.
        for (x in 0 until width) {
            velocityY[index(x, 0)] = 0f
            velocityY[index(x, height - 1)] = 0f
        }
        for (y in 0 until height) {
            velocityX[index(0, y)] = 0f
            velocityX[index(width - 1, y)] = 0f
        }
    }

    private fun advectDye(dt: Float) {
        dyeR.copyInto(dyeRPrev)
        dyeG.copyInto(dyeGPrev)
        dyeB.copyInto(dyeBPrev)

        for (y in 0 until height) {
            for (x in 0 until width) {
                val i = index(x, y)
                val backX = x - velocityX[i] * dt
                val backY = y - velocityY[i] * dt
                dyeR[i] = sample(dyeRPrev, backX, backY)
                dyeG[i] = sample(dyeGPrev, backX, backY)
                dyeB[i] = sample(dyeBPrev, backX, backY)
            }
        }
    }

    private fun dissipate() {
        for (i in 0 until count) {
            velocityX[i] *= 0.992f
            velocityY[i] *= 0.992f
            dyeR[i] *= 0.9935f
            dyeG[i] *= 0.9935f
            dyeB[i] *= 0.9935f
        }
    }

    fun red(x: Int, y: Int): Float = dyeR[index(x, y)]

    fun green(x: Int, y: Int): Float = dyeG[index(x, y)]

    fun blue(x: Int, y: Int): Float = dyeB[index(x, y)]

    fun speed(x: Int, y: Int): Float {
        val i = index(x, y)
        return sqrt(velocityX[i] * velocityX[i] + velocityY[i] * velocityY[i])
    }
}

/**
 * Penguin's first actual fluid-field backdrop.
 *
 * Touches inject both momentum and RGB dye into a projected velocity grid. Keywi still owns the
 * MotionEvents; Penguin merely receives normalized touch data from the root IME view.
 */
@Composable
fun PenguinFluidBackdrop(
    touchX: Float,
    touchY: Float,
    touchEnergy: Float,
    velocityX: Float,
    velocityY: Float,
    modifier: Modifier = Modifier,
) {
    val grid = remember { PenguinFluidGrid() }
    val currentTouchX by rememberUpdatedState(touchX)
    val currentTouchY by rememberUpdatedState(touchY)
    val currentTouchEnergy by rememberUpdatedState(touchEnergy)
    val currentVelocityX by rememberUpdatedState(velocityX)
    val currentVelocityY by rememberUpdatedState(velocityY)
    var frame by remember { mutableIntStateOf(0) }

    LaunchedEffect(grid) {
        var tick = 0
        while (true) {
            delay(16L)
            if (currentTouchEnergy > 0.01f) {
                grid.inject(
                    normalizedX = currentTouchX,
                    normalizedY = currentTouchY,
                    normalizedVelocityX = currentVelocityX,
                    normalizedVelocityY = currentVelocityY,
                    energy = currentTouchEnergy,
                    huePhase = tick,
                )
            }
            grid.step(0.016f)
            tick++
            frame++
        }
    }

    // Reading frame here intentionally invalidates this composable after every simulation step.
    // The value itself is not visually meaningful; the grid arrays are the render state.
    val renderFrame = frame

    Canvas(modifier = modifier) {
        if (renderFrame < 0) return@Canvas

        val w = size.width.coerceAtLeast(1f)
        val h = size.height.coerceAtLeast(1f)
        val cellW = w / PENGUIN_GRID_W
        val cellH = h / PENGUIN_GRID_H

        drawRect(
            brush =
                Brush.linearGradient(
                    colors = listOf(Color(0xFF031224), Color(0xFF10284A), Color(0xFF200F3D)),
                    start = Offset.Zero,
                    end = Offset(w, h),
                ),
        )

        for (y in 0 until PENGUIN_GRID_H) {
            for (x in 0 until PENGUIN_GRID_W) {
                val r = grid.red(x, y)
                val g = grid.green(x, y)
                val b = grid.blue(x, y)
                val dye = max(r, max(g, b))
                if (dye < 0.006f) continue

                val speedGlow = (grid.speed(x, y) / 18f).coerceIn(0f, 0.28f)
                val alpha = (dye * 0.72f + speedGlow).coerceIn(0f, 0.92f)
                val maxChannel = max(0.001f, max(r, max(g, b)))
                val color =
                    Color(
                        red = (r / maxChannel).coerceIn(0f, 1f),
                        green = (g / maxChannel).coerceIn(0f, 1f),
                        blue = (b / maxChannel).coerceIn(0f, 1f),
                        alpha = alpha,
                    )

                // Slight overlap avoids hairline seams when scaled to odd device widths.
                drawRect(
                    color = color,
                    topLeft = Offset(x * cellW - 0.5f, y * cellH - 0.5f),
                    size = Size(cellW + 1f, cellH + 1f),
                )
            }
        }

        if (touchEnergy > 0.01f) {
            val finger = Offset(touchX.coerceIn(0f, 1f) * w, touchY.coerceIn(0f, 1f) * h)
            val radius = min(w, h) * (0.11f + touchEnergy * 0.08f)
            drawCircle(
                brush =
                    Brush.radialGradient(
                        listOf(Color(0x55D7FFFF), Color.Transparent),
                        center = finger,
                        radius = radius,
                    ),
                radius = radius,
                center = finger,
            )
        }
    }
}
