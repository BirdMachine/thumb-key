package com.dessalines.thumbkey.ui.components.keyboard

import android.content.Context
import android.graphics.Typeface
import android.net.Uri
import java.io.File

object FontPreferences {
    private const val PREFS = "keyboard_font_preferences"
    private const val KEY_FILE = "font_file"
    private const val KEY_NAME = "font_name"

    fun displayName(context: Context): String =
        context
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_NAME, null) ?: "System default"

    fun importFont(
        context: Context,
        uri: Uri,
    ): Boolean =
        runCatching {
            val dir = File(context.filesDir, "keyboard-fonts").apply { mkdirs() }
            val name =
                uri.lastPathSegment
                    ?.substringAfterLast('/')
                    ?.takeIf { it.isNotBlank() } ?: "custom-font"
            val extension = if (name.lowercase().endsWith(".otf")) ".otf" else ".ttf"
            val target = File(dir, "active$extension")
            context.contentResolver.openInputStream(uri)!!.use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            }
            Typeface.createFromFile(target)
            context
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_FILE, target.absolutePath)
                .putString(KEY_NAME, name)
                .apply()
            true
        }.getOrDefault(false)

    fun clear(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.getString(KEY_FILE, null)?.let { File(it).delete() }
        prefs.edit().clear().apply()
    }

    fun typeface(context: Context): Typeface? {
        val path = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_FILE, null) ?: return null
        return runCatching { Typeface.createFromFile(path) }.getOrNull()
    }
}
