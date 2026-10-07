package com.waveyy.app.ui.library

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.waveyy.app.domain.model.Song
import com.waveyy.app.ui.player.sharedPlayerViewModel

/**
 * Enqueues a song at the end of the shared queue. Ports
 * `components/playlists/add-to-queue-button.jsx`.
 *
 * @param song song to enqueue.
 * @param modifier layout modifier.
 */
@Composable
fun AddToQueueButton(
    song: Song,
    modifier: Modifier = Modifier
) {
    val player = sharedPlayerViewModel()
    IconButton(onClick = { player.appendSong(song) }, modifier = modifier) {
        Icon(imageVector = Icons.Filled.QueueMusic, contentDescription = "Add to queue")
    }
}
