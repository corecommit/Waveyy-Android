package com.waveyy.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import com.waveyy.app.util.ImageUrl

/**
 * Image with the web app's fallback chain: WebP first, then the Saavn JPG
 * fallback (`toWebpUrl` → `toSaavnJpgUrl` in `lib/image-url.js`), rendered
 * through Coil. Ports `components/ui/adaptive-image.jsx`.
 *
 * @param imageUrl preferred image URL (blank renders nothing).
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
    if (imageUrl.isBlank()) return
    var model by remember(imageUrl) {
        mutableStateOf(ImageUrl.toWebpUrl(imageUrl) ?: imageUrl)
    }
    AsyncImage(
        model = model,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
        onError = {
            val fallback = ImageUrl.toSaavnJpgUrl(imageUrl)
            if (fallback != null && model != fallback) model = fallback
        }
    )
}
