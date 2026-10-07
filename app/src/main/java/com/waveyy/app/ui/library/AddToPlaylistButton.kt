package com.waveyy.app.ui.library

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.waveyy.app.domain.model.Song

/**
 * Playlist picker menu. Ports
 * `components/playlists/add-to-playlist-button.jsx`.
 *
 * @param song song to add.
 * @param modifier layout modifier.
 * @param viewModel library logic (destination-scoped).
 */
@Composable
fun AddToPlaylistButton(
    song: Song,
    modifier: Modifier = Modifier,
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var expanded by remember { mutableStateOf(false) }
    IconButton(onClick = { expanded = true }, modifier = modifier) {
        Icon(imageVector = Icons.Filled.List, contentDescription = "Add to playlist")
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            state.playlists.forEach { playlist ->
                DropdownMenuItem(
                    text = { Text(playlist.name) },
                    onClick = {
                        expanded = false
                        viewModel.addSong(playlist.id, song)
                    }
                )
            }
        }
    }
}
