package com.dessalines.thumbkey.inputcontext

import android.text.InputType
import android.view.inputmethod.EditorInfo

/**
 * Stable snapshot of the host editor state that Keywi can reason about.
 *
 * Keep this model independent from UI and feature-specific policy: the Context Engine,
 * Advanced Characters, and the Tools surface should all consume the same facts rather
 * than each interpreting EditorInfo on their own.
 */
data class InputContext(
    val packageName: String?,
    val fieldId: Int,
    val inputType: Int,
    val imeOptions: Int,
    val privateImeOptions: String?,
    val fieldKind: FieldKind,
    val isMultiLine: Boolean,
    val isPassword: Boolean,
    val isReadOnly: Boolean,
    val selection: SelectionContext = SelectionContext(),
) {
    val hasSelection: Boolean
        get() = selection.hasSelection

    companion object {
        fun fromEditorInfo(editorInfo: EditorInfo?): InputContext {
            if (editorInfo == null) {
                return InputContext(
                    packageName = null,
                    fieldId = 0,
                    inputType = InputType.TYPE_NULL,
                    imeOptions = 0,
                    privateImeOptions = null,
                    fieldKind = FieldKind.UNKNOWN,
                    isMultiLine = false,
                    isPassword = false,
                    isReadOnly = false,
                )
            }

            val inputType = editorInfo.inputType
            val inputClass = inputType and InputType.TYPE_MASK_CLASS
            val variation = inputType and InputType.TYPE_MASK_VARIATION
            val flags = inputType and InputType.TYPE_MASK_FLAGS

            val fieldKind =
                when (inputClass) {
                    InputType.TYPE_CLASS_TEXT -> FieldKind.TEXT
                    InputType.TYPE_CLASS_NUMBER -> FieldKind.NUMBER
                    InputType.TYPE_CLASS_PHONE -> FieldKind.PHONE
                    InputType.TYPE_CLASS_DATETIME -> FieldKind.DATE_TIME
                    InputType.TYPE_NULL -> FieldKind.UNKNOWN
                    else -> FieldKind.OTHER
                }

            val passwordVariations =
                setOf(
                    InputType.TYPE_TEXT_VARIATION_PASSWORD,
                    InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
                    InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD,
                )
            val numberPassword =
                inputClass == InputType.TYPE_CLASS_NUMBER &&
                    variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD

            return InputContext(
                packageName = editorInfo.packageName,
                fieldId = editorInfo.fieldId,
                inputType = inputType,
                imeOptions = editorInfo.imeOptions,
                privateImeOptions = editorInfo.privateImeOptions,
                fieldKind = fieldKind,
                isMultiLine =
                    inputClass == InputType.TYPE_CLASS_TEXT &&
                        (flags and InputType.TYPE_TEXT_FLAG_MULTI_LINE) != 0,
                isPassword =
                    (inputClass == InputType.TYPE_CLASS_TEXT && variation in passwordVariations) ||
                        numberPassword,
                isReadOnly = inputType == InputType.TYPE_NULL,
            )
        }
    }
}

data class SelectionContext(
    val start: Int = 0,
    val end: Int = 0,
    val selectedText: String? = null,
) {
    val hasSelection: Boolean
        get() = start >= 0 && end >= 0 && start != end

    val normalizedStart: Int
        get() = minOf(start, end)

    val normalizedEnd: Int
        get() = maxOf(start, end)
}

enum class FieldKind {
    TEXT,
    NUMBER,
    PHONE,
    DATE_TIME,
    OTHER,
    UNKNOWN,
}
