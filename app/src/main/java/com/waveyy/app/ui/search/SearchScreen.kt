package com.waveyy.app.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowOutward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.waveyy.app.R
import com.waveyy.app.domain.model.Song
import com.waveyy.app.ui.components.AdaptiveImage
import com.waveyy.app.ui.components.AlbumCard
import com.waveyy.app.ui.components.ArtistCard
import com.waveyy.app.ui.components.WaveyyProgress
import com.waveyy.app.ui.player.sharedPlayerViewModel
import kotlinx.coroutines.launch

/** Result-section filter. */
enum class SearchFilter(val label: String) {
    ALL("All"),
    SONGS("Songs"),
    ALBUMS("Albums"),
    ARTISTS("Artists")
}

/**
 * Search: pill field with clear button, filter chips, vertical song rows
 * with overflow actions, artist rows with circular avatars, album rail,
 * and trending suggestions when blank. Ranking reuses [SearchViewModel].
 */
@Composable
fun SearchScreen(
    viewModel: SearchViewModel = hiltViewModel(),
    onPlaySong: (List<Song>, Int) -> Unit = { _, _ -> },
    onAlbum: (String) -> Unit = {},
    onArtist: (String) -> Unit = {},
    onSubmitQuery: (String) -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    val suggestions by viewModel.suggestions.collectAsState()
    val activeQuery by viewModel.query.collectAsState()
    var text by remember(activeQuery) { mutableStateOf(activeQuery) }
    var filter by remember { mutableStateOf(SearchFilter.ALL) }
    val showSongs = filter == SearchFilter.ALL || filter == SearchFilter.SONGS
    val showAlbums = filter == SearchFilter.ALL || filter == SearchFilter.ALBUMS
    val showArtists = filter == SearchFilter.ALL || filter == SearchFilter.ARTISTS

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                placeholder = { Text(stringResource(R.string.search_hint)) },
                leadingIcon = { Icon(imageVector = Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (text.isNotEmpty()) {
                        IconButton(onClick = { text = "" }) {
                            Icon(imageVector = Icons.Filled.Close, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(percent = 50),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { viewModel.submitQuery(text) })
            )
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(SearchFilter.entries.toList()) { entry ->
                    FilterChip(
                        selected = filter == entry,
                        onClick = { filter = entry },
                        label = { Text(entry.label) }
                    )
                }
            }
        }
        when (val current = state) {
            SearchUiState.Loading, is SearchUiState.Error -> {
                item {
                    if (text.isBlank() && suggestions.isNotEmpty()) {
                        SuggestionRows(suggestions = suggestions, onPick = {
                            text = it
                            onSubmitQuery(it)
                        })
                    } else {
                        WaveyyProgress(progress = null, modifier = Modifier.padding(32.dp))
                    }
                }
            }
            is SearchUiState.Success -> {
                if (current.songs.isEmpty() && current.albums.isEmpty() && current.artists.isEmpty()) {
                    if (text.isBlank() && suggestions.isNotEmpty()) {
                        item {
                            SuggestionRows(suggestions = suggestions, onPick = {
                                text = it
                                onSubmitQuery(it)
                            })
                        }
                    } else {
                        item {
                            Text(
                                text = stringResource(R.string.search_hint),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(32.dp)
                            )
                        }
                    }
                } else {
                    if (showSongs) {
                        itemsIndexed(current.songs, key = { _, song -> song.id }) { index, song ->
                            SongResultRow(
                                song = song,
                                songs = current.songs,
                                index = index,
                                onPlaySong = onPlaySong
                            )
                        }
                    }
                    if (showArtists && current.artists.isNotEmpty()) {
                        item {
                            SectionTitle("Artists")
                            Column {
                                current.artists.forEach { artist ->
                                    ArtistResultRow(
                                        name = artist.name,
                                        imageUrl = artist.imageUrl,
                                        onOpen = { onArtist(artist.id) }
                                    )
                                }
                            }
                        }
                    }
                    if (showAlbums && current.albums.isNotEmpty()) {
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
                }
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
                    imageVector = Icons.AutoMirrored.Filled.ArrowOutward,
                    contentDescription = null
                )
            }
        }
    }
}

/** Song result row with overflow actions (play, queue, share). */
@Composable
private fun SongResultRow(
    song: Song,
    songs: List<Song>,
    index: Int,
    onPlaySong: (List<Song>, Int) -> Unit
) {
    val player = sharedPlayerViewModel()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPlaySong(songs, index) }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AdaptiveImage(
            imageUrl = song.imageUrl,
            contentDescription = song.name,
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(12.dp))
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
        IconButton(onClick = { expanded = true }) {
            Icon(imageVector = Icons.Filled.MoreVert, contentDescription = "More")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("Play now") },
                onClick = {
                    expanded = false
                    onPlaySong(songs, index)
                }
            )
            DropdownMenuItem(
                text = { Text("Add to queue") },
                onClick = {
                    expanded = false
                    player.appendSong(song)
                }
            )
            DropdownMenuItem(
                text = { Text("Share") },
                onClick = {
                    expanded = false
                    scope.launch { shareText(context, song) }
                }
            )
        }
    }
}

/** Artist result row: 48.dp circular avatar + name. */
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
                .size(48.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
        )
        Text(
            text = name,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}

/** Section header. */
@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.headlineSmall,
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
