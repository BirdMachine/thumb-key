package com.dessalines.thumbkey.ui.components.keyboard

import android.content.Context
import org.json.JSONArray

object KaomojiPreferences {
    private const val PREFS = "keywi_kaomoji_preferences"
    private const val FAVORITES = "favorites"
    private const val RECENTS = "recents"
    private const val MAX_RECENTS = 24

    fun loadFavorites(context: Context): Set<String> =
        context
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getStringSet(FAVORITES, emptySet())
            ?.toSet()
            ?: emptySet()

    fun toggleFavorite(
        context: Context,
        text: String,
    ): Set<String> {
        val next = loadFavorites(context).toMutableSet()
        if (!next.add(text)) next.remove(text)
        context
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putStringSet(FAVORITES, next)
            .apply()
        return next
    }

    fun loadRecents(context: Context): List<String> {
        val raw =
            context
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(RECENTS, null)
                ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) add(array.getString(index))
            }
        }.getOrDefault(emptyList())
    }

    fun recordRecent(
        context: Context,
        text: String,
    ): List<String> {
        val next = loadRecents(context).toMutableList()
        next.remove(text)
        next.add(0, text)
        while (next.size > MAX_RECENTS) next.removeAt(next.lastIndex)

        val array = JSONArray()
        next.forEach(array::put)
        context
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(RECENTS, array.toString())
            .apply()
        return next
    }
}
