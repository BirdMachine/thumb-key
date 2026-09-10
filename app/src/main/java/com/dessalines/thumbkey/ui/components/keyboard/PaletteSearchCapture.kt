package com.dessalines.thumbkey.ui.components.keyboard

import android.view.KeyEvent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Routes Keywi text into an Input Palette search query instead of the host editor.
 *
 * An IME cannot summon a second IME for a text field inside its own window. Capturing at the
 * InputConnection boundary lets every Keywi layout keep working as the palette's search keyboard.
 */
object PaletteSearchCapture {
    var active by mutableStateOf(false)
        private set

    var query by mutableStateOf("")
        private set

    private var bypassDepth = 0

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

    fun consumeCommitText(text: CharSequence?): Boolean {
        if (!shouldCapture()) return false
        query += text?.toString().orEmpty()
        return true
    }

    fun consumeKeyEvent(event: KeyEvent): Boolean {
        if (!shouldCapture()) return false
        return when (event.keyCode) {
            KeyEvent.KEYCODE_DEL -> {
                if (event.action == KeyEvent.ACTION_DOWN) query = query.dropLastCodePoint()
                true
            }

            KeyEvent.KEYCODE_ENTER -> {
                if (event.action == KeyEvent.ACTION_DOWN) release()
                true
            }

            else -> false
        }
    }

    fun consumeDeleteBeforeCursor(beforeLength: Int): Boolean {
        if (!shouldCapture() || beforeLength <= 0) return false
        repeat(beforeLength) {
            query = query.dropLastCodePoint()
        }
        return true
    }

    fun <T> bypass(block: () -> T): T {
        bypassDepth += 1
        return try {
            block()
        } finally {
            bypassDepth -= 1
        }
    }

    fun shouldCapture(): Boolean = active && bypassDepth == 0
}

private fun String.dropLastCodePoint(): String {
    if (isEmpty()) return this
    val end = offsetByCodePoints(length, -1)
    return substring(0, end)
}
