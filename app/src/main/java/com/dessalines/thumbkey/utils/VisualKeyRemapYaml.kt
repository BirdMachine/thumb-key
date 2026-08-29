package com.dessalines.thumbkey.utils

import com.dessalines.thumbkey.ui.components.keyboard.VisualRemapSlot
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlin.reflect.KParameter
import kotlin.reflect.full.instanceParameter
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.full.memberProperties

private fun KeyboardCSerializable.keyItem(
    row: Int,
    col: Int,
): KeyItemCSerializable? {
    val propertyName = "key${row}_$col"
    val property = KeyboardCSerializable::class.memberProperties.firstOrNull { it.name == propertyName }
    return property?.getter?.call(this) as? KeyItemCSerializable
}

private fun KeyboardCSerializable.withKeyItem(
    row: Int,
    col: Int,
    item: KeyItemCSerializable,
): KeyboardCSerializable {
    val propertyName = "key${row}_$col"
    val copyFunction = KeyboardCSerializable::class.memberFunctions.first { it.name == "copy" }
    val instance = copyFunction.instanceParameter ?: error("KeyboardCSerializable.copy has no instance parameter")
    val target =
        copyFunction.parameters.firstOrNull { it.name == propertyName }
            ?: error("Unsupported key coordinate $row,$col")
    val args = mutableMapOf<KParameter, Any?>(instance to this, target to item)
    return copyFunction.callBy(args) as KeyboardCSerializable
}

private fun KeyItemCSerializable.slot(slot: VisualRemapSlot): KeyCSerializable? =
    when (slot) {
        VisualRemapSlot.TOP_LEFT -> topLeft
        VisualRemapSlot.TOP -> top
        VisualRemapSlot.TOP_RIGHT -> topRight
        VisualRemapSlot.LEFT -> left
        VisualRemapSlot.CENTER -> center
        VisualRemapSlot.RIGHT -> right
        VisualRemapSlot.BOTTOM_LEFT -> bottomLeft
        VisualRemapSlot.BOTTOM -> bottom
        VisualRemapSlot.BOTTOM_RIGHT -> bottomRight
    }

private fun KeyItemCSerializable.withSlot(
    slot: VisualRemapSlot,
    value: KeyCSerializable?,
): KeyItemCSerializable =
    when (slot) {
        VisualRemapSlot.TOP_LEFT -> copy(topLeft = value)
        VisualRemapSlot.TOP -> copy(top = value)
        VisualRemapSlot.TOP_RIGHT -> copy(topRight = value)
        VisualRemapSlot.LEFT -> copy(left = value)
        VisualRemapSlot.CENTER -> copy(center = value)
        VisualRemapSlot.RIGHT -> copy(right = value)
        VisualRemapSlot.BOTTOM_LEFT -> copy(bottomLeft = value)
        VisualRemapSlot.BOTTOM -> copy(bottom = value)
        VisualRemapSlot.BOTTOM_RIGHT -> copy(bottomRight = value)
    }

private fun KeyboardDefinitionModesSerializable.mode(mode: KeyboardMode): KeyboardCSerializable? =
    when (mode) {
        KeyboardMode.MAIN -> main
        KeyboardMode.SHIFTED -> shifted
        KeyboardMode.NUMERIC -> numeric
        KeyboardMode.CTRLED -> ctrled
        KeyboardMode.ALTED -> alted
        KeyboardMode.EMOJI -> emoji
        KeyboardMode.CLIPBOARD -> null
    }

private fun KeyboardDefinitionModesSerializable.withMode(
    mode: KeyboardMode,
    keyboard: KeyboardCSerializable,
): KeyboardDefinitionModesSerializable =
    when (mode) {
        KeyboardMode.MAIN -> copy(main = keyboard)
        KeyboardMode.SHIFTED -> copy(shifted = keyboard)
        KeyboardMode.NUMERIC -> copy(numeric = keyboard)
        KeyboardMode.CTRLED -> copy(ctrled = keyboard)
        KeyboardMode.ALTED -> copy(alted = keyboard)
        KeyboardMode.EMOJI -> copy(emoji = keyboard)
        KeyboardMode.CLIPBOARD -> this
    }

fun getVisualTextOverride(
    yaml: String,
    layoutName: String,
    mode: KeyboardMode,
    row: Int,
    col: Int,
    slot: VisualRemapSlot,
): String? {
    if (yaml.isBlank()) return null
    val all = deserializeKeyModifications(yaml)
    return all[layoutName]
        ?.mode(mode)
        ?.keyItem(row, col)
        ?.slot(slot)
        ?.text
}

fun updateVisualTextOverride(
    yaml: String,
    layoutName: String,
    mode: KeyboardMode,
    row: Int,
    col: Int,
    slot: VisualRemapSlot,
    text: String?,
): String {
    val all = if (yaml.isBlank()) emptyMap() else deserializeKeyModifications(yaml)
    val layout = all[layoutName] ?: KeyboardDefinitionModesSerializable()
    val keyboard = layout.mode(mode) ?: KeyboardCSerializable()
    val item = keyboard.keyItem(row, col) ?: KeyItemCSerializable()
    val override = text?.takeIf { it.isNotEmpty() }?.let { KeyCSerializable(text = it) }
    val updatedKeyboard = keyboard.withKeyItem(row, col, item.withSlot(slot, override))
    val updated = all.toMutableMap().apply { put(layoutName, layout.withMode(mode, updatedKeyboard)) }
    val serializer = MapSerializer(String.serializer(), KeyboardDefinitionModesSerializable.serializer())
    return getYaml().encodeToString(serializer, updated)
}
