package com.waveyy.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.waveyy.app.util.ImageUrl

/**
 * Image with a resilient loading chain:
 *   1. Preferred URL (JioSaavn: WebP swap; YT: untouched).
 *   2. On error — Saavn WebP→JPG fallback, or YT small-size fallback.
 *   3. On second error — a neutral placeholder block with a music note.
 *
 * A dim background is always drawn behind the image so there is never a
 * blank rectangle while loading or on failure.
 *
 * @param imageUrl preferred image URL (blank renders the placeholder).
 * @param contentDescription accessibility description.
 * @param modifier layout modifier.
 * @param contentScale scaling inside the bounds.
 */
@Composable
fun AdaptiveImage(
    imageUrl: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val isYt = remember(imageUrl) { ImageUrl.isYouTubeThumbnail(imageUrl) }
    // Preferred URL: JioSaavn gets WebP upgrade; YT uses as-is; blank → null.
    val preferred = remember(imageUrl) {
        when {
            imageUrl.isBlank() -> null
            isYt -> imageUrl
            else -> ImageUrl.toWebpUrl(imageUrl) ?: imageUrl
        }
    }
    var model by remember(imageUrl) { mutableStateOf(preferred) }
    var attempt by remember(imageUrl) { mutableIntStateOf(0) }
    androidx.compose.runtime.LaunchedEffect(model) {
        if (model != null) {
            timber.log.Timber.tag("AdaptiveImage")
                .d("load id=%s attempt=%d model=%s", imageUrl, attempt, model)
        }
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        // Placeholder background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
        )

        if (model != null) {
            AsyncImage(
                model = model,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale,
                onError = {
                    val next: String? = when (attempt) {
                        0 -> {
                            // 1st retry: swap WebP→JPG for Saavn, or shrink
                            // YT thumbnail.
                            when {
                                isYt -> ImageUrl.youTubeFallback(imageUrl)
                                else -> ImageUrl.toSaavnJpgUrl(imageUrl)
                            }
                        }
                        1 -> {
                            // 2nd retry: swap rendition to 150x150. Often
                            // the smaller size still exists even when the
                            // 500x500 was rota'd off the CDN.
                            ImageUrl.swapSaavnRendition(imageUrl, "150x150")
                        }
                        2 -> {
                            // 3rd retry: original URL, no swaps. Catches
                            // cases where WebP/JPG negotiation was the
                            // problem all along.
                            imageUrl.takeIf { it != model && it.isNotBlank() }
                        }
                        else -> null
                    }
                    attempt += 1
                    if (next != null && next != model) {
                        model = next
                    } else {
                        model = null
                    }
                }
            )
        }

        if (model == null) {
            FallbackIcon()
        }
    }
}

/** Neutral music-note icon shown when no image could be loaded. */
@Composable
private fun FallbackIcon() {
    Icon(
        imageVector = Icons.Outlined.MusicNote,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
        modifier = Modifier.sizeIn(minWidth = 24.dp, minHeight = 24.dp)
    )
}
