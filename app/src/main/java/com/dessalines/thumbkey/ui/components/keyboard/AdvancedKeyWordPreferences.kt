package com.dessalines.thumbkey.ui.components.keyboard

import android.content.Context

enum class VisualRemapSlot {
    TOP_LEFT,
    TOP,
    TOP_RIGHT,
    LEFT,
    CENTER,
    RIGHT,
    BOTTOM_LEFT,
    BOTTOM,
    BOTTOM_RIGHT,
}

object AdvancedKeyWordPreferences {
    private const val PREFS = "advanced_key_word_preferences"
    private const val SHOW_CURRENT_WORD = "show_current_word"
    private const val LONG_PRESS_ADD_WORD = "long_press_add_word"

    fun showCurrentWord(context: Context): Boolean =
        context
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(SHOW_CURRENT_WORD, true)

    fun setShowCurrentWord(
        context: Context,
        enabled: Boolean,
    ) {
        context
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(SHOW_CURRENT_WORD, enabled)
            .apply()
    }

    fun longPressAddWord(context: Context): Boolean =
        context
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(LONG_PRESS_ADD_WORD, true)

    fun setLongPressAddWord(
        context: Context,
        enabled: Boolean,
    ) {
        context
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(LONG_PRESS_ADD_WORD, enabled)
            .apply()
    }
}
