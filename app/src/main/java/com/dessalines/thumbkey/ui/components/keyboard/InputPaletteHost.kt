package com.dessalines.thumbkey.ui.components.keyboard

import android.content.Context
import android.media.AudioManager
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import com.dessalines.thumbkey.inputcontext.ContextEnginePolicy
import com.dessalines.thumbkey.inputcontext.ContextEnginePreferences

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
    val capabilities =
        ContextEnginePolicy.evaluate(
            context = ime.inputContext,
            settings = ContextEnginePreferences.load(ime),
        )
    if (!capabilities.canOfferInputPalettes) return

    val configuration = LocalConfiguration.current
    val view = LocalView.current
    val audioManager = ime.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    val paletteHeight =
        (configuration.screenHeightDp * 0.62f)
            .coerceIn(380f, 680f)
            .dp
    val paletteBackdrop = BackdropThemePreferences.load(ime)

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(paletteHeight),
        contentAlignment = Alignment.BottomCenter,
    ) {
        if (paletteBackdrop.mode != BackdropMode.NONE) {
            BackdropVisualLayer(
                state = paletteBackdrop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)),
            )
        }

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
