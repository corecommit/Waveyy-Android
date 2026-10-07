package com.waveyy.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Filterable command list. Ports `components/ui/command.jsx`
 * (search input + ranked row list).
 *
 * @param query current filter text.
 * @param onQueryChange filter text handler.
 * @param items all rows; filtered by [filter].
 * @param filter matching predicate over query and item label.
 * @param onSelect row selection handler.
 * @param modifier layout modifier.
 * @param emptyText text shown when the filter matches nothing.
 */
@Composable
fun WaveyyCommandPalette(
    query: String,
    onQueryChange: (String) -> Unit,
    items: List<String>,
    filter: (query: String, item: String) -> Boolean,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    emptyText: String = "No results"
) {
    var current by remember(query) { mutableStateOf(query) }
    Column(modifier = modifier) {
        WaveyyInput(
            value = current,
            onValueChange = {
                current = it
                onQueryChange(it)
            },
            modifier = Modifier.fillMaxWidth()
        )
        val visible = items.filter { filter(current, it) }
        if (visible.isEmpty()) {
            Text(
                text = emptyText,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(12.dp)
            )
        } else {
            LazyColumn {
                items(visible, key = { it }) { item ->
                    Text(
                        text = item,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(item) }
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    )
                }
            }
        }
    }
}
