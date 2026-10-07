package com.waveyy.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

/** One dropdown menu entry. */
data class WaveyyMenuEntry(
 /** Visible label. */
 val label: String,
 /** Selection handler. */
 val onSelect: () -> Unit
)

/**
 * Dropdown menu with an anchor trigger. Ports `components/ui/dropdown-menu.jsx`.
 *
 * @param entries menu entries.
 * @param modifier layout modifier.
 * @param anchor trigger content opening the menu.
 */
@Composable
fun WaveyyDropdownMenu(
    entries: List<WaveyyMenuEntry>,
    modifier: Modifier = Modifier,
    anchor: @Composable (open: () -> Unit) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        anchor { expanded = true }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            entries.forEach { entry ->
                DropdownMenuItem(
                    text = { Text(entry.label) },
                    onClick = {
                        expanded = false
                        entry.onSelect()
                    }
                )
            }
        }
    }
}
