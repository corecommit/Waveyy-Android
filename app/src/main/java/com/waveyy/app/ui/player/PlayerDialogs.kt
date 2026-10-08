package com.waveyy.app.ui.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.waveyy.app.domain.model.Song
import com.waveyy.app.ui.library.LibraryViewModel

/**
 * Playlist picker for "Add to a playlist": lists user playlists, tapping
 * adds the song. Guests see an empty list (creation needs a session).
 */
@Composable
fun PlaylistPickerDialog(
    song: Song,
    libraryVm: LibraryViewModel,
    onDismiss: () -> Unit
) {
    val state by libraryVm.state.collectAsState()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to playlist") },
        text = {
            LazyColumn {
                items(state.playlists, key = { it.id }) { playlist ->
                    Text(
                        text = playlist.name,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                libraryVm.addSong(playlist.id, song)
                                onDismiss()
                            }
                            .padding(vertical = 12.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

/** Sleep timer options (minutes, or end-of-song one-shot). */
@Composable
fun SleepTimerDialog(
    onDismiss: () -> Unit,
    onSelect: (Long?) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sleep timer") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(15L to "15 minutes", 30L to "30 minutes", 45L to "45 minutes", 60L to "60 minutes").forEach { (minutes, label) ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(minutes) }
                            .padding(vertical = 10.dp)
                    )
                }
                Text(
                    text = "End of song",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(null) }
                        .padding(vertical = 10.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

/** Playback speed picker (0.5x-2.0x). */
@Composable
fun SpeedDialog(
    initial: Float,
    onDismiss: () -> Unit,
    onSelect: (Float) -> Unit
) {
    var speed by remember(initial) { mutableFloatStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Playback speed") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "%.1fx".format(speed),
                    style = MaterialTheme.typography.headlineSmall
                )
                Slider(
                    value = speed,
                    onValueChange = { speed = it },
                    valueRange = 0.5f..2.0f,
                    steps = 14,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSelect(speed) }) { Text("Apply") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

/** Lyrics provider picker (lrclib fetch vs disabled). */
@Composable
fun LyricsProviderDialog(
    viewModel: PlayerViewModel,
    onDismiss: () -> Unit
) {
    val current by viewModel.lyricsProvider.collectAsState()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Lyrics provider") },
        text = {
            Column {
                listOf("lrclib", "none").forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.setLyricsProvider(option)
                                onDismiss()
                            }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = current == option,
                            onClick = {
                                viewModel.setLyricsProvider(option)
                                onDismiss()
                            }
                        )
                        Text(
                            text = if (option == "none") "None (disabled)" else option,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
