package com.dessalines.thumbkey.ui.components.keyboard

import android.content.Context
import androidx.compose.ui.graphics.Color

enum class KeySurfaceStyle { GRADIENT, SOLID, NONE }

enum class KeyBorderStyle { GRADIENT, SOLID, SHADOW, NONE }

data class KeyThemeState(
    val surfaceStyle: KeySurfaceStyle = KeySurfaceStyle.GRADIENT,
    val surfaceGradient: KeyboardBackdrop = BIRDIE_KEY_GRADIENT,
    val surfaceColor: Color = Color(0xFF242631),
    val borderStyle: KeyBorderStyle = KeyBorderStyle.GRADIENT,
    val borderGradient: KeyboardBackdrop = BIRDIE_GOLD_BORDER,
    val borderColor: Color = Color(0xFFFFD86B),
    val shadowColor: Color = Color.Black,
    val shadowAlpha: Float = 0.55f,
    val shadowElevation: Float = 4f,
)

object KeyThemePreferences {
    private const val PREFS = "key_theme_preferences"

    var current: KeyThemeState = KeyThemeState()
        private set

    fun load(context: Context): KeyThemeState {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val defaults = KeyThemeState()
        current =
            KeyThemeState(
                surfaceStyle = enumValue(prefs.getString("surface_style", null), KeySurfaceStyle.GRADIENT),
                surfaceGradient = readGradient(prefs, "surface", BIRDIE_KEY_GRADIENT),
                surfaceColor = readColor(prefs, "surface_color", defaults.surfaceColor),
                borderStyle = enumValue(prefs.getString("border_style", null), KeyBorderStyle.GRADIENT),
                borderGradient = readGradient(prefs, "border", BIRDIE_GOLD_BORDER),
                borderColor = readColor(prefs, "border_color", defaults.borderColor),
                shadowColor = readColor(prefs, "shadow_color", defaults.shadowColor),
                shadowAlpha = prefs.getFloat("shadow_alpha", 0.55f),
                shadowElevation = prefs.getFloat("shadow_elevation", 4f),
            )
        return current
    }

    fun save(
        context: Context,
        state: KeyThemeState,
    ) {
        current = state
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
                putLong("shadow_color", state.shadowColor.value.toLong())
                putFloat("shadow_alpha", state.shadowAlpha)
                putFloat("shadow_elevation", state.shadowElevation)
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
