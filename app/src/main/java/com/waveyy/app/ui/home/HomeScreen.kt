package com.waveyy.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.waveyy.app.domain.model.Song
import com.waveyy.app.ui.components.AlbumCard
import com.waveyy.app.ui.components.ArtistCard
import com.waveyy.app.ui.components.SongCard
import com.waveyy.app.ui.components.WaveyyProgress
import com.waveyy.app.ui.components.WaveyySkeleton

/**
 * Home rails: Made-for-You, Trending, Artists, Albums. Song taps hand to
 * playback in Phase 8 (currently no-ops).
 */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onPlay: (String) -> Unit = {},
    onAlbum: (String) -> Unit = {},
    onArtist: (String) -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    Scaffold { padding ->
        when (val current = state) {
            HomeUiState.Loading -> HomeLoading(modifier = Modifier.padding(padding))
            is HomeUiState.Error -> HomeLoading(modifier = Modifier.padding(padding))
            is HomeUiState.Success -> HomeContent(
                sections = current.sections,
                onPlay = onPlay,
                onAlbum = onAlbum,
                onArtist = onArtist,
                modifier = Modifier.padding(padding)
            )
        }
    }
}

/** Skeleton rails while loading (or on error, matching web skeletons). */
@Composable
private fun HomeLoading(modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(3) {
            WaveyySkeleton(modifier = Modifier.fillParentMaxWidth())
        }
    }
}

/** Loaded rails. */
@Composable
private fun HomeContent(
    sections: HomeSections,
    onPlay: (String) -> Unit,
    onAlbum: (String) -> Unit,
    onArtist: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (sections.recommended.isNotEmpty()) {
            item {
                RailTitle("Made for You")
                SongRail(songs = sections.recommended, onPlay = onPlay)
            }
        }
        if (sections.recent.isNotEmpty()) {
            item {
                RailTitle("Recently Played")
                SongRail(songs = sections.recent, onPlay = onPlay)
            }
        }
        item {
            RailTitle("Trending")
            SongRail(songs = sections.trending, onPlay = onPlay)
        }
        if (sections.popularArtists.isNotEmpty()) {
            item {
                RailTitle("Popular Artists")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(sections.popularArtists, key = { it.id }) { artist ->
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
        if (sections.popularAlbums.isNotEmpty()) {
            item {
                RailTitle("Popular Albums")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(sections.popularAlbums, key = { it.id }) { album ->
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
    }
}

/** Section header. */
@Composable
private fun RailTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

/** Horizontal song rail. */
@Composable
private fun SongRail(songs: List<Song>, onPlay: (String) -> Unit) {
    if (songs.isEmpty()) {
        WaveyyProgress(progress = null, modifier = Modifier.padding(16.dp))
        return
    }
    LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        items(songs, key = { it.id }) { song ->
            SongCard(
                title = song.name,
                artist = song.artist,
                imageUrl = song.imageUrl,
                songId = song.id,
                description = null,
                onPlay = onPlay
            )
        }
    }
}
