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
import com.dessalines.thumbkey.ui.components.keyboard.KeyboardGradientStop
import com.dessalines.thumbkey.ui.components.keyboard.SuggestionMotionPreferences
import com.dessalines.thumbkey.ui.components.keyboard.SuggestionMotionStyle
import kotlin.math.roundToInt

@Composable
fun BackdropSettingsSection(onChanged: () -> Unit) {
    val context = LocalContext.current
    var state by remember { mutableStateOf(BackdropThemePreferences.load(context)) }
    var stopsExpanded by remember { mutableStateOf(false) }
    var expandedStopIndex by remember { mutableStateOf<Int?>(null) }
    var mediaPickMode by remember { mutableStateOf(BackdropMode.IMAGE) }
    var motionStyle by remember { mutableStateOf(SuggestionMotionPreferences.load(context)) }

    fun persist(next: BackdropThemeState) {
        state = next
        BackdropThemePreferences.save(context, next)
        onChanged()
    }

    fun customize(next: BackdropThemeState) {
        persist(next.copy(preset = BackdropPreset.CUSTOM))
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
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("Backdrop source", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BackdropMode.entries.forEach { mode ->
                FilterChip(
                    selected = state.mode == mode,
                    onClick = { persist(state.copy(mode = mode)) },
                    label = {
                        Text(
                            when (mode) {
                                BackdropMode.COLORFUL -> "Colorful"
                                BackdropMode.IMAGE -> "Image"
                                BackdropMode.GIF -> "GIF"
                                BackdropMode.NONE -> "None"
                            },
                        )
                    },
                )
            }
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
                Text(if (state.mediaUri == null) "Choose ${state.mode.name.lowercase()}" else "Change ${state.mode.name.lowercase()}")
            }
        }

        if (state.mode != BackdropMode.NONE) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(54.dp),
            ) {
                BackdropVisualLayer(state = state)
            }
        }

        if (state.mode == BackdropMode.COLORFUL) {
            Text("Colorful gradient", style = MaterialTheme.typography.titleMedium)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PresetChip(
                    label = "Birdie Rainbow",
                    selected = state.preset == BackdropPreset.BIRDIE_RAINBOW,
                    onClick = {
                        val preset = BackdropThemePreferences.stateForPreset(BackdropPreset.BIRDIE_RAINBOW)
                        persist(
                            state.copy(
                                preset = BackdropPreset.BIRDIE_RAINBOW,
                                angleDegrees = preset.angleDegrees,
                                stops = preset.stops,
                            ),
                        )
                    },
                )
                PresetChip(
                    label = "Sinebow",
                    selected = state.preset == BackdropPreset.SINEBOW,
                    onClick = {
                        val preset = BackdropThemePreferences.stateForPreset(BackdropPreset.SINEBOW)
                        persist(
                            state.copy(
                                preset = BackdropPreset.SINEBOW,
                                angleDegrees = preset.angleDegrees,
                                stops = preset.stops,
                            ),
                        )
                    },
                )
                FilterChip(
                    selected = state.preset == BackdropPreset.CUSTOM,
                    onClick = {},
                    label = { Text("Custom") },
                )
            }

            Text("Angle: ${state.angleDegrees.roundToInt()}°")
            Slider(
                value = state.angleDegrees,
                onValueChange = { customize(state.copy(angleDegrees = it)) },
                valueRange = 0f..360f,
            )

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = { stopsExpanded = !stopsExpanded },
            ) {
                Text("Color stops (${state.stops.size}) ${if (stopsExpanded) "▲" else "▼"}")
            }

            if (stopsExpanded) {
                state.stops.forEachIndexed { index, stop ->
                    CompactGradientStopEditor(
                        index = index,
                        stop = stop,
                        expanded = expandedStopIndex == index,
                        canRemove = state.stops.size > 1,
                        onToggleExpanded = {
                            expandedStopIndex = if (expandedStopIndex == index) null else index
                        },
                        onChange = { updated ->
                            val stops =
                                state.stops
                                    .toMutableList()
                                    .also { it[index] = updated }
                                    .sortedBy { it.position }
                            customize(state.copy(stops = stops))
                        },
                        onRemove = {
                            expandedStopIndex = null
                            customize(state.copy(stops = state.stops.toMutableList().also { it.removeAt(index) }))
                        },
                    )
                }

                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        val position =
                            if (state.stops.isEmpty()) {
                                0.5f
                            } else {
                                val largestGap =
                                    state.stops
                                        .sortedBy { it.position }
                                        .zipWithNext()
                                        .maxByOrNull { (a, b) -> b.position - a.position }
                                if (largestGap != null) {
                                    (largestGap.first.position + largestGap.second.position) / 2f
                                } else {
                                    0.5f
                                }
                            }
                        val color = state.stops.firstOrNull()?.color ?: Color.White
                        customize(
                            state.copy(
                                stops = (state.stops + KeyboardGradientStop(position, color)).sortedBy { it.position },
                            ),
                        )
                    },
                ) {
                    Text("Add color stop")
                }
            }
        }

        Text("Suggestion motion", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SuggestionMotionStyle.entries.forEach { style ->
                FilterChip(
                    selected = motionStyle == style,
                    onClick = {
                        motionStyle = style
                        SuggestionMotionPreferences.save(context, style)
                    },
                    label = {
                        Text(
                            when (style) {
                                SuggestionMotionStyle.NONE -> "None"
                                SuggestionMotionStyle.SPRINGY -> "Springy"
                                SuggestionMotionStyle.GOOEY -> "Gooey"
                            },
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun PresetChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
    )
}

@Composable
private fun CompactGradientStopEditor(
    index: Int,
    stop: KeyboardGradientStop,
    expanded: Boolean,
    canRemove: Boolean,
    onToggleExpanded: () -> Unit,
    onChange: (KeyboardGradientStop) -> Unit,
    onRemove: () -> Unit,
) {
    val red = stop.color.red
    val green = stop.color.green
    val blue = stop.color.blue
    val alpha = stop.color.alpha

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier =
                    Modifier
                        .size(28.dp)
                        .background(stop.color),
            )
            Button(
                modifier = Modifier.weight(1f),
                onClick = onToggleExpanded,
            ) {
                Text(
                    "Stop ${index + 1} • ${(stop.position * 100).roundToInt()}% • A ${(alpha * 100).roundToInt()}% " +
                        if (expanded) "▲" else "▼",
                )
            }
            if (canRemove) {
                Button(onClick = onRemove) { Text("×") }
            }
        }

        if (expanded) {
            ChannelSlider("Position", stop.position, percent = true) { value ->
                onChange(stop.copy(position = value))
            }
            ChannelSlider("R", red) { value ->
                onChange(stop.copy(color = Color(value, green, blue, alpha)))
            }
            ChannelSlider("G", green) { value ->
                onChange(stop.copy(color = Color(red, value, blue, alpha)))
            }
            ChannelSlider("B", blue) { value ->
                onChange(stop.copy(color = Color(red, green, value, alpha)))
            }
            ChannelSlider("A", alpha, percent = true) { value ->
                onChange(stop.copy(color = Color(red, green, blue, value)))
            }
        }
    }
}

@Composable
private fun ChannelSlider(
    label: String,
    value: Float,
    percent: Boolean = false,
    onValueChange: (Float) -> Unit,
) {
    val displayValue = if (percent) (value * 100).roundToInt() else (value * 255).roundToInt()
    Text("$label $displayValue${if (percent) "%" else ""}")
    Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = 0f..1f,
    )
}
