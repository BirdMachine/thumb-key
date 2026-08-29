package com.dessalines.thumbkey.fishwi

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/**
 * Fishwi is intentionally isolated from the keyboard engine. The keyboard only emits tiny
 * interaction events; the aquarium owns simulation, rendering and persistence.
 */
sealed interface FishwiEvent {
    data class Tap(val x: Float, val y: Float) : FishwiEvent
    data class Swipe(val dx: Float, val dy: Float, val y: Float) : FishwiEvent
    data object Space : FishwiEvent
    data object Enter : FishwiEvent
    data object Backspace : FishwiEvent
}

object FishwiEvents {
    val events = MutableSharedFlow<FishwiEvent>(extraBufferCapacity = 96)
    fun emit(event: FishwiEvent) { events.tryEmit(event) }
}

/** Observes keyboard touches without consuming them. */
fun Modifier.fishwiInputObserver(): Modifier = pointerInput(Unit) {
    var start = Offset.Zero
    var last = Offset.Zero
    var tracking = false
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            val change = event.changes.firstOrNull() ?: continue
            if (change.pressed && !change.previousPressed) {
                start = change.position
                last = start
                tracking = true
            } else if (change.pressed && tracking) {
                last = change.position
            } else if (!change.pressed && change.previousPressed && tracking) {
                last = change.position
                val w = size.width.coerceAtLeast(1).toFloat()
                val h = size.height.coerceAtLeast(1).toFloat()
                val dx = (last.x - start.x) / w
                val dy = (last.y - start.y) / h
                val y = (start.y / h).coerceIn(0f, 1f)
                if (abs(dx) + abs(dy) < 0.035f) {
                    FishwiEvents.emit(FishwiEvent.Tap((start.x / w).coerceIn(0f, 1f), y))
                } else {
                    FishwiEvents.emit(FishwiEvent.Swipe(dx, dy, y))
                }
                tracking = false
            }
        }
    }
}

data class FishPersonality(
    val greed: Float,
    val curiosity: Float,
    val laziness: Float,
    val sociability: Float,
    val preferredDepth: Float,
)

private class Fish(
    val id: Int,
    val name: String,
    val palette: Int,
    val personality: FishPersonality,
    x: Float,
    y: Float,
) {
    var x by mutableFloatStateOf(x)
    var y by mutableFloatStateOf(y)
    var vx by mutableFloatStateOf(if (Random.nextBoolean()) 0.045f else -0.045f)
    var vy by mutableFloatStateOf(0f)
    var hunger by mutableFloatStateOf(0.3f)
    var happiness by mutableFloatStateOf(0.65f)
    var growth by mutableFloatStateOf(0.15f)
    var experience by mutableIntStateOf(0)
    var backwardsUntil by mutableLongStateOf(0L)
    var napUntil by mutableLongStateOf(0L)
    var targetFoodId by mutableIntStateOf(-1)
}

private class Food(
    val id: Int,
    x: Float,
    y: Float,
    vx: Float,
    vy: Float,
) {
    var x by mutableFloatStateOf(x)
    var y by mutableFloatStateOf(y)
    var vx by mutableFloatStateOf(vx)
    var vy by mutableFloatStateOf(vy)
    var age by mutableFloatStateOf(0f)
}

private class Bubble(
    x: Float,
    y: Float,
    radius: Float,
) {
    var x by mutableFloatStateOf(x)
    var y by mutableFloatStateOf(y)
    val radius = radius
}

private object FishwiStore {
    private const val PREFS = "fishwi_aquarium_v1"
    private const val LAST_SEEN = "last_seen"
    private const val INTERACTIONS = "interactions"

    fun load(context: Context): Pair<List<Fish>, Int> {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val offlineHours = ((now - p.getLong(LAST_SEEN, now)).coerceAtLeast(0L) / 3_600_000f).coerceAtMost(24f)
        val defaults = defaultFish()
        val fish = defaults.map { f ->
            f.apply {
                x = p.getFloat("f_${id}_x", x).coerceIn(0.08f, 0.92f)
                y = p.getFloat("f_${id}_y", y).coerceIn(0.12f, 0.88f)
                hunger = (p.getFloat("f_${id}_hunger", hunger) + offlineHours * 0.025f).coerceIn(0f, 0.82f)
                happiness = (p.getFloat("f_${id}_happy", happiness) - offlineHours * 0.004f).coerceIn(0.25f, 1f)
                growth = p.getFloat("f_${id}_growth", growth).coerceIn(0f, 1f)
                experience = p.getInt("f_${id}_xp", experience)
            }
        }
        return fish to p.getInt(INTERACTIONS, 0)
    }

    fun save(context: Context, fish: List<Fish>, interactions: Int) {
        val e = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
        e.putLong(LAST_SEEN, System.currentTimeMillis())
        e.putInt(INTERACTIONS, interactions)
        fish.forEach { f ->
            e.putFloat("f_${f.id}_x", f.x)
            e.putFloat("f_${f.id}_y", f.y)
            e.putFloat("f_${f.id}_hunger", f.hunger)
            e.putFloat("f_${f.id}_happy", f.happiness)
            e.putFloat("f_${f.id}_growth", f.growth)
            e.putInt("f_${f.id}_xp", f.experience)
        }
        e.apply()
    }

    private fun defaultFish(): List<Fish> = listOf(
        Fish(0, "Pixel", 0, FishPersonality(.92f, .78f, .10f, .55f, .35f), .35f, .32f),
        Fish(1, "Miso", 1, FishPersonality(.45f, .96f, .20f, .82f, .55f), .62f, .52f),
        Fish(2, "Dot", 2, FishPersonality(.35f, .40f, .82f, .30f, .73f), .45f, .72f),
    )
}

@Composable
fun FishwiAquarium(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val loaded = remember { FishwiStore.load(context) }
    val fish = remember { mutableStateListOf<Fish>().also { it.addAll(loaded.first) } }
    val food = remember { mutableStateListOf<Food>() }
    val bubbles = remember { mutableStateListOf<Bubble>() }
    var interactions by remember { mutableIntStateOf(loaded.second) }
    var nextFoodId by remember { mutableIntStateOf(1) }
    var shimmer by remember { mutableFloatStateOf(0f) }

    fun addFood(y: Float, count: Int = 1, horizontalKick: Float = 0f) {
        repeat(count.coerceAtMost(8)) {
            if (food.size >= 24) return@repeat
            food += Food(
                id = nextFoodId++,
                x = (0.50f + Random.nextFloat() * .20f - .10f).coerceIn(.08f, .92f),
                y = (y * .78f + .04f).coerceIn(.04f, .78f),
                vx = horizontalKick * .16f + (Random.nextFloat() - .5f) * .025f,
                vy = .025f + Random.nextFloat() * .015f,
            )
        }
    }

    LaunchedEffect(Unit) {
        FishwiEvents.events.collectLatest { event ->
            interactions++
            when (event) {
                is FishwiEvent.Tap -> addFood(event.y)
                is FishwiEvent.Swipe -> addFood(event.y, 3, event.dx)
                FishwiEvent.Space -> repeat(5) { bubbles += Bubble(.10f + Random.nextFloat() * .18f, .94f, .009f + Random.nextFloat() * .012f) }
                FishwiEvent.Enter -> repeat(8) { bubbles += Bubble(.05f + Random.nextFloat() * .90f, .98f, .008f + Random.nextFloat() * .014f) }
                FishwiEvent.Backspace -> fish.forEach { it.backwardsUntil = System.currentTimeMillis() + 700L }
            }
            fish.forEach {
                it.happiness = (it.happiness + .006f).coerceAtMost(1f)
                it.experience += 1
                it.growth = min(1f, it.growth + .00025f)
            }
        }
    }

    LaunchedEffect(Unit) {
        var ticks = 0
        while (true) {
            delay(50)
            val dt = .05f
            val now = System.currentTimeMillis()
            shimmer = (shimmer + dt * .18f) % 1f

            food.toList().forEach { pellet ->
                pellet.age += dt
                pellet.vy = min(.11f, pellet.vy + .018f * dt)
                pellet.vx *= .992f
                pellet.x = (pellet.x + pellet.vx * dt).coerceIn(.03f, .97f)
                pellet.y += pellet.vy * dt
                if (pellet.y > .92f || pellet.age > 12f) food.remove(pellet)
            }

            bubbles.toList().forEach { b ->
                b.y -= .12f * dt
                b.x += kotlin.math.sin((b.y + shimmer) * 18f) * .0012f
                if (b.y < .02f) bubbles.remove(b)
            }
            if (bubbles.size < 9 && Random.nextFloat() < .045f) {
                bubbles += Bubble(.06f + Random.nextFloat() * .88f, .97f, .006f + Random.nextFloat() * .010f)
            }

            fish.forEachIndexed { index, f ->
                f.hunger = min(.92f, f.hunger + dt * .0007f)

                val closest = food.minByOrNull { pellet ->
                    val dx = pellet.x - f.x
                    val dy = pellet.y - f.y
                    dx * dx + dy * dy
                }
                val noticesFood = closest != null && (f.hunger * .65f + f.personality.greed * .55f) > .42f
                if (noticesFood && closest != null) {
                    f.targetFoodId = closest.id
                    val dx = closest.x - f.x
                    val dy = closest.y - f.y
                    f.vx += dx * (.19f + f.personality.greed * .12f) * dt
                    f.vy += dy * .22f * dt
                    if (abs(dx) < .055f && abs(dy) < .06f) {
                        food.remove(closest)
                        f.hunger = max(0f, f.hunger - .25f)
                        f.happiness = min(1f, f.happiness + .05f)
                        f.experience += 12
                        f.growth = min(1f, f.growth + .004f)
                        f.targetFoodId = -1
                    }
                } else {
                    f.targetFoodId = -1
                    val depthPull = f.personality.preferredDepth - f.y
                    f.vy += depthPull * .022f * dt
                    if (Random.nextFloat() < .012f * (1f - f.personality.laziness * .65f)) {
                        f.vx += (Random.nextFloat() - .5f) * .035f
                        f.vy += (Random.nextFloat() - .5f) * .025f
                    }
                    if (f.personality.sociability > .65f && fish.size > 1) {
                        val buddy = fish[(index + 1) % fish.size]
                        f.vx += (buddy.x - f.x) * .006f * dt
                    }
                }

                val speedLimit = .055f + f.personality.curiosity * .045f
                f.vx = f.vx.coerceIn(-speedLimit, speedLimit)
                f.vy = f.vy.coerceIn(-.055f, .055f)
                f.vx *= .996f
                f.vy *= .992f

                val reverse = now < f.backwardsUntil
                f.x += (if (reverse) -f.vx else f.vx) * dt
                f.y += f.vy * dt
                if (f.x < .07f) { f.x = .07f; f.vx = abs(f.vx) }
                if (f.x > .93f) { f.x = .93f; f.vx = -abs(f.vx) }
                if (f.y < .10f) { f.y = .10f; f.vy = abs(f.vy) }
                if (f.y > .88f) { f.y = .88f; f.vy = -abs(f.vy) }
            }

            ticks++
            if (ticks % 200 == 0) FishwiStore.save(context, fish, interactions)
        }
    }

    DisposableEffect(Unit) {
        onDispose { FishwiStore.save(context, fish, interactions) }
    }

    Canvas(modifier = modifier.fillMaxWidth().fillMaxHeight()) {
        drawTank(shimmer)
        food.forEach { pellet -> drawFood(pellet.x, pellet.y) }
        bubbles.forEach { bubble -> drawBubble(bubble.x, bubble.y, bubble.radius) }
        fish.forEach { f -> drawFish(f) }
    }
}

private fun DrawScope.drawTank(shimmer: Float) {
    drawRect(Color(0xE8103440))
    drawRect(Color(0xAA185C68), topLeft = Offset(0f, size.height * .04f), size = Size(size.width, size.height * .70f))
    drawRect(Color(0xFF163C3D), topLeft = Offset(0f, size.height * .88f), size = Size(size.width, size.height * .12f))

    // Pixel gravel
    val step = max(6f, size.width / 24f)
    var x = 0f
    var i = 0
    while (x < size.width) {
        val h = if (i % 3 == 0) step * .75f else step * .45f
        drawRect(
            if (i % 2 == 0) Color(0xFF6D6B55) else Color(0xFF8D775C),
            topLeft = Offset(x, size.height * .91f - h),
            size = Size(step + 1f, h),
        )
        x += step
        i++
    }

    // Plants: chunky pixel stems/leaves
    fun plant(px: Float, base: Float, tall: Float, c: Color) {
        val stem = max(3f, size.width / 65f)
        drawRect(c, Offset(px, base - tall), Size(stem, tall))
        repeat(4) { n ->
            val yy = base - tall * (.18f + n * .19f)
            val side = if (n % 2 == 0) -1f else 1f
            drawRect(c, Offset(px + side * stem * 2f, yy), Size(stem * 3f, stem * 1.6f))
        }
    }
    plant(size.width * .13f, size.height * .90f, size.height * .34f, Color(0xFF3D9D63))
    plant(size.width * .82f, size.height * .90f, size.height * .25f, Color(0xFF52B970))
    plant(size.width * .72f, size.height * .90f, size.height * .18f, Color(0xFF2D8155))

    // Surface shimmer
    val shimmerX = (shimmer * (size.width + size.width * .35f)) - size.width * .35f
    drawRect(Color(0x225EE7E7), Offset(shimmerX, size.height * .08f), Size(size.width * .28f, size.height * .025f))
}

private fun DrawScope.drawFish(f: Fish) {
    val base = max(5f, size.width / 34f)
    val scale = 1f + f.growth * .38f
    val bodyW = base * 3.2f * scale
    val bodyH = base * 1.55f * scale
    val cx = f.x * size.width
    val cy = f.y * size.height
    val facingRight = f.vx >= 0f
    val palettes = listOf(
        Triple(Color(0xFFFF6F61), Color(0xFFFFC857), Color(0xFFF7F4E8)),
        Triple(Color(0xFF58C7D9), Color(0xFF7A8CFF), Color(0xFFE7FAFF)),
        Triple(Color(0xFFE885D1), Color(0xFFA66FEA), Color(0xFFFFE9FA)),
    )
    val (body, fin, shine) = palettes[f.palette % palettes.size]

    val left = cx - bodyW / 2f
    val top = cy - bodyH / 2f
    drawRect(body, Offset(left, top), Size(bodyW, bodyH))
    drawRect(fin, Offset(left + bodyW * .25f, top + bodyH * .18f), Size(bodyW * .35f, bodyH * .28f))
    drawRect(shine, Offset(left + bodyW * .20f, top + bodyH * .10f), Size(bodyW * .25f, max(2f, bodyH * .12f)))

    val tailX = if (facingRight) left else left + bodyW
    val tail = Path().apply {
        moveTo(tailX, cy)
        lineTo(if (facingRight) tailX - bodyW * .46f else tailX + bodyW * .46f, cy - bodyH * .55f)
        lineTo(if (facingRight) tailX - bodyW * .46f else tailX + bodyW * .46f, cy + bodyH * .55f)
        close()
    }
    drawPath(tail, fin)

    val eyeX = if (facingRight) left + bodyW * .78f else left + bodyW * .22f
    drawCircle(Color(0xFFF8FBFA), max(2.2f, base * .20f), Offset(eyeX, top + bodyH * .35f))
    drawCircle(Color(0xFF071517), max(1.2f, base * .10f), Offset(eyeX + if (facingRight) 1f else -1f, top + bodyH * .35f))
}

private fun DrawScope.drawFood(x: Float, y: Float) {
    val s = max(2.5f, size.width / 90f)
    drawRect(Color(0xFFF5A24C), Offset(x * size.width - s / 2f, y * size.height - s / 2f), Size(s, s))
}

private fun DrawScope.drawBubble(x: Float, y: Float, radius: Float) {
    drawCircle(Color(0x558FE7EC), max(2f, size.width * radius), Offset(x * size.width, y * size.height))
    drawCircle(Color(0x99D8FFFF), max(1f, size.width * radius * .35f), Offset(x * size.width - 2f, y * size.height - 2f))
}
