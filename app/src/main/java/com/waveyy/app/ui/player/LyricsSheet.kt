package com.waveyy.app.ui.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.waveyy.app.BuildConfig
import com.waveyy.app.domain.model.LyricLine
import com.waveyy.app.domain.model.Song
import com.waveyy.app.playback.PlaybackConnection
import com.waveyy.app.ui.components.SkeletonLyrics
import com.waveyy.app.util.Hinglish

/**
 * Lyrics sheet: raced lyrics with synced-line highlight and a Hinglish
 * romanization toggle. Ports `components/player/sidebar-player.jsx`.
 *
 * @param viewModel player logic (lyrics + toggle state).
 * @param connection playback connection (position for highlight).
 * @param song song to show lyrics for.
 */
@Composable
fun LyricsSheet(
    viewModel: PlayerViewModel,
    connection: PlaybackConnection,
    song: Song?,
    modifier: Modifier = Modifier
) {
    val lyrics by viewModel.lyrics.collectAsState()
    val hinglish by viewModel.hinglishEnabled.collectAsState()
    val state by connection.state.collectAsState()
    val winner by viewModel.raceWinner.collectAsState()
    val debug by viewModel.lyricsDebug.collectAsStateWithLifecycle()

    LaunchedEffect(song?.id) {
        if (song != null) viewModel.loadLyrics(song)
    }

    when (val current = lyrics) {
        LyricsUiState.Empty -> {
            Column(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.MusicNote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    text = "Lyrics not available",
                    style = MaterialTheme.typography.bodyLarge
                )
                TextButton(onClick = { song?.let { viewModel.loadLyrics(it, bypassCache = true) } }) {
                    Text("Retry")
                }
            }
        }
        LyricsUiState.Loading -> {
            SkeletonLyrics(modifier = modifier.padding(vertical = 24.dp))
        }
        LyricsUiState.Instrumental -> {
            Column(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.MusicOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(48.dp)
                )
                Text(text = "Instrumental", style = MaterialTheme.typography.bodyLarge)
            }
        }
        is LyricsUiState.Plain -> {
            Column(modifier = modifier.fillMaxSize()) {
                WinnerCaption(winner = winner?.provider to winner?.score, debug = debug)
                Text(
                    text = if (hinglish) Hinglish.toHinglish(current.text) else current.text,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                )
            }
        }
        is LyricsUiState.Ready -> Column(modifier = modifier.fillMaxSize()) {
            WinnerCaption(winner = winner?.provider to winner?.score, debug = debug)
            LyricsLines(
                lines = current.lines,
                positionMs = state.positionMs,
                hinglish = hinglish,
                onToggleHinglish = viewModel::toggleHinglish,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/** Debug-only race-winner caption (provider + score). */
@Composable
private fun WinnerCaption(
    winner: Pair<String?, Double?>,
    debug: Boolean
) {
    if (!BuildConfig.DEBUG || !debug) return
    val (provider, score) = winner
    if (provider == null) return
    Text(
        text = "via $provider (score=${"%.1f".format(score ?: 0.0)})",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        textAlign = TextAlign.Center
    )
}

/** Scrollable synced lines with the active line highlighted. */
@Composable
private fun LyricsLines(
    lines: List<LyricLine>,
    positionMs: Long,
    hinglish: Boolean,
    onToggleHinglish: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val active = lines.indexOfLast { it.timeMs <= positionMs }.coerceAtLeast(0)
    LaunchedEffect(active) {
        if (lines.isNotEmpty()) listState.animateScrollToItem(active)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "Lyrics", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.weight(1f))
        Text(text = "Hinglish", style = MaterialTheme.typography.titleMedium)
        Switch(
            checked = hinglish,
            onCheckedChange = { onToggleHinglish() },
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                checkedBorderColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                uncheckedBorderColor = MaterialTheme.colorScheme.outline
            )
        )
    }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = listState,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(lines.size) { index ->
            val line = lines[index]
            val text = if (hinglish) Hinglish.toHinglish(line.text) else line.text
            Text(
                text = text,
                style = if (index == active) {
                    MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                } else {
                    MaterialTheme.typography.bodyLarge
                },
                color = if (index == active) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )
        }
    }
}
