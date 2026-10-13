package com.waveyy.app.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.waveyy.app.playback.QueueSource
import com.waveyy.app.ui.components.SkeletonRows
import com.waveyy.app.ui.components.WaveyyBackButton
import com.waveyy.app.ui.components.SongRow
import com.waveyy.app.ui.player.rememberChromeBottomReserve

/**
 * History: full-screen grid of recently-played songs (newest first),
 * backed by the local Room history table.
 *
 * @param viewModel screen logic.
 * @param onBack back handler.
 * @param onPlaySong queue playback handler.
 */
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
    onPlaySong: (QueueSource, Int) -> Unit = { _, _ -> }
) {
    val state by viewModel.state.collectAsState()
    val chromeBottom = rememberChromeBottomReserve()

    when (val current = state) {
        HistoryUiState.Loading -> SkeletonRows(count = 8)
        is HistoryUiState.Ready -> {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding(),
                contentPadding = PaddingValues(
                    top = 8.dp,
                    bottom = chromeBottom
                ),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        WaveyyBackButton(onClick = onBack)
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 12.dp)
                        ) {
                            Text(
                                text = "History",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${current.songs.size} ${if (current.songs.size == 1) "song" else "songs"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (current.songs.isNotEmpty()) {
                            TextButton(onClick = { viewModel.clear() }) {
                                Text("Clear")
                            }
                        }
                    }
                }

                if (current.songs.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 48.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = "Nothing played yet",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Songs you play will appear here.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(current.songs, key = { it.id }) { song ->
                        SongRow(
                            title = song.name,
                            artist = song.artist,
                            imageUrl = song.imageUrl,
                            songId = song.id,
                            onPlay = { _ ->
                                val index = current.songs.indexOf(song)
                                onPlaySong(QueueSource.Custom(current.songs), index)
                            },
                            duration = formatDurationSecs(song.durationSec)
                        )
                    }
                }
            }
        }
    }
}

/** Formats seconds as m:ss; empty when zero. */
private fun formatDurationSecs(totalSeconds: Long): String {
    if (totalSeconds <= 0L) return ""
    val minutes = totalSeconds / 60L
    val seconds = (totalSeconds % 60L).toString().padStart(2, '0')
    return "$minutes:$seconds"
}
