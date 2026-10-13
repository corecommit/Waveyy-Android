package com.waveyy.app.ui.player

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.waveyy.app.domain.model.Song
import com.waveyy.app.ui.components.AdaptiveImage
import com.waveyy.app.util.PlayCountFormatter

/**
 * Index of the currently-highlighted line, or -1 when nothing should be
 * highlighted:
 *   - before the first line's timestamp (intro),
 *   - more than [LAST_LINE_FADE_MS] past the last line's timestamp
 *     (outro / instrumental ending),
 *   - lyrics aren't synced or the list is empty.
 */
fun activeLyricIndex(lyrics: LyricsUiState, positionMs: Long): Int {
    val lines = (lyrics as? LyricsUiState.Ready)?.lines ?: return -1
    if (lines.isEmpty()) return -1
    if (positionMs < lines.first().timeMs) return -1
    val fadeEnd = lines.last().timeMs + LAST_LINE_FADE_MS
    if (positionMs > fadeEnd) return -1
    return lines.indexOfLast { it.timeMs <= positionMs }
}

/** How long the last line stays highlighted after its timestamp. */
private const val LAST_LINE_FADE_MS = 8_000L

/**
 * Compact lyrics card for the scrollable full player: header (title,
 * share, Show-all), a window of lines around the current one, and a
 * provider caption. Loads lyrics for [song] on entry.
 */
@Composable
fun LyricsCard(
    viewModel: PlayerViewModel,
    song: Song,
    lyrics: LyricsUiState,
    winnerProvider: String?,
    positionMs: Long,
    accent: Color,
    onShare: () -> Unit,
    onShowFull: () -> Unit,
    onPositioned: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(song.id) { viewModel.loadLyrics(song) }
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .onGloballyPositioned { onPositioned(it.boundsInParent().top.toInt()) },
        shape = RoundedCornerShape(24.dp),
        color = accent.copy(alpha = 0.9f)
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Lyrics",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onShare) {
                    Icon(
                        imageVector = Icons.Filled.Share,
                        contentDescription = "Share",
                        tint = Color.White
                    )
                }
                TextButton(onClick = onShowFull) {
                    Text(text = "Show", color = Color.White)
                }
            }

            Spacer(Modifier.height(16.dp))

            when (val current = lyrics) {
                LyricsUiState.Empty -> Text(
                    text = "Lyrics not available",
                    color = Color.White.copy(alpha = 0.7f)
                )
                LyricsUiState.Loading -> Text(
                    text = "Loading…",
                    color = Color.White.copy(alpha = 0.7f)
                )
                LyricsUiState.Instrumental -> Text(
                    text = "Instrumental",
                    color = Color.White.copy(alpha = 0.7f)
                )
                is LyricsUiState.Plain -> Text(
                    text = current.text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.9f),
                    maxLines = 8,
                    overflow = TextOverflow.Ellipsis
                )
                is LyricsUiState.Ready -> {
                    val lines = current.lines
                    // activeIdx drives the "is this the current line?"
                    // highlighting. -1 means nothing is current (intro
                    // or outro).
                    val activeIdx = activeLyricIndex(current, positionMs)
                    // windowCenter drives which window of lines is
                    // visible. During the intro show the first few; during
                    // the outro show the last few; otherwise show around
                    // the active line.
                    val windowCenter = when {
                        activeIdx >= 0 -> activeIdx
                        lines.isEmpty() -> 0
                        positionMs < lines.first().timeMs -> 0
                        else -> lines.lastIndex
                    }
                    val start = (windowCenter - 3).coerceAtLeast(0)
                    val end = (windowCenter + 4).coerceAtMost(lines.size)
                    Column {
                        lines.subList(start, end).forEachIndexed { i, line ->
                            val lineIdx = start + i
                            val distance = kotlin.math.abs(lineIdx - activeIdx)
                            val isCurrent = distance == 0
                            // Gentler falloff than the full screen since the card is small.
                            val blurRadius = (distance * 0.4f).coerceAtMost(2f)
                            val alpha = (1f - distance * 0.15f).coerceIn(0.35f, 1f)
                            val scale = (1f - distance * 0.02f).coerceIn(0.92f, 1f)

                            val animatedAlpha by animateFloatAsState(
                                targetValue = alpha,
                                animationSpec = tween(400),
                                label = "cardAlpha$i"
                            )
                            val animatedScale by animateFloatAsState(
                                targetValue = scale,
                                animationSpec = tween(400),
                                label = "cardScale$i"
                            )

                            Text(
                                text = line.text,
                                style = if (isCurrent) {
                                    MaterialTheme.typography.headlineSmall
                                } else {
                                    MaterialTheme.typography.bodyLarge
                                },
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                color = Color.White.copy(alpha = animatedAlpha),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .graphicsLayer {
                                        scaleX = animatedScale
                                        scaleY = animatedScale
                                    }
                                    .blur(radius = blurRadius.dp)
                                    .padding(vertical = 6.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "Line Synced",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                        Text(
                            text = "Lyrics provided by ${(winnerProvider ?: "lrclib").uppercase()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Artist banner card: artwork, name, subscriber count. Hidden when
 * [artistId] is blank or the artist fails to load.
 */
@Composable
fun ArtistCard(
    viewModel: PlayerViewModel,
    artistId: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (artistId.isBlank()) return
    val artist by remember(artistId) { viewModel.artistFlow(artistId) }
        .collectAsState(initial = null)
    val data = artist ?: return
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column {
            AdaptiveImage(
                imageUrl = data.imageUrl,
                contentDescription = data.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.3f),
                contentScale = ContentScale.Crop
            )
            Column(Modifier.padding(16.dp)) {
                Text(
                    text = data.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val followers = PlayCountFormatter.format(data.followerCount)
                if (followers.isNotEmpty()) {
                    Text(
                        text = "$followers subscribers",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Track metadata card: year, play count, album, with a "More" expander
 * for identifiers. Only shows fields the model actually carries.
 */
@Composable
fun SongMetadataCard(
    song: Song,
    modifier: Modifier = Modifier
) {
    var expanded by remember(song.id) { mutableStateOf(false) }
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                text = "About",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(12.dp))
            val playCount = PlayCountFormatter.format(song.playCount)
            if (playCount.isNotEmpty()) {
                Text(
                    text = playCount,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "plays" + (song.year.takeIf { it.isNotBlank() }?.let { " • $it" } ?: ""),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Start
                )
                Spacer(Modifier.height(8.dp))
            }
            Text(
                text = song.album,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            TextButton(onClick = { expanded = !expanded }) {
                Text(text = if (expanded) "Less" else "More")
            }
            if (expanded) {
                Text(
                    text = "ID: ${song.id}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Source: ${song.source.name}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
