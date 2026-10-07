package com.waveyy.app.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.waveyy.app.R
import com.waveyy.app.domain.model.Song
import com.waveyy.app.ui.components.AdaptiveImage
import com.waveyy.app.ui.components.AlbumCard
import com.waveyy.app.ui.components.ArtistCard
import com.waveyy.app.ui.components.SongCard
import com.waveyy.app.ui.components.WaveyyProgress

/**
 * Search results: Songs rail, Top Result artist card, Albums and Artists
 * rails. Mirrors the sections of `Search.jsx`.
 */
@Composable
fun SearchScreen(
    viewModel: SearchViewModel = hiltViewModel(),
    onPlaySong: (List<Song>, Int) -> Unit = { _, _ -> },
    onAlbum: (String) -> Unit = {},
    onArtist: (String) -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    when (val current = state) {
        SearchUiState.Loading, is SearchUiState.Error -> {
            WaveyyProgress(progress = null, modifier = Modifier.padding(32.dp))
        }
        is SearchUiState.Success -> {
            if (current.songs.isEmpty() && current.albums.isEmpty() && current.artists.isEmpty()) {
                Text(
                    text = stringResource(R.string.search_hint),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(32.dp)
                )
            } else {
                SearchResults(current, onPlaySong, onAlbum, onArtist)
            }
        }
    }
}

/** Loaded results rails. */
@Composable
private fun SearchResults(
    current: SearchUiState.Success,
    onPlaySong: (List<Song>, Int) -> Unit,
    onAlbum: (String) -> Unit,
    onArtist: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        current.topArtist?.let { top ->
            item {
                SectionTitle("Top Result")
                ArtistCard(
                    name = top.artist.name,
                    imageUrl = top.artist.imageUrl,
                    artistId = top.artist.id,
                    onOpen = onArtist,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }
        }
        item {
            SectionTitle("Songs")
        }
        itemsIndexed(current.songs, key = { _, song -> song.id }) { index, song ->
            SongRow(
                song = song,
                onPlay = { onPlaySong(current.songs, index) }
            )
        }
        if (current.albums.isNotEmpty()) {
            item {
                SectionTitle("Albums")
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
                    items(current.albums, key = { it.id }) { album ->
                        AlbumCard(
                            title = album.name,
                            artist = album.artist,
                            imageUrl = album.imageUrl,
                            albumId = album.id,
                            onOpen = onAlbum
                        )
                    }
                }
            }
        }
        if (current.artists.isNotEmpty()) {
            item {
                SectionTitle("Artists")
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
                    items(current.artists, key = { it.id }) { artist ->
                        ArtistCard(
                            name = artist.name,
                            imageUrl = artist.imageUrl,
                            artistId = artist.id,
                            onOpen = onArtist
                        )
                    }
                }
            }
        }
    }
}

/** Section header. */
@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp)
    )
}

/** Vertical song row: thumbnail, title, artist, duration. Tappable to play. */
@Composable
private fun SongRow(
    song: Song,
    onPlay: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPlay)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AdaptiveImage(
            imageUrl = song.imageUrl,
            contentDescription = song.name,
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp))
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
        ) {
            Text(
                text = song.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = formatDuration(song.durationSec),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Formats seconds as m:ss. */
private fun formatDuration(totalSeconds: Long): String {
    if (totalSeconds <= 0L) return "--:--"
    val minutes = totalSeconds / 60L
    val seconds = (totalSeconds % 60L).toString().padStart(2, '0')
    return "$minutes:$seconds"
}
