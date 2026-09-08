package com.dessalines.thumbkey.ui.components.keyboard

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
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

private const val FAVORITES_CATEGORY = "__favorites__"
private const val RECENTS_CATEGORY = "__recents__"

@Composable
fun KaomojiRoom(
    onCommit: (String) -> Unit,
    onBackToLetters: () -> Unit,
    onGoToEmoji: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var selectedCategoryId by remember { mutableStateOf(KaomojiLibrary.categories.first().id) }
    var favorites by remember { mutableStateOf(KaomojiPreferences.loadFavorites(context)) }
    var recents by remember { mutableStateOf(KaomojiPreferences.loadRecents(context)) }

    val items =
        when (selectedCategoryId) {
            FAVORITES_CATEGORY -> KaomojiLibrary.allItems.filter { it.text in favorites }
            RECENTS_CATEGORY -> recents.mapNotNull { recent -> KaomojiLibrary.allItems.firstOrNull { it.text == recent } }
            else ->
                KaomojiLibrary.categories
                    .firstOrNull { it.id == selectedCategoryId }
                    ?.items
                    ?: KaomojiLibrary.categories.first().items
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
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "KEYWI // KAOMOJI",
                style = MaterialTheme.typography.labelLarge,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                onGoToEmoji?.let { goToEmoji ->
                    TextButton(onClick = goToEmoji) {
                        Text("☺")
                    }
                }
                Button(onClick = onBackToLetters) {
                    Text("ABC")
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            AssistChip(
                onClick = { selectedCategoryId = FAVORITES_CATEGORY },
                label = { Text("★ Favorites") },
            )
            AssistChip(
                onClick = { selectedCategoryId = RECENTS_CATEGORY },
                label = { Text("↻ Recent") },
            )
            KaomojiLibrary.categories.forEach { entry ->
                AssistChip(
                    onClick = { selectedCategoryId = entry.id },
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
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text =
                        if (selectedCategoryId == FAVORITES_CATEGORY) {
                            "No favorites yet — tap ☆ on a kaomoji to pin it here."
                        } else {
                            "Nothing here yet. Your recently used kaomoji will appear here."
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
                    Card(onClick = { commit(entry.text) }) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = entry.text,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                )
                                TextButton(
                                    onClick = {
                                        favorites = KaomojiPreferences.toggleFavorite(context, entry.text)
                                    },
                                ) {
                                    Text(if (entry.text in favorites) "★" else "☆")
                                }
                            }
                            entry.label?.let { label ->
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
