package com.dessalines.thumbkey

import android.inputmethodservice.InputMethodService
import android.util.Log
import android.view.inputmethod.CursorAnchorInfo
import android.view.inputmethod.EditorInfo
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.dessalines.thumbkey.db.DEFAULT_CLIPBOARD_HISTORY_ENABLED
import com.dessalines.thumbkey.db.DEFAULT_DISABLE_FULLSCREEN_EDITOR
import com.dessalines.thumbkey.db.DEFAULT_SHOW_ON_SCREEN_KEYBOARD
import com.dessalines.thumbkey.db.DEFAULT_USE_PRIVATE_CLIPBOARD
import com.dessalines.thumbkey.inputcontext.ContextEnginePreferences
import com.dessalines.thumbkey.inputcontext.InputContext
import com.dessalines.thumbkey.inputcontext.SelectionContext
import com.dessalines.thumbkey.utils.KeyboardDefinition
import com.dessalines.thumbkey.utils.KeyboardLayout
import com.dessalines.thumbkey.utils.TAG
import com.dessalines.thumbkey.utils.ThumbKeyClipboardManager
import com.dessalines.thumbkey.utils.toBool

class IMEService :
    InputMethodService(),
    LifecycleOwner,
    ViewModelStoreOwner,
    SavedStateRegistryOwner {
    private fun setupView(): ComposeKeyboardView {
        val app = application as ThumbkeyApplication
        val settingsRepo = app.appSettingsRepository
        val clipboardRepo = app.clipboardRepository

        val layoutIndex = settingsRepo.appSettings.value?.keyboardLayout
        if (layoutIndex != null) {
            currentKeyboardDefinition = KeyboardLayout.entries[layoutIndex].keyboardDefinition
        }

        val view = ComposeKeyboardView(this, settingsRepo, clipboardRepo)
        window?.window?.decorView?.let { decorView ->
            decorView.setViewTreeLifecycleOwner(this)
            decorView.setViewTreeViewModelStoreOwner(this)
            decorView.setViewTreeSavedStateRegistryOwner(this)
        }
        view.let {
            view.setViewTreeLifecycleOwner(this)
            view.setViewTreeViewModelStoreOwner(this)
            view.setViewTreeSavedStateRegistryOwner(this)
        }
        return view
    }

    var currentKeyboardDefinition: KeyboardDefinition? = null
    var inputContext: InputContext = InputContext.fromEditorInfo(null)
        private set
    private var clipboardManager: ThumbKeyClipboardManager? = null

    /**
     * This is called every time the keyboard is brought up.
     * You can't use onCreate, because that can't pick up new numeric inputs
     */
    override fun onStartInput(
        attribute: EditorInfo?,
        restarting: Boolean,
    ) {
        super.onStartInput(attribute, restarting)

        inputContext = InputContext.fromEditorInfo(attribute)
        applySmartEnterPolicy(attribute)

        val view = this.setupView()
        this.setInputView(view)
    }

    /**
     * Thumb-Key's existing IME-complete action already performs a concrete Android editor action
     * when one exists, and emits Enter when the action is IME_ACTION_NONE. Smart Enter therefore
     * only needs to normalize multiline fields to the latter so apps cannot accidentally turn a
     * paragraph break into Send/Done.
     */
    private fun applySmartEnterPolicy(attribute: EditorInfo?) {
        if (attribute == null) return

        val settings = ContextEnginePreferences.load(this)
        if (!settings.adaptToField || !settings.smartEnter || !inputContext.isMultiLine) return

        attribute.imeOptions =
            (attribute.imeOptions and EditorInfo.IME_MASK_ACTION.inv()) or EditorInfo.IME_ACTION_NONE
    }

    // Lifecycle Methods
    private var lifecycleRegistry: LifecycleRegistry = LifecycleRegistry(this)

    private fun handleLifecycleEvent(event: Lifecycle.Event) = lifecycleRegistry.handleLifecycleEvent(event)

    override val lifecycle = lifecycleRegistry

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        // Initialize clipboard manager
        val app = application as ThumbkeyApplication
        clipboardManager = ThumbKeyClipboardManager(this, app.clipboardRepository)
        clipboardManager?.startListening()
        clipboardManager?.clearExpired()
    }

    override fun onDestroy() {
        clipboardManager?.stopListening()
        clipboardManager = null
        super.onDestroy()
        handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    }

    // Cursor update Methods
    override fun onUpdateCursorAnchorInfo(cursorAnchorInfo: CursorAnchorInfo) {
        super.onUpdateCursorAnchorInfo(cursorAnchorInfo)

        cursorMoved =
            if (ignoreCursorMove) {
                ignoreCursorMove = false
                false
            } else {
                Log.d(TAG, "cursor moved")
                cursorAnchorInfo.selectionStart != selectionStart ||
                    cursorAnchorInfo.selectionEnd != selectionEnd
            }

        currentKeyboardDefinition?.settings?.textProcessor?.handleCursorUpdate(
            this,
            selectionStart,
            selectionEnd,
            cursorAnchorInfo.selectionStart,
            cursorAnchorInfo.selectionEnd,
        )

        selectionStart = cursorAnchorInfo.selectionStart
        selectionEnd = cursorAnchorInfo.selectionEnd
        inputContext =
            inputContext.copy(
                selection =
                    SelectionContext(
                        start = selectionStart,
                        end = selectionEnd,
                        selectedText = currentInputConnection?.getSelectedText(0)?.toString(),
                    ),
            )
    }

    override fun onEvaluateInputViewShown(): Boolean {
        val settingsRepo = (application as ThumbkeyApplication).appSettingsRepository
        val settings = settingsRepo.appSettings.getValue()
        val showOnScreenKeyboard =
            (settings?.showOnScreenKeyboard ?: DEFAULT_SHOW_ON_SCREEN_KEYBOARD).toBool()

        // Always call super implementation because Android docs says:
        // "If you override this method you must call through to the superclass implementation."
        // https://developer.android.com/reference/android/inputmethodservice/InputMethodService#onEvaluateInputViewShown()
        return super.onEvaluateInputViewShown() ||
            showOnScreenKeyboard
    }

    // Disable the fullscreen text editor if set by the user
    override fun onUpdateExtractingVisibility(ei: EditorInfo) {
        val settingsRepo = (application as ThumbkeyApplication).appSettingsRepository
        val settings = settingsRepo.appSettings.getValue()
        if ((settings?.disableFullscreenEditor ?: DEFAULT_DISABLE_FULLSCREEN_EDITOR).toBool()) {
            ei.imeOptions =
                ei.imeOptions or EditorInfo.IME_FLAG_NO_EXTRACT_UI or EditorInfo.IME_FLAG_NO_FULLSCREEN
        }
        super.onUpdateExtractingVisibility(ei)
    }

    fun didCursorMove(): Boolean = cursorMoved

    fun ignoreNextCursorMove() {
        // This gets reset on the next call to `onUpdateCursorAnchorInfo`
        ignoreCursorMove = true
    }

    override fun onWindowHidden() {
        currentKeyboardDefinition?.settings?.textProcessor?.handleFinishInput(this)
        super.onWindowHidden()
    }

    private var ignoreCursorMove: Boolean = false
    private var cursorMoved: Boolean = false
    private var selectionStart: Int = 0
    private var selectionEnd: Int = 0

    // ViewModelStore Methods
    override val viewModelStore = ViewModelStore()

    // SaveStateRegistry Methods
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry =
        savedStateRegistryController.savedStateRegistry

    fun clipboardUsePrivate(): Boolean {
        val settingsRepo = (application as ThumbkeyApplication).appSettingsRepository
        val settings = settingsRepo.appSettings.getValue()
        val clipboardHistoryEnabled = (settings?.clipboardHistoryEnabled ?: DEFAULT_CLIPBOARD_HISTORY_ENABLED).toBool()
        val usePrivateClipboard = (settings?.usePrivateClipboard ?: DEFAULT_USE_PRIVATE_CLIPBOARD).toBool()
        return clipboardHistoryEnabled && usePrivateClipboard
    }

    fun clipboardAddPrivateClip(text: String): Unit? = clipboardManager?.addPrivateClip(text)

    fun clipboardWasLastCopyDoneViaSystem(): Boolean = clipboardManager?.wasLastCopyOperationDoneViaSystem() ?: true

    fun clipboardGetLastClip(): String? = clipboardManager?.getLastClip()
}
