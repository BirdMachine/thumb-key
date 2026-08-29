package com.dessalines.thumbkey.ui.components.keyboard

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

private const val TOOLBAR_PREFS_NAME = "birdie_toolbar_theme"
private const val KEY_MIGRATED = "migrated"
private const val KEY_PRESET = "preset"
private const val KEY_ANGLE = "angle"
private const val KEY_STOPS = "stops"
private const val KEY_MODE = "mode"
private const val KEY_OPACITY = "opacity"
private const val KEY_MEDIA_URI = "media_uri"
private const val KEY_BORDER_WIDTH = "border_width"
private const val KEY_BORDER_COLOR = "border_color"

object ToolbarThemePreferences {
    fun load(context: Context): BackdropThemeState {
        val prefs = context.getSharedPreferences(TOOLBAR_PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_MIGRATED, false)) {
            val legacy = BackdropThemePreferences.load(context)
            save(context, legacy)
            prefs.edit().putBoolean(KEY_MIGRATED, true).apply()
            if (legacy.mode == BackdropMode.IMAGE || legacy.mode == BackdropMode.GIF) {
                BackdropThemePreferences.save(
                    context,
                    BackdropThemePreferences
                        .stateForPreset(BackdropPreset.BIRDIE_RAINBOW)
                        .copy(mode = BackdropMode.COLORFUL),
                )
            }
            return legacy
        }

        val preset =
            runCatching {
                BackdropPreset.valueOf(
                    prefs.getString(KEY_PRESET, BackdropPreset.BIRDIE_RAINBOW.name)!!,
                )
            }.getOrDefault(BackdropPreset.BIRDIE_RAINBOW)
        val fallback =
            BackdropThemePreferences.stateForPreset(
                preset.takeUnless { it == BackdropPreset.CUSTOM } ?: BackdropPreset.BIRDIE_RAINBOW,
            )
        val stops = decodeStops(prefs.getString(KEY_STOPS, null)) ?: fallback.stops
        val mode =
            runCatching {
                BackdropMode.valueOf(
                    prefs.getString(KEY_MODE, BackdropMode.COLORFUL.name)!!,
                )
            }.getOrDefault(BackdropMode.COLORFUL)

        return BackdropThemeState(
            preset = preset,
            angleDegrees = prefs.getFloat(KEY_ANGLE, fallback.angleDegrees),
            stops = stops,
            mode = mode,
            opacity = prefs.getFloat(KEY_OPACITY, 1f).coerceIn(0f, 1f),
            mediaUri = prefs.getString(KEY_MEDIA_URI, null),
        )
    }

    fun save(
        context: Context,
        state: BackdropThemeState,
    ) {
        context
            .getSharedPreferences(TOOLBAR_PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_MIGRATED, true)
            .putString(KEY_PRESET, state.preset.name)
            .putFloat(KEY_ANGLE, state.angleDegrees)
            .putString(KEY_STOPS, encodeStops(state.stops))
            .putString(KEY_MODE, state.mode.name)
            .putFloat(KEY_OPACITY, state.opacity.coerceIn(0f, 1f))
            .putString(KEY_MEDIA_URI, state.mediaUri)
            .apply()
    }

    private fun encodeStops(stops: List<KeyboardGradientStop>): String = stops.joinToString(";") { "${it.position},${it.color.toArgb()}" }

    private fun decodeStops(encoded: String?): List<KeyboardGradientStop>? {
        if (encoded.isNullOrBlank()) return null
        return runCatching {
            encoded
                .split(';')
                .map { encodedStop ->
                    val parts = encodedStop.split(',')
                    KeyboardGradientStop(
                        parts[0].toFloat().coerceIn(0f, 1f),
                        Color(parts[1].toInt()),
                    )
                }.sortedBy { it.position }
        }.getOrNull()?.takeIf { it.isNotEmpty() }
    }
}

object ToolbarBorderPreferences {
    private const val PREFS_NAME = "birdie_toolbar_border"
    private const val WIDTH = "width"
    private const val COLOR = "color"

    fun loadWidth(context: Context): Float =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getFloat(WIDTH, 0.6f)

    fun saveWidth(context: Context, value: Float) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putFloat(WIDTH, value.coerceIn(0f, 4f)).apply()
    }

    fun loadColor(context: Context): Color =
        Color(context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getInt(COLOR, Color(0xFFFFC247).toArgb()))

    fun saveColor(context: Context, value: Color) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putInt(COLOR, value.toArgb()).apply()
    }
}
