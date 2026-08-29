package com.dessalines.thumbkey.ui.components.settings.lookandfeel

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.dessalines.thumbkey.ui.components.keyboard.BackdropMode
import com.dessalines.thumbkey.ui.components.keyboard.BackdropPreset
import com.dessalines.thumbkey.ui.components.keyboard.BackdropThemePreferences
import com.dessalines.thumbkey.ui.components.keyboard.BackdropThemeState
import com.dessalines.thumbkey.ui.components.keyboard.BackdropVisualLayer
import com.dessalines.thumbkey.ui.components.keyboard.FontPreferences
import com.dessalines.thumbkey.ui.components.keyboard.GradientLibrary
import com.dessalines.thumbkey.ui.components.keyboard.KeyBorderStyle
import com.dessalines.thumbkey.ui.components.keyboard.KeySurfaceStyle
import com.dessalines.thumbkey.ui.components.keyboard.KeyThemePreferences
import com.dessalines.thumbkey.ui.components.keyboard.KeyThemeState
import com.dessalines.thumbkey.ui.components.keyboard.KeyboardBackdrop
import com.dessalines.thumbkey.ui.components.keyboard.KeyboardGradientStop
import com.dessalines.thumbkey.ui.components.keyboard.KeywiAppearancePreferences
import com.dessalines.thumbkey.ui.components.keyboard.SavedGradient
import com.dessalines.thumbkey.ui.components.keyboard.SuggestionLozengeBorderStyle
import com.dessalines.thumbkey.ui.components.keyboard.SuggestionLozengeSurfaceStyle
import com.dessalines.thumbkey.ui.components.keyboard.SuggestionLozengeThemePreferences
import com.dessalines.thumbkey.ui.components.keyboard.SuggestionLozengeThemeState
import com.dessalines.thumbkey.ui.components.keyboard.SuggestionMotionPreferences
import com.dessalines.thumbkey.ui.components.keyboard.SuggestionMotionStyle
import com.dessalines.thumbkey.ui.components.keyboard.ToolbarBorderPreferences
import com.dessalines.thumbkey.ui.components.keyboard.ToolbarThemePreferences
import com.dessalines.thumbkey.ui.components.keyboard.keyboardGradientBackground
import com.dessalines.thumbkey.utils.SimpleTopAppBar
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedLookAndFeelScreen(navController: NavController) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    var keywiEnabled by remember { mutableStateOf(KeywiAppearancePreferences.load(context)) }

    Scaffold(
        topBar = {
            SimpleTopAppBar(text = "Advanced look & feel", navController = navController)
        },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .padding(padding)
                    .verticalScroll(scrollState)
                    .background(MaterialTheme.colorScheme.surface)
                    .imePadding(),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(
                        "Enable custom keyboard backgrounds",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        "Turn off to use Thumb-Key's original styling.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Switch(
                    checked = keywiEnabled,
                    onCheckedChange = { enabled ->
                        keywiEnabled = enabled
                        KeywiAppearancePreferences.save(context, enabled)
                    },
                )
            }

            if (keywiEnabled) {
                SurfaceThemeSection(
                    title = "Main backdrop",
                    subtitle = "The large space behind and around the keys.",
                    load = BackdropThemePreferences::load,
                    save = BackdropThemePreferences::save,
                )
                SurfaceThemeSection(
                    title = "Suggestion toolbar",
                    subtitle = "The strip behind suggestion lozenges and the ✨ toggle.",
                    load = ToolbarThemePreferences::load,
                    save = ToolbarThemePreferences::save,
                    showToolbarBorder = true,
                )
                SuggestionLozengeAppearanceSection()
                KeyAppearanceSection()
                FontAndSuggestionSection()
                Text(
                    text = "Planned surface slot: one-shot key-press overlays (GIF/image effects).",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodySmall,
                )
            } else {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .alpha(0.42f)
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("Main backdrop", style = MaterialTheme.typography.titleLarge)
                    Text("Suggestion toolbar", style = MaterialTheme.typography.titleLarge)
                    Text("Keys", style = MaterialTheme.typography.titleSmall)
                    Text("Typography", style = MaterialTheme.typography.titleSmall)
                    Text("Suggestions", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Enable custom keyboard backgrounds to edit Keywi appearance settings.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun SurfaceThemeSection(
    title: String,
    subtitle: String,
    load: (android.content.Context) -> BackdropThemeState,
    save: (android.content.Context, BackdropThemeState) -> Unit,
    showToolbarBorder: Boolean = false,
) {
    val context = LocalContext.current
    var state by remember { mutableStateOf(load(context)) }
    var mediaPickMode by remember { mutableStateOf(BackdropMode.IMAGE) }

    fun persist(next: BackdropThemeState) {
        state = next
        save(context, next)
    }

    val mediaPicker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                runCatching {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION,
                    )
                }
                persist(state.copy(mode = mediaPickMode, mediaUri = uri.toString()))
            }
        }

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 7.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        Text(subtitle, style = MaterialTheme.typography.bodySmall)
        if (showToolbarBorder) {
            var borderWidth by remember { mutableStateOf(ToolbarBorderPreferences.loadWidth(context)) }
            var borderColorText by remember {
                mutableStateOf(String.format("#%08X", ToolbarBorderPreferences.loadColor(context).value.toLong()))
            }
            Text("Toolbar border: ${String.format("%.1f", borderWidth)} px", style = MaterialTheme.typography.titleSmall)
            Slider(
                value = borderWidth,
                onValueChange = { value ->
                    borderWidth = value
                    ToolbarBorderPreferences.saveWidth(context, value)
                },
                valueRange = 0f..3f,
            )
            OutlinedTextField(
                value = borderColorText,
                onValueChange = { text ->
                    borderColorText = text
                    runCatching { Color(android.graphics.Color.parseColor(text)) }.getOrNull()?.let { color ->
                        ToolbarBorderPreferences.saveColor(context, color)
                    }
                },
                label = { Text("Border color (#AARRGGBB or #RRGGBB)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        ChipRow(
            values = BackdropMode.entries,
            selected = state.mode,
            label = { it.name.lowercase().replaceFirstChar(Char::uppercase) },
        ) { persist(state.copy(mode = it)) }
        Text("Opacity: ${(state.opacity * 100).roundToInt()}%")
        Slider(
            value = state.opacity,
            onValueChange = { persist(state.copy(opacity = it)) },
            valueRange = 0f..1f,
        )

        if (state.mode == BackdropMode.IMAGE || state.mode == BackdropMode.GIF) {
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    mediaPickMode = state.mode
                    mediaPicker.launch(arrayOf("image/*"))
                },
            ) {
                Text(if (state.mediaUri == null) "Choose media" else "Change media")
            }
        }

        if (state.mode != BackdropMode.NONE) {
            Box(Modifier.fillMaxWidth().height(54.dp)) {
                BackdropVisualLayer(state)
            }
        }

        if (state.mode == BackdropMode.COLORFUL) {
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
        }
    }
}

@Composable
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

@Composable
private fun SuggestionLozengeAppearanceSection() {
    val context = LocalContext.current
    var state by remember { mutableStateOf(SuggestionLozengeThemePreferences.load(context)) }

    fun persist(next: SuggestionLozengeThemeState) {
        state = next
        SuggestionLozengeThemePreferences.save(context, next)
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 7.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("Toolbar lozenges", style = MaterialTheme.typography.titleSmall)
        Text(
            "Suggestion pills and the ✨ toggle get their own surface and border theme.",
            style = MaterialTheme.typography.bodySmall,
        )
        Text("Lozenge background", style = MaterialTheme.typography.titleSmall)
        ChipRow(
            values = SuggestionLozengeSurfaceStyle.entries,
            selected = state.surfaceStyle,
            label = { it.name.lowercase().replaceFirstChar(Char::uppercase) },
        ) {
            persist(state.copy(surfaceStyle = it))
        }
        when (state.surfaceStyle) {
            SuggestionLozengeSurfaceStyle.GRADIENT -> {
                ManagedGradientEditor(
                    title = "Lozenge background gradient",
                    gradient = state.surfaceGradient,
                    onGradientChange = { persist(state.copy(surfaceGradient = it)) },
                )
            }

            SuggestionLozengeSurfaceStyle.SOLID -> {
                ColorEditor("Lozenge color", state.surfaceColor) {
                    persist(state.copy(surfaceColor = it))
                }
            }

            SuggestionLozengeSurfaceStyle.NONE -> {
                Text("Transparent lozenge faces.")
            }
        }

        Text("Lozenge border", style = MaterialTheme.typography.titleSmall)
        ChipRow(
            values = SuggestionLozengeBorderStyle.entries,
            selected = state.borderStyle,
            label = { it.name.lowercase().replaceFirstChar(Char::uppercase) },
        ) {
            persist(state.copy(borderStyle = it))
        }
        Text("Border width: ${String.format("%.1f", state.borderWidth)} dp")
        Slider(
            value = state.borderWidth,
            onValueChange = { persist(state.copy(borderWidth = it)) },
            valueRange = 0f..4f,
        )
        when (state.borderStyle) {
            SuggestionLozengeBorderStyle.GRADIENT -> {
                ManagedGradientEditor(
                    title = "Lozenge border gradient",
                    gradient = state.borderGradient,
                    onGradientChange = { persist(state.copy(borderGradient = it)) },
                )
            }

            SuggestionLozengeBorderStyle.SOLID -> {
                ColorEditor("Lozenge border color", state.borderColor) {
                    persist(state.copy(borderColor = it))
                }
            }

            SuggestionLozengeBorderStyle.NONE -> {
                Text("No lozenge border.")
            }
        }
    }
}

@Composable
private fun KeyAppearanceSection() {
    val context = LocalContext.current
    var state by remember { mutableStateOf(KeyThemePreferences.load(context)) }

    fun persist(next: KeyThemeState) {
        state = next
        KeyThemePreferences.save(context, next)
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 7.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("Keys", style = MaterialTheme.typography.titleSmall)
        Text("Key surface", style = MaterialTheme.typography.titleSmall)
        ChipRow(
            values = KeySurfaceStyle.entries,
            selected = state.surfaceStyle,
            label = { it.name.lowercase().replaceFirstChar(Char::uppercase) },
        ) { persist(state.copy(surfaceStyle = it)) }
        when (state.surfaceStyle) {
            KeySurfaceStyle.GRADIENT -> {
                GradientEditor("Key-space gradient", state.surfaceGradient) {
                    persist(state.copy(surfaceGradient = it))
                }
            }

            KeySurfaceStyle.SOLID -> {
                ColorEditor("Key color", state.surfaceColor) {
                    persist(state.copy(surfaceColor = it))
                }
            }

            KeySurfaceStyle.NONE -> {
                Text("Transparent key faces.")
            }
        }

        Text("Key border", style = MaterialTheme.typography.titleSmall)
        ChipRow(
            values = KeyBorderStyle.entries,
            selected = state.borderStyle,
            label = { it.name.lowercase().replaceFirstChar(Char::uppercase) },
        ) { persist(state.copy(borderStyle = it)) }
        when (state.borderStyle) {
            KeyBorderStyle.GRADIENT -> {
                ManagedGradientEditor(
                    title = "Border gradient",
                    gradient = state.borderGradient,
                    onGradientChange = { persist(state.copy(borderGradient = it)) },
                )
            }

            KeyBorderStyle.SOLID -> {
                ColorEditor("Border color", state.borderColor) {
                    persist(state.copy(borderColor = it))
                }
            }

            KeyBorderStyle.SHADOW -> {
                ColorEditor("Shadow color", state.shadowColor) {
                    persist(state.copy(shadowColor = it))
                }
                Text("Shadow opacity: ${(state.shadowAlpha * 100).roundToInt()}%")
                Slider(
                    value = state.shadowAlpha,
                    onValueChange = { persist(state.copy(shadowAlpha = it)) },
                    valueRange = 0f..1f,
                )
                Text("Shadow elevation: ${state.shadowElevation.roundToInt()} dp")
                Slider(
                    value = state.shadowElevation,
                    onValueChange = { persist(state.copy(shadowElevation = it)) },
                    valueRange = 0f..16f,
                )
            }

            KeyBorderStyle.NONE -> {
                Text("No key border.")
            }
        }
    }
}

@Composable
private fun FontAndSuggestionSection() {
    val context = LocalContext.current
    var fontName by remember { mutableStateOf(FontPreferences.displayName(context)) }
    var motion by remember { mutableStateOf(SuggestionMotionPreferences.load(context)) }
    val fontPicker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null && FontPreferences.importFont(context, uri)) {
                fontName = FontPreferences.displayName(context)
            }
        }

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 7.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("Typography", style = MaterialTheme.typography.titleSmall)
        Text(fontName)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    fontPicker.launch(
                        arrayOf(
                            "font/*",
                            "application/x-font-ttf",
                            "application/x-font-opentype",
                            "application/octet-stream",
                        ),
                    )
                },
            ) {
                Text("Import font")
            }
            Button(
                onClick = {
                    FontPreferences.clear(context)
                    fontName = FontPreferences.displayName(context)
                },
            ) {
                Text("System default")
            }
        }

        Text("Suggestions", style = MaterialTheme.typography.titleSmall)
        Text("Motion style", style = MaterialTheme.typography.titleSmall)
        ChipRow(
            values = SuggestionMotionStyle.entries,
            selected = motion,
            label = { it.name.lowercase().replaceFirstChar(Char::uppercase) },
        ) {
            motion = it
            SuggestionMotionPreferences.save(context, it)
        }
    }
}

@Composable
private fun <T> ChipRow(
    values: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        values.forEach { value ->
            FilterChip(
                selected = selected == value,
                onClick = { onSelect(value) },
                label = { Text(label(value)) },
            )
        }
    }
}

@Composable
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

@Composable
private fun ColorEditor(
    label: String,
    color: Color,
    onChange: (Color) -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(22.dp).clip(RoundedCornerShape(7.dp)).background(color))
            Text("$label • A ${(color.alpha * 100).roundToInt()}%", style = MaterialTheme.typography.labelMedium)
        }
        Channel("R", color.red) { onChange(Color(it, color.green, color.blue, color.alpha)) }
        Channel("G", color.green) { onChange(Color(color.red, it, color.blue, color.alpha)) }
        Channel("B", color.blue) { onChange(Color(color.red, color.green, it, color.alpha)) }
        Channel("A", color.alpha) { onChange(Color(color.red, color.green, color.blue, it)) }
    }
}

@Composable
private fun Channel(
    label: String,
    value: Float,
    onChange: (Float) -> Unit,
) {
    Text("$label ${(value * 255).roundToInt()}", style = MaterialTheme.typography.labelSmall)
    Slider(
        value = value,
        onValueChange = onChange,
        valueRange = 0f..1f,
    )
}
