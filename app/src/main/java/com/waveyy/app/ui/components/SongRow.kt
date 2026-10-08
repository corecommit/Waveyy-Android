package com.waveyy.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.waveyy.app.ui.theme.ThemePreviews
import com.waveyy.app.ui.theme.WaveyyTheme

/**
 * Song list row: 56.dp artwork, title, artist, duration. Flat (no fill),
 * for vertical lists (search results, queue, details). Ports the row half
 * of `components/cards/song.jsx`.
 *
 * @param title song title.
 * @param artist artist label.
 * @param imageUrl artwork URL.
 * @param songId song id passed to [onPlay].
 * @param onPlay play handler.
 * @param modifier layout modifier.
 * @param duration formatted duration (`m:ss`), blank hides it.
 * @param description replaces the artist line when present (e.g. status).
 */
@Composable
fun SongRow(
    title: String,
    artist: String,
    imageUrl: String,
    songId: String,
    onPlay: (String) -> Unit,
    modifier: Modifier = Modifier,
    duration: String = "",
    description: String? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onPlay(songId) }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AdaptiveImage(
            imageUrl = imageUrl,
            contentDescription = title,
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(12.dp))
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = description ?: artist,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (duration.isNotBlank()) {
            Text(
                text = duration,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Preview of [SongRow]. */
@ThemePreviews
@Composable
private fun SongRowPreview() {
    WaveyyTheme {
        SongRow(
            title = "Midnight Waves",
            artist = "Ocean Band",
            imageUrl = "",
            songId = "s1",
            onPlay = {},
            duration = "3:24"
        )
    }
}
