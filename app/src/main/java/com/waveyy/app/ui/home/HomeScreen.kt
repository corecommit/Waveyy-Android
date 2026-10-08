package com.waveyy.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.waveyy.app.MainActivity
import com.waveyy.app.R
import com.waveyy.app.domain.model.Album
import com.waveyy.app.domain.model.Song
import com.waveyy.app.playback.QueueSource
import com.waveyy.app.ui.components.AdaptiveImage
import com.waveyy.app.ui.components.ArtistCard
import com.waveyy.app.ui.components.SkeletonHomeScreen
import com.waveyy.app.ui.components.WaveyyButton
import timber.log.Timber

/**
 * Home: header row, greeting, hero card, continue grid, and section grids
 * (Made for You, Popular Artists rail, Popular Albums, Trending Now, New
 * Releases). Taps play through the shared player queue.
 */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onPlaySong: (QueueSource, Int) -> Unit = { _, _ -> },
    onAlbum: (String) -> Unit = {},
    onArtist: (String) -> Unit = {},
    onSearch: () -> Unit = {},
    onSettings: () -> Unit = {},
    onDiscover: () -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    val userName by viewModel.userName.collectAsState()
    LaunchedEffect(Unit) {
        Timber.tag(MainActivity.WAVEYY_TIMING).d("HomeScreen first composition")
    }
    Scaffold { padding ->
        when (val current = state) {
            HomeUiState.Loading -> SkeletonHomeScreen(
                modifier = Modifier.padding(padding)
            )
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
                onSearch = onSearch,
                onSettings = onSettings,
                modifier = Modifier.padding(padding)
            )
        }
    }
}

/** Visible error with retry — an empty-data state must never be blank. */
@Composable
private fun HomeError(onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.home_error),
            style = MaterialTheme.typography.titleMedium
        )
        WaveyyButton(
            text = stringResource(R.string.action_retry),
            onClick = onRetry,
            modifier = Modifier.padding(top = 12.dp)
        )
    }
}

/** Loaded home: header, greeting, hero, grids, artists rail. */
@Composable
private fun HomeContent(
    sections: HomeSections,
    userName: String?,
    onPlaySong: (QueueSource, Int) -> Unit,
    onAlbum: (String) -> Unit,
    onArtist: (String) -> Unit,
    onSearch: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hero = sections.trending.firstOrNull()
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item(span = { GridItemSpan(2) }) {
            HomeHeader(
                onExplore = onDiscover,
                onSearch = onSearch,
                onSettings = onSettings
            )
        }
        item(span = { GridItemSpan(2) }) {
            GreetingHeader(username = userName)
        }
        if (hero != null) {
            item(span = { GridItemSpan(2) }) {
                HeroCard(
                    song = hero,
                    songs = sections.trending,
                    onPlaySong = onPlaySong
                )
            }
        }
        if (sections.recent.isNotEmpty()) {
            item(span = { GridItemSpan(2) }) {
                GridSectionTitle("Continue")
            }
            items(sections.recent, key = { it.id }) { song ->
                val index = sections.recent.indexOf(song)
                GridSongCard(
                    song = song,
                    onPlay = { onPlaySong(QueueSource.HomeFeed(sections.recent), index) }
                )
            }
        }
        if (sections.recommended.isNotEmpty()) {
            item(span = { GridItemSpan(2) }) {
                GridSectionTitle("Made for You")
            }
            items(sections.recommended, key = { it.id }) { song ->
                val index = sections.recommended.indexOf(song)
                GridSongCard(
                    song = song,
                    onPlay = { onPlaySong(QueueSource.HomeFeed(sections.recommended), index) }
                )
            }
        }
        if (sections.popularArtists.isNotEmpty()) {
            item(span = { GridItemSpan(2) }) {
                GridSectionTitle("Popular Artists")
            }
            item(span = { GridItemSpan(2) }) {
                androidx.compose.foundation.lazy.LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
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
            item(span = { GridItemSpan(2) }) {
                GridSectionTitle("Popular Albums")
            }
            items(sections.popularAlbums, key = { it.id }) { album ->
                GridAlbumCard(album = album, onOpen = { onAlbum(album.id) })
            }
        }
        if (sections.trending.isNotEmpty()) {
            item(span = { GridItemSpan(2) }) {
                GridSectionTitle("Trending Now")
            }
            items(sections.trending, key = { it.id }) { song ->
                val index = sections.trending.indexOf(song)
                GridSongCard(
                    song = song,
                    onPlay = { onPlaySong(QueueSource.HomeFeed(sections.trending), index) }
                )
            }
        }
        if (sections.newReleases.isNotEmpty()) {
            item(span = { GridItemSpan(2) }) {
                GridSectionTitle("New Releases")
            }
            items(sections.newReleases, key = { it.id }) { song ->
                val index = sections.newReleases.indexOf(song)
                GridSongCard(
                    song = song,
                    onPlay = { onPlaySong(QueueSource.HomeFeed(sections.newReleases), index) }
                )
            }
        }
    }
}

/** Section header: titleLarge bold with reference padding. */
@Composable
private fun GridSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 16.dp)
    )
}

/**
 * Hero card: full-width 16:9 artwork with a bottom gradient scrim and
 * overlaid title/artist. Tapping plays from the section start.
 */
@Composable
private fun HeroCard(
    song: Song,
    songs: List<Song>,
    onPlaySong: (QueueSource, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clickable { onPlaySong(QueueSource.HomeFeed(songs), songs.indexOf(song).coerceAtLeast(0)) }
            .clip(RoundedCornerShape(24.dp))
    ) {
        AdaptiveImage(
            imageUrl = song.imageUrl,
            contentDescription = song.name,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f),
            contentScale = ContentScale.Crop
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.7f)
                    )
                )
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        ) {
            Text(
                text = song.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.artist,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.8f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** 2-column grid song card: square art, 2-line title, muted subtitle. */
@Composable
fun GridSongCard(
    song: Song,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clickable(onClick = onPlay)
    ) {
        AdaptiveImage(
            imageUrl = song.imageUrl,
            contentDescription = song.name,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
        )
        Text(
            text = song.name,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp)
        )
        Text(
            text = song.artist,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** 2-column grid album card. */
@Composable
private fun GridAlbumCard(
    album: Album,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clickable(onClick = onOpen)
    ) {
        AdaptiveImage(
            imageUrl = album.imageUrl,
            contentDescription = album.name,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
        )
        Text(
            text = album.name,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp)
        )
        Text(
            text = album.artist,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
