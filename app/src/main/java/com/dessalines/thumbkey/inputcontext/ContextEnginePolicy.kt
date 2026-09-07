package com.dessalines.thumbkey.inputcontext

import android.view.inputmethod.EditorInfo

enum class SmartEnterBehavior {
    NEWLINE,
    IME_ACTION,
    LEGACY,
}

data class InputCapabilities(
    val isSensitive: Boolean,
    val canOfferSuggestions: Boolean,
    val canTransformSelection: Boolean,
    val canReplaceSelection: Boolean,
    val supportsNewline: Boolean,
    val prefersImeAction: Boolean,
    val smartEnterBehavior: SmartEnterBehavior,
    val shouldPreserveStructuredToken: Boolean,
)

object ContextEnginePolicy {
    fun evaluate(
        context: InputContext,
        settings: ContextEngineSettings = ContextEngineSettings(),
    ): InputCapabilities {
        val adaptive = settings.adaptToField
        val sensitive = adaptive && context.isPassword
        val editable = !context.isReadOnly
        val textLike = context.fieldKind == FieldKind.TEXT

        val imeAction = context.imeOptions and EditorInfo.IME_MASK_ACTION
        val hasExplicitImeAction =
            imeAction != EditorInfo.IME_ACTION_NONE &&
                imeAction != EditorInfo.IME_ACTION_UNSPECIFIED
        val supportsNewline = editable && textLike && (!adaptive || context.isMultiLine)
        val prefersImeAction = adaptive && hasExplicitImeAction && !context.isMultiLine
        val smartEnterBehavior =
            when {
                !settings.smartEnter || !adaptive -> SmartEnterBehavior.LEGACY
                context.isMultiLine && supportsNewline -> SmartEnterBehavior.NEWLINE
                prefersImeAction -> SmartEnterBehavior.IME_ACTION
                else -> SmartEnterBehavior.LEGACY
            }

        return InputCapabilities(
            isSensitive = sensitive,
            canOfferSuggestions =
                editable &&
                    textLike &&
                    !(sensitive && settings.suppressSensitiveSuggestions),
            canTransformSelection = editable && textLike && context.hasSelection && !sensitive,
            canReplaceSelection = editable && context.hasSelection,
            supportsNewline = supportsNewline,
            prefersImeAction = prefersImeAction,
            smartEnterBehavior = smartEnterBehavior,
            shouldPreserveStructuredToken = adaptive && settings.preserveEmailAndUrlTokens && textLike,
        )
    }
}
