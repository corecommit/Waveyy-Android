package com.waveyy.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
 * Artist tile: circular image over the artist name. Ports
 * `components/cards/artist.jsx`.
 *
 * @param name artist name.
 * @param imageUrl image URL.
 * @param artistId artist id passed to [onOpen].
 * @param onOpen open handler.
 * @param modifier layout modifier.
 */
@Composable
fun ArtistCard(
    name: String,
    imageUrl: String,
    artistId: String,
    onOpen: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(112.dp)
            .clickable { onOpen(artistId) }
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AdaptiveImage(
            imageUrl = imageUrl,
            contentDescription = name,
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
        )
        Text(
            text = name,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}

/** Preview of [ArtistCard]. */
@ThemePreviews
@Composable
private fun ArtistCardPreview() {
    WaveyyTheme {
        ArtistCard(name = "Ocean Band", imageUrl = "", artistId = "a1", onOpen = {})
    }
}
