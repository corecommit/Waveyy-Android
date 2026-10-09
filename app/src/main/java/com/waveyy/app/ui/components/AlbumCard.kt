package com.waveyy.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
 * Album tile: 140.dp artwork with `RoundedCornerShape(16.dp)`, title
 * (`titleMedium`), artist (`bodySmall`, muted). Flat — no fill, no shadow.
 * Ports `components/cards/album.jsx`.
 *
 * @param title album title.
 * @param artist artist label.
 * @param imageUrl artwork URL.
 * @param albumId album id passed to [onOpen].
 * @param onOpen open handler.
 * @param modifier layout modifier.
 */
@Composable
fun AlbumCard(
    title: String,
    artist: String,
    imageUrl: String,
    albumId: String,
    onOpen: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(140.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onOpen(albumId) }
    ) {
        AdaptiveImage(
            imageUrl = imageUrl,
            contentDescription = title,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(16.dp))
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp)
        )
        Text(
            text = artist,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Preview of [AlbumCard]. */
@ThemePreviews
@Composable
private fun AlbumCardPreview() {
    WaveyyTheme {
        AlbumCard(
            title = "Tidal Dreams",
            artist = "Ocean Band",
            imageUrl = "",
            albumId = "a1",
            onOpen = {}
        )
    }
}
