package com.dessalines.thumbkey.ui.components.keyboard

/**
 * Built-in whole-string palette for the Kaomoji room.
 *
 * Keep the data model generic: the same category/item shape can later back user snippets,
 * decorative Unicode palettes, ASCII-art fragments, or a remote EmojiCombos provider.
 */
data class StringPaletteItem(
    val text: String,
    val label: String? = null,
    val alternates: List<String> = emptyList(),
    val tags: Set<String> = emptySet(),
)

data class StringPaletteCategory(
    val id: String,
    val title: String,
    val glyph: String,
    val items: List<StringPaletteItem>,
)

object KaomojiLibrary {
    val categories: List<StringPaletteCategory> =
        listOf(
            StringPaletteCategory(
                id = "happy",
                title = "Happy",
                glyph = "(＾▽＾)",
                items =
                    listOf(
                        item("(＾▽＾)", "happy", "smile"),
                        item("(｡•̀ᴗ-)✧", "wink", "sparkle"),
                        item("(ﾉ◕ヮ◕)ﾉ*:･ﾟ✧", "celebrate", "sparkle"),
                        item("ヽ(・∀・)ﾉ", "yay", "wave"),
                        item("٩(◕‿◕｡)۶", "excited", "yay"),
                        item("(✿◠‿◠)", "sweet", "flower"),
                    ),
            ),
            StringPaletteCategory(
                id = "affection",
                title = "Affection",
                glyph = "♡",
                items =
                    listOf(
                        item("(づ｡◕‿‿◕｡)づ", "hug", "love"),
                        item("(っ˘з(˘⌣˘ )", "kiss", "love"),
                        item("♡( ◡‿◡ )", "love", "soft"),
                        item("( ˘ ³˘)♥", "kiss", "heart"),
                        item("(づ￣ ³￣)づ", "hug", "kiss"),
                    ),
            ),
            StringPaletteCategory(
                id = "smug",
                title = "Smug",
                glyph = "(¬‿¬)",
                items =
                    listOf(
                        item("(¬‿¬)", "smug", "side-eye"),
                        item("(￣ω￣)", "smug", "calm"),
                        item("( ͡° ͜ʖ ͡°)", "lenny", "mischief"),
                        item("(￣▽￣)ノ", "casual", "wave"),
                        item("( •̀ᴗ•́ )و", "confident", "determined"),
                    ),
            ),
            StringPaletteCategory(
                id = "chaos",
                title = "Chaos",
                glyph = "╯︵┻━┻",
                items =
                    listOf(
                        item("(╯°□°）╯︵ ┻━┻", "table flip", "rage"),
                        item("┬─┬ノ( º _ ºノ)", "table restore", "calm"),
                        item("┻━┻ ︵ヽ(`Д´)ﾉ︵ ┻━┻", "double flip", "rage"),
                        item("ヘ(°◇、°)ノ", "derp", "chaos"),
                        item("༼ つ ◕_◕ ༽つ", "summon", "give"),
                    ),
            ),
            StringPaletteCategory(
                id = "sad",
                title = "Sad",
                glyph = "(╥﹏╥)",
                items =
                    listOf(
                        item("(╥﹏╥)", "cry", "sad"),
                        item("(｡•́︿•̀｡)", "sad", "soft"),
                        item("(っ- ‸ - ς)", "upset", "sad"),
                        item("ಥ_ಥ", "tears", "sad"),
                        item("(ノ_<。)", "cry", "upset"),
                    ),
            ),
            StringPaletteCategory(
                id = "angry",
                title = "Angry",
                glyph = "(ಠ益ಠ)",
                items =
                    listOf(
                        item("(ಠ益ಠ)", "rage", "angry"),
                        item("ヽ( `д´*)ノ", "angry", "shout"),
                        item("(ง'̀-'́)ง", "fight", "determined"),
                        item("(╬ಠ益ಠ)", "furious", "rage"),
                        item("凸(￣ヘ￣)", "rude", "angry"),
                    ),
            ),
            StringPaletteCategory(
                id = "animals",
                title = "Animals",
                glyph = "ʕ•ᴥ•ʔ",
                items =
                    listOf(
                        item("ʕ•ᴥ•ʔ", "bear", "animal"),
                        item("ฅ^•ﻌ•^ฅ", "cat", "animal"),
                        item("(=^･ω･^=)", "cat", "animal"),
                        item("U・ᴥ・U", "dog", "animal"),
                        item("(•ө•)♡", "bird", "animal", "love"),
                    ),
            ),
            StringPaletteCategory(
                id = "shrug",
                title = "Shrug & React",
                glyph = "¯\\_(ツ)_/¯",
                items =
                    listOf(
                        item("¯\\_(ツ)_/¯", "shrug", "react"),
                        item("ಠ_ಠ", "disapprove", "react"),
                        item("(・_・;)", "awkward", "react"),
                        item("(⊙_⊙)", "shock", "react"),
                        item("(・・ ) ?", "confused", "react"),
                    ),
            ),
        )

    val allItems: List<StringPaletteItem>
        get() = categories.flatMap(StringPaletteCategory::items)

    fun search(query: String): List<StringPaletteItem> {
        val needle = query.trim().lowercase()
        if (needle.isEmpty()) return allItems

        return allItems.filter { entry ->
            entry.text.contains(query, ignoreCase = true) ||
                entry.label?.contains(needle, ignoreCase = true) == true ||
                entry.tags.any { it.contains(needle, ignoreCase = true) }
        }
    }

    private fun item(
        text: String,
        vararg tags: String,
    ): StringPaletteItem =
        StringPaletteItem(
            text = text,
            label = tags.firstOrNull(),
            tags = tags.toSet(),
        )
}
