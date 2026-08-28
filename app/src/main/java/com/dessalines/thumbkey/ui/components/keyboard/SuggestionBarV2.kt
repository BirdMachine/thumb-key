package com.dessalines.thumbkey.ui.components.keyboard

import android.content.Context
import android.text.InputType
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dessalines.thumbkey.IMEService
import kotlinx.coroutines.delay
import java.util.Locale

private const val BAR_HEIGHT_DP = 42
private const val MOTION_PREFS = "suggestion_motion_preferences"
private const val MOTION_STYLE = "motion_style"
private const val MAX_VISIBLE_SUGGESTIONS = 5
private val WORD_PATTERN_V2 = Regex("[A-Za-z']+$")

enum class SuggestionMotionStyle {
    NONE,
    SPRINGY,
    GOOEY,
}

object SuggestionMotionPreferences {
    fun load(context: Context): SuggestionMotionStyle {
        val stored =
            context
                .getSharedPreferences(MOTION_PREFS, Context.MODE_PRIVATE)
                .getString(MOTION_STYLE, SuggestionMotionStyle.SPRINGY.name)
        return runCatching { SuggestionMotionStyle.valueOf(stored!!) }
            .getOrDefault(SuggestionMotionStyle.SPRINGY)
    }

    fun save(
        context: Context,
        style: SuggestionMotionStyle,
    ) {
        context
            .getSharedPreferences(MOTION_PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(MOTION_STYLE, style.name)
            .apply()
    }
}

private object LocalSuggestionEngineV2 {
    private val commonWords =
        """
        about above absolutely actually add added after again all almost already also always amazing and android animation another answer any anything app application are around ask asked awesome back background bad bar because become been before best better between big bird birds black blue border both build button called can change changes clean clear clipboard close code color colors come commit complete completion computer control controls cool correct correction create created current cursor custom data day default definitely delete device dictionary different disable disabled does easy edit editor email emoji enable enabled english enough enter especially even every everything exactly example feature features feel feels file files finally find first fix fixed font found friend friends full fun funny fuzzy game games get give go gold good gradient great guess happen happened happy hard have hello help here hey hide how idea image images input install installed issue just keep keyboard key keys keywi kind know language last layer learn learning left less light like little live load loaded local long look love lovely made make many match matching maybe mean means message messagease middle might more most move moved much name need needed never new next nice night nope nothing number of offline okay old on online only opacity open option options other output overlay part persist persistent phone photo photos picture pink place play please point position possible possibly prefer preset pretty preview private probably problem public purple question quick rainbow read reading really reason red remember remove removed right run running same samsung save saved screen setting settings shiny short should show side simple single size slow small software something sometimes space spell spelling start still stop stops strip style suggestion suggestions sure swipe system take tap test testing text than thank thanks that the their them then there these thing things think this thought through thumbkey time today together toggle top touch transparent transparency try tried trying type typing under understand update use used using version very want wanted water way we week well what when where which while white why width will wish with word words work working works world worse would write writing yeah year yes yesterday you your
        """.trimIndent()
            .split(Regex("\\s+"))
            .map { it.trim().lowercase(Locale.US) }
            .filter { it.length > 1 }
            .distinct()

    fun suggest(
        prefix: String,
        limit: Int = MAX_VISIBLE_SUGGESTIONS,
    ): List<String> {
        if (prefix.length < 2) return emptyList()
        val normalized = prefix.lowercase(Locale.US)
        val matches =
            commonWords
                .asSequence()
                .filter { it.startsWith(normalized) && it != normalized }
                .take(limit)
                .toList()
        return if (prefix.firstOrNull()?.isUpperCase() == true) {
            matches.map { word -> word.replaceFirstChar { it.titlecase(Locale.US) } }
        } else {
            matches
        }
    }
}

private fun lozengeTransform(style: SuggestionMotionStyle): ContentTransform =
    when (style) {
        SuggestionMotionStyle.NONE -> {
            fadeIn(tween(1)) togetherWith fadeOut(tween(1))
        }

        SuggestionMotionStyle.SPRINGY -> {
            val enter =
                fadeIn(tween(70)) +
                    scaleIn(
                        initialScale = 0.90f,
                        animationSpec =
                            keyframes {
                                durationMillis = 190
                                0.90f at 0
                                1.055f at 70
                                0.975f at 125
                                1f at 190
                            },
                    )
            val exit =
                fadeOut(tween(120)) +
                    scaleOut(
                        targetScale = 0.86f,
                        animationSpec =
                            keyframes {
                                durationMillis = 155
                                1f at 0
                                1.045f at 48
                                0.965f at 92
                                0.86f at 155
                            },
                    )
            enter togetherWith exit
        }

        SuggestionMotionStyle.GOOEY -> {
            val enter =
                fadeIn(tween(95)) +
                    scaleIn(
                        initialScale = 0.58f,
                        animationSpec =
                            keyframes {
                                durationMillis = 300
                                0.58f at 0
                                1.14f at 105
                                0.92f at 175
                                1.055f at 235
                                1f at 300
                            },
                    )
            val exit =
                fadeOut(tween(190)) +
                    scaleOut(
                        targetScale = 0.48f,
                        animationSpec =
                            keyframes {
                                durationMillis = 245
                                1f at 0
                                1.09f at 65
                                0.91f at 125
                                0.48f at 245
                            },
                    )
            enter togetherWith exit
        }
    }

@Composable
private fun SuggestionLozengeSlot(
    suggestion: String?,
    slotIndex: Int,
    displayedCount: Int,
    motionStyle: SuggestionMotionStyle,
    onSuggestionClick: (String) -> Unit,
) {
    AnimatedContent(
        targetState = suggestion,
        transitionSpec = { lozengeTransform(motionStyle) },
        contentAlignment = Alignment.Center,
        label = "suggestion-lozenge-$slotIndex",
    ) { currentSuggestion ->
        if (currentSuggestion == null) {
            Box(modifier = Modifier)
        } else {
            val isBest = (displayedCount > 1 && slotIndex == 1) || displayedCount == 1
            Box(modifier = Modifier.padding(end = 6.dp)) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color =
                        MaterialTheme.colorScheme.surfaceVariant.copy(
                            alpha = if (isBest) 0.72f else 0.50f,
                        ),
                    tonalElevation = if (isBest) 2.dp else 0.dp,
                    shadowElevation = if (isBest) 2.dp else 1.dp,
                    border =
                        BorderStroke(
                            if (isBest) 1.2.dp else 0.8.dp,
                            MaterialTheme.colorScheme.onSurface.copy(
                                alpha = if (isBest) 0.28f else 0.16f,
                            ),
                        ),
                    modifier =
                        Modifier
                            .widthIn(min = 58.dp, max = 180.dp)
                            .animateContentSize(
                                animationSpec =
                                    when (motionStyle) {
                                        SuggestionMotionStyle.NONE -> {
                                            spring(stiffness = Spring.StiffnessHigh)
                                        }

                                        SuggestionMotionStyle.SPRINGY -> {
                                            spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessMediumLow,
                                            )
                                        }

                                        SuggestionMotionStyle.GOOEY -> {
                                            spring(
                                                dampingRatio = 0.30f,
                                                stiffness = Spring.StiffnessLow,
                                            )
                                        }
                                    },
                            ).clickable { onSuggestionClick(currentSuggestion) },
                ) {
                    Text(
                        text = currentSuggestion,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color =
                            MaterialTheme.colorScheme.onSurface.copy(
                                alpha = if (isBest) 1f else 0.88f,
                            ),
                        fontWeight = if (isBest) FontWeight.Bold else FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 13.dp, vertical = 6.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun SuggestionBarV2(ime: IMEService) {
    var enabled by remember { mutableStateOf(SuggestionPreferences.enabled(ime)) }
    var prefix by remember { mutableStateOf("") }
    var suggestions by remember { mutableStateOf(emptyList<String>()) }
    val motionStyle = remember { SuggestionMotionPreferences.load(ime) }

    val inputType = ime.currentInputEditorInfo?.inputType ?: 0
    val variation = inputType and InputType.TYPE_MASK_VARIATION
    val privateField =
        variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
            variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
            variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD

    LaunchedEffect(enabled, privateField) {
        while (enabled && !privateField) {
            val beforeCursor =
                ime.currentInputConnection
                    ?.getTextBeforeCursor(64, 0)
                    ?.toString()
                    .orEmpty()
            val nextPrefix = WORD_PATTERN_V2.find(beforeCursor)?.value.orEmpty()
            if (nextPrefix != prefix) {
                prefix = nextPrefix
                suggestions = LocalSuggestionEngineV2.suggest(nextPrefix)
            }
            delay(80)
        }
        if (!enabled || privateField) {
            prefix = ""
            suggestions = emptyList()
        }
    }

    val displayed =
        when (suggestions.size) {
            0, 1 -> suggestions
            2 -> listOf(suggestions[1], suggestions[0])
            else -> listOf(suggestions[1], suggestions[0]) + suggestions.drop(2)
        }
    val borderStops = BIRDIE_GOLD_BORDER.stops.map { it.position to it.color }.toTypedArray()

    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = if (enabled) 0.30f else 0.20f),
        modifier =
            Modifier
                .fillMaxWidth()
                .height(BAR_HEIGHT_DP.dp)
                .drawWithCache {
                    val brush = Brush.linearGradient(colorStops = borderStops)
                    val strokeWidth = 2.dp.toPx()
                    onDrawWithContent {
                        drawContent()
                        drawLine(
                            brush = brush,
                            start = Offset(0f, size.height - (strokeWidth / 2f)),
                            end = Offset(size.width, size.height - (strokeWidth / 2f)),
                            strokeWidth = strokeWidth,
                        )
                    }
                },
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.weight(1f).fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    modifier =
                        Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    repeat(MAX_VISIBLE_SUGGESTIONS) { slotIndex ->
                        SuggestionLozengeSlot(
                            suggestion = displayed.getOrNull(slotIndex),
                            slotIndex = slotIndex,
                            displayedCount = displayed.size,
                            motionStyle = motionStyle,
                            onSuggestionClick = { suggestion ->
                                val currentPrefix = prefix
                                if (currentPrefix.isNotEmpty()) {
                                    ime.currentInputConnection?.deleteSurroundingText(currentPrefix.length, 0)
                                    ime.currentInputConnection?.commitText("$suggestion ", 1)
                                    prefix = ""
                                    suggestions = emptyList()
                                }
                            },
                        )
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(18.dp),
                color =
                    if (enabled) {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f)
                    },
                tonalElevation = if (enabled) 2.dp else 0.dp,
                shadowElevation = 1.dp,
                border =
                    BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 0.24f else 0.12f),
                    ),
                modifier =
                    Modifier
                        .padding(start = 5.dp)
                        .clickable {
                            enabled = !enabled
                            SuggestionPreferences.setEnabled(ime, enabled)
                        },
            ) {
                Text(
                    text = if (enabled) "✨" else "○",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
        }
    }
}
