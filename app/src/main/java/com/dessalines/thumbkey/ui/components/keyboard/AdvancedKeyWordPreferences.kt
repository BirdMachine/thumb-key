package com.dessalines.thumbkey.ui.components.keyboard

import android.content.Context
import androidx.compose.ui.graphics.Color

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
    private const val NEW_WORD_HIGHLIGHT_COLOR = "new_word_highlight_color"
    private val DEFAULT_NEW_WORD_HIGHLIGHT_COLOR = Color(0xFF52FF52)

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

    fun newWordHighlightColor(context: Context): Color {
        val stored =
            context
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getLong(NEW_WORD_HIGHLIGHT_COLOR, DEFAULT_NEW_WORD_HIGHLIGHT_COLOR.value.toLong())
        return Color(stored.toULong())
    }

    fun setNewWordHighlightColor(
        context: Context,
        color: Color,
    ) {
        context
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putLong(NEW_WORD_HIGHLIGHT_COLOR, color.value.toLong())
            .apply()
    }
}
