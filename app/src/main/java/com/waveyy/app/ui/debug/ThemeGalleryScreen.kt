package com.waveyy.app.ui.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.waveyy.app.R
import com.waveyy.app.ui.theme.ThemePreviews
import com.waveyy.app.ui.theme.WaveyyShapes
import com.waveyy.app.ui.theme.WaveyyTheme

private data class Swatch(val name: String, val color: Color)

private data class TypeSample(val name: String, val style: TextStyle)

/**
 * Debug-only gallery rendering every color role, type style, and shape.
 * Registered as a debug-only nav route; never linked from release UI.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ThemeGalleryScreen() {
    val scheme = MaterialTheme.colorScheme
    val swatches = listOf(
        Swatch("primary", scheme.primary),
        Swatch("onPrimary", scheme.onPrimary),
        Swatch("secondary", scheme.secondary),
        Swatch("onSecondary", scheme.onSecondary),
        Swatch("tertiary", scheme.tertiary),
        Swatch("error", scheme.error),
        Swatch("background", scheme.background),
        Swatch("onBackground", scheme.onBackground),
        Swatch("surface", scheme.surface),
        Swatch("onSurface", scheme.onSurface),
        Swatch("surfaceVariant", scheme.surfaceVariant),
        Swatch("outline", scheme.outline)
    )
    val type = MaterialTheme.typography
    val samples = listOf(
        TypeSample("displayLarge", type.displayLarge),
        TypeSample("displayMedium", type.displayMedium),
        TypeSample("displaySmall", type.displaySmall),
        TypeSample("headlineLarge", type.headlineLarge),
        TypeSample("headlineMedium", type.headlineMedium),
        TypeSample("headlineSmall", type.headlineSmall),
        TypeSample("titleLarge", type.titleLarge),
        TypeSample("titleMedium", type.titleMedium),
        TypeSample("titleSmall", type.titleSmall),
        TypeSample("bodyLarge", type.bodyLarge),
        TypeSample("bodyMedium", type.bodyMedium),
        TypeSample("bodySmall", type.bodySmall),
        TypeSample("labelLarge", type.labelLarge),
        TypeSample("labelMedium", type.labelMedium),
        TypeSample("labelSmall", type.labelSmall)
    )
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.gallery_title),
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = stringResource(R.string.gallery_colors),
                style = MaterialTheme.typography.titleMedium
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                swatches.forEach { swatch ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(WaveyyShapes.small)
                                .background(swatch.color)
                        )
                        Text(text = swatch.name, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            Text(
                text = stringResource(R.string.gallery_typography),
                style = MaterialTheme.typography.titleMedium
            )
            samples.forEach { sample ->
                Text(text = "Waveyy " + sample.name, style = sample.style)
            }
            Text(
                text = stringResource(R.string.gallery_shapes),
                style = MaterialTheme.typography.titleMedium
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "extraSmall" to WaveyyShapes.extraSmall,
                    "small" to WaveyyShapes.small,
                    "medium" to WaveyyShapes.medium,
                    "large" to WaveyyShapes.large,
                    "extraLarge" to WaveyyShapes.extraLarge
                ).forEach { (name, shape) ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(shape)
                                .background(MaterialTheme.colorScheme.secondary)
                        )
                        Text(text = name, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

/** Preview of the theme gallery in the app theme. */
@ThemePreviews
@Composable
private fun ThemeGalleryPreview() {
    WaveyyTheme {
        ThemeGalleryScreen()
    }
}
