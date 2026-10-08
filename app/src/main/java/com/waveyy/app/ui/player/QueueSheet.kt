package com.waveyy.app.ui.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.waveyy.app.domain.model.Song
import com.waveyy.app.ui.components.SongRow

/**
 * Queue sheet: reorder (up/down), remove, and clear. Ports
 * `components/player/queue-list.jsx`. The web's drag reorder becomes
 * accessible move buttons (no extra dependency).
 */
@Composable
fun QueueSheet(
    queue: List<Song>,
    currentId: String?,
    onMove: (Int, Int) -> Unit,
    onRemove: (Int) -> Unit,
    onClear: () -> Unit,
    onPlay: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (queue.isEmpty()) {
        Text(
            text = "Queue is empty",
            style = MaterialTheme.typography.bodyMedium,
            modifier = modifier.padding(24.dp)
        )
        return
    }
    LazyColumn(modifier = modifier) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Up Next", style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = onClear) {
                    Icon(imageVector = Icons.Filled.Clear, contentDescription = "Clear queue")
                }
            }
        }
        itemsIndexed(queue, key = { _, song -> song.id }) { index, song ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                SongRow(
                    title = song.name,
                    artist = song.artist,
                    imageUrl = song.imageUrl,
                    songId = song.id,
                    description = if (song.id == currentId) "Playing now" else null,
                    onPlay = { onPlay(index) },
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = { onMove(index, index - 1) },
                    enabled = index > 0
                ) {
                    Icon(imageVector = Icons.Filled.KeyboardArrowUp, contentDescription = "Move up")
                }
                IconButton(
                    onClick = { onMove(index, index + 1) },
                    enabled = index < queue.lastIndex
                ) {
                    Icon(imageVector = Icons.Filled.KeyboardArrowDown, contentDescription = "Move down")
                }
                IconButton(onClick = { onRemove(index) }) {
                    Icon(imageVector = Icons.Filled.Delete, contentDescription = "Remove")
                }
            }
        }
    }
}
