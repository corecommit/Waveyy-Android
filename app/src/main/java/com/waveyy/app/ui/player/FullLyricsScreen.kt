package com.waveyy.app.ui.player

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.waveyy.app.ui.theme.rememberAlbumAccent
import kotlinx.coroutines.launch

/**
 * Full-screen lyrics: accent gradient, top bar, auto-scrolling line list.
 * Tapping a line seeks when this song is the one playing.
 *
 * @param songId song to show lyrics for (resolved from the player queue).
 * @param viewModel player logic (shared instance).
 * @param onClose back handler.
 */
@Composable
fun FullLyricsScreen(
    songId: String,
    viewModel: PlayerViewModel = sharedPlayerViewModel(),
    onClose: () -> Unit = {}
) {
    val connection = viewModel.connection
    val state by connection.state.collectAsState()
    val queue by viewModel.queue.collectAsState()
    val lyrics by viewModel.lyrics.collectAsState()
    val scope = rememberCoroutineScope()
    val song = queue.firstOrNull { it.id == songId }
    LaunchedEffect(song?.id) { song?.let { viewModel.loadLyrics(it) } }
    val lines = (lyrics as? LyricsUiState.Ready)?.lines.orEmpty()
    val idx = activeLyricIndex(lyrics, state.positionMs).coerceAtLeast(0)
    val listState = rememberLazyListState()
    LaunchedEffect(idx) {
        if (lines.isNotEmpty()) listState.animateScrollToItem(idx)
    }
    val accent = rememberAlbumAccent(song?.imageUrl, song?.id)
    val infiniteTransition = rememberInfiniteTransition(label = "lyricsBg")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "lyricsBgProgress"
    )
    // Diagonal gradient whose endpoints drift over time so the color bands
    // appear to slide from top-left to bottom-right and back.
    val driftY = progress * 800f
    val driftX = progress * 400f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        accent.copy(alpha = 0.85f),
                        accent.copy(alpha = 0.35f),
                        MaterialTheme.colorScheme.background,
                        accent.copy(alpha = 0.25f)
                    ),
                    start = Offset(-200f + driftX, -400f + driftY),
                    end = Offset(1400f, 2200f + driftY)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Text(
                    text = song?.name ?: "Lyrics",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { song?.let(viewModel::shareSong) }) {
                    Icon(
                        imageVector = Icons.Filled.Share,
                        contentDescription = "Share",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
            when {
                song == null -> Text(
                    text = "Song unavailable",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp)
                )
                lyrics is LyricsUiState.Loading -> Text(
                    text = "Loading…",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp)
                )
                lines.isEmpty() -> Text(
                    text = when (lyrics) {
                        LyricsUiState.Instrumental -> "Instrumental"
                        is LyricsUiState.Plain -> (lyrics as LyricsUiState.Plain).text
                        else -> "Lyrics not available"
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp)
                )
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = listState
                ) {
                    itemsIndexed(lines) { i, line ->
                        val isCurrent = i == idx
                        Text(
                            text = line.text,
                            style = if (isCurrent) {
                                MaterialTheme.typography.headlineMedium
                            } else {
                                MaterialTheme.typography.titleMedium
                            },
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                            color = Color.White.copy(alpha = if (isCurrent) 1f else 0.4f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (songId == state.mediaId) {
                                        scope.launch { connection.seekTo(line.timeMs) }
                                    }
                                }
                                .padding(horizontal = 24.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}
