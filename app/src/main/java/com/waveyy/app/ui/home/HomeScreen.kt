package com.waveyy.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.waveyy.app.MainActivity
import com.waveyy.app.R
import com.waveyy.app.domain.model.Song
import com.waveyy.app.playback.QueueSource
import com.waveyy.app.ui.MainViewModel
import com.waveyy.app.ui.sharedMainViewModel
import com.waveyy.app.ui.navigation.Routes
import com.waveyy.app.ui.player.rememberChromeBottomReserve
import com.waveyy.app.ui.components.AlbumCard
import com.waveyy.app.ui.components.ArtistCard
import com.waveyy.app.ui.components.SkeletonHomeScreen
import com.waveyy.app.ui.components.SongCard
import com.waveyy.app.ui.components.SongRow
import com.waveyy.app.ui.components.WaveyyButton
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.flow.filter
import timber.log.Timber

/**
 * Home: header row + Quick Picks list + four horizontal rails (Recently
 * Played, Made for You, Artists, Albums). Taps play through the shared
 * player queue.
 */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onPlaySong: (QueueSource, Int) -> Unit = { _, _ -> },
    onAlbum: (String) -> Unit = {},
    onArtist: (String) -> Unit = {},
    onSearch: () -> Unit = {},
    onSettings: () -> Unit = {},
    onHistory: () -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    val refreshing by viewModel.isRefreshing.collectAsState()
    LaunchedEffect(Unit) {
        Timber.tag(MainActivity.WAVEYY_TIMING).d("HomeScreen first composition")
    }
    Scaffold(contentWindowInsets = WindowInsets(0)) { padding ->
        when (val current = state) {
            HomeUiState.Loading -> HomeSkeleton(
                padding = padding,
                onHistory = onHistory,
                onSearch = onSearch,
                onSettings = onSettings
            )
            is HomeUiState.Error -> HomeError(
                onRetry = viewModel::refresh,
                modifier = Modifier.padding(padding)
            )
            is HomeUiState.Success -> {
                val sections = current.sections
                val nothingArrived = sections.quickPicks.isEmpty() &&
                    sections.recentlyPlayed.isEmpty() &&
                    sections.madeForYou.isEmpty() &&
                    sections.artists.isEmpty() &&
                    sections.albums.isEmpty()
                if (nothingArrived) {
                    HomeSkeleton(
                        padding = padding,
                        onHistory = onHistory,
                        onSearch = onSearch,
                        onSettings = onSettings
                    )
                } else {
                    HomeContent(
                        sections = sections,
                        onPlaySong = onPlaySong,
                        onAlbum = onAlbum,
                        onArtist = onArtist,
                        onSearch = onSearch,
                        onSettings = onSettings,
                        onHistory = onHistory,
                        isRefreshing = refreshing,
                        onRefresh = viewModel::refreshQuiet,
                        modifier = Modifier.padding(padding)
                    )
                }
            }
        }
    }
}

/** Loading skeleton branch — extracted so both Loading and empty Success use it. */
@Composable
private fun HomeSkeleton(
    padding: PaddingValues,
    onHistory: () -> Unit,
    onSearch: () -> Unit,
    onSettings: () -> Unit
) {
    val hazeState = remember { HazeState() }
    val density = LocalDensity.current
    val statusTop = with(density) {
        WindowInsets.statusBars.getTop(density).toDp()
    }
    SkeletonHomeScreen(
        modifier = Modifier.padding(padding),
        topContentPadding = statusTop + 64.dp,
        hazeState = hazeState,
        header = {
            HomeHeader(
                onHistory = onHistory,
                onSearch = onSearch,
                onSettings = onSettings,
                modifier = Modifier
                    .fillMaxWidth()
                    .hazeEffect(
                        state = hazeState,
                        style = HazeStyle(
                            backgroundColor = MaterialTheme.colorScheme.background.copy(alpha = 0.85f),
                            tint = null,
                            blurRadius = 40.dp,
                            noiseFactor = 0f
                        )
                    )
            )
        }
    )
}

/** Visible error with retry. */
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

/** Loaded home: fixed header overlay + scrollable rail column. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeContent(
    sections: HomeSections,
    onPlaySong: (QueueSource, Int) -> Unit,
    onAlbum: (String) -> Unit,
    onArtist: (String) -> Unit,
    onSearch: () -> Unit,
    onSettings: () -> Unit,
    onHistory: () -> Unit,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    mainVm: MainViewModel = sharedMainViewModel()
) {
    val listState = rememberLazyListState()
    val chromeBottom = rememberChromeBottomReserve()
    val hazeState = remember { HazeState() }
    val density = LocalDensity.current
    val statusTop = with(density) { WindowInsets.statusBars.getTop(density).toDp() }
    val listTopPadding = statusTop + 64.dp

    LaunchedEffect(Unit) {
        mainVm.scrollToTop
            .filter { it == Routes.HOME }
            .collect {
                if (listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0) {
                    listState.animateScrollToItem(0)
                }
            }
    }

    Box(modifier = modifier.fillMaxSize()) {
        val pullRefreshState = rememberPullToRefreshState()
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize(),
            state = pullRefreshState,
            indicator = {
                PullToRefreshDefaults.Indicator(
                    state = pullRefreshState,
                    isRefreshing = isRefreshing,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = listTopPadding)
                )
            }
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .hazeSource(state = hazeState),
                state = listState,
                contentPadding = PaddingValues(
                    top = listTopPadding,
                    bottom = chromeBottom
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // ─── Quick Picks ───
                if (sections.quickPicks.isNotEmpty()) {
                    item {
                        HomeRailTitle(
                            text = "Quick Picks",
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                        )
                    }
                    itemsIndexed(
                        sections.quickPicks,
                        key = { _, song -> "qp:${song.id}" }
                    ) { index, song ->
                        SongRow(
                            title = song.name,
                            artist = song.artist,
                            imageUrl = song.imageUrl,
                            songId = song.id,
                            onPlay = { _ ->
                                onPlaySong(QueueSource.HomeFeed(sections.quickPicks), index)
                            },
                            duration = formatDuration(song.durationSec),
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }

                // ─── Recently Played rail ───
                if (sections.recentlyPlayed.isNotEmpty()) {
                    item {
                        HomeSongRail(
                            title = "Recently played",
                            songs = sections.recentlyPlayed,
                            onPlay = { index ->
                                onPlaySong(QueueSource.HomeFeed(sections.recentlyPlayed), index)
                            }
                        )
                    }
                }

                // ─── Made for You rail ───
                if (sections.madeForYou.isNotEmpty()) {
                    item {
                        HomeSongRail(
                            title = "Made for You",
                            songs = sections.madeForYou,
                            onPlay = { index ->
                                onPlaySong(QueueSource.HomeFeed(sections.madeForYou), index)
                            }
                        )
                    }
                }

                // ─── Artists rail ───
                if (sections.artists.isNotEmpty()) {
                    item {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            HomeRailTitle(
                                text = "Artists you listen to",
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                            )
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 20.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(sections.artists, key = { it.id }) { artist ->
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

                // ─── Albums rail ───
                if (sections.albums.isNotEmpty()) {
                    item {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            HomeRailTitle(
                                text = "Albums for you",
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                            )
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 20.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(sections.albums, key = { it.id }) { album ->
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
        }

        HomeHeader(
            onHistory = onHistory,
            onSearch = onSearch,
            onSettings = onSettings,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .hazeEffect(
                    state = hazeState,
                    style = HazeStyle(
                        backgroundColor = MaterialTheme.colorScheme.background.copy(alpha = 0.85f),
                        tint = null,
                        blurRadius = 40.dp,
                        noiseFactor = 0f
                    )
                )
        )
    }
}

/** Rail title bar. */
@Composable
private fun HomeRailTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        modifier = modifier
    )
}

/** Horizontal rail of song tiles. */
@Composable
private fun HomeSongRail(
    title: String,
    songs: List<Song>,
    onPlay: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        HomeRailTitle(
            text = title,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(songs, key = { _, song -> song.id }) { index, song ->
                SongCard(
                    title = song.name,
                    artist = song.artist,
                    imageUrl = song.imageUrl,
                    songId = song.id,
                    onPlay = { _ -> onPlay(index) }
                )
            }
        }
    }
}

/** Formats seconds as m:ss; empty when zero. */
private fun formatDuration(totalSeconds: Long): String {
    if (totalSeconds <= 0L) return ""
    val minutes = totalSeconds / 60L
    val seconds = (totalSeconds % 60L).toString().padStart(2, '0')
    return "$minutes:$seconds"
}
