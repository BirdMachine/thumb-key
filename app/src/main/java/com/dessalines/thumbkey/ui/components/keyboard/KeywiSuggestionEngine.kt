package com.dessalines.thumbkey.ui.components.keyboard

import android.content.Context
import android.provider.UserDictionary
import java.util.Locale

private const val PERSONAL_PREFS = "keywi_personal_dictionary"
private const val PERSONAL_WORDS = "words"
private const val ENGLISH_ASSET = "keywi_en_frequency.txt"

object KeywiPersonalDictionary {
    fun add(
        context: Context,
        word: String,
    ) {
        val clean = word.trim()
        if (clean.isEmpty()) return
        val prefs = context.getSharedPreferences(PERSONAL_PREFS, Context.MODE_PRIVATE)
        val next = prefs.getStringSet(PERSONAL_WORDS, emptySet()).orEmpty().toMutableSet()
        next.removeAll { it.equals(clean, ignoreCase = true) }
        next.add(clean)
        prefs.edit().putStringSet(PERSONAL_WORDS, next).apply()
    }

    fun words(context: Context): List<String> =
        context
            .getSharedPreferences(PERSONAL_PREFS, Context.MODE_PRIVATE)
            .getStringSet(PERSONAL_WORDS, emptySet())
            .orEmpty()
            .toList()
}

object KeywiSuggestionEngine {
    private var bundledWords: List<String>? = null
    private var androidWords: List<Pair<String, Int>> = emptyList()
    private var androidWordsLoadedAt = 0L

    private fun loadBundled(context: Context): List<String> {
        bundledWords?.let { return it }
        return runCatching {
            context.assets.open(ENGLISH_ASSET).bufferedReader().useLines { lines ->
                lines.map(String::trim).filter(String::isNotEmpty).toList()
            }
        }.getOrDefault(emptyList()).also { bundledWords = it }
    }

    private fun loadAndroidWords(context: Context): List<Pair<String, Int>> {
        val now = System.currentTimeMillis()
        if (now - androidWordsLoadedAt < 5_000L) return androidWords
        androidWordsLoadedAt = now
        androidWords =
            runCatching {
                val result = mutableListOf<Pair<String, Int>>()
                context.contentResolver
                    .query(
                        UserDictionary.Words.CONTENT_URI,
                        arrayOf(UserDictionary.Words.WORD, UserDictionary.Words.FREQUENCY),
                        null,
                        null,
                        "${UserDictionary.Words.FREQUENCY} DESC",
                    )?.use { cursor ->
                        val wordColumn = cursor.getColumnIndex(UserDictionary.Words.WORD)
                        val frequencyColumn = cursor.getColumnIndex(UserDictionary.Words.FREQUENCY)
                        while (cursor.moveToNext() && result.size < 1000) {
                            val word = cursor.getString(wordColumn)?.trim().orEmpty()
                            if (word.isNotEmpty()) {
                                val frequency = if (frequencyColumn >= 0) cursor.getInt(frequencyColumn) else 0
                                result += word to frequency
                            }
                        }
                    }
                result
            }.getOrDefault(emptyList())
        return androidWords
    }

    fun suggest(
        context: Context,
        token: String,
        alphaPrefix: String,
        limit: Int = 5,
    ): List<String> {
        val tokenQuery = token.trim()
        val alphaQuery = alphaPrefix.trim()
        if (tokenQuery.length < 2 && alphaQuery.length < 2) return emptyList()

        val query = if (tokenQuery.any { !it.isLetter() && it != '\'' }) tokenQuery else alphaQuery
        val normalizedQuery = query.lowercase(Locale.US)
        val seen = linkedSetOf<String>()
        val results = mutableListOf<String>()

        fun add(candidate: String) {
            if (results.size >= limit) return
            val clean = candidate.trim()
            if (clean.isEmpty() || clean.equals(query, ignoreCase = true)) return
            if (!clean.lowercase(Locale.US).startsWith(normalizedQuery)) return
            val key = clean.lowercase(Locale.US)
            if (seen.add(key)) results += clean
        }

        KeywiPersonalDictionary
            .words(context)
            .sortedBy { it.lowercase(Locale.US) }
            .forEach(::add)

        loadAndroidWords(context)
            .sortedByDescending { it.second }
            .forEach { (word, _) -> add(word) }

        if (results.size < limit && alphaQuery.length >= 2 && query == alphaQuery) {
            val normalizedAlpha = alphaQuery.lowercase(Locale.US)
            for (word in loadBundled(context)) {
                if (results.size >= limit) break
                if (word.lowercase(Locale.US).startsWith(normalizedAlpha)) add(word)
            }
        }

        val preserveCase = alphaQuery.firstOrNull()?.isUpperCase() == true && query == alphaQuery
        return if (preserveCase) {
            results.map { word -> word.replaceFirstChar { it.titlecase(Locale.US) } }
        } else {
            results
        }
    }
}
