package com.dessalines.thumbkey.ui.components.keyboard

import android.content.Context

object KeywiAppearancePreferences {
    private const val PREFS = "keywi_appearance_preferences"
    private const val KEY_ENABLED = "custom_keyboard_backgrounds_enabled"

    var currentEnabled: Boolean = true
        private set

    fun load(context: Context): Boolean {
        currentEnabled =
            context
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getBoolean(KEY_ENABLED, true)
        return currentEnabled
    }

    fun save(
        context: Context,
        enabled: Boolean,
    ) {
        currentEnabled = enabled
        context
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ENABLED, enabled)
            .apply()
    }
}
