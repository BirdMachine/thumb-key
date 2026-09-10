package com.dessalines.thumbkey.ui.components.keyboard

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

private data class PenguinParcel(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var life: Float,
    val hueIndex: Int,
)

/**
 * Penguin's second-stage prototype: still cheap enough for an IME, but now motion has inertia.
 * Finger velocity creates advected dye parcels that continue travelling, curling and fading after
 * touch release. This is the bridge between the original reactive plasma proof and a real GPU
 * velocity / pressure / dye solver.
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
    val transition = rememberInfiniteTransition(label = "penguin-fluid")
    val phase =
        transition.animateFloat(
            initialValue = 0f,
            targetValue = (Math.PI * 2.0).toFloat(),
            animationSpec = infiniteRepeatable(tween(durationMillis = 9000), RepeatMode.Restart),
            label = "penguin-fluid-phase",
        ).value

    val parcels = remember { mutableStateListOf<PenguinParcel>() }
    var spawnClock by remember { androidx.compose.runtime.mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(16L)
            spawnClock++

            val iterator = parcels.listIterator()
            while (iterator.hasNext()) {
                val p = iterator.next()

                // Soft rotational flow field: enough curl to look like a substance instead of
                // ballistic confetti, without pretending this is incompressible fluid yet.
                val swirlX = sin((p.y + phase * 0.08f) * 10f) * 0.0008f
                val swirlY = cos((p.x - phase * 0.06f) * 9f) * 0.0008f
                p.vx = (p.vx + swirlX) * 0.985f
                p.vy = (p.vy + swirlY) * 0.985f
                p.x += p.vx
                p.y += p.vy
                p.life -= 0.0125f

                if (p.life <= 0f || p.x < -0.25f || p.x > 1.25f || p.y < -0.25f || p.y > 1.25f) {
                    iterator.remove()
                }
            }

            if (touchEnergy > 0.01f && parcels.size < 180 && spawnClock % 1 == 0) {
                repeat(3) { i ->
                    val spread = (i - 1) * 0.004f
                    parcels.add(
                        PenguinParcel(
                            x = touchX + spread,
                            y = touchY - spread,
                            vx = velocityX * 0.0045f + spread * 0.4f,
                            vy = velocityY * 0.0045f - spread * 0.4f,
                            life = 1f,
                            hueIndex = (spawnClock + i) % 3,
                        ),
                    )
                }
            }
        }
    }

    Canvas(modifier = modifier) {
        val w = size.width.coerceAtLeast(1f)
        val h = size.height.coerceAtLeast(1f)
        val minSide = minOf(w, h)

        drawRect(
            brush =
                Brush.linearGradient(
                    colors =
                        listOf(
                            Color(0xFF06182F),
                            Color(0xFF102B55),
                            Color(0xFF251343),
                        ),
                    start = Offset.Zero,
                    end = Offset(w, h),
                ),
        )

        // Slow ambient currents remain underneath the dye so the surface never feels dead.
        val orbitA = Offset(w * (0.5f + 0.28f * cos(phase * 0.63f)), h * (0.5f + 0.34f * sin(phase * 0.82f)))
        val orbitB = Offset(w * (0.5f + 0.36f * cos(phase * 0.42f + 2.2f)), h * (0.5f + 0.28f * sin(phase * 0.59f + 1.3f)))

        fun glow(center: Offset, radius: Float, inner: Color) {
            drawCircle(
                brush = Brush.radialGradient(listOf(inner, inner.copy(alpha = 0f)), center, radius),
                radius = radius,
                center = center,
            )
        }

        glow(orbitA, minSide * 0.95f, Color(0x6659E8FF))
        glow(orbitB, minSide * 0.88f, Color(0x557D50FF))

        parcels.forEach { p ->
            val center = Offset(p.x * w, p.y * h)
            val life = p.life.coerceIn(0f, 1f)
            val color =
                when (p.hueIndex) {
                    0 -> Color(0xFF64F4FF)
                    1 -> Color(0xFFFF4FD2)
                    else -> Color(0xFF9A72FF)
                }
            val radius = minSide * (0.06f + 0.16f * life)
            glow(center, radius, color.copy(alpha = 0.12f + 0.42f * life))

            // A dimmer, stretched-looking wake behind each parcel sells directional advection.
            val wake = Offset(center.x - p.vx * w * 10f, center.y - p.vy * h * 10f)
            glow(wake, radius * 0.72f, color.copy(alpha = 0.08f + 0.24f * life))
        }

        if (touchEnergy > 0.01f) {
            val finger = Offset(touchX.coerceIn(0f, 1f) * w, touchY.coerceIn(0f, 1f) * h)
            val speed = (kotlin.math.abs(velocityX) + kotlin.math.abs(velocityY)).coerceIn(0f, 4f)
            glow(
                finger,
                minSide * (0.20f + touchEnergy * 0.18f + speed * 0.025f),
                Color(0xFFCBFFFF).copy(alpha = 0.30f + touchEnergy * 0.35f),
            )
        }
    }
}
