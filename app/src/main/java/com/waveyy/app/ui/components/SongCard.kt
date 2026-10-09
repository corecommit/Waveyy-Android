package com.waveyy.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.waveyy.app.ui.theme.ThemePreviews
import com.waveyy.app.ui.theme.WaveyyTheme

/**
 * Song rail tile: 140.dp artwork with `RoundedCornerShape(16.dp)`, title
 * (`titleMedium`, 2 lines), artist (`bodySmall`, muted). Flat — no fill,
 * no shadow — matching the SimpMusic tile style. Ports the rail half of
 * `components/cards/song.jsx`.
 *
 * @param title song title.
 * @param artist artist label.
 * @param imageUrl artwork URL.
 * @param songId song id passed to [onPlay].
 * @param onPlay play handler.
 * @param modifier layout modifier.
 */
@Composable
fun SongCard(
    title: String,
    artist: String,
    imageUrl: String,
    songId: String,
    onPlay: (String) -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null
) {
    Column(
        modifier = modifier
            .width(140.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onPlay(songId) }
    ) {
        AdaptiveImage(
            imageUrl = imageUrl,
            contentDescription = title,
            modifier = Modifier
                .size(140.dp)
                .clip(RoundedCornerShape(16.dp))
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp)
        )
        Text(
            text = description ?: artist,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Preview of [SongCard]. */
@ThemePreviews
@Composable
private fun SongCardPreview() {
    WaveyyTheme {
        SongCard(
            title = "Midnight Waves",
            artist = "Ocean Band",
            imageUrl = "",
            songId = "s1",
            onPlay = {}
        )
    }
}
