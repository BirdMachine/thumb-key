package com.dessalines.thumbkey.ui.components.settings.advancedinput

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.dessalines.thumbkey.R
import com.dessalines.thumbkey.inputcontext.ContextEnginePreferences
import me.zhanghai.compose.preference.Preference
import me.zhanghai.compose.preference.ProvidePreferenceTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedInputScreen(navController: NavController) {
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.advanced_input)) },
            )
        },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .padding(padding)
                    .verticalScroll(scrollState)
                    .background(MaterialTheme.colorScheme.surface),
        ) {
            ProvidePreferenceTheme {
                Preference(
                    title = { Text(stringResource(R.string.context_engine)) },
                    summary = { Text(stringResource(R.string.context_engine_description)) },
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.AutoAwesome,
                            contentDescription = null,
                        )
                    },
                    onClick = { navController.navigate("contextEngine") },
                )
                Preference(
                    title = { Text(stringResource(R.string.advanced_characters)) },
                    summary = { Text(stringResource(R.string.advanced_characters_description)) },
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.TextFields,
                            contentDescription = null,
                        )
                    },
                    onClick = { navController.navigate("advancedCharacters") },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContextEngineScreen() {
    val context = LocalContext.current
    val initial = remember { ContextEnginePreferences.load(context) }
    var adaptToField by remember { mutableStateOf(initial.adaptToField) }
    var smartEnter by remember { mutableStateOf(initial.smartEnter) }
    var suppressSensitiveSuggestions by remember { mutableStateOf(initial.suppressSensitiveSuggestions) }
    var preserveStructuredTokens by remember { mutableStateOf(initial.preserveEmailAndUrlTokens) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.context_engine)) },
            )
        },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Card {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("KEYWI // CONTEXT ENGINE", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Keywi derives a capability profile from each Android text field instead of " +
                            "treating every editor like the same blank box.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            Card {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ContextSwitchRow(
                        title = "Adapt to text field",
                        summary =
                            "Use field type, multiline state, IME action, selection, and sensitivity " +
                                "when deciding which features are appropriate.",
                        checked = adaptToField,
                        onCheckedChange = {
                            adaptToField = it
                            ContextEnginePreferences.setAdaptToField(context, it)
                        },
                    )
                    ContextSwitchRow(
                        title = "Smart Enter",
                        summary =
                            "Use newline in multiline editors and prefer Send, Search, Done, or Next " +
                                "actions in single-line fields when Android exposes one.",
                        checked = smartEnter,
                        enabled = adaptToField,
                        onCheckedChange = {
                            smartEnter = it
                            ContextEnginePreferences.setSmartEnter(context, it)
                        },
                    )
                    ContextSwitchRow(
                        title = "Suppress suggestions in sensitive fields",
                        summary = "Do not query suggestion dictionaries for password-style fields.",
                        checked = suppressSensitiveSuggestions,
                        enabled = adaptToField,
                        onCheckedChange = {
                            suppressSensitiveSuggestions = it
                            ContextEnginePreferences.setSuppressSensitiveSuggestions(context, it)
                        },
                    )
                    ContextSwitchRow(
                        title = "Keep @ and . inside structured tokens",
                        summary =
                            "Treat email addresses, handles, URLs, and dotted identifiers as one " +
                                "working token instead of chopping them at punctuation.",
                        checked = preserveStructuredTokens,
                        enabled = adaptToField,
                        onCheckedChange = {
                            preserveStructuredTokens = it
                            ContextEnginePreferences.setPreserveEmailAndUrlTokens(context, it)
                        },
                    )
                }
            }

            Card {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text("Capability rules now active", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "• Password fields are treated as sensitive.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        "• Selected-text transforms only appear in editable text fields with a real selection.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        "• Smart Enter distinguishes multiline newline behavior from single-line IME actions.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        "• Suggestions, Advanced Characters, and ✨ Tools all consume the same shared policy.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun ContextSwitchRow(
    title: String,
    summary: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(summary, style = MaterialTheme.typography.bodySmall)
        }
        Switch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedCharactersScreen() {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.advanced_characters)) },
            )
        },
    ) { padding ->
        Text(
            text = stringResource(R.string.advanced_characters_placeholder),
            modifier = Modifier.padding(padding),
        )
    }
}
