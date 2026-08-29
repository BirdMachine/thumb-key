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
import kotlin.math.sin
import kotlin.random.Random

/** Fishwi stays isolated from the keyboard engine: keys only emit tiny interaction events. */
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

/** Observe keyboard touches without consuming them. */
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
    var growth by mutableFloatStateOf(0.10f)
    var experience by mutableIntStateOf(0)
    var backwardsUntil by mutableLongStateOf(0L)
    var targetFoodId by mutableIntStateOf(-1)
    var swimPhase by mutableFloatStateOf(Random.nextFloat() * 6.28318f)
    var wiggle by mutableFloatStateOf(0f)
}

private class Food(val id: Int, x: Float, y: Float, vx: Float, vy: Float) {
    var x by mutableFloatStateOf(x)
    var y by mutableFloatStateOf(y)
    var vx by mutableFloatStateOf(vx)
    var vy by mutableFloatStateOf(vy)
    var age by mutableFloatStateOf(0f)
}

private class Bubble(x: Float, y: Float, radius: Float) {
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
        val fish = defaultFish().map { f ->
            f.apply {
                x = p.getFloat("f_${id}_x", x).coerceIn(0.06f, 0.94f)
                y = p.getFloat("f_${id}_y", y).coerceIn(0.08f, 0.88f)
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
        Fish(0, "Pixel", 0, FishPersonality(.92f, .78f, .10f, .55f, .26f), .26f, .24f),
        Fish(1, "Miso", 1, FishPersonality(.45f, .96f, .20f, .82f, .40f), .67f, .38f),
        Fish(2, "Dot", 2, FishPersonality(.35f, .40f, .82f, .30f, .70f), .42f, .69f),
        Fish(3, "Lemon", 3, FishPersonality(.55f, .72f, .28f, .70f, .53f), .72f, .55f),
        Fish(4, "Guppy", 4, FishPersonality(.62f, .88f, .18f, .86f, .34f), .36f, .42f),
        Fish(5, "Peach", 5, FishPersonality(.48f, .66f, .38f, .74f, .62f), .58f, .62f),
        Fish(6, "Mint", 6, FishPersonality(.30f, .92f, .22f, .52f, .78f), .25f, .78f),
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
            delay(40)
            val dt = .04f
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
                b.x += sin((b.y + shimmer) * 18f) * .0012f
                if (b.y < .02f) bubbles.remove(b)
            }
            if (bubbles.size < 11 && Random.nextFloat() < .05f) {
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
                    if (Random.nextFloat() < .015f * (1f - f.personality.laziness * .62f)) {
                        f.vx += (Random.nextFloat() - .5f) * .038f
                        f.vy += (Random.nextFloat() - .5f) * .026f
                    }
                    if (f.personality.sociability > .65f && fish.size > 1) {
                        val buddy = fish[(index + 1) % fish.size]
                        f.vx += (buddy.x - f.x) * .006f * dt
                    }
                }

                val speedLimit = .058f + f.personality.curiosity * .050f
                f.vx = f.vx.coerceIn(-speedLimit, speedLimit)
                f.vy = f.vy.coerceIn(-.058f, .058f)
                f.vx *= .996f
                f.vy *= .992f
                val swimSpeed = 5.2f + abs(f.vx) * 48f + f.personality.curiosity * 1.5f
                f.swimPhase = (f.swimPhase + dt * swimSpeed) % 6.28318f
                f.wiggle = sin(f.swimPhase)
                val reverse = now < f.backwardsUntil
                f.x += (if (reverse) -f.vx else f.vx) * dt
                f.y += f.vy * dt + f.wiggle * (.00042f + abs(f.vx) * .0032f)

                if (f.x < .055f) { f.x = .055f; f.vx = abs(f.vx) }
                if (f.x > .945f) { f.x = .945f; f.vx = -abs(f.vx) }
                if (f.y < .075f) { f.y = .075f; f.vy = abs(f.vy) }
                if (f.y > .875f) { f.y = .875f; f.vy = -abs(f.vy) }
            }

            ticks++
            if (ticks % 250 == 0) FishwiStore.save(context, fish, interactions)
        }
    }

    DisposableEffect(Unit) { onDispose { FishwiStore.save(context, fish, interactions) } }

    Canvas(modifier = modifier.fillMaxWidth().fillMaxHeight()) {
        drawTank(shimmer)
        food.forEach { pellet -> drawFood(pellet.x, pellet.y) }
        bubbles.forEach { bubble -> drawBubble(bubble.x, bubble.y, bubble.radius) }
        fish.forEach { f -> drawFish(f) }
    }
}

private fun DrawScope.drawTank(shimmer: Float) {
    drawRect(Color(0xE8103440))
    drawRect(Color(0xAA185C68), topLeft = Offset(0f, size.height * .035f), size = Size(size.width, size.height * .79f))
    drawRect(Color(0xFF163C3D), topLeft = Offset(0f, size.height * .89f), size = Size(size.width, size.height * .11f))

    val step = max(3f, size.width / 42f)
    var x = 0f
    var i = 0
    while (x < size.width) {
        val h = when (i % 4) { 0 -> step * .80f; 1 -> step * .48f; 2 -> step * .62f; else -> step * .38f }
        val gravel = when (i % 5) {
            0 -> Color(0xFF4B554D)
            1 -> Color(0xFF706A58)
            2 -> Color(0xFF92745C)
            3 -> Color(0xFFB08B6E)
            else -> Color(0xFF5C6256)
        }
        drawRect(gravel, Offset(x, size.height * .93f - h), Size(step + 1f, h))
        if (i % 3 == 1) drawRect(Color(0xFFC6A783), Offset(x + step * .22f, size.height * .93f - h * .58f), Size(step * .35f, max(1f, h * .16f)))
        x += step
        i++
    }

    fun plant(px: Float, base: Float, tall: Float, c: Color) {
        val stem = max(1.7f, size.width / 105f)
        drawRect(c, Offset(px, base - tall), Size(stem, tall))
        repeat(6) { n ->
            val yy = base - tall * (.12f + n * .145f)
            val side = if (n % 2 == 0) -1f else 1f
            val leaf = Path().apply {
                moveTo(px + stem * .5f, yy + stem)
                lineTo(px + side * stem * 2.0f, yy - stem * .55f)
                lineTo(px + side * stem * 4.4f, yy)
                lineTo(px + side * stem * 2.6f, yy + stem * 1.35f)
                close()
            }
            drawPath(leaf, c)
            drawPath(leaf, Color.White.copy(alpha = .055f))
        }
    }
    plant(size.width * .12f, size.height * .91f, size.height * .31f, Color(0xFF43A86B))
    plant(size.width * .84f, size.height * .91f, size.height * .27f, Color(0xFF61C67B))
    plant(size.width * .72f, size.height * .91f, size.height * .17f, Color(0xFF31865B))

    val shimmerX = shimmer * (size.width * 1.45f) - size.width * .30f
    drawRect(Color(0x1F8FFFF0), Offset(shimmerX, size.height * .06f), Size(size.width * .15f, size.height * .78f))
    drawRect(Color(0x145EE7E7), Offset(shimmerX + size.width * .21f, size.height * .03f), Size(size.width * .07f, size.height * .84f))
}

private fun DrawScope.drawFish(f: Fish) {
    val px = max(1.55f, size.width / 118f)
    val scale = 1f + f.growth * .25f
    val bodyW = px * 13.5f * scale
    val bodyH = px * 6.6f * scale
    val cx = f.x * size.width
    val baseCy = f.y * size.height
    val cy = baseCy + sin(f.swimPhase * .5f) * px * .42f
    val facingRight = f.vx >= 0f
    val dir = if (facingRight) 1f else -1f
    val tailSwing = f.wiggle * bodyH * (.30f + min(.22f, abs(f.vx) * 2.0f))

    val palettes = listOf(
        listOf(Color(0xFF8C272A), Color(0xFFE84A3F), Color(0xFFFF7B50), Color(0xFFFFC35E), Color(0xFFFFF2B2)),
        listOf(Color(0xFF155C78), Color(0xFF279DBA), Color(0xFF48D4D1), Color(0xFF8EF1E9), Color(0xFFE8FFFF)),
        listOf(Color(0xFF84224F), Color(0xFFD73E72), Color(0xFFFF668F), Color(0xFFFFA6B9), Color(0xFFFFEDF2)),
        listOf(Color(0xFF9B6610), Color(0xFFE9A71B), Color(0xFFFFD52C), Color(0xFFFFED78), Color(0xFFFFFFC7)),
        listOf(Color(0xFF8A3618), Color(0xFFEF5A20), Color(0xFFFF8A2A), Color(0xFFFFC34A), Color(0xFFFFF0A2)),
        listOf(Color(0xFFB44A42), Color(0xFFF27A67), Color(0xFFFFAA8A), Color(0xFFFFD3B8), Color(0xFFFFF4E8)),
        listOf(Color(0xFF217869), Color(0xFF39B09D), Color(0xFF61DAC6), Color(0xFF9AF3DF), Color(0xFFE7FFF8)),
    )
    val palette = palettes[f.palette % palettes.size]
    val shadow = palette[0]
    val body = palette[1]
    val mid = palette[2]
    val highlight = palette[3]
    val shine = palette[4]

    val bodyPath = Path().apply {
        moveTo(cx - dir * bodyW * .49f, cy)
        lineTo(cx - dir * bodyW * .36f, cy - bodyH * .34f)
        lineTo(cx - dir * bodyW * .09f, cy - bodyH * .50f)
        lineTo(cx + dir * bodyW * .24f, cy - bodyH * .45f)
        lineTo(cx + dir * bodyW * .46f, cy - bodyH * .22f)
        lineTo(cx + dir * bodyW * .53f, cy)
        lineTo(cx + dir * bodyW * .43f, cy + bodyH * .27f)
        lineTo(cx + dir * bodyW * .13f, cy + bodyH * .46f)
        lineTo(cx - dir * bodyW * .22f, cy + bodyH * .40f)
        lineTo(cx - dir * bodyW * .43f, cy + bodyH * .20f)
        close()
    }
    drawPath(bodyPath, body)

    val tailBaseX = cx - dir * bodyW * .42f
    val tailTipX = cx - dir * bodyW * .82f
    val tailTipY = cy + tailSwing
    val tail = Path().apply {
        moveTo(tailBaseX, cy - bodyH * .10f)
        lineTo(tailTipX, tailTipY - bodyH * .48f)
        lineTo(tailTipX + dir * px * .95f, tailTipY)
        lineTo(tailTipX, tailTipY + bodyH * .48f)
        lineTo(tailBaseX, cy + bodyH * .12f)
        close()
    }
    drawPath(tail, mid)
    drawRect(shadow, Offset(minOf(tailBaseX, tailBaseX - dir * px), cy - bodyH * .22f), Size(px, bodyH * .44f))

    val finWiggle = -f.wiggle * bodyH * .12f
    val dorsal = Path().apply {
        moveTo(cx - dir * bodyW * .11f, cy - bodyH * .42f)
        lineTo(cx - dir * bodyW * .01f, cy - bodyH * .72f + finWiggle)
        lineTo(cx + dir * bodyW * .20f, cy - bodyH * .43f)
        close()
    }
    drawPath(dorsal, highlight)
    val bellyFin = Path().apply {
        moveTo(cx - dir * bodyW * .02f, cy + bodyH * .35f)
        lineTo(cx - dir * bodyW * .02f, cy + bodyH * .67f - finWiggle)
        lineTo(cx + dir * bodyW * .18f, cy + bodyH * .39f)
        close()
    }
    drawPath(bellyFin, shadow)

    val midStripeX = cx - dir * bodyW * .13f
    drawRect(mid, Offset(minOf(midStripeX, midStripeX + dir * bodyW * .27f), cy - bodyH * .26f), Size(bodyW * .27f, bodyH * .49f))
    val highlightX = cx + dir * bodyW * .10f
    drawRect(highlight, Offset(minOf(highlightX, highlightX + dir * bodyW * .21f), cy - bodyH * .34f), Size(bodyW * .21f, max(px, bodyH * .15f)))
    drawRect(shine, Offset(cx - dir * bodyW * .02f - px * .5f, cy - bodyH * .34f), Size(px * 1.6f, max(px, bodyH * .10f)))
    drawRect(shine.copy(alpha = .62f), Offset(cx - bodyW * .07f, cy + bodyH * .04f), Size(px, px))
    drawRect(highlight.copy(alpha = .75f), Offset(cx + bodyW * .08f, cy + bodyH * .14f), Size(px, px))

    val eyeX = cx + dir * bodyW * .35f
    val eyeY = cy - bodyH * .14f
    drawCircle(shine, max(1.45f, px * .76f), Offset(eyeX, eyeY))
    drawCircle(Color(0xFF071517), max(.9f, px * .36f), Offset(eyeX + dir * px * .18f, eyeY))
    drawRect(shadow, Offset(cx + dir * bodyW * .49f - if (facingRight) 0f else px, cy + px * .34f), Size(px, max(1f, px * .42f)))
}

private fun DrawScope.drawFood(x: Float, y: Float) {
    val s = max(2.2f, size.width / 105f)
    drawRect(Color(0xFFF5A24C), Offset(x * size.width - s / 2f, y * size.height - s / 2f), Size(s, s))
}

private fun DrawScope.drawBubble(x: Float, y: Float, radius: Float) {
    drawCircle(Color(0x558FE7EC), max(2f, size.width * radius), Offset(x * size.width, y * size.height))
    drawCircle(Color(0x99D8FFFF), max(1f, size.width * radius * .35f), Offset(x * size.width - 2f, y * size.height - 2f))
}
