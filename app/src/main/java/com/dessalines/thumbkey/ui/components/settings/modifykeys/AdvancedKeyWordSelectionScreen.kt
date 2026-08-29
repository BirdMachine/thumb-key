package com.dessalines.thumbkey.ui.components.settings.modifykeys

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.dessalines.thumbkey.db.AppSettingsViewModel
import com.dessalines.thumbkey.db.DEFAULT_KEYBOARD_LAYOUT
import com.dessalines.thumbkey.db.DEFAULT_KEY_MODIFICATIONS
import com.dessalines.thumbkey.db.KeyModificationsUpdate
import com.dessalines.thumbkey.ui.components.keyboard.AdvancedKeyWordPreferences
import com.dessalines.thumbkey.ui.components.keyboard.VisualRemapSlot
import com.dessalines.thumbkey.utils.KeyAction
import com.dessalines.thumbkey.utils.KeyC
import com.dessalines.thumbkey.utils.KeyDisplay
import com.dessalines.thumbkey.utils.KeyItemC
import com.dessalines.thumbkey.utils.KeyboardC
import com.dessalines.thumbkey.utils.KeyboardLayout
import com.dessalines.thumbkey.utils.KeyboardMode
import com.dessalines.thumbkey.utils.SimpleTopAppBar
import com.dessalines.thumbkey.utils.getModifiedKeyboardDefinition
import com.dessalines.thumbkey.utils.getVisualTextOverride
import com.dessalines.thumbkey.utils.updateVisualTextOverride

private val MATRIX_GREEN = Color(0xFF52FF52)
private val MATRIX_DIM = Color(0xFF1FAE3B)
private val MATRIX_BLACK = Color(0xFF001006)

@Composable
fun AdvancedKeyWordSelectionScreen(
    navController: NavController,
    appSettingsViewModel: AppSettingsViewModel,
) {
    val settings by appSettingsViewModel.appSettings.observeAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val scrollState = rememberScrollState()
    val layout =
        KeyboardLayout.entries.getOrElse(settings?.keyboardLayout ?: DEFAULT_KEYBOARD_LAYOUT) {
            KeyboardLayout.entries.first()
        }
    val yaml = settings?.keyModifications ?: DEFAULT_KEY_MODIFICATIONS
    var selectedMode by remember { mutableStateOf(KeyboardMode.MAIN) }
    var selectedRow by remember { mutableIntStateOf(0) }
    var selectedCol by remember { mutableIntStateOf(0) }
    var showCurrentWord by remember { mutableStateOf(AdvancedKeyWordPreferences.showCurrentWord(context)) }
    var longPressAdd by remember { mutableStateOf(AdvancedKeyWordPreferences.longPressAddWord(context)) }
    var editorMessage by remember { mutableStateOf<String?>(null) }

    val effectiveDefinition =
        if (yaml.isNotBlank()) {
            getModifiedKeyboardDefinition(layout, yaml) ?: layout.keyboardDefinition
        } else {
            layout.keyboardDefinition
        }
    val keyboard = effectiveDefinition.keyboardForMode(selectedMode)
    val selectedKey = keyboard?.arr?.getOrNull(selectedRow)?.getOrNull(selectedCol)
    val overrides = remember { mutableStateMapOf<VisualRemapSlot, String>() }

    LaunchedEffect(layout.name, selectedMode, selectedRow, selectedCol, yaml) {
        overrides.clear()
        VisualRemapSlot.entries.forEach { slot ->
            overrides[slot] =
                runCatching {
                    getVisualTextOverride(yaml, layout.name, selectedMode, selectedRow, selectedCol, slot)
                }.getOrNull().orEmpty()
        }
    }

    Scaffold(
        topBar = { SimpleTopAppBar(text = "Advanced Key & Word Selection", navController = navController) },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .padding(padding)
                    .verticalScroll(scrollState)
                    .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MATRIX_BLACK),
                border = BorderStroke(1.dp, MATRIX_DIM),
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "KEYWI // MATRIX REMAPPER",
                        color = MATRIX_GREEN,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                    )
                    Text(
                        "Tap a key below, then override any of its nine MessageEase-style swipe positions. Blank fields inherit the original mapping.",
                        color = MATRIX_GREEN.copy(alpha = 0.78f),
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        "Layout: ${layout.keyboardDefinition.title}",
                        color = MATRIX_GREEN,
                        fontFamily = FontFamily.Monospace,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(KeyboardMode.MAIN, KeyboardMode.SHIFTED, KeyboardMode.NUMERIC).forEach { mode ->
                            FilterChip(
                                selected = selectedMode == mode,
                                onClick = {
                                    selectedMode = mode
                                    selectedRow = 0
                                    selectedCol = 0
                                },
                                label = { Text(mode.name.lowercase(), fontFamily = FontFamily.Monospace) },
                            )
                        }
                    }
                }
            }

            keyboard?.let {
                MatrixKeyboardPreview(
                    keyboard = it,
                    selectedRow = selectedRow,
                    selectedCol = selectedCol,
                    onSelect = { row, col ->
                        selectedRow = row
                        selectedCol = col
                        editorMessage = null
                    },
                )
            }

            selectedKey?.let { key ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MATRIX_BLACK),
                    border = BorderStroke(1.dp, MATRIX_GREEN),
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "EDIT key${selectedRow}_$selectedCol",
                            color = MATRIX_GREEN,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                        )
                        RemapEditorGrid(key, overrides)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Button(
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    runCatching {
                                        var nextYaml = yaml
                                        VisualRemapSlot.entries.forEach { slot ->
                                            nextYaml =
                                                updateVisualTextOverride(
                                                    yaml = nextYaml,
                                                    layoutName = layout.name,
                                                    mode = selectedMode,
                                                    row = selectedRow,
                                                    col = selectedCol,
                                                    slot = slot,
                                                    text = overrides[slot].orEmpty(),
                                                )
                                        }
                                        appSettingsViewModel.updateKeyModifications(
                                            KeyModificationsUpdate(id = 1, keyModifications = nextYaml),
                                        )
                                    }.onSuccess {
                                        editorMessage = "Overrides saved ✓"
                                    }.onFailure {
                                        editorMessage = "Could not save: ${it.message}"
                                    }
                                },
                            ) {
                                Text("SAVE KEY")
                            }
                            Button(
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    VisualRemapSlot.entries.forEach { overrides[it] = "" }
                                    editorMessage = "Cleared in editor — tap SAVE KEY to restore originals."
                                },
                            ) {
                                Text("CLEAR")
                            }
                        }
                        editorMessage?.let {
                            Text(it, color = MATRIX_GREEN, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }

            Card {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Current word + personal dictionary", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "This adds the unfinished word (like “scr”) to the left side of the suggestion strip, MessageEase-style.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    SettingSwitchRow(
                        title = "Show unfinished word in suggestions",
                        checked = showCurrentWord,
                        onCheckedChange = {
                            showCurrentWord = it
                            AdvancedKeyWordPreferences.setShowCurrentWord(context, it)
                        },
                    )
                    SettingSwitchRow(
                        title = "Long-press unfinished word → add to personal dictionary",
                        checked = longPressAdd,
                        enabled = showCurrentWord,
                        onCheckedChange = {
                            longPressAdd = it
                            AdvancedKeyWordPreferences.setLongPressAddWord(context, it)
                        },
                    )
                    Text(
                        "Long-press uses Android's user dictionary provider from the keyboard itself.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = { navController.navigate("modifyKeys") },
            ) {
                Text("OPEN RAW YAML KEY EDITOR")
            }
        }
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, enabled = enabled, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun MatrixKeyboardPreview(
    keyboard: KeyboardC,
    selectedRow: Int,
    selectedCol: Int,
    onSelect: (Int, Int) -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.Black),
        border = BorderStroke(1.dp, MATRIX_DIM),
    ) {
        Column(Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            keyboard.arr.forEachIndexed { row, keys ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    keys.forEachIndexed { col, key ->
                        MatrixKey(
                            key = key,
                            selected = row == selectedRow && col == selectedCol,
                            modifier = Modifier.weight(key.widthMultiplier.toFloat().coerceAtLeast(1f)),
                            onClick = { onSelect(row, col) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MatrixKey(
    key: KeyItemC,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val borderColor = if (selected) MATRIX_GREEN else MATRIX_DIM.copy(alpha = 0.55f)
    Card(
        modifier = modifier.heightIn(min = 82.dp).clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = if (selected) Color(0xFF05240C) else Color(0xFF010804)),
        border = BorderStroke(if (selected) 2.dp else 1.dp, borderColor),
        shape = RoundedCornerShape(5.dp),
    ) {
        Column(Modifier.padding(3.dp), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            MatrixLabelRow(key.topLeft, key.top, key.topRight)
            MatrixLabelRow(key.left, key.center, key.right, centerBold = true)
            MatrixLabelRow(key.bottomLeft, key.bottom, key.bottomRight)
        }
    }
}

@Composable
private fun MatrixLabelRow(
    left: KeyC?,
    center: KeyC?,
    right: KeyC?,
    centerBold: Boolean = false,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(1.dp)) {
        MatrixLabel(left, Modifier.weight(1f))
        MatrixLabel(center, Modifier.weight(1f), centerBold)
        MatrixLabel(right, Modifier.weight(1f))
    }
}

@Composable
private fun MatrixLabel(
    key: KeyC?,
    modifier: Modifier,
    bold: Boolean = false,
) {
    Text(
        text = keyLabel(key),
        modifier = modifier,
        color = if (bold) MATRIX_GREEN else MATRIX_GREEN.copy(alpha = 0.72f),
        fontFamily = FontFamily.Monospace,
        fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
        fontSize = if (bold) 16.sp else 10.sp,
        textAlign = TextAlign.Center,
        maxLines = 1,
    )
}

@Composable
private fun RemapEditorGrid(
    key: KeyItemC,
    overrides: MutableMap<VisualRemapSlot, String>,
) {
    val originals =
        mapOf(
            VisualRemapSlot.TOP_LEFT to keyLabel(key.topLeft),
            VisualRemapSlot.TOP to keyLabel(key.top),
            VisualRemapSlot.TOP_RIGHT to keyLabel(key.topRight),
            VisualRemapSlot.LEFT to keyLabel(key.left),
            VisualRemapSlot.CENTER to keyLabel(key.center),
            VisualRemapSlot.RIGHT to keyLabel(key.right),
            VisualRemapSlot.BOTTOM_LEFT to keyLabel(key.bottomLeft),
            VisualRemapSlot.BOTTOM to keyLabel(key.bottom),
            VisualRemapSlot.BOTTOM_RIGHT to keyLabel(key.bottomRight),
        )
    val rows =
        listOf(
            listOf(VisualRemapSlot.TOP_LEFT, VisualRemapSlot.TOP, VisualRemapSlot.TOP_RIGHT),
            listOf(VisualRemapSlot.LEFT, VisualRemapSlot.CENTER, VisualRemapSlot.RIGHT),
            listOf(VisualRemapSlot.BOTTOM_LEFT, VisualRemapSlot.BOTTOM, VisualRemapSlot.BOTTOM_RIGHT),
        )
    rows.forEach { slots ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            slots.forEach { slot ->
                OutlinedTextField(
                    value = overrides[slot].orEmpty(),
                    onValueChange = { overrides[slot] = it },
                    label = { Text(slot.name.lowercase().replace('_', ' '), fontSize = 9.sp) },
                    placeholder = { Text(originals[slot].orEmpty(), fontFamily = FontFamily.Monospace) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

private fun keyLabel(key: KeyC?): String {
    if (key == null) return ""
    return when (val display = key.display) {
        is KeyDisplay.TextDisplay -> {
            display.text
        }

        is KeyDisplay.IconDisplay -> {
            "◈"
        }

        null -> {
            when (val action = key.action) {
                is KeyAction.CommitText -> action.text
                else -> action::class.simpleName?.take(3).orEmpty()
            }
        }
    }
}

private fun com.dessalines.thumbkey.utils.KeyboardDefinition.keyboardForMode(mode: KeyboardMode): KeyboardC? =
    when (mode) {
        KeyboardMode.MAIN -> {
            modes.main
        }

        KeyboardMode.SHIFTED -> {
            modes.shifted
        }

        KeyboardMode.NUMERIC -> {
            modes.numeric
        }

        KeyboardMode.CTRLED -> {
            modes.ctrled
        }

        KeyboardMode.ALTED -> {
            modes.alted
        }

        KeyboardMode.EMOJI -> {
            modes.emoji
        }

        KeyboardMode.CLIPBOARD -> {
            null
        }
    }
