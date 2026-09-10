package com.dessalines.thumbkey.ui.components.keyboard

import android.view.KeyEvent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.dessalines.thumbkey.utils.KeyAction

/**
 * Routes Keywi key presses into an Input Palette's search query instead of the host editor.
 *
 * InputMethodService cannot summon a second IME for a text field inside its own window, so
 * searchable palettes deliberately reuse Keywi itself as their input surface.
 */
object PaletteSearchCapture {
    var active by mutableStateOf(false)
        private set

    var query by mutableStateOf("")
        private set

    fun activate() {
        active = true
    }

    fun release(clear: Boolean = false) {
        active = false
        if (clear) query = ""
    }

    fun clear() {
        query = ""
    }

    fun consume(action: KeyAction): Boolean {
        if (!active) return false

        return when (action) {
            is KeyAction.CommitText -> {
                query += action.text
                true
            }

            is KeyAction.DeleteKeyAction -> {
                query = query.dropLastCodePoint()
                true
            }

            is KeyAction.IMECompleteAction -> {
                release()
                true
            }

            is KeyAction.SendEvent -> {
                when (action.event.keyCode) {
                    KeyEvent.KEYCODE_DEL -> {
                        query = query.dropLastCodePoint()
                        true
                    }

                    KeyEvent.KEYCODE_ENTER -> {
                        release()
                        true
                    }

                    else -> false
                }
            }

            else -> false
        }
    }
}

private fun String.dropLastCodePoint(): String {
    if (isEmpty()) return this
    val end = offsetByCodePoints(length, -1)
    return substring(0, end)
}
