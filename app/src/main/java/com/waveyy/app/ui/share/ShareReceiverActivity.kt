package com.waveyy.app.ui.share

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.waveyy.app.data.repository.SongRepository
import com.waveyy.app.playback.PlaybackConnection
import com.waveyy.app.ui.navigation.DeepLinks
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Invisible receiver for ACTION_SEND text shares. When another app shares
 * a `waveyy://song/{id}` link, resolves the song (using the shared
 * "Title by Artist" line as a search hint) and plays it next in the
 * current queue. Finishes immediately — no UI.
 */
@AndroidEntryPoint
class ShareReceiverActivity : ComponentActivity() {

    @Inject lateinit var songRepository: SongRepository
    @Inject lateinit var playbackConnection: PlaybackConnection

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val text = intent?.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
        if (text.isBlank()) {
            finish()
            return
        }
        val uri = DeepLinks.extractWaveyyUri(text)
        val songId = DeepLinks.songIdFrom(uri)
        if (songId.isNullOrBlank()) {
            Toast.makeText(this, "Not a Waveyy song link", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        val hint = DeepLinks.titleHintFrom(text)
        lifecycleScope.launch {
            val song = resolveSong(songId, hint)
            if (song == null) {
                Toast.makeText(this@ShareReceiverActivity, "Could not resolve song", Toast.LENGTH_SHORT).show()
            } else {
                playbackConnection.playNextSong(song)
                Toast.makeText(
                    this@ShareReceiverActivity,
                    "Playing next: ${song.name}",
                    Toast.LENGTH_SHORT
                ).show()
            }
            finish()
        }
    }

    /**
     * Resolves a song by id. The direct deploy is dead, so this uses the
     * search-by-title path: search for the shared title hint, then match
     * the exact id from the URL.
     */
    private suspend fun resolveSong(songId: String, hint: String?): com.waveyy.app.domain.model.Song? {
        if (hint.isNullOrBlank()) return null
        val results = runCatching {
            songRepository.searchSongs(hint).first()
        }.getOrDefault(emptyList())
        return results.firstOrNull { it.id == songId }
            ?: results.firstOrNull { it.streams.any { s -> s.url.isNotBlank() } }
    }
}
