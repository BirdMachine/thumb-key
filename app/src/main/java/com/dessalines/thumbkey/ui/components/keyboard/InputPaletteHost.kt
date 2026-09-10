package com.dessalines.thumbkey.ui.components.keyboard

import android.content.Context
import android.media.AudioManager
import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
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
    keyboardHeight: Dp,
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
    val searchActive = PaletteSearchCapture.active
    val browseHeight =
        (configuration.screenHeightDp * 0.62f)
            .coerceIn(380f, 680f)
            .dp
    val searchHeight =
        (configuration.screenHeightDp * 0.78f)
            .coerceIn(460f, 780f)
            .dp
    val targetHeight = if (searchActive) searchHeight else browseHeight
    val targetContentHeight =
        if (searchActive) {
            (searchHeight - keyboardHeight).coerceAtLeast(190.dp)
        } else {
            browseHeight
        }
    val animatedHeight by
        animateDpAsState(
            targetValue = targetHeight,
            animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
            label = "palette-height",
        )
    val animatedContentHeight by
        animateDpAsState(
            targetValue = targetContentHeight,
            animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
            label = "palette-content-height",
        )
    val paletteBackdrop = BackdropThemePreferences.load(ime)

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(animatedHeight),
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(animatedContentHeight),
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
                            PaletteSearchCapture.bypass {
                                ime.currentInputConnection?.commitText(text, 1)
                            }
                        },
                        onBackToLetters = onDismiss,
                        onGoToEmoji = null,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}
