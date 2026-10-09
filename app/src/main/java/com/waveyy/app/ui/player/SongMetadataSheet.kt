package com.waveyy.app.ui.player

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.waveyy.app.domain.model.MusicSource
import com.waveyy.app.domain.model.Song
import com.waveyy.app.util.PlayCountFormatter

/**
 * Full metadata viewer for the current song. Mirrors the YouTube Music /
 * SimpMusic info panel: centered section header + value, stacked vertically,
 * scrollable. Values not present on the domain model are shown as "Unknown"
 * so the panel is honest about what we don't have.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongMetadataSheet(
    song: Song,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(16.dp))

            MetadataSection("Title", song.name)
            MetadataSection("Artists", song.artist)
            MetadataSection("Album", song.album)
            MetadataSection("Duration", formatSecs(song.durationSec))
            song.year.takeIf { it.isNotBlank() }?.let {
                MetadataSection("Year", it)
            }

            MetadataSection(
                "Source",
                when (song.source) {
                    MusicSource.YTMUSIC -> "YouTube Music"
                    MusicSource.JIOSAAVN -> "JioSaavn"
                }
            )

            // Stream-derived fields: use the best stream.
            val stream = song.streams.maxByOrNull { it.bitrateKbps }
            MetadataSection(
                "Bitrate",
                stream?.bitrateKbps?.takeIf { it > 0 }?.let { "$it kbps" } ?: "Unknown"
            )
            MetadataSection("Codec", codecOf(stream?.url))
            MetadataSection("Mime Type", mimeOf(stream?.url))
            MetadataSection(
                "Stream Quality",
                stream?.quality?.takeIf { it.isNotBlank() } ?: "Unknown"
            )
            MetadataSection(
                "Lossless",
                if (stream?.lossless == true) "Yes" else "No"
            )

            // Fields we don't carry in the model — shown as Unknown so the
            // panel is complete-looking, matching the reference design.
            MetadataSection("BPM", "Unknown")
            MetadataSection("Key", "Unknown")
            MetadataSection("Scale", "Unknown")
            MetadataSection("Extract Source", extractSourceOf(stream?.url))

            MetadataSection("Play / View Count", PlayCountFormatter.format(song.playCount))
            MetadataSection("ID", song.id)

            if (song.source == MusicSource.YTMUSIC) {
                val videoId = song.id.removePrefix("yt:")
                MetadataSection(
                    "YouTube URL",
                    "https://music.youtube.com/watch?v=$videoId"
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

/** Bold centered header + muted centered value, with a gap between sections. */
@Composable
private fun MetadataSection(label: String, value: String) {
    Spacer(Modifier.height(14.dp))
    Text(
        text = label,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(4.dp))
    Text(
        text = value,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        maxLines = 4,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.fillMaxWidth()
    )
}

/** Formats seconds as m:ss. */
private fun formatSecs(totalSeconds: Long): String {
    if (totalSeconds <= 0L) return "Unknown"
    val minutes = totalSeconds / 60L
    val seconds = (totalSeconds % 60L).toString().padStart(2, '0')
    return "$minutes:$seconds"
}

/** Best-effort codec guess from the stream URL extension. */
private fun codecOf(url: String?): String {
    if (url.isNullOrBlank()) return "Unknown"
    val path = url.substringBefore('?').lowercase()
    return when {
        path.endsWith(".m4a") -> "AAC"
        path.endsWith(".mp3") -> "MP3"
        path.endsWith(".opus") -> "Opus"
        path.endsWith(".webm") -> "Opus (WebM)"
        path.endsWith(".flac") -> "FLAC"
        path.endsWith(".wav") -> "PCM"
        path.contains(".m4a") -> "AAC"
        path.contains(".mp3") -> "MP3"
        else -> "Unknown"
    }
}

/** Best-effort mime type from the stream URL extension. */
private fun mimeOf(url: String?): String {
    if (url.isNullOrBlank()) return "Unknown"
    val path = url.substringBefore('?').lowercase()
    return when {
        path.endsWith(".m4a") || path.contains(".m4a") -> "audio/mp4"
        path.endsWith(".mp3") || path.contains(".mp3") -> "audio/mpeg"
        path.endsWith(".opus") || path.contains(".opus") -> "audio/opus"
        path.endsWith(".webm") || path.contains(".webm") -> "audio/webm"
        path.endsWith(".flac") || path.contains(".flac") -> "audio/flac"
        else -> "Unknown"
    }
}

/** Host of the stream URL, or "Unknown". */
private fun extractSourceOf(url: String?): String {
    if (url.isNullOrBlank()) return "Unknown"
    return runCatching {
        java.net.URI(url).host ?: "Unknown"
    }.getOrDefault("Unknown")
}
