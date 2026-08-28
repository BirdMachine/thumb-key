package com.dessalines.thumbkey.ui.components.keyboard

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

private const val PREFS_NAME = "birdie_backdrop_theme"
private const val KEY_PRESET = "preset"
private const val KEY_ANGLE = "angle"
private const val KEY_STOPS = "stops"
private const val KEY_MODE = "mode"
private const val KEY_OPACITY = "opacity"
private const val KEY_MEDIA_URI = "media_uri"

enum class BackdropPreset {
    BIRDIE_RAINBOW,
    SINEBOW,
    CUSTOM,
}

enum class BackdropMode {
    COLORFUL,
    IMAGE,
    GIF,
    NONE,
}

data class BackdropThemeState(
    val preset: BackdropPreset,
    val angleDegrees: Float,
    val stops: List<KeyboardGradientStop>,
    val mode: BackdropMode = BackdropMode.COLORFUL,
    val opacity: Float = 1f,
    val mediaUri: String? = null,
) {
    fun toBackdrop(): KeyboardBackdrop =
        KeyboardBackdrop(
            stops = stops,
            angleDegrees = angleDegrees,
        )
}

object BackdropThemePreferences {
    fun load(context: Context): BackdropThemeState {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val preset =
            runCatching {
                BackdropPreset.valueOf(prefs.getString(KEY_PRESET, BackdropPreset.BIRDIE_RAINBOW.name)!!)
            }.getOrDefault(BackdropPreset.BIRDIE_RAINBOW)
        val fallback = stateForPreset(preset.takeUnless { it == BackdropPreset.CUSTOM } ?: BackdropPreset.BIRDIE_RAINBOW)
        val angle = prefs.getFloat(KEY_ANGLE, fallback.angleDegrees)
        val stops = decodeStops(prefs.getString(KEY_STOPS, null)) ?: fallback.stops
        val mode =
            runCatching {
                BackdropMode.valueOf(prefs.getString(KEY_MODE, BackdropMode.COLORFUL.name)!!)
            }.getOrDefault(BackdropMode.COLORFUL)
        val opacity = prefs.getFloat(KEY_OPACITY, 1f).coerceIn(0f, 1f)
        val mediaUri = prefs.getString(KEY_MEDIA_URI, null)
        return BackdropThemeState(
            preset = preset,
            angleDegrees = angle,
            stops = stops,
            mode = mode,
            opacity = opacity,
            mediaUri = mediaUri,
        )
    }

    fun save(
        context: Context,
        state: BackdropThemeState,
    ) {
        context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PRESET, state.preset.name)
            .putFloat(KEY_ANGLE, state.angleDegrees)
            .putString(KEY_STOPS, encodeStops(state.stops))
            .putString(KEY_MODE, state.mode.name)
            .putFloat(KEY_OPACITY, state.opacity.coerceIn(0f, 1f))
            .putString(KEY_MEDIA_URI, state.mediaUri)
            .apply()
    }

    fun stateForPreset(preset: BackdropPreset): BackdropThemeState =
        when (preset) {
            BackdropPreset.BIRDIE_RAINBOW -> {
                BackdropThemeState(
                    preset = preset,
                    angleDegrees = BIRDIE_RAINBOW_BACKDROP.angleDegrees,
                    stops = BIRDIE_RAINBOW_BACKDROP.stops,
                )
            }

            BackdropPreset.SINEBOW -> {
                BackdropThemeState(
                    preset = preset,
                    angleDegrees = SINEBOW_BACKDROP.angleDegrees,
                    stops = SINEBOW_BACKDROP.stops,
                )
            }

            BackdropPreset.CUSTOM -> {
                stateForPreset(BackdropPreset.BIRDIE_RAINBOW).copy(preset = preset)
            }
        }

    private fun encodeStops(stops: List<KeyboardGradientStop>): String =
        stops.joinToString(";") { stop ->
            "${stop.position},${stop.color.toArgb()}"
        }

    private fun decodeStops(encoded: String?): List<KeyboardGradientStop>? {
        if (encoded.isNullOrBlank()) return null
        return runCatching {
            encoded
                .split(';')
                .map { encodedStop ->
                    val parts = encodedStop.split(',')
                    KeyboardGradientStop(
                        position = parts[0].toFloat().coerceIn(0f, 1f),
                        color = Color(parts[1].toInt()),
                    )
                }.sortedBy { it.position }
        }.getOrNull()?.takeIf { it.isNotEmpty() }
    }
}
