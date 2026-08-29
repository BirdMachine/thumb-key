from pathlib import Path


def replace(path, old, new):
    p = Path(path)
    text = p.read_text()
    if old not in text:
        raise RuntimeError(f"missing fragment in {path}: {old[:100]!r}")
    p.write_text(text.replace(old, new, 1))

# 1) Add the current gold key-border gradient to the shared saved-gradient library.
p = "app/src/main/java/com/dessalines/thumbkey/ui/components/keyboard/GradientLibrary.kt"
replace(
    p,
    '''            SavedGradient(\n                id = "builtin_sinebow",\n                name = "Sinebow",\n                angleDegrees = BackdropThemePreferences.stateForPreset(BackdropPreset.SINEBOW).angleDegrees,\n                stops = BackdropThemePreferences.stateForPreset(BackdropPreset.SINEBOW).stops,\n                builtIn = true,\n            ),\n''',
    '''            SavedGradient(\n                id = "builtin_sinebow",\n                name = "Sinebow",\n                angleDegrees = BackdropThemePreferences.stateForPreset(BackdropPreset.SINEBOW).angleDegrees,\n                stops = BackdropThemePreferences.stateForPreset(BackdropPreset.SINEBOW).stops,\n                builtIn = true,\n            ),\n            SavedGradient(\n                id = "builtin_golden_key_border",\n                name = "Golden Key Border",\n                angleDegrees = BIRDIE_GOLD_BORDER.angleDegrees,\n                stops = BIRDIE_GOLD_BORDER.stops,\n                builtIn = true,\n            ),\n''',
)

# 2) Add persistent suggestion-lozenge appearance controls.
prefs_path = Path("app/src/main/java/com/dessalines/thumbkey/ui/components/keyboard/SuggestionLozengePreferences.kt")
prefs_path.write_text(
    '''package com.dessalines.thumbkey.ui.components.keyboard\n\nimport android.content.Context\nimport androidx.compose.ui.graphics.Color\n\ndata class SuggestionLozengeState(\n    val backgroundColor: Color = Color(0xFF3A3D48),\n    val backgroundAlpha: Float = 0.50f,\n    val bestBackgroundAlpha: Float = 0.72f,\n    val borderColor: Color = Color.White,\n    val borderAlpha: Float = 0.16f,\n    val bestBorderAlpha: Float = 0.28f,\n    val borderWidth: Float = 0.8f,\n    val bestBorderWidth: Float = 1.2f,\n    val cornerRadius: Float = 18f,\n)\n\nobject SuggestionLozengePreferences {\n    private const val PREFS = "suggestion_lozenge_preferences"\n\n    fun load(context: Context): SuggestionLozengeState {\n        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)\n        return SuggestionLozengeState(\n            backgroundColor = Color(prefs.getLong("background_color", 0xFF3A3D48L).toULong()),\n            backgroundAlpha = prefs.getFloat("background_alpha", 0.50f).coerceIn(0f, 1f),\n            bestBackgroundAlpha = prefs.getFloat("best_background_alpha", 0.72f).coerceIn(0f, 1f),\n            borderColor = Color(prefs.getLong("border_color", 0xFFFFFFFFL).toULong()),\n            borderAlpha = prefs.getFloat("border_alpha", 0.16f).coerceIn(0f, 1f),\n            bestBorderAlpha = prefs.getFloat("best_border_alpha", 0.28f).coerceIn(0f, 1f),\n            borderWidth = prefs.getFloat("border_width", 0.8f).coerceIn(0f, 4f),\n            bestBorderWidth = prefs.getFloat("best_border_width", 1.2f).coerceIn(0f, 4f),\n            cornerRadius = prefs.getFloat("corner_radius", 18f).coerceIn(0f, 32f),\n        )\n    }\n\n    fun save(\n        context: Context,\n        state: SuggestionLozengeState,\n    ) {\n        context\n            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)\n            .edit()\n            .putLong("background_color", state.backgroundColor.value.toLong())\n            .putFloat("background_alpha", state.backgroundAlpha.coerceIn(0f, 1f))\n            .putFloat("best_background_alpha", state.bestBackgroundAlpha.coerceIn(0f, 1f))\n            .putLong("border_color", state.borderColor.value.toLong())\n            .putFloat("border_alpha", state.borderAlpha.coerceIn(0f, 1f))\n            .putFloat("best_border_alpha", state.bestBorderAlpha.coerceIn(0f, 1f))\n            .putFloat("border_width", state.borderWidth.coerceIn(0f, 4f))\n            .putFloat("best_border_width", state.bestBorderWidth.coerceIn(0f, 4f))\n            .putFloat("corner_radius", state.cornerRadius.coerceIn(0f, 32f))\n            .apply()\n    }\n}\n'''
)

# 3) Suggestion bar: remove the legacy extra 2dp bottom line and use lozenge prefs.
p = "app/src/main/java/com/dessalines/thumbkey/ui/components/keyboard/SuggestionBarV2.kt"
for imp in [
    'import androidx.compose.ui.draw.drawWithCache\\n',
    'import androidx.compose.ui.geometry.Offset\\n',
    'import androidx.compose.ui.graphics.Brush\\n',
]:
    replace(p, imp, '')
replace(
    p,
    '''    motionStyle: SuggestionMotionStyle,\n    onSuggestionClick: (String) -> Unit,\n''',
    '''    motionStyle: SuggestionMotionStyle,\n    appearance: SuggestionLozengeState,\n    onSuggestionClick: (String) -> Unit,\n''',
)
replace(p, 'shape = RoundedCornerShape(18.dp),', 'shape = RoundedCornerShape(appearance.cornerRadius.dp),')
replace(
    p,
    '''                    color =\n                        MaterialTheme.colorScheme.surfaceVariant.copy(\n                            alpha = if (isBest) 0.72f else 0.50f,\n                        ),\n''',
    '''                    color =\n                        appearance.backgroundColor.copy(\n                            alpha = if (isBest) appearance.bestBackgroundAlpha else appearance.backgroundAlpha,\n                        ),\n''',
)
replace(
    p,
    '''                        BorderStroke(\n                            if (isBest) 1.2.dp else 0.8.dp,\n                            MaterialTheme.colorScheme.onSurface.copy(\n                                alpha = if (isBest) 0.28f else 0.16f,\n                            ),\n                        ),\n''',
    '''                        BorderStroke(\n                            if (isBest) appearance.bestBorderWidth.dp else appearance.borderWidth.dp,\n                            appearance.borderColor.copy(\n                                alpha = if (isBest) appearance.bestBorderAlpha else appearance.borderAlpha,\n                            ),\n                        ),\n''',
)
replace(
    p,
    '''    val motionStyle = remember { SuggestionMotionPreferences.load(ime) }\n''',
    '''    val motionStyle = remember { SuggestionMotionPreferences.load(ime) }\n    val lozengeAppearance = SuggestionLozengePreferences.load(ime)\n''',
)
replace(
    p,
    '''    val borderStops = BIRDIE_GOLD_BORDER.stops.map { it.position to it.color }.toTypedArray()\n\n    Surface(\n        color = MaterialTheme.colorScheme.surface.copy(alpha = if (enabled) 0.30f else 0.20f),\n        modifier =\n            Modifier\n                .fillMaxWidth()\n                .height(BAR_HEIGHT_DP.dp)\n                .drawWithCache {\n                    val brush = Brush.linearGradient(colorStops = borderStops)\n                    val strokeWidth = 2.dp.toPx()\n                    onDrawWithContent {\n                        drawContent()\n                        drawLine(\n                            brush = brush,\n                            start = Offset(0f, size.height - (strokeWidth / 2f)),\n                            end = Offset(size.width, size.height - (strokeWidth / 2f)),\n                            strokeWidth = strokeWidth,\n                        )\n                    }\n                },\n''',
    '''    Surface(\n        color = MaterialTheme.colorScheme.surface.copy(alpha = if (enabled) 0.30f else 0.20f),\n        modifier = Modifier.fillMaxWidth().height(BAR_HEIGHT_DP.dp),\n''',
)
replace(
    p,
    '''                            motionStyle = motionStyle,\n                            onSuggestionClick = { suggestion ->\n''',
    '''                            motionStyle = motionStyle,\n                            appearance = lozengeAppearance,\n                            onSuggestionClick = { suggestion ->\n''',
)
# Apply the same lozenge styling to the sparkle toggle.
replace(p, 'shape = RoundedCornerShape(18.dp),', 'shape = RoundedCornerShape(lozengeAppearance.cornerRadius.dp),')
replace(
    p,
    '''                color =\n                    if (enabled) {\n                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f)\n                    } else {\n                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f)\n                    },\n''',
    '''                color =\n                    lozengeAppearance.backgroundColor.copy(\n                        alpha = if (enabled) lozengeAppearance.bestBackgroundAlpha else lozengeAppearance.backgroundAlpha,\n                    ),\n''',
)
replace(
    p,
    '''                    BorderStroke(\n                        1.dp,\n                        MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 0.24f else 0.12f),\n                    ),\n''',
    '''                    BorderStroke(\n                        if (enabled) lozengeAppearance.bestBorderWidth.dp else lozengeAppearance.borderWidth.dp,\n                        lozengeAppearance.borderColor.copy(\n                            alpha = if (enabled) lozengeAppearance.bestBorderAlpha else lozengeAppearance.borderAlpha,\n                        ),\n                    ),\n''',
)

# 4) Advanced settings: lozenge section + managed key-border gradients.
p = "app/src/main/java/com/dessalines/thumbkey/ui/components/settings/lookandfeel/AdvancedLookAndFeelScreen.kt"
replace(
    p,
    'import com.dessalines.thumbkey.ui.components.keyboard.BackdropVisualLayer\n',
    'import com.dessalines.thumbkey.ui.components.keyboard.BIRDIE_GOLD_BORDER\nimport com.dessalines.thumbkey.ui.components.keyboard.BackdropVisualLayer\n',
)
replace(
    p,
    'import com.dessalines.thumbkey.ui.components.keyboard.SuggestionMotionPreferences\n',
    'import com.dessalines.thumbkey.ui.components.keyboard.SuggestionLozengePreferences\nimport com.dessalines.thumbkey.ui.components.keyboard.SuggestionLozengeState\nimport com.dessalines.thumbkey.ui.components.keyboard.SuggestionMotionPreferences\n',
)
replace(
    p,
    '''                SurfaceThemeSection(\n                    title = "Suggestion toolbar",\n                    subtitle = "The strip behind suggestion lozenges and the ✨ toggle.",\n                    load = ToolbarThemePreferences::load,\n                    save = ToolbarThemePreferences::save,\n                    showToolbarBorder = true,\n                )\n                KeyAppearanceSection()\n''',
    '''                SurfaceThemeSection(\n                    title = "Suggestion toolbar",\n                    subtitle = "The strip behind suggestion lozenges and the ✨ toggle.",\n                    load = ToolbarThemePreferences::load,\n                    save = ToolbarThemePreferences::save,\n                    showToolbarBorder = true,\n                )\n                SuggestionLozengeAppearanceSection()\n                KeyAppearanceSection()\n''',
)

lozenge_section = '''\n@Composable\nprivate fun SuggestionLozengeAppearanceSection() {\n    val context = LocalContext.current\n    var state by remember { mutableStateOf(SuggestionLozengePreferences.load(context)) }\n\n    fun persist(next: SuggestionLozengeState) {\n        state = next\n        SuggestionLozengePreferences.save(context, next)\n    }\n\n    Column(\n        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),\n        verticalArrangement = Arrangement.spacedBy(8.dp),\n    ) {\n        Text("Suggestion lozenges", style = MaterialTheme.typography.titleLarge)\n        Text(\n            "Background and border styling for suggestion pills and the ✨ toggle.",\n            style = MaterialTheme.typography.bodySmall,\n        )\n        ColorEditor("Lozenge background", state.backgroundColor) {\n            persist(state.copy(backgroundColor = it))\n        }\n        Text("Background opacity: ${(state.backgroundAlpha * 100).roundToInt()}%")\n        Slider(\n            value = state.backgroundAlpha,\n            onValueChange = { persist(state.copy(backgroundAlpha = it)) },\n            valueRange = 0f..1f,\n        )\n        Text("Primary suggestion opacity: ${(state.bestBackgroundAlpha * 100).roundToInt()}%")\n        Slider(\n            value = state.bestBackgroundAlpha,\n            onValueChange = { persist(state.copy(bestBackgroundAlpha = it)) },\n            valueRange = 0f..1f,\n        )\n        ColorEditor("Lozenge border", state.borderColor) {\n            persist(state.copy(borderColor = it))\n        }\n        Text("Border opacity: ${(state.borderAlpha * 100).roundToInt()}%")\n        Slider(\n            value = state.borderAlpha,\n            onValueChange = { persist(state.copy(borderAlpha = it)) },\n            valueRange = 0f..1f,\n        )\n        Text("Primary border opacity: ${(state.bestBorderAlpha * 100).roundToInt()}%")\n        Slider(\n            value = state.bestBorderAlpha,\n            onValueChange = { persist(state.copy(bestBorderAlpha = it)) },\n            valueRange = 0f..1f,\n        )\n        Text("Border width: ${String.format("%.1f", state.borderWidth)} dp")\n        Slider(\n            value = state.borderWidth,\n            onValueChange = { persist(state.copy(borderWidth = it)) },\n            valueRange = 0f..4f,\n        )\n        Text("Primary border width: ${String.format("%.1f", state.bestBorderWidth)} dp")\n        Slider(\n            value = state.bestBorderWidth,\n            onValueChange = { persist(state.copy(bestBorderWidth = it)) },\n            valueRange = 0f..4f,\n        )\n        Text("Corner radius: ${state.cornerRadius.roundToInt()} dp")\n        Slider(\n            value = state.cornerRadius,\n            onValueChange = { persist(state.copy(cornerRadius = it)) },\n            valueRange = 0f..32f,\n        )\n    }\n}\n\n'''
replace(p, '\n@Composable\nprivate fun KeyAppearanceSection() {\n', lozenge_section + '@Composable\nprivate fun KeyAppearanceSection() {\n')

old_border = '''            KeyBorderStyle.GRADIENT -> {\n                GradientEditor("Border gradient", state.borderGradient) {\n                    persist(state.copy(borderGradient = it))\n                }\n            }\n'''
new_border = '''            KeyBorderStyle.GRADIENT -> {\n                var gradients by remember { mutableStateOf(GradientLibrary.load(context)) }\n                var menuOpen by remember { mutableStateOf(false) }\n                var selectedName by remember {\n                    mutableStateOf(if (state.borderGradient == BIRDIE_GOLD_BORDER) "Golden Key Border" else "Current border gradient")\n                }\n                var saveName by remember { mutableStateOf("") }\n\n                Text("Saved border gradient", style = MaterialTheme.typography.titleMedium)\n                Box {\n                    Button(onClick = { menuOpen = true }, modifier = Modifier.fillMaxWidth()) {\n                        Text("$selectedName  ▾")\n                    }\n                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {\n                        gradients.forEach { saved ->\n                            DropdownMenuItem(\n                                text = { Text(saved.name) },\n                                onClick = {\n                                    selectedName = saved.name\n                                    menuOpen = false\n                                    persist(state.copy(borderGradient = saved.toBackdrop()))\n                                },\n                            )\n                        }\n                        DropdownMenuItem(\n                            text = { Text("＋ New border gradient") },\n                            onClick = {\n                                selectedName = "New border gradient"\n                                menuOpen = false\n                                persist(\n                                    state.copy(\n                                        borderGradient =\n                                            KeyboardBackdrop(\n                                                angleDegrees = 0f,\n                                                stops =\n                                                    listOf(\n                                                        KeyboardGradientStop(0f, Color(0xFFFFE89A)),\n                                                        KeyboardGradientStop(1f, Color(0xFF9A6419)),\n                                                    ),\n                                            ),\n                                    ),\n                                )\n                            },\n                        )\n                    }\n                }\n                GradientEditor("Border gradient", state.borderGradient) {\n                    selectedName = "Edited border gradient"\n                    persist(state.copy(borderGradient = it))\n                }\n                Row(\n                    horizontalArrangement = Arrangement.spacedBy(8.dp),\n                    verticalAlignment = Alignment.CenterVertically,\n                ) {\n                    OutlinedTextField(\n                        value = saveName,\n                        onValueChange = { saveName = it },\n                        label = { Text("Gradient name") },\n                        singleLine = true,\n                        modifier = Modifier.weight(1f),\n                    )\n                    Button(\n                        enabled = saveName.isNotBlank(),\n                        onClick = {\n                            val saved =\n                                GradientLibrary.saveCustom(\n                                    context,\n                                    SavedGradient(\n                                        id = "",\n                                        name = saveName.trim(),\n                                        angleDegrees = state.borderGradient.angleDegrees,\n                                        stops = state.borderGradient.stops,\n                                    ),\n                                )\n                            selectedName = saved.name\n                            saveName = ""\n                            gradients = GradientLibrary.load(context)\n                        },\n                    ) { Text("Save") }\n                }\n            }\n'''
replace(p, old_border, new_border)
