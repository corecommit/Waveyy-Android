package com.waveyy.app.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.waveyy.app.R
import com.waveyy.app.ui.components.WaveyyButton
import com.waveyy.app.ui.components.SkeletonRows
import com.waveyy.app.ui.components.WaveyyInput

/**
 * Library: playlist grid with create/delete, plus the backend-missing card
 * when `supabase/playlists.sql` was never applied.
 */
@Composable
fun PlaylistsScreen(
    viewModel: LibraryViewModel = hiltViewModel(),
    onOpen: (String) -> Unit
) {
    val state by viewModel.state.collectAsState()
    var creating by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }

    if (state.loading) {
        SkeletonRows(count = 6, modifier = Modifier.padding(16.dp))
        return
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.library_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = { creating = true }) {
                    Icon(imageVector = Icons.Filled.Add, contentDescription = stringResource(R.string.playlist_create))
                }
            }
        }
        if (state.backendMissing) {
            item {
                Card(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text(
                        text = stringResource(R.string.playlist_backend_missing),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
        if (state.userId == null && !state.backendMissing) {
            item {
                Text(
                    text = stringResource(R.string.library_guest),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
        items(state.playlists, key = { it.id }) { playlist ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpen(playlist.id) }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (playlist.id == state.likedPlaylistId) {
                    Icon(
                        imageVector = Icons.Filled.Favorite,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = playlist.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "${playlist.songs.size} songs",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (playlist.id != state.likedPlaylistId) {
                    IconButton(onClick = { viewModel.deletePlaylist(playlist.id) }) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = stringResource(R.string.playlist_delete)
                        )
                    }
                }
            }
        }
        if (creating) {
            item {
            Card(modifier = Modifier.padding(horizontal = 16.dp)) {
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
