from pathlib import Path

p = Path('app/src/main/java/com/dessalines/thumbkey/ui/components/settings/lookandfeel/AdvancedLookAndFeelScreen.kt')
text = p.read_text()

# Imports for the compact gradient preview rail.
text = text.replace(
    'import androidx.compose.foundation.background\n',
    'import androidx.compose.foundation.background\nimport androidx.compose.foundation.clickable\n',
)
text = text.replace(
    'import androidx.compose.ui.draw.alpha\n',
    'import androidx.compose.ui.draw.alpha\nimport androidx.compose.ui.draw.clip\n',
)
text = text.replace(
    'import androidx.compose.ui.unit.dp\n',
    'import androidx.compose.ui.unit.dp\nimport androidx.compose.ui.unit.sp\n',
)
text = text.replace(
    'import androidx.navigation.NavController\n',
    'import androidx.navigation.NavController\nimport androidx.compose.foundation.shape.RoundedCornerShape\n',
)

# Overall density and hierarchy tweaks.
text = text.replace(
    'modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),',
    'modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),',
)
text = text.replace(
    'style = MaterialTheme.typography.titleMedium,',
    'style = MaterialTheme.typography.titleSmall,',
    1,
)
text = text.replace(
    'modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),',
    'modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 7.dp),',
)
text = text.replace(
    'verticalArrangement = Arrangement.spacedBy(8.dp),',
    'verticalArrangement = Arrangement.spacedBy(6.dp),',
)
text = text.replace(
    'Text(title, style = MaterialTheme.typography.titleLarge)',
    'Text(title, style = MaterialTheme.typography.titleMedium)',
)
text = text.replace(
    'Text("Toolbar lozenges", style = MaterialTheme.typography.titleLarge)',
    'Text("Toolbar lozenges", style = MaterialTheme.typography.titleMedium)',
)
text = text.replace(
    'Text("Keys", style = MaterialTheme.typography.titleLarge)',
    'Text("Keys", style = MaterialTheme.typography.titleMedium)',
)
text = text.replace(
    'Text("Typography", style = MaterialTheme.typography.titleLarge)',
    'Text("Typography", style = MaterialTheme.typography.titleMedium)',
)
text = text.replace(
    'Text("Suggestions", style = MaterialTheme.typography.titleLarge)',
    'Text("Suggestions", style = MaterialTheme.typography.titleMedium)',
)

# Replace the duplicate backdrop gradient-management UI with the shared manager.
start = text.index('        if (state.mode == BackdropMode.COLORFUL) {')
end = text.index('\n        }\n    }\n}\n\n@Composable\nprivate fun ManagedGradientEditor', start) + len('\n        }')
replacement = '''        if (state.mode == BackdropMode.COLORFUL) {
            ManagedGradientEditor(
                title = "Gradient",
                gradient = state.toBackdrop(),
                onGradientChange = { gradient ->
                    persist(
                        state.copy(
                            preset = BackdropPreset.CUSTOM,
                            angleDegrees = gradient.angleDegrees,
                            stops = gradient.stops,
                        ),
                    )
                },
            )
        }'''
text = text[:start] + replacement + text[end:]

# Replace the shared manager with a compact preview + accordion implementation.
start = text.index('@Composable\nprivate fun ManagedGradientEditor(')
end = text.index('\n@Composable\nprivate fun SuggestionLozengeAppearanceSection()', start)
new_manager = '''@Composable
private fun ManagedGradientEditor(
    title: String,
    gradient: KeyboardBackdrop,
    onGradientChange: (KeyboardBackdrop) -> Unit,
) {
    val context = LocalContext.current
    var gradients by remember { mutableStateOf(GradientLibrary.load(context)) }
    var menuOpen by remember { mutableStateOf(false) }
    var selectedName by remember { mutableStateOf("Current gradient") }
    var saveName by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(
                text = if (expanded) "Hide details  ▲" else "Edit gradient  ▼",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { expanded = !expanded }.padding(6.dp),
            )
        }

        Box {
            Button(onClick = { menuOpen = true }, modifier = Modifier.fillMaxWidth()) {
                Text(selectedName, fontSize = 13.sp)
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                gradients.forEach { saved ->
                    DropdownMenuItem(
                        text = { Text(saved.name) },
                        onClick = {
                            selectedName = saved.name
                            menuOpen = false
                            onGradientChange(saved.toBackdrop())
                        },
                    )
                }
                DropdownMenuItem(
                    text = { Text("＋ New gradient") },
                    onClick = {
                        selectedName = "New gradient"
                        menuOpen = false
                        expanded = true
                        onGradientChange(
                            KeyboardBackdrop(
                                angleDegrees = 0f,
                                stops =
                                    listOf(
                                        KeyboardGradientStop(0f, Color(0xFF151A2C)),
                                        KeyboardGradientStop(1f, Color(0xFFB5D8FF)),
                                    ),
                            ),
                        )
                    },
                )
            }
        }

        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .keyboardGradientBackground(gradient),
        )

        if (expanded) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f),
                            RoundedCornerShape(12.dp),
                        ).padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                GradientEditor(title, gradient, onGradientChange)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = saveName,
                        onValueChange = { saveName = it },
                        label = { Text("Save as") },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f),
                    )
                    Button(
                        enabled = saveName.isNotBlank(),
                        onClick = {
                            val saved =
                                GradientLibrary.saveCustom(
                                    context,
                                    SavedGradient(
                                        id = "",
                                        name = saveName.trim(),
                                        angleDegrees = gradient.angleDegrees,
                                        stops = gradient.stops,
                                    ),
                                )
                            selectedName = saved.name
                            saveName = ""
                            gradients = GradientLibrary.load(context)
                        },
                    ) {
                        Text("Save", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
'''
text = text[:start] + new_manager + text[end:]

# Replace GradientEditor with a denser details-only editor. Stops remain a nested disclosure.
start = text.index('@Composable\nprivate fun GradientEditor(')
end = text.index('\n@Composable\nprivate fun ColorEditor(', start)
new_editor = '''@Composable
private fun GradientEditor(
    label: String,
    gradient: KeyboardBackdrop,
    onChange: (KeyboardBackdrop) -> Unit,
) {
    var stopsExpanded by remember { mutableStateOf(false) }
    Text(
        "$label angle • ${gradient.angleDegrees.roundToInt()}°",
        style = MaterialTheme.typography.labelMedium,
    )
    Slider(
        value = gradient.angleDegrees,
        onValueChange = { onChange(gradient.copy(angleDegrees = it)) },
        valueRange = 0f..360f,
    )
    Text(
        text = "${gradient.stops.size} color stops  ${if (stopsExpanded) "▲" else "▼"}",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.clickable { stopsExpanded = !stopsExpanded }.padding(vertical = 4.dp),
    )
    if (stopsExpanded) {
        gradient.stops.forEachIndexed { index, stop ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.30f),
                        RoundedCornerShape(10.dp),
                    ).padding(7.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    "Stop ${index + 1} • ${(stop.position * 100).roundToInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                )
                Slider(
                    value = stop.position,
                    onValueChange = { position ->
                        val next =
                            gradient.stops
                                .toMutableList()
                                .also { it[index] = stop.copy(position = position) }
                                .sortedBy { it.position }
                        onChange(gradient.copy(stops = next))
                    },
                    valueRange = 0f..1f,
                )
                ColorEditor("Color", stop.color) { color ->
                    val next = gradient.stops.toMutableList().also { it[index] = stop.copy(color = color) }
                    onChange(gradient.copy(stops = next))
                }
            }
        }
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                val color = gradient.stops.firstOrNull()?.color ?: Color.White
                onChange(
                    gradient.copy(
                        stops =
                            (gradient.stops + KeyboardGradientStop(0.5f, color))
                                .sortedBy { it.position },
                    ),
                )
            },
        ) {
            Text("＋ Add stop", fontSize = 12.sp)
        }
    }
}
'''
text = text[:start] + new_editor + text[end:]

# Make color editor itself more compact.
text = text.replace(
    'Column(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {',
    'Column(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {',
)
text = text.replace(
    'Box(Modifier.size(28.dp).background(color))',
    'Box(Modifier.size(22.dp).clip(RoundedCornerShape(7.dp)).background(color))',
)
text = text.replace(
    'Text("$label • A ${(color.alpha * 100).roundToInt()}%")',
    'Text("$label • A ${(color.alpha * 100).roundToInt()}%", style = MaterialTheme.typography.labelMedium)',
)
text = text.replace(
    '    Text("$label ${(value * 255).roundToInt()}")\n',
    '    Text("$label ${(value * 255).roundToInt()}", style = MaterialTheme.typography.labelSmall)\n',
)

# Compact subsection labels.
text = text.replace('style = MaterialTheme.typography.titleMedium)', 'style = MaterialTheme.typography.titleSmall)')

# Ensure gradient modifier extension import exists.
text = text.replace(
    'import com.dessalines.thumbkey.ui.components.keyboard.KeyboardGradientStop\n',
    'import com.dessalines.thumbkey.ui.components.keyboard.KeyboardGradientStop\nimport com.dessalines.thumbkey.ui.components.keyboard.keyboardGradientBackground\n',
)

p.write_text(text)
