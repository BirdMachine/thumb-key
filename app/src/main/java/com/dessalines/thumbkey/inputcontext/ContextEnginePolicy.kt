package com.dessalines.thumbkey.inputcontext

import android.view.inputmethod.EditorInfo

data class InputCapabilities(
    val isSensitive: Boolean,
    val canOfferSuggestions: Boolean,
    val canTransformSelection: Boolean,
    val canReplaceSelection: Boolean,
    val supportsNewline: Boolean,
    val prefersImeAction: Boolean,
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

        return InputCapabilities(
            isSensitive = sensitive,
            canOfferSuggestions =
                editable &&
                    textLike &&
                    !(sensitive && settings.suppressSensitiveSuggestions),
            canTransformSelection = editable && textLike && context.hasSelection && !sensitive,
            canReplaceSelection = editable && context.hasSelection,
            supportsNewline = editable && textLike && (!adaptive || context.isMultiLine),
            prefersImeAction = adaptive && hasExplicitImeAction && !context.isMultiLine,
            shouldPreserveStructuredToken = adaptive && settings.preserveEmailAndUrlTokens && textLike,
        )
    }
}
