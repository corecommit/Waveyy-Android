package com.waveyy.app.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.waveyy.app.R
import com.waveyy.app.domain.model.Playlist
import com.waveyy.app.ui.MainViewModel
import com.waveyy.app.ui.sharedMainViewModel
import com.waveyy.app.ui.components.AdaptiveImage
import com.waveyy.app.ui.components.WaveyyButton
import com.waveyy.app.ui.components.SkeletonRows
import com.waveyy.app.ui.components.WaveyyInput
import com.waveyy.app.ui.player.rememberChromeBottomReserve
import kotlinx.coroutines.flow.filter

/**
 * Library: 2-column playlist grid with create flow, plus the
 * backend-missing card when `supabase/playlists.sql` was never applied.
 * Guests get local-only DataStore playlists.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistsScreen(
    viewModel: LibraryViewModel = hiltViewModel(),
    onOpen: (String) -> Unit,
    mainVm: MainViewModel = sharedMainViewModel()
) {
    val state by viewModel.state.collectAsState()
    val refreshing by viewModel.isRefreshing.collectAsState()
    var creating by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    val gridState = rememberLazyGridState()
    val chromeBottom = rememberChromeBottomReserve()
    LaunchedEffect(Unit) {
        mainVm.scrollToTop
            .filter { it == "playlists" }
            .collect {
                if (gridState.firstVisibleItemIndex > 0 || gridState.firstVisibleItemScrollOffset > 0) {
                    gridState.animateScrollToItem(0)
                }
            }
    }

    if (state.loading) {
        SkeletonRows(count = 6)
        return
    }

    val density = LocalDensity.current
    val indicatorTop = with(density) {
        WindowInsets.statusBars.getTop(density).toDp()
    } + 64.dp
    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = viewModel::refreshQuiet,
        modifier = Modifier.fillMaxSize(),
        indicator = {
            PullToRefreshDefaults.Indicator(
                state = it,
                isRefreshing = refreshing,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = indicatorTop)
            )
        }
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            state = gridState,
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 8.dp,
                bottom = chromeBottom
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(span = { GridItemSpan(2) }) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.library_title),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .clickable { creating = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = stringResource(R.string.playlist_create),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            if (state.backendMissing) {
                item(span = { GridItemSpan(2) }) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Text(
                            text = stringResource(R.string.playlist_backend_missing),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }

            items(state.playlists, key = { it.id }) { playlist ->
                PlaylistGridCard(
                    playlist = playlist,
                    isLiked = playlist.id == state.likedPlaylistId,
                    onClick = { onOpen(playlist.id) }
                )
            }

            if (creating) {
                item(span = { GridItemSpan(2) }) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            WaveyyInput(
                                value = name,
                                onValueChange = { name = it },
                                label = stringResource(R.string.playlist_name_hint),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                horizontalArrangement = Arrangement.End
                            ) {
                                WaveyyButton(
                                    text = stringResource(R.string.action_cancel),
                                    onClick = { creating = false }
                                )
                                WaveyyButton(
                                    text = stringResource(R.string.action_create),
                                    onClick = {
                                        viewModel.createPlaylist(name)
                                        name = ""
                                        creating = false
                                    },
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaylistGridCard(
    playlist: Playlist,
    isLiked: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            if (isLiked) {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = null,
                    tint = Color(0xFFE53E3E),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(48.dp)
                )
            } else if (playlist.songs.isNotEmpty()) {
                AdaptiveImage(
                    imageUrl = playlist.songs.first().imageUrl,
                    contentDescription = playlist.name,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.QueueMusic,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(48.dp)
                )
            }
        }
        Text(
            text = playlist.name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp)
        )
        Text(
            text = "${playlist.songs.size} ${if (playlist.songs.size == 1) "song" else "songs"}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
