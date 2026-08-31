package com.dessalines.thumbkey.ui.components.keyboard

import android.content.Context

private const val POWER_PREFS = "keywi_power_preferences"
private const val POWER_MODE = "mode"

enum class KeywiPowerMode {
    UNBRIDLED,
    CONSERVED,
    RESTRICTED,
}

object KeywiPowerPreferences {
    fun load(context: Context): KeywiPowerMode {
        val stored =
            context
                .getSharedPreferences(POWER_PREFS, Context.MODE_PRIVATE)
                .getString(POWER_MODE, KeywiPowerMode.CONSERVED.name)
        return runCatching { KeywiPowerMode.valueOf(stored!!) }
            .getOrDefault(KeywiPowerMode.CONSERVED)
    }

    fun save(
        context: Context,
        mode: KeywiPowerMode,
    ) {
        context
            .getSharedPreferences(POWER_PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(POWER_MODE, mode.name)
            .apply()
    }

    fun animationsEnabled(context: Context): Boolean = load(context) != KeywiPowerMode.RESTRICTED

    fun gifsEnabled(context: Context): Boolean = load(context) != KeywiPowerMode.RESTRICTED

    fun suggestionPollMillis(context: Context): Long =
        when (load(context)) {
            KeywiPowerMode.UNBRIDLED -> 80L
            KeywiPowerMode.CONSERVED -> 180L
            KeywiPowerMode.RESTRICTED -> 350L
        }
}
