package com.dessalines.thumbkey.ui.components.settings.lookandfeel

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun BackdropSettingsSection(onChanged: () -> Unit) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            text = "Keywi appearance controls moved to Advanced look & feel.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = "Backdrop, toolbar, key surface, borders, fonts, and suggestion motion now live there.",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
