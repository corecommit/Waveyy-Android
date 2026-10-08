package com.waveyy.app.ui.debug

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.waveyy.app.data.remote.ytmusic.InnerTubeClient
import com.waveyy.app.data.repository.YtMusicRepository
import com.waveyy.app.domain.model.AudioStream
import com.waveyy.app.domain.model.MusicSource
import com.waveyy.app.domain.model.Song
import com.waveyy.app.ui.player.sharedPlayerViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject

/** Probe state holder (kept outside composition for testability). */
@HiltViewModel
class YtMusicProbeViewModel @Inject constructor(
    val transport: InnerTubeClient,
    val repository: YtMusicRepository
) : androidx.lifecycle.ViewModel() {
    /** Shared pretty printer. */
    val pretty = Json { prettyPrint = true }
}

/**
 * Manual InnerTube probe screen (debug builds only): runs a search, shows
 * the raw response JSON, lists parsed tracks, resolves a stream URL on tap
 * (URL redacted to host/length only), and plays through the shared Media3
 * queue. This is how Part A gets verified on-device.
 */
@Composable
fun YtMusicDebugScreen(
    probe: YtMusicProbeViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val player = sharedPlayerViewModel()
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("runaway aurora") }
    var rawJson by remember { mutableStateOf("") }
    var tracks by remember { mutableStateOf(emptyList<TrackRow>()) }
    var status by remember { mutableStateOf("idle") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text = "YT Music probe",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(16.dp)
            )
        }
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Query") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )
        }
        item {
            TextButton(onClick = {
                scope.launch {
                    status = "searching…"
                    rawJson = ""
                    tracks = emptyList()
                    val payload = buildJsonObject { put("query", JsonPrimitive(query)) }
                    val response = probe.transport.post("search", payload, visitorData = null)
                    rawJson = response?.let { probe.pretty.encodeToString(JsonObject.serializer(), it) }
                        .orEmpty().take(4000)
                    val found = probe.repository.searchSongs(query).first()
                    tracks = found.filter { it.source == MusicSource.YTMUSIC }
                        .map { TrackRow(it.id.removePrefix("yt:"), it.name, it.artist) }
                    status = "done: ${tracks.size} tracks"
                }
            }) {
                Text("Run search")
            }
            Text(text = status, style = MaterialTheme.typography.bodySmall)
        }
        if (rawJson.isNotBlank()) {
            item {
                Text(
                    text = rawJson,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
            }
        }
        items(tracks, key = { it.videoId }) { track ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        scope.launch {
                            val url = probe.repository.streamUrl(track.videoId)
                            status = if (url != null) {
                                val host = runCatching {
                                    java.net.URI(url).host
                                }.getOrNull() ?: "?"
                                val song = Song(
                                    id = "yt:${track.videoId}",
                                    name = track.title,
                                    artist = track.artist,
                                    album = "unknown",
                                    imageUrl = "",
                                    durationSec = 0L,
                                    playCount = 0L,
                                    streams = listOf(
                                        AudioStream(
                                            url = url,
                                            quality = "",
                                            bitrateKbps = 0,
                                            lossless = false
                                        )
                                    ),
                                    source = MusicSource.YTMUSIC
                                )
                                player.playSongs(listOf(song), 0)
                                "playing ${track.title} via $host (${url.length} chars)"
                            } else {
                                "no stream (gated?)"
                            }
                        }
                    }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(text = track.title, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = "${track.artist} • ${track.videoId}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** Parsed track row for the probe list. */
data class TrackRow(val videoId: String, val title: String, val artist: String)
