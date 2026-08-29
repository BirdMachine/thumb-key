package com.dessalines.thumbkey.ui.components.keyboard

import android.content.Context
import androidx.compose.ui.graphics.Color

enum class SuggestionLozengeSurfaceStyle {
    GRADIENT,
    SOLID,
    NONE,
}

enum class SuggestionLozengeBorderStyle {
    GRADIENT,
    SOLID,
    NONE,
}

data class SuggestionLozengeThemeState(
    val surfaceStyle: SuggestionLozengeSurfaceStyle = SuggestionLozengeSurfaceStyle.SOLID,
    val surfaceGradient: KeyboardBackdrop = BIRDIE_KEY_GRADIENT,
    val surfaceColor: Color = Color(0xB8343540),
    val borderStyle: SuggestionLozengeBorderStyle = SuggestionLozengeBorderStyle.SOLID,
    val borderGradient: KeyboardBackdrop = BIRDIE_GOLD_BORDER,
    val borderColor: Color = Color(0x70FFFFFF),
    val borderWidth: Float = 1f,
)

object SuggestionLozengeThemePreferences {
    private const val PREFS = "suggestion_lozenge_theme"

    fun load(context: Context): SuggestionLozengeThemeState {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val defaults = SuggestionLozengeThemeState()
        return SuggestionLozengeThemeState(
            surfaceStyle =
                enumValue(
                    prefs.getString("surface_style", null),
                    SuggestionLozengeSurfaceStyle.SOLID,
                ),
            surfaceGradient = readGradient(prefs, "surface", BIRDIE_KEY_GRADIENT),
            surfaceColor = readColor(prefs, "surface_color", defaults.surfaceColor),
            borderStyle =
                enumValue(
                    prefs.getString("border_style", null),
                    SuggestionLozengeBorderStyle.SOLID,
                ),
            borderGradient = readGradient(prefs, "border", BIRDIE_GOLD_BORDER),
            borderColor = readColor(prefs, "border_color", defaults.borderColor),
            borderWidth = prefs.getFloat("border_width", 1f).coerceIn(0f, 6f),
        )
    }

    fun save(
        context: Context,
        state: SuggestionLozengeThemeState,
    ) {
        context
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .apply {
                putString("surface_style", state.surfaceStyle.name)
                writeGradient(this, "surface", state.surfaceGradient)
                putLong("surface_color", state.surfaceColor.value.toLong())
                putString("border_style", state.borderStyle.name)
                writeGradient(this, "border", state.borderGradient)
                putLong("border_color", state.borderColor.value.toLong())
                putFloat("border_width", state.borderWidth.coerceIn(0f, 6f))
            }.apply()
    }

    private fun readColor(
        prefs: android.content.SharedPreferences,
        key: String,
        fallback: Color,
    ): Color =
        runCatching {
            Color(prefs.getLong(key, fallback.value.toLong()).toULong())
        }.getOrDefault(fallback)

    private fun readGradient(
        prefs: android.content.SharedPreferences,
        prefix: String,
        fallback: KeyboardBackdrop,
    ): KeyboardBackdrop {
        val raw = prefs.getString("${prefix}_stops", null) ?: return fallback
        val stops =
            raw.split(";").mapNotNull { item ->
                val parts = item.split(":")
                if (parts.size != 2) {
                    null
                } else {
                    runCatching {
                        KeyboardGradientStop(parts[0].toFloat(), Color(parts[1].toULong()))
                    }.getOrNull()
                }
            }
        return if (stops.isEmpty()) {
            fallback
        } else {
            KeyboardBackdrop(stops, prefs.getFloat("${prefix}_angle", fallback.angleDegrees))
        }
    }

    private fun writeGradient(
        editor: android.content.SharedPreferences.Editor,
        prefix: String,
        gradient: KeyboardBackdrop,
    ) {
        editor.putFloat("${prefix}_angle", gradient.angleDegrees)
        editor.putString(
            "${prefix}_stops",
            gradient.stops.joinToString(";") { "${it.position}:${it.color.value}" },
        )
    }

    private inline fun <reified T : Enum<T>> enumValue(
        value: String?,
        fallback: T,
    ): T = runCatching { enumValueOf<T>(value ?: "") }.getOrDefault(fallback)
}
