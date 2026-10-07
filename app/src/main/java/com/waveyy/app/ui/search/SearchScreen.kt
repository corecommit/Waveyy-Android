package com.waveyy.app.ui.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
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
    onPlay: (String) -> Unit = {},
    onAlbum: (String) -> Unit = {},
    onArtist: (String) -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    when (val current = state) {
        SearchUiState.Loading, is SearchUiState.Error -> {
            WaveyyProgress(progress = null, modifier = Modifier.padding(32.dp))
        }
        is SearchUiState.Success -> LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
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
                LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(current.songs, key = { it.id }) { song ->
                        SongCard(
                            title = song.name,
                            artist = song.artist,
                            imageUrl = song.imageUrl,
                            songId = song.id,
                            onPlay = onPlay
                        )
                    }
                }
            }
            if (current.albums.isNotEmpty()) {
                item {
                    SectionTitle("Albums")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
}

/** Section header. */
@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}
