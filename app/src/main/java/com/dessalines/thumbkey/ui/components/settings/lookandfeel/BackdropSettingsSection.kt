package com.dessalines.thumbkey.ui.components.settings.lookandfeel

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.dessalines.thumbkey.ui.components.keyboard.BackdropMode
import com.dessalines.thumbkey.ui.components.keyboard.BackdropPreset
import com.dessalines.thumbkey.ui.components.keyboard.BackdropThemePreferences
import com.dessalines.thumbkey.ui.components.keyboard.BackdropThemeState
import com.dessalines.thumbkey.ui.components.keyboard.BackdropVisualLayer
import com.dessalines.thumbkey.ui.components.keyboard.FontPreferences
import com.dessalines.thumbkey.ui.components.keyboard.KeyBorderStyle
import com.dessalines.thumbkey.ui.components.keyboard.KeySurfaceStyle
import com.dessalines.thumbkey.ui.components.keyboard.KeyThemePreferences
import com.dessalines.thumbkey.ui.components.keyboard.KeyThemeState
import com.dessalines.thumbkey.ui.components.keyboard.KeyboardBackdrop
import com.dessalines.thumbkey.ui.components.keyboard.KeyboardGradientStop
import com.dessalines.thumbkey.ui.components.keyboard.SuggestionMotionPreferences
import com.dessalines.thumbkey.ui.components.keyboard.SuggestionMotionStyle
import kotlin.math.roundToInt

@Composable
fun BackdropSettingsSection(onChanged: () -> Unit) {
    val context = LocalContext.current
    var state by remember { mutableStateOf(BackdropThemePreferences.load(context)) }
    var keyTheme by remember { mutableStateOf(KeyThemePreferences.load(context)) }
    var motionStyle by remember { mutableStateOf(SuggestionMotionPreferences.load(context)) }
    var mediaPickMode by remember { mutableStateOf(BackdropMode.IMAGE) }
    var fontName by remember { mutableStateOf(FontPreferences.displayName(context)) }

    fun persist(next: BackdropThemeState) {
        state = next
        BackdropThemePreferences.save(context, next)
        onChanged()
    }

    fun persistKeys(next: KeyThemeState) {
        keyTheme = next
        KeyThemePreferences.save(context, next)
        onChanged()
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
    val fontPicker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null && FontPreferences.importFont(context, uri)) {
                fontName = FontPreferences.displayName(context)
                onChanged()
            }
        }

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Backdrop source", style = MaterialTheme.typography.titleMedium)
        ChipRow(
            BackdropMode.entries,
            state.mode,
            { it.name.lowercase().replaceFirstChar(Char::uppercase) },
        ) { mode ->
            persist(state.copy(mode = mode))
        }
        Text("Backdrop opacity: ${(state.opacity * 100).roundToInt()}%")
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
                val action = if (state.mediaUri == null) "Choose" else "Change"
                Text("$action ${state.mode.name.lowercase()}")
            }
        }
        if (state.mode != BackdropMode.NONE) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(54.dp),
            ) {
                BackdropVisualLayer(state)
            }
        }
        if (state.mode == BackdropMode.COLORFUL) {
            Text("Colorful gradient", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(BackdropPreset.BIRDIE_RAINBOW, BackdropPreset.SINEBOW).forEach { preset ->
                    FilterChip(
                        selected = state.preset == preset,
                        onClick = {
                            val selectedPreset = BackdropThemePreferences.stateForPreset(preset)
                            persist(
                                state.copy(
                                    preset = preset,
                                    angleDegrees = selectedPreset.angleDegrees,
                                    stops = selectedPreset.stops,
                                ),
                            )
                        },
                        label = {
                            Text(
                                if (preset == BackdropPreset.BIRDIE_RAINBOW) {
                                    "Birdie Rainbow"
                                } else {
                                    "Sinebow"
                                },
                            )
                        },
                    )
                }
            }
            GradientEditor("Backdrop gradient", state.toBackdrop()) { gradient ->
                persist(
                    state.copy(
                        preset = BackdropPreset.CUSTOM,
                        angleDegrees = gradient.angleDegrees,
                        stops = gradient.stops,
                    ),
                )
            }
        }

        Text("Key surface", style = MaterialTheme.typography.titleMedium)
        ChipRow(
            KeySurfaceStyle.entries,
            keyTheme.surfaceStyle,
            { it.name.lowercase().replaceFirstChar(Char::uppercase) },
        ) {
            persistKeys(keyTheme.copy(surfaceStyle = it))
        }
        when (keyTheme.surfaceStyle) {
            KeySurfaceStyle.GRADIENT -> {
                GradientEditor("Key-space gradient", keyTheme.surfaceGradient) {
                    persistKeys(keyTheme.copy(surfaceGradient = it))
                }
            }

            KeySurfaceStyle.SOLID -> {
                ColorEditor("Key color", keyTheme.surfaceColor) {
                    persistKeys(keyTheme.copy(surfaceColor = it))
                }
            }

            KeySurfaceStyle.NONE -> {
                Text("Transparent key faces; the backdrop shows through.")
            }
        }

        Text("Key border", style = MaterialTheme.typography.titleMedium)
        ChipRow(
            KeyBorderStyle.entries,
            keyTheme.borderStyle,
            { it.name.lowercase().replaceFirstChar(Char::uppercase) },
        ) {
            persistKeys(keyTheme.copy(borderStyle = it))
        }
        when (keyTheme.borderStyle) {
            KeyBorderStyle.GRADIENT -> {
                GradientEditor("Border gradient", keyTheme.borderGradient) {
                    persistKeys(keyTheme.copy(borderGradient = it))
                }
            }

            KeyBorderStyle.SOLID -> {
                ColorEditor("Border color", keyTheme.borderColor) {
                    persistKeys(keyTheme.copy(borderColor = it))
                }
            }

            KeyBorderStyle.SHADOW -> {
                ColorEditor("Shadow color", keyTheme.shadowColor) {
                    persistKeys(keyTheme.copy(shadowColor = it))
                }
                Text("Shadow opacity: ${(keyTheme.shadowAlpha * 100).roundToInt()}%")
                Slider(
                    value = keyTheme.shadowAlpha,
                    onValueChange = { persistKeys(keyTheme.copy(shadowAlpha = it)) },
                    valueRange = 0f..1f,
                )
                Text("Shadow elevation: ${keyTheme.shadowElevation.roundToInt()} dp")
                Slider(
                    value = keyTheme.shadowElevation,
                    onValueChange = { persistKeys(keyTheme.copy(shadowElevation = it)) },
                    valueRange = 0f..16f,
                )
            }

            KeyBorderStyle.NONE -> {
                Text("No key border.")
            }
        }

        Text("Keyboard font", style = MaterialTheme.typography.titleMedium)
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
                Text("Import TTF / OTF")
            }
            Button(
                onClick = {
                    FontPreferences.clear(context)
                    fontName = FontPreferences.displayName(context)
                    onChanged()
                },
            ) {
                Text("System default")
            }
        }
        Text(
            "Imported fonts are copied into Keywi-managed storage, " +
                "so they keep working if the original file moves.",
        )

        Text("Suggestion motion", style = MaterialTheme.typography.titleMedium)
        ChipRow(
            SuggestionMotionStyle.entries,
            motionStyle,
            { it.name.lowercase().replaceFirstChar(Char::uppercase) },
        ) {
            motionStyle = it
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
    var expanded by remember { mutableStateOf(false) }
    Text("$label angle: ${gradient.angleDegrees.roundToInt()}°")
    Slider(
        value = gradient.angleDegrees,
        onValueChange = { onChange(gradient.copy(angleDegrees = it)) },
        valueRange = 0f..360f,
    )
    Button(
        modifier = Modifier.fillMaxWidth(),
        onClick = { expanded = !expanded },
    ) {
        Text("Color stops (${gradient.stops.size}) ${if (expanded) "▲" else "▼"}")
    }
    if (expanded) {
        gradient.stops.forEachIndexed { index, stop ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(8.dp),
            ) {
                Text("Stop ${index + 1} • ${(stop.position * 100).roundToInt()}%")
                Slider(
                    value = stop.position,
                    onValueChange = { value ->
                        val next =
                            gradient.stops
                                .toMutableList()
                                .also { it[index] = stop.copy(position = value) }
                                .sortedBy { it.position }
                        onChange(gradient.copy(stops = next))
                    },
                    valueRange = 0f..1f,
                )
                ColorEditor("Color", stop.color) { color ->
                    val next =
                        gradient.stops
                            .toMutableList()
                            .also { it[index] = stop.copy(color = color) }
                    onChange(gradient.copy(stops = next))
                }
            }
        }
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                val sortedStops = gradient.stops.sortedBy { it.position }
                val position =
                    if (sortedStops.size < 2) {
                        0.5f
                    } else {
                        sortedStops
                            .zipWithNext()
                            .maxByOrNull { it.second.position - it.first.position }
                            ?.let { (a, b) -> (a.position + b.position) / 2f } ?: 0.5f
                    }
                val color = gradient.stops.firstOrNull()?.color ?: Color.White
                onChange(
                    gradient.copy(
                        stops =
                            (gradient.stops + KeyboardGradientStop(position, color))
                                .sortedBy { it.position },
                    ),
                )
            },
        ) {
            Text("Add color stop")
        }
    }
}

@Composable
private fun ColorEditor(
    label: String,
    color: Color,
    onChange: (Color) -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(28.dp).background(color))
            Text("$label • A ${(color.alpha * 100).roundToInt()}%")
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
    Text("$label ${(value * 255).roundToInt()}")
    Slider(
        value = value,
        onValueChange = onChange,
        valueRange = 0f..1f,
    )
}
