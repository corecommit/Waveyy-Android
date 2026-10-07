package com.waveyy.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.waveyy.app.MainActivity
import com.waveyy.app.domain.model.Song
import com.waveyy.app.ui.components.AlbumCard
import com.waveyy.app.ui.components.ArtistCard
import com.waveyy.app.ui.components.SongCard
import com.waveyy.app.ui.components.WaveyyProgress
import com.waveyy.app.ui.components.WaveyySkeleton
import timber.log.Timber

/**
 * Home rails: greeting, Made-for-You, Trending, Artists, Albums. Taps play
 * through the shared player queue.
 */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onPlaySong: (List<Song>, Int) -> Unit = { _, _ -> },
    onAlbum: (String) -> Unit = {},
    onArtist: (String) -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    val userName by viewModel.userName.collectAsState()
    LaunchedEffect(Unit) {
        Timber.tag(MainActivity.WAVEYY_TIMING).d("HomeScreen first composition")
    }
    Scaffold { padding ->
        when (val current = state) {
            HomeUiState.Loading -> HomeLoading(modifier = Modifier.padding(padding))
            is HomeUiState.Error -> HomeError(
                onRetry = viewModel::refresh,
                modifier = Modifier.padding(padding)
            )
            is HomeUiState.Success -> HomeContent(
                sections = current.sections,
                userName = userName,
                onPlaySong = onPlaySong,
                onAlbum = onAlbum,
                onArtist = onArtist,
                modifier = Modifier.padding(padding)
            )
        }
    }
}

/** Skeleton rails while loading: card-sized shimmer tiles per rail. */
@Composable
private fun HomeLoading(modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(3) {
            com.waveyy.app.ui.components.ShimmerRail()
        }
    }
}

/** Visible error with retry — an empty-data state must never be blank. */
@Composable
private fun HomeError(onRetry: () -> Unit, modifier: Modifier = Modifier) {
    androidx.compose.foundation.layout.Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
    ) {
        Text(
            text = androidx.compose.ui.res.stringResource(com.waveyy.app.R.string.home_error),
            style = MaterialTheme.typography.titleMedium
        )
        com.waveyy.app.ui.components.WaveyyButton(
            text = androidx.compose.ui.res.stringResource(com.waveyy.app.R.string.action_retry),
            onClick = onRetry,
            modifier = Modifier.padding(top = 12.dp)
        )
    }
}

/** Loaded rails. */
@Composable
private fun HomeContent(
    sections: HomeSections,
    userName: String?,
    onPlaySong: (List<Song>, Int) -> Unit,
    onAlbum: (String) -> Unit,
    onArtist: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            GreetingHeader(username = userName)
        }
        if (sections.recommended.isNotEmpty()) {
            item {
                RailTitle("Made for You")
                SongRail(songs = sections.recommended, onPlaySong = onPlaySong)
            }
        }
        if (sections.recent.isNotEmpty()) {
            item {
                RailTitle("Recently Played")
                SongRail(songs = sections.recent, onPlaySong = onPlaySong)
            }
        }
        item {
            RailTitle("Trending")
            SongRail(songs = sections.trending, onPlaySong = onPlaySong)
        }
        if (sections.popularArtists.isNotEmpty()) {
            item {
                RailTitle("Popular Artists")
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
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
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
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
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp)
    )
}

/** Horizontal song rail; taps play from the tapped index. */
@Composable
private fun SongRail(
    songs: List<Song>,
    onPlaySong: (List<Song>, Int) -> Unit
) {
    if (songs.isEmpty()) {
        WaveyyProgress(progress = null, modifier = Modifier.padding(16.dp))
        return
    }
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        itemsIndexed(songs, key = { _, song -> song.id }) { index, song ->
            SongCard(
                title = song.name,
                artist = song.artist,
                imageUrl = song.imageUrl,
                songId = song.id,
                description = null,
                onPlay = { onPlaySong(songs, index) }
            )
        }
    }
}
