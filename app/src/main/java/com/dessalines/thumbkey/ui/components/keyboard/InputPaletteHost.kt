package com.dessalines.thumbkey.ui.components.keyboard

import android.content.Context
import android.media.AudioManager
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import com.dessalines.thumbkey.IMEService

/** Browsable input surfaces that are deliberately opened instead of cycled as keyboard modes. */
enum class InputPalette {
    KAOMOJI,
}

@Composable
fun ExpandedInputPaletteHost(
    palette: InputPalette,
    ime: IMEService,
    vibrateOnTap: Boolean,
    soundOnTap: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val configuration = LocalConfiguration.current
    val view = LocalView.current
    val audioManager = ime.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    val paletteHeight =
        (configuration.screenHeightDp * 0.62f)
            .coerceIn(380f, 680f)
            .dp

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(paletteHeight)
                .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.BottomCenter,
    ) {
        when (palette) {
            InputPalette.KAOMOJI -> {
                KaomojiRoom(
                    onCommit = { text ->
                        if (vibrateOnTap) view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        if (soundOnTap) audioManager.playSoundEffect(AudioManager.FX_KEY_CLICK, .1f)
                        ime.currentInputConnection?.commitText(text, 1)
                    },
                    onBackToLetters = onDismiss,
                    onGoToEmoji = null,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
