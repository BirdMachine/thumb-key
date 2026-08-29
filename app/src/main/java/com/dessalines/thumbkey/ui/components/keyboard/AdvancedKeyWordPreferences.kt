package com.dessalines.thumbkey.ui.components.keyboard

import android.content.Context
import com.dessalines.thumbkey.utils.KeyAction
import com.dessalines.thumbkey.utils.KeyC
import com.dessalines.thumbkey.utils.KeyDisplay
import com.dessalines.thumbkey.utils.KeyItemC
import com.dessalines.thumbkey.utils.KeyboardC

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
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(SHOW_CURRENT_WORD, true)

    fun setShowCurrentWord(
        context: Context,
        enabled: Boolean,
    ) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(SHOW_CURRENT_WORD, enabled).apply()
    }

    fun longPressAddWord(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(LONG_PRESS_ADD_WORD, true)

    fun setLongPressAddWord(
        context: Context,
        enabled: Boolean,
    ) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(LONG_PRESS_ADD_WORD, enabled).apply()
    }
}

object VisualKeyRemapPreferences {
    private const val PREFS = "visual_key_remaps_v1"

    private fun key(
        layoutName: String,
        modeName: String,
        row: Int,
        col: Int,
        slot: VisualRemapSlot,
    ): String = "$layoutName|$modeName|$row|$col|${slot.name}"

    fun get(
        context: Context,
        layoutName: String,
        modeName: String,
        row: Int,
        col: Int,
        slot: VisualRemapSlot,
    ): String? =
        context
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(key(layoutName, modeName, row, col, slot), null)

    fun set(
        context: Context,
        layoutName: String,
        modeName: String,
        row: Int,
        col: Int,
        slot: VisualRemapSlot,
        text: String?,
    ) {
        val editor = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
        val prefKey = key(layoutName, modeName, row, col, slot)
        if (text.isNullOrBlank()) {
            editor.remove(prefKey)
        } else {
            editor.putString(prefKey, text)
        }
        editor.apply()
    }

    fun clearKey(
        context: Context,
        layoutName: String,
        modeName: String,
        row: Int,
        col: Int,
    ) {
        val editor = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
        VisualRemapSlot.entries.forEach { slot ->
            editor.remove(key(layoutName, modeName, row, col, slot))
        }
        editor.apply()
    }

    fun apply(
        context: Context,
        layoutName: String,
        modeName: String,
        keyboard: KeyboardC,
    ): KeyboardC =
        keyboard.copy(
            arr =
                keyboard.arr.mapIndexed { row, rowKeys ->
                    rowKeys.mapIndexed { col, keyItem ->
                        applyToKey(context, layoutName, modeName, row, col, keyItem)
                    }
                },
        )

    private fun applyToKey(
        context: Context,
        layoutName: String,
        modeName: String,
        row: Int,
        col: Int,
        item: KeyItemC,
    ): KeyItemC =
        item.copy(
            topLeft = overrideKey(context, layoutName, modeName, row, col, VisualRemapSlot.TOP_LEFT, item.topLeft),
            top = overrideKey(context, layoutName, modeName, row, col, VisualRemapSlot.TOP, item.top),
            topRight = overrideKey(context, layoutName, modeName, row, col, VisualRemapSlot.TOP_RIGHT, item.topRight),
            left = overrideKey(context, layoutName, modeName, row, col, VisualRemapSlot.LEFT, item.left),
            center = overrideKey(context, layoutName, modeName, row, col, VisualRemapSlot.CENTER, item.center) ?: item.center,
            right = overrideKey(context, layoutName, modeName, row, col, VisualRemapSlot.RIGHT, item.right),
            bottomLeft = overrideKey(context, layoutName, modeName, row, col, VisualRemapSlot.BOTTOM_LEFT, item.bottomLeft),
            bottom = overrideKey(context, layoutName, modeName, row, col, VisualRemapSlot.BOTTOM, item.bottom),
            bottomRight = overrideKey(context, layoutName, modeName, row, col, VisualRemapSlot.BOTTOM_RIGHT, item.bottomRight),
        )

    private fun overrideKey(
        context: Context,
        layoutName: String,
        modeName: String,
        row: Int,
        col: Int,
        slot: VisualRemapSlot,
        original: KeyC?,
    ): KeyC? {
        val text = get(context, layoutName, modeName, row, col, slot) ?: return original
        return (original ?: KeyC(text)).copy(
            action = KeyAction.CommitText(text),
            display = KeyDisplay.TextDisplay(text),
        )
    }
}
