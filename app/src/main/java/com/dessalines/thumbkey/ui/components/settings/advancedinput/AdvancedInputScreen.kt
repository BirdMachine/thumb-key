package com.dessalines.thumbkey.ui.components.settings.advancedinput

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.dessalines.thumbkey.R
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
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.context_engine)) },
            )
        },
    ) { padding ->
        Text(
            text = stringResource(R.string.context_engine_placeholder),
            modifier = Modifier.padding(padding),
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
