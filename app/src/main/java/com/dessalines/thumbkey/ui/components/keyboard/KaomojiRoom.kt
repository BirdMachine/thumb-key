package com.dessalines.thumbkey.ui.components.keyboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

private const val FAVORITES_CATEGORY = "__favorites__"
private const val RECENTS_CATEGORY = "__recents__"

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun KaomojiRoom(
    onCommit: (String) -> Unit,
    onBackToLetters: () -> Unit,
    onGoToEmoji: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var selectedCategoryId by remember { mutableStateOf(KaomojiLibrary.categories.first().id) }
    var searchQuery by remember { mutableStateOf("") }
    var favorites by remember { mutableStateOf(KaomojiPreferences.loadFavorites(context)) }
    var recents by remember { mutableStateOf(KaomojiPreferences.loadRecents(context)) }
    val lozengeTheme = SuggestionLozengeThemePreferences.load(context)

    val items =
        if (searchQuery.isNotBlank()) {
            KaomojiLibrary.search(searchQuery).distinctBy { it.text }
        } else {
            when (selectedCategoryId) {
                FAVORITES_CATEGORY -> KaomojiLibrary.allItems.filter { it.text in favorites }
                RECENTS_CATEGORY -> recents.mapNotNull { recent -> KaomojiLibrary.allItems.firstOrNull { it.text == recent } }
                else ->
                    KaomojiLibrary.categories
                        .firstOrNull { it.id == selectedCategoryId }
                        ?.items
                        ?: KaomojiLibrary.categories.first().items
            }
        }

    fun commit(text: String) {
        recents = KaomojiPreferences.recordRecent(context, text)
        onCommit(text)
    }

    Column(
        modifier = modifier.padding(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Text(
                text = "KAOMOJI",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Search…") },
                singleLine = true,
            )

            onGoToEmoji?.let { goToEmoji ->
                PaletteLozengeButton(
                    text = "☺",
                    onClick = goToEmoji,
                    theme = lozengeTheme,
                )
            }
            PaletteLozengeButton(
                text = "✕",
                onClick = onBackToLetters,
                theme = lozengeTheme,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            AssistChip(
                onClick = {
                    searchQuery = ""
                    selectedCategoryId = FAVORITES_CATEGORY
                },
                label = { Text("★ Favorites") },
            )
            AssistChip(
                onClick = {
                    searchQuery = ""
                    selectedCategoryId = RECENTS_CATEGORY
                },
                label = { Text("↻ Recent") },
            )
            KaomojiLibrary.categories.forEach { entry ->
                AssistChip(
                    onClick = {
                        searchQuery = ""
                        selectedCategoryId = entry.id
                    },
                    label = {
                        Text(
                            text = "${entry.glyph} ${entry.title}",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                )
            }
        }

        if (items.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
            ) {
                Text(
                    text =
                        when {
                            searchQuery.isNotBlank() -> "No kaomoji match “$searchQuery”."
                            selectedCategoryId == FAVORITES_CATEGORY -> "No favorites yet — double-tap a kaomoji to pin it here."
                            else -> "Nothing here yet. Your recently used kaomoji will appear here."
                        },
                    modifier = Modifier.padding(14.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 132.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f),
            ) {
                items(items, key = { it.text }) { entry ->
                    val favorite = entry.text in favorites
                    val shape = RoundedCornerShape(14.dp)
                    val borderColor =
                        if (favorite) {
                            MaterialTheme.colorScheme.tertiary
                        } else {
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.75f)
                        }
                    Surface(
                        shape = shape,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = if (favorite) 0.82f else 0.68f),
                        border = BorderStroke(if (favorite) 2.dp else 1.dp, borderColor),
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clip(shape)
                                .combinedClickable(
                                    onClick = { commit(entry.text) },
                                    onDoubleClick = {
                                        favorites = KaomojiPreferences.toggleFavorite(context, entry.text)
                                    },
                                ),
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = entry.text,
                                style = MaterialTheme.typography.titleMedium,
                                color =
                                    if (favorite) {
                                        MaterialTheme.colorScheme.tertiary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                                fontWeight = if (favorite) FontWeight.SemiBold else FontWeight.Normal,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            entry.label?.let { label ->
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color =
                                        if (favorite) {
                                            MaterialTheme.colorScheme.tertiary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PaletteLozengeButton(
    text: String,
    onClick: () -> Unit,
    theme: SuggestionLozengeThemeState,
) {
    val shape = RoundedCornerShape(18.dp)
    val modifier =
        Modifier
            .clip(shape)
            .then(
                when (theme.surfaceStyle) {
                    SuggestionLozengeSurfaceStyle.GRADIENT -> Modifier.keyboardGradientBackground(theme.surfaceGradient)
                    SuggestionLozengeSurfaceStyle.SOLID,
                    SuggestionLozengeSurfaceStyle.NONE,
                    -> Modifier
                },
            ).then(
                when (theme.borderStyle) {
                    SuggestionLozengeBorderStyle.GRADIENT ->
                        Modifier.keyboardGradientBorder(
                            backdrop = theme.borderGradient,
                            width = theme.borderWidth.dp,
                            radius = 18.dp,
                        )
                    SuggestionLozengeBorderStyle.SOLID,
                    SuggestionLozengeBorderStyle.NONE,
                    -> Modifier
                },
            )

    Surface(
        onClick = onClick,
        shape = shape,
        color =
            when (theme.surfaceStyle) {
                SuggestionLozengeSurfaceStyle.SOLID -> theme.surfaceColor
                SuggestionLozengeSurfaceStyle.GRADIENT,
                SuggestionLozengeSurfaceStyle.NONE,
                -> Color.Transparent
            },
        border =
            when (theme.borderStyle) {
                SuggestionLozengeBorderStyle.SOLID -> BorderStroke(theme.borderWidth.dp, theme.borderColor)
                SuggestionLozengeBorderStyle.GRADIENT,
                SuggestionLozengeBorderStyle.NONE,
                -> null
            },
        tonalElevation = 2.dp,
        shadowElevation = 1.dp,
        modifier = modifier,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
        )
    }
}
