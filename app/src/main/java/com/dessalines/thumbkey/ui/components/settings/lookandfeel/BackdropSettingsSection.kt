package com.dessalines.thumbkey.ui.components.settings.lookandfeel

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import com.dessalines.thumbkey.ui.components.keyboard.BackdropPreset
import com.dessalines.thumbkey.ui.components.keyboard.BackdropThemePreferences
import com.dessalines.thumbkey.ui.components.keyboard.BackdropThemeState
import com.dessalines.thumbkey.ui.components.keyboard.KeyboardGradientStop
import com.dessalines.thumbkey.ui.components.keyboard.keyboardGradientBackground
import kotlin.math.roundToInt

@Composable
fun BackdropSettingsSection(onChanged: () -> Unit) {
    val context = LocalContext.current
    var state by remember { mutableStateOf(BackdropThemePreferences.load(context)) }

    fun persist(next: BackdropThemeState) {
        state = next
        BackdropThemePreferences.save(context, next)
        onChanged()
    }

    fun customize(next: BackdropThemeState) {
        persist(next.copy(preset = BackdropPreset.CUSTOM))
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("Backdrop gradient", style = MaterialTheme.typography.titleMedium)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PresetChip(
                label = "Birdie Rainbow",
                selected = state.preset == BackdropPreset.BIRDIE_RAINBOW,
                onClick = {
                    persist(BackdropThemePreferences.stateForPreset(BackdropPreset.BIRDIE_RAINBOW))
                },
            )
            PresetChip(
                label = "Sinebow",
                selected = state.preset == BackdropPreset.SINEBOW,
                onClick = {
                    persist(BackdropThemePreferences.stateForPreset(BackdropPreset.SINEBOW))
                },
            )
            FilterChip(
                selected = state.preset == BackdropPreset.CUSTOM,
                onClick = {},
                label = { Text("Custom") },
            )
        }

        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .keyboardGradientBackground(state.toBackdrop()),
        )

        Text("Angle: ${state.angleDegrees.roundToInt()}°")
        Slider(
            value = state.angleDegrees,
            onValueChange = { customize(state.copy(angleDegrees = it)) },
            valueRange = 0f..360f,
        )

        state.stops.forEachIndexed { index, stop ->
            GradientStopEditor(
                index = index,
                stop = stop,
                canRemove = state.stops.size > 1,
                onChange = { updated ->
                    val stops =
                        state.stops
                            .toMutableList()
                            .also { it[index] = updated }
                            .sortedBy { it.position }
                    customize(state.copy(stops = stops))
                },
                onRemove = {
                    customize(state.copy(stops = state.stops.toMutableList().also { it.removeAt(index) }))
                },
            )
        }

        Button(
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
private fun GradientStopEditor(
    index: Int,
    stop: KeyboardGradientStop,
    canRemove: Boolean,
    onChange: (KeyboardGradientStop) -> Unit,
    onRemove: () -> Unit,
) {
    val red = stop.color.red
    val green = stop.color.green
    val blue = stop.color.blue

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Stop ${index + 1} • ${(stop.position * 100).roundToInt()}%")
            if (canRemove) {
                Button(onClick = onRemove) { Text("Remove") }
            }
        }

        Text("Position")
        Slider(
            value = stop.position,
            onValueChange = { onChange(stop.copy(position = it)) },
            valueRange = 0f..1f,
        )

        ChannelSlider("R", red) { value ->
            onChange(stop.copy(color = Color(value, green, blue, stop.color.alpha)))
        }
        ChannelSlider("G", green) { value ->
            onChange(stop.copy(color = Color(red, value, blue, stop.color.alpha)))
        }
        ChannelSlider("B", blue) { value ->
            onChange(stop.copy(color = Color(red, green, value, stop.color.alpha)))
        }
    }
}

@Composable
private fun ChannelSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
) {
    Text("$label ${(value * 255).roundToInt()}")
    Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = 0f..1f,
    )
}
