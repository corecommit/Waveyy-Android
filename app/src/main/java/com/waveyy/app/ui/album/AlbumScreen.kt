package com.waveyy.app.ui.album

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.waveyy.app.domain.model.Album
import com.waveyy.app.domain.model.Song
import com.waveyy.app.ui.components.AdaptiveImage
import com.waveyy.app.ui.components.SongCard
import com.waveyy.app.ui.components.WaveyyBadge
import com.waveyy.app.ui.components.WaveyyProgress
import com.waveyy.app.ui.theme.WaveyyShapes

/**
 * Album header (artwork, title, artists, song count) plus track list.
 * Mirrors `Album.jsx`.
 */
@Composable
fun AlbumScreen(
    viewModel: AlbumViewModel = hiltViewModel(),
    onPlaySong: (List<Song>, Int) -> Unit = { _, _ -> }
) {
    val state by viewModel.state.collectAsState()
    when (val current = state) {
        AlbumUiState.Loading, is AlbumUiState.Error -> {
            WaveyyProgress(progress = null, modifier = Modifier.padding(32.dp))
        }
        is AlbumUiState.Success -> AlbumContent(album = current.album, onPlaySong = onPlaySong)
    }
}

/** Loaded album body. */
@Composable
private fun AlbumContent(album: Album, onPlaySong: (List<Song>, Int) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        item {
            Column(modifier = Modifier.padding(16.dp)) {
                AdaptiveImage(
                    imageUrl = album.imageUrl,
                    contentDescription = album.name,
                    modifier = Modifier
                        .size(200.dp)
                        .aspectRatio(1f)
                        .clip(WaveyyShapes.medium)
                )
                Text(
                    text = album.name,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(top = 12.dp)
                )
                Text(
                    text = album.artist,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                WaveyyBadge(
                    text = "${album.songs.size} songs",
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
        itemsIndexed(album.songs, key = { _, song -> song.id }) { index, song ->
            SongCard(
                title = song.name,
                artist = song.artist,
                imageUrl = song.imageUrl,
                songId = song.id,
                onPlay = { onPlaySong(album.songs, index) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
