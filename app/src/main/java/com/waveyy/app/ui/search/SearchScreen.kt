package com.waveyy.app.ui.search

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.waveyy.app.R
import com.waveyy.app.domain.model.Song
import com.waveyy.app.playback.QueueSource
import com.waveyy.app.ui.components.AdaptiveImage
import com.waveyy.app.ui.components.SkeletonRows
import com.waveyy.app.ui.player.sharedPlayerViewModel
import com.waveyy.app.ui.player.rememberChromeBottomReserve
import kotlinx.coroutines.launch

/** Result-section filter. */
enum class SearchFilter(val label: String) {
    ALL("All"),
    TRACKS("Tracks"),
    ALBUMS("Albums"),
    ARTISTS("Artists"),
    PLAYLISTS("Playlists")
}

/**
 * Search: pill field with clear button, filter chips, vertical song rows
 * with overflow actions, artist rows with circular avatars, album rail.
 * Loading shows skeleton rows (never a spinner, never stale results);
 * blank+focused shows history; blank+unfocused shows trending suggestions.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel = hiltViewModel(),
    onPlaySong: (QueueSource, Int) -> Unit = { _, _ -> },
    onAlbum: (String) -> Unit = {},
    onArtist: (String) -> Unit = {},
    onBack: () -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    val suggestions by viewModel.suggestions.collectAsState()
    val history by viewModel.searchHistory.collectAsState()
    val activeQuery by viewModel.query.collectAsState()
    var text by remember(activeQuery) { mutableStateOf(activeQuery) }
    LaunchedEffect(text) {
        if (text.isNotBlank() && text != activeQuery) {
            kotlinx.coroutines.delay(300)
            viewModel.submitQuery(text, addToHistory = false)
        }
    }
    var filter by remember { mutableStateOf(SearchFilter.ALL) }
    var focused by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
    val showSongs = filter == SearchFilter.ALL || filter == SearchFilter.TRACKS
    val showAlbums = filter == SearchFilter.ALL || filter == SearchFilter.ALBUMS
    val showArtists = filter == SearchFilter.ALL || filter == SearchFilter.ARTISTS
    val showPlaylists = filter == SearchFilter.ALL || filter == SearchFilter.PLAYLISTS
    val chromeBottom = rememberChromeBottomReserve()

    fun submit(query: String) {
        focusManager.clearFocus()
        viewModel.submitQuery(query, addToHistory = true)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Fixed header: search bar (with status-bar inset).
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .statusBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Back button as a circular icon button, outside the pill.
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Pill search field.
                OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester)
                    .onFocusChanged { focused = it.isFocused },
                placeholder = {
                    Text(
                        text = "Search songs, artists, albums",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (text.isNotEmpty()) {
                        IconButton(onClick = { text = "" }) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = "Clear",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(percent = 50),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { submit(text) })
                )
            }
        }

        val showHistory = text.isBlank() && focused && history.isNotEmpty()
        if (!showHistory) {
            // Fixed filter chips.
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                items(SearchFilter.entries.toList()) { entry ->
                    FilterChip(
                        selected = filter == entry,
                        onClick = { filter = entry },
                        label = { Text(entry.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }

        // Scrollable body.
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(bottom = chromeBottom),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (showHistory) {
                item {
                    HistoryList(
                        history = history,
                        onPick = { submit(it) },
                        onRemove = viewModel::removeHistoryEntry,
                        onClear = viewModel::clearHistory
                    )
                }
                item {
                    GenreChipsSection(
                        onPick = { genre -> submit(genre) }
                    )
                }
            } else {
                item {
                    Crossfade(
                        targetState = state is SearchUiState.Loading,
                        animationSpec = tween(200),
                        label = "searchCrossfade"
                    ) { loading ->
                        if (loading) {
                            SkeletonRows(count = 8)
                        } else {
                            ResultsBody(
                                state = state,
                                text = text,
                                showSongs = showSongs,
                                showAlbums = showAlbums,
                                showArtists = showArtists,
                                showPlaylists = showPlaylists,
                                suggestions = suggestions,
                                onPlaySong = onPlaySong,
                                onAlbum = onAlbum,
                                onArtist = onArtist,
                                onPickSuggestion = { submit(it) },
                                onRetry = viewModel::refresh
                            )
                        }
                }
                }
            }
        }
    }
}

/** Loaded results, empty hint, or error panel. */
@Composable
private fun ResultsBody(
    state: SearchUiState,
    text: String,
    showSongs: Boolean,
    showAlbums: Boolean,
    showArtists: Boolean,
    showPlaylists: Boolean,
    suggestions: List<String>,
    onPlaySong: (QueueSource, Int) -> Unit,
    onAlbum: (String) -> Unit,
    onArtist: (String) -> Unit,
    onPickSuggestion: (String) -> Unit,
    onRetry: () -> Unit
) {
    when (state) {
        SearchUiState.Loading -> SkeletonRows(count = 8)
        is SearchUiState.Error -> SearchError(onRetry = onRetry)
        is SearchUiState.Success -> {
            if (state.songs.isEmpty() && state.albums.isEmpty() && state.artists.isEmpty()) {
                if (text.isNotBlank()) {
                    EmptyResults(query = text)
                } else {
                    EmptySearchHint()
                }
            } else {
                Column {
                    if (showSongs && state.songs.isNotEmpty()) {
                        SectionTitle("Songs")
                        state.songs.forEachIndexed { index, song ->
                            SongResultRow(
                                song = song,
                                songs = state.songs,
                                index = index,
                                onPlaySong = onPlaySong
                            )
                        }
                    }
                    if (showArtists && state.artists.isNotEmpty()) {
                        SectionTitle("Artists")
                        Column {
                            state.artists.forEach { artist ->
                                ArtistResultRow(
                                    name = artist.name,
                                    imageUrl = artist.imageUrl,
                                    onOpen = { onArtist(artist.id) }
                                )
                            }
                        }
                    }
                    if (showAlbums && state.albums.isNotEmpty()) {
                        SectionTitle("Albums")
                        Column {
                            state.albums.forEach { album ->
                                AlbumResultRow(
                                    name = album.name,
                                    artist = album.artist,
                                    imageUrl = album.imageUrl,
                                    onOpen = { onAlbum(album.id) }
                                )
                            }
                        }
                    }
                    if (showPlaylists) {
                        SectionTitle("Playlists")
                        Text(
                            text = "Playlists coming soon",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    if (suggestions.isNotEmpty()) {
                        SectionTitle("Suggestions")
                        SuggestionRows(suggestions = suggestions, onPick = onPickSuggestion)
                    }
                }
            }
        }
    }
}

/** Centered empty state for queries with no results. */
@Composable
private fun EmptyResults(query: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.SearchOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(48.dp)
        )
        Text(
            text = "No results for '$query'",
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = "Try a different search term",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Centered hint for a blank query with no history. */
@Composable
private fun EmptySearchHint() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.History,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(48.dp)
        )
        Text(
            text = "Search for songs, artists, or albums",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Centered error state with a working retry. */
@Composable
private fun SearchError(onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.WifiOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(48.dp)
        )
        Text(
            text = "Couldn't load results",
            style = MaterialTheme.typography.titleMedium
        )
        TextButton(onClick = onRetry) {
            Text("Retry")
        }
    }
}

/** Recent-search history list with per-row remove and clear-all. */
@Composable
private fun HistoryList(
    history: List<String>,
    onPick: (String) -> Unit,
    onRemove: (String) -> Unit,
    onClear: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "Recent searches",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        TextButton(onClick = onClear) {
            Text("Clear all")
        }
    }
    history.forEach { entry ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onPick(entry) }
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.History,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = entry,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            IconButton(onClick = { onRemove(entry) }) {
                Icon(imageVector = Icons.Outlined.Close, contentDescription = "Remove")
            }
        }
    }
}

/** Trending suggestion rows with outward-arrow affordance. */
@Composable
private fun SuggestionRows(suggestions: List<String>, onPick: (String) -> Unit) {
    Column {
        suggestions.forEach { suggestion ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPick(suggestion) }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = suggestion,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null
                )
            }
        }
    }
}

/** Song result row with overflow actions (play, queue, share). */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SongResultRow(
    song: Song,
    songs: List<Song>,
    index: Int,
    onPlaySong: (QueueSource, Int) -> Unit
) {
    val player = sharedPlayerViewModel()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { onPlaySong(QueueSource.SearchResults(songs), index) },
                onLongClick = { expanded = true }
            )
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
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
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
        IconButton(onClick = { expanded = true }) {
            Icon(imageVector = Icons.Filled.MoreVert, contentDescription = "More")
        }
        androidx.compose.material3.DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            androidx.compose.material3.DropdownMenuItem(
                text = { Text("Play now") },
                onClick = {
                    expanded = false
                    onPlaySong(QueueSource.SearchResults(songs), index)
                }
            )
            androidx.compose.material3.DropdownMenuItem(
                text = { Text("Add to queue") },
                onClick = {
                    expanded = false
                    player.appendSong(song)
                }
            )
            androidx.compose.material3.DropdownMenuItem(
                text = { Text("Share") },
                onClick = {
                    expanded = false
                    scope.launch { shareText(context, song) }
                }
            )
        }
    }
}

/** Artist result row: 56.dp circular avatar + name + subtitle. */
@Composable
private fun ArtistResultRow(name: String, imageUrl: String, onOpen: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AdaptiveImage(
            imageUrl = imageUrl,
            contentDescription = name,
            modifier = Modifier
                .size(56.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
        )
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Artist",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** Album result row: same shape as the song row, album art leading. */
@Composable
private fun AlbumResultRow(
    name: String,
    artist: String,
    imageUrl: String,
    onOpen: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AdaptiveImage(
            imageUrl = imageUrl,
            contentDescription = name,
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
                text = name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = artist,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** Section header. */
@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp)
    )
}

/** Formats seconds as m:ss. */
private fun formatDuration(totalSeconds: Long): String {
    if (totalSeconds <= 0L) return "--:--"
    val minutes = totalSeconds / 60L
    val seconds = (totalSeconds % 60L).toString().padStart(2, '0')
    return "$minutes:$seconds"
}

/** Shares a song link via the system sheet. */
private fun shareText(context: android.content.Context, song: Song) {
    val send = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(android.content.Intent.EXTRA_TEXT, "${song.name} by ${song.artist}\nwaveyy://song/${song.id}")
    }
    context.startActivity(android.content.Intent.createChooser(send, null))
}

/** Genre/mood chips for the blank-query discovery section. */
@Composable
private fun GenreChipsSection(onPick: (String) -> Unit) {
    val genres = listOf(
        "Pop", "Rock", "Hip-Hop", "Lo-Fi",
        "Electronic", "Indie", "R&B", "Bollywood",
        "Jazz", "Metal", "Acoustic", "Chill",
        "Anime", "Classical", "Synthwave"
    )

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Explore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "Explore genres & moods",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        // Flow layout: wrap chips across multiple rows.
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            genres.forEach { genre ->
                FilterChip(
                    selected = false,
                    onClick = { onPick(genre) },
                    label = { Text(genre) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        labelColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        }
    }
}
