package com.waveyy.app.ui.discover

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.waveyy.app.domain.model.Song
import com.waveyy.app.ui.components.WaveyyBackButton
import com.waveyy.app.ui.components.WaveyyErrorPanel
import com.waveyy.app.ui.components.SkeletonRows
import com.waveyy.app.ui.player.rememberChromeBottomReserve

/**
 * Discover: full-screen trending grid. Target of the compass button.
 *
 * @param onPlaySong queue playback handler.
 */
@Composable
fun DiscoverScreen(
    viewModel: DiscoverViewModel = hiltViewModel(),
    onPlaySong: (com.waveyy.app.playback.QueueSource, Int) -> Unit = { _, _ -> },
    onBack: () -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    when (val current = state) {
        DiscoverUiState.Loading -> {
            SkeletonRows(count = 8)
        }
        is DiscoverUiState.Error -> {
            WaveyyErrorPanel(onRetry = viewModel::refresh)
        }
        is DiscoverUiState.Success -> {
            val chromeBottom = rememberChromeBottomReserve()
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding(),
                contentPadding = PaddingValues(bottom = chromeBottom),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        WaveyyBackButton(onClick = onBack)
                        Text(
                            text = "Discover",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                items(current.songs, key = { it.id }) { song ->
                    DiscoverTile(
                        song = song,
                        onPlay = {
                            val index = current.songs.indexOf(song)
                            onPlaySong(
                                com.waveyy.app.playback.QueueSource.Recommendations(current.songs),
                                index
                            )
                        }
                    )
                }
            }
        }
    }
}

/** Square discover tile: art, 2-line title, muted artist. */
@Composable
private fun DiscoverTile(song: Song, onPlay: () -> Unit) {
    com.waveyy.app.ui.home.GridSongCard(song = song, onPlay = onPlay)
}
