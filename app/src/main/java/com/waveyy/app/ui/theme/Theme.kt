package com.waveyy.app.ui.theme

import android.content.res.Configuration
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

/**
 * Waveyy theme. The web app forces dark mode (`forcedTheme="dark"` in
 * `app/layout.js`), so dark is the default; light exists only as a sane
 * fallback for previews and debugging.
 *
 * @param darkTheme whether to use [WaveyyDarkColorScheme].
 * @param content screen content.
 */
@Composable
fun WaveyyTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) WaveyyDarkColorScheme else WaveyyLightColorScheme,
        typography = WaveyyTypography,
        shapes = WaveyyShapes,
        content = content
    )
}

/**
 * Multi-preview covering theme × font scale × width, for every leaf
 * Composable per project conventions.
 */
@Preview(name = "Dark 360", uiMode = Configuration.UI_MODE_NIGHT_YES, widthDp = 360)
@Preview(name = "Dark 600", uiMode = Configuration.UI_MODE_NIGHT_YES, widthDp = 600)
@Preview(
    name = "Dark 360 large text",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    fontScale = 1.3f,
    widthDp = 360
)
@Preview(
    name = "Dark 600 large text",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    fontScale = 1.3f,
    widthDp = 600
)
@Preview(name = "Light 360", uiMode = Configuration.UI_MODE_NIGHT_NO, widthDp = 360)
@Preview(name = "Light 600", uiMode = Configuration.UI_MODE_NIGHT_NO, widthDp = 600)
@Preview(
    name = "Light 360 large text",
    uiMode = Configuration.UI_MODE_NIGHT_NO,
    fontScale = 1.3f,
    widthDp = 360
)
@Preview(
    name = "Light 600 large text",
    uiMode = Configuration.UI_MODE_NIGHT_NO,
    fontScale = 1.3f,
    widthDp = 600
)
annotation class ThemePreviews
