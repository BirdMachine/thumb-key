from pathlib import Path


def replace_once(path: str, old: str, new: str) -> None:
    file_path = Path(path)
    text = file_path.read_text()
    if old not in text:
        if new in text:
            return
        raise RuntimeError(f"Expected source fragment not found in {path}: {old!r}")
    file_path.write_text(text.replace(old, new, 1))


replace_once(
    "app/src/main/java/com/dessalines/thumbkey/ui/components/keyboard/KeyboardScreen.kt",
    "import com.dessalines.thumbkey.BuildConfig\n",
    "",
)
replace_once(
    "app/src/main/java/com/dessalines/thumbkey/ui/components/keyboard/KeyboardScreen.kt",
    "    val backdropEnabled = (settings?.backdropEnabled ?: DEFAULT_BACKDROP_ENABLED).toBool()\n",
    "    val keywiEnabled = KeywiAppearancePreferences.load(ctx)\n"
    "    val backdropEnabled =\n"
    "        keywiEnabled && (settings?.backdropEnabled ?: DEFAULT_BACKDROP_ENABLED).toBool()\n",
)
replace_once(
    "app/src/main/java/com/dessalines/thumbkey/ui/components/keyboard/KeyboardScreen.kt",
    "        val keyGradient = if (BuildConfig.DEBUG) BIRDIE_KEY_GRADIENT else null\n",
    "        val keyGradient = if (keywiEnabled) BIRDIE_KEY_GRADIENT else null\n",
)
replace_once(
    "app/src/main/java/com/dessalines/thumbkey/ui/components/keyboard/KeyboardKey.kt",
    "    keyBorderGradient: KeyboardBackdrop? = BIRDIE_GOLD_BORDER,\n",
    "    keyBorderGradient: KeyboardBackdrop? =\n"
    "        if (KeywiAppearancePreferences.currentEnabled) BIRDIE_GOLD_BORDER else null,\n",
)
