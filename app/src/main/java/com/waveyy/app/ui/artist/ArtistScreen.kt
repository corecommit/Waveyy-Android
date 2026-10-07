package com.waveyy.app.ui.artist

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.waveyy.app.domain.model.Song
import com.waveyy.app.ui.components.AdaptiveImage
import com.waveyy.app.ui.components.AlbumCard
import com.waveyy.app.ui.components.HeroPlayButton
import com.waveyy.app.ui.components.SongCard
import com.waveyy.app.ui.components.WaveyyProgress
import com.waveyy.app.util.HtmlDecoder
import com.waveyy.app.util.PlayCountFormatter

/**
 * Artist page: header (image, name, followers), latest release with hero
 * play, Popular podium, tracks, and albums. Ports `artist/[id]/page.js`
 * with its profile/featured/podium cards inlined.
 */
@Composable
fun ArtistScreen(
    viewModel: ArtistViewModel = hiltViewModel(),
    onPlay: (String) -> Unit = {},
    onAlbum: (String) -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    when (val current = state) {
        ArtistUiState.Loading, is ArtistUiState.Error -> {
            WaveyyProgress(progress = null, modifier = Modifier.padding(32.dp))
        }
        is ArtistUiState.Success -> ArtistContent(page = current.page, onPlay = onPlay, onAlbum = onAlbum)
    }
}

/** Loaded artist body. */
@Composable
private fun ArtistContent(page: ArtistPage, onPlay: (String) -> Unit, onAlbum: (String) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AdaptiveImage(
                    imageUrl = page.artist.imageUrl,
                    contentDescription = page.artist.name,
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                )
                Column(modifier = Modifier.padding(start = 16.dp)) {
                    Text(
                        text = HtmlDecoder.decode(page.artist.name),
                        style = MaterialTheme.typography.headlineMedium
                    )
                    if (page.artist.followerCount > 0L) {
                        Text(
                            text = PlayCountFormatter.format(page.artist.followerCount) + " Followers",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        page.latestRelease?.let { release ->
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text(text = "Latest Release", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = HtmlDecoder.decode(release.name),
                        style = MaterialTheme.typography.titleLarge
                    )
                    HeroPlayButton(songId = release.id, onPlay = onPlay)
                }
            }
        }
        if (page.podium.size >= 3) {
            item {
                Text(
                    text = "Popular",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                PodiumRow(songs = page.podium, onPlay = onPlay)
            }
        }
        if (page.tracks.isNotEmpty()) {
            item {
                Text(
                    text = "Tracks",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(page.tracks, key = { it.id }) { song ->
                        SongCard(
                            title = song.name,
                            artist = song.artist,
                            imageUrl = song.imageUrl,
                            songId = song.id,
                            onPlay = onPlay
                        )
                    }
                }
            }
        }
        if (page.albums.isNotEmpty()) {
            item {
                Text(
                    text = "Albums",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(page.albums, key = { it.id }) { album ->
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

/** Top-3 podium row (silver, gold, bronze order). */
@Composable
private fun PodiumRow(songs: List<Song>, onPlay: (String) -> Unit) {
    val ordered = listOf(2 to songs[1], 1 to songs[0], 3 to songs[2])
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        ordered.forEach { (rank, song) ->
            val size = if (rank == 1) 96.dp else 72.dp
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                AdaptiveImage(
                    imageUrl = song.imageUrl,
                    contentDescription = song.name,
                    modifier = Modifier
                        .size(size)
                        .clip(CircleShape)
                )
                Text(
                    text = "#$rank",
                    style = MaterialTheme.typography.labelSmall
                )
                SongCard(
                    title = song.name,
                    artist = PlayCountFormatter.format(song.playCount) + " streams",
                    imageUrl = "",
                    songId = song.id,
                    onPlay = onPlay
                )
            }
        }
    }
}
