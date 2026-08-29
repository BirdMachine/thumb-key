package com.dessalines.thumbkey

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.dessalines.thumbkey.db.AppSettingsRepository
import com.dessalines.thumbkey.db.ClipboardRepository
import com.dessalines.thumbkey.db.DEFAULT_BACKDROP_ENABLED
import com.dessalines.thumbkey.ui.components.keyboard.BackdropMode
import com.dessalines.thumbkey.ui.components.keyboard.BackdropThemePreferences
import com.dessalines.thumbkey.ui.components.keyboard.BackdropVisualLayer
import com.dessalines.thumbkey.ui.components.keyboard.KeyboardScreen
import com.dessalines.thumbkey.ui.components.keyboard.SuggestionBarV2
import com.dessalines.thumbkey.ui.components.keyboard.ToolbarThemePreferences
import com.dessalines.thumbkey.ui.theme.ThumbkeyTheme
import com.dessalines.thumbkey.utils.KeyboardPosition
import com.dessalines.thumbkey.utils.keyboardLayoutsSetFromDbIndexString
import com.dessalines.thumbkey.utils.toBool
import com.dessalines.thumbkey.utils.toInt
import kotlinx.coroutines.launch

@SuppressLint("ViewConstructor")
class ComposeKeyboardView(
    context: Context,
    private val settingsRepo: AppSettingsRepository,
    private val clipboardRepo: ClipboardRepository,
) : AbstractComposeView(context) {
    @Composable
    override fun Content() {
        val settingsState = settingsRepo.appSettings.observeAsState()
        val settings by settingsState
        val ctx = context as IMEService

        ThumbkeyTheme(settings = settings) {
            val backdropEnabled =
                BuildConfig.DEBUG || (settings?.backdropEnabled ?: DEFAULT_BACKDROP_ENABLED).toBool()
            val mainBackdrop = BackdropThemePreferences.load(ctx)
            val toolbarBackdrop = ToolbarThemePreferences.load(ctx)
            val keyboardColorScheme =
                if (backdropEnabled) {
                    MaterialTheme.colorScheme.copy(background = Color.Transparent)
                } else {
                    MaterialTheme.colorScheme
                }
            val density = LocalDensity.current
            var keyboardHeightPx by remember { mutableIntStateOf(0) }

            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                MaterialTheme(colorScheme = keyboardColorScheme) {
                    Box(modifier = Modifier.fillMaxWidth().height(42.dp)) {
                        if (backdropEnabled && toolbarBackdrop.mode != BackdropMode.NONE) {
                            BackdropVisualLayer(
                                state = toolbarBackdrop,
                                modifier = Modifier.fillMaxWidth().height(42.dp),
                            )
                        }
                        SuggestionBarV2(ctx)
                    }

                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .onSizeChanged { size ->
                                    if (size.height != keyboardHeightPx) {
                                        keyboardHeightPx = size.height
                                    }
                                },
                    ) {
                        if (
                            backdropEnabled &&
                            keyboardHeightPx > 0 &&
                            mainBackdrop.mode != BackdropMode.COLORFUL &&
                            mainBackdrop.mode != BackdropMode.NONE
                        ) {
                            BackdropVisualLayer(
                                state = mainBackdrop,
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .height(with(density) { keyboardHeightPx.toDp() }),
                            )
                        }

                        val keyboardSettings =
                            if (mainBackdrop.mode == BackdropMode.COLORFUL) {
                                settings
                            } else {
                                settings?.copy(backdropEnabled = 0)
                            }

                        KeyboardScreen(
                            settings = keyboardSettings,
                            clipboardRepository = clipboardRepo,
                            onSwitchLanguage = {
                                ctx.lifecycleScope.launch {
                                    val state = settingsState.value
                                    state?.let { s ->
                                        val layouts = keyboardLayoutsSetFromDbIndexString(s.keyboardLayouts).toList()
                                        val currentLayout = s.keyboardLayout
                                        val index = layouts.map { it.ordinal }.indexOf(currentLayout)
                                        val nextIndex = (index + 1).mod(layouts.size)
                                        val nextLayout = layouts.getOrNull(nextIndex)
                                        nextLayout?.let { layout ->
                                            val s2 = s.copy(keyboardLayout = layout.ordinal)
                                            settingsRepo.update(s2)

                                            ctx.currentKeyboardDefinition
                                                ?.settings
                                                ?.textProcessor
                                                ?.handleFinishInput(ctx)
                                            ctx.currentKeyboardDefinition = layouts[nextIndex].keyboardDefinition
                                            ctx.currentKeyboardDefinition
                                                ?.settings
                                                ?.textProcessor
                                                ?.updateCursorPosition(ctx)

                                            if (s.showToastOnLayoutSwitch.toBool()) {
                                                Toast
                                                    .makeText(context, layout.keyboardDefinition.title, Toast.LENGTH_SHORT)
                                                    .show()
                                            }
                                        }
                                    }
                                }
                            },
                            onChangePosition = { f ->
                                ctx.lifecycleScope.launch {
                                    settingsState.value?.let { state ->
                                        val nextPosition = f(KeyboardPosition.entries[state.position]).ordinal
                                        settingsRepo.update(state.copy(position = nextPosition))
                                    }
                                }
                            },
                            onToggleHideLetters = {
                                ctx.lifecycleScope.launch {
                                    settingsState.value?.let { state ->
                                        val hidden = (!state.hideLetters.toBool()).toInt()
                                        settingsRepo.update(state.copy(hideLetters = hidden))
                                    }
                                }
                            },
                            onGoToClipboardSettings = {
                                val intent =
                                    Intent(context, MainActivity::class.java).apply {
                                        putExtra("startRoute", "clipboardSettings")
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                    }
                                context.startActivity(intent)
                            },
                        )
                    }
                }
            }
        }
    }
}
