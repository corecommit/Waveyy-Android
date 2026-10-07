package com.waveyy.app.ui.placeholder

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * Temporary stand-in for routes whose real screens land in later phases.
 * Each usage is replaced by its feature screen (Phases 6-10).
 *
 * @param label route label shown in the placeholder body.
 */
@Composable
fun PlaceholderScreen(label: String) {
    Scaffold { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Text(text = label, style = MaterialTheme.typography.titleMedium)
        }
    }
}
