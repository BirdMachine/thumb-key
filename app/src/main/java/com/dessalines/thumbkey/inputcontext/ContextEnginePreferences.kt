package com.dessalines.thumbkey.inputcontext

import android.content.Context

data class ContextEngineSettings(
    val adaptToField: Boolean = true,
    val suppressSensitiveSuggestions: Boolean = true,
    val preserveEmailAndUrlTokens: Boolean = true,
)

object ContextEnginePreferences {
    private const val PREFS = "context_engine_preferences"
    private const val ADAPT_TO_FIELD = "adapt_to_field"
    private const val SUPPRESS_SENSITIVE_SUGGESTIONS = "suppress_sensitive_suggestions"
    private const val PRESERVE_EMAIL_AND_URL_TOKENS = "preserve_email_and_url_tokens"

    fun load(context: Context): ContextEngineSettings {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return ContextEngineSettings(
            adaptToField = prefs.getBoolean(ADAPT_TO_FIELD, true),
            suppressSensitiveSuggestions = prefs.getBoolean(SUPPRESS_SENSITIVE_SUGGESTIONS, true),
            preserveEmailAndUrlTokens = prefs.getBoolean(PRESERVE_EMAIL_AND_URL_TOKENS, true),
        )
    }

    fun setAdaptToField(
        context: Context,
        enabled: Boolean,
    ) {
        context
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(ADAPT_TO_FIELD, enabled)
            .apply()
    }

    fun setSuppressSensitiveSuggestions(
        context: Context,
        enabled: Boolean,
    ) {
        context
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(SUPPRESS_SENSITIVE_SUGGESTIONS, enabled)
            .apply()
    }

    fun setPreserveEmailAndUrlTokens(
        context: Context,
        enabled: Boolean,
    ) {
        context
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(PRESERVE_EMAIL_AND_URL_TOKENS, enabled)
            .apply()
    }
}
