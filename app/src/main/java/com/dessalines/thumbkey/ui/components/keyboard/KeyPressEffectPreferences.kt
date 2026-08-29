package com.dessalines.thumbkey.ui.components.keyboard

import android.content.Context

/** Persisted configuration for Keywi's one-shot key press media effect. */
data class KeyPressEffectState(
    val enabled: Boolean = false,
    val mediaUri: String? = null,
    val opacity: Float = 1f,
)

object KeyPressEffectPreferences {
    private const val PREFS = "keywi_key_press_effect"
    private const val ENABLED = "enabled"
    private const val MEDIA_URI = "media_uri"
    private const val OPACITY = "opacity"

    fun load(context: Context): KeyPressEffectState {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return KeyPressEffectState(
            enabled = prefs.getBoolean(ENABLED, false),
            mediaUri = prefs.getString(MEDIA_URI, null),
            opacity = prefs.getFloat(OPACITY, 1f).coerceIn(0f, 1f),
        )
    }

    fun save(context: Context, state: KeyPressEffectState) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(ENABLED, state.enabled)
            .putString(MEDIA_URI, state.mediaUri)
            .putFloat(OPACITY, state.opacity.coerceIn(0f, 1f))
            .apply()
    }
}
