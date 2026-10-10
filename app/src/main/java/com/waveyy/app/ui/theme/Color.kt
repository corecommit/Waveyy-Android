package com.waveyy.app.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Raw Waveyy palette, ported from `app/globals.css` CSS variables
 * (HSL triplets converted to ARGB hex). The web app is dark-only
 * (forced dark `ThemeProvider`), so these roles target the dark scheme.
 */
object WaveyyPalette {
    /** --background, --card, --popover: near-black blue. */
    val Background = Color(0xFF09090B)

    /** Bright emerald — the app's primary accent. */
    val Emerald = Color(0xFF10B981)

    /** Lighter emerald for emphasis / hover states. */
    val EmeraldBright = Color(0xFF34D399)

    /** Dark emerald — containers (chip backgrounds, tonal surfaces). */
    val DeepGreen = Color(0xFF065F46)

    /** Deepest emerald — primary container surfaces. */
    val DeepGreenDark = Color(0xFF064E3B)

    /** Near-black text on emerald surfaces (matches background). */
    val OnEmerald = Color(0xFF09090B)

    /** --foreground and most --*-foreground roles: near-white. */
    val Foreground = Color(0xFFFAFAFA)

    /** --primary-foreground: button text on light surfaces. */
    val PrimaryForeground = Color(0xFF18181B)

    /** --secondary, --muted, --accent, --border, --input: raised surfaces. */
    val Secondary = Color(0xFF27272A)

    /** --muted-foreground: secondary text. */
    val MutedForeground = Color(0xFFA1A1AA)

    /** --ring: focus rings and hairline highlights. */
    val Ring = Color(0xFFD4D4D8)

    /** --destructive: delete/remove actions. */
    val Destructive = Color(0xFF7F1D1D)
}

/**
 * Chart accents (`--chart-1` … `--chart-5`), kept for future stats/history
 * visuals. Currently unused by the UI, matching the web app.
 */
object WaveyyChartColors {
    val Chart1 = Color(0xFF2662D9)
    val Chart2 = Color(0xFF2EB88A)
    val Chart3 = Color(0xFFE88C30)
    val Chart4 = Color(0xFFAF57DB)
    val Chart5 = Color(0xFFE23670)
}

/** Dark scheme: direct port of the web app's forced-dark theme. */
val WaveyyDarkColorScheme = darkColorScheme(
    primary = WaveyyPalette.Emerald,
    onPrimary = WaveyyPalette.OnEmerald,
    primaryContainer = WaveyyPalette.DeepGreenDark,
    onPrimaryContainer = WaveyyPalette.EmeraldBright,
    secondary = WaveyyPalette.DeepGreen,
    onSecondary = WaveyyPalette.Foreground,
    secondaryContainer = WaveyyPalette.DeepGreen,
    onSecondaryContainer = WaveyyPalette.EmeraldBright,
    tertiary = WaveyyPalette.MutedForeground,
    onTertiary = WaveyyPalette.Background,
    tertiaryContainer = WaveyyPalette.Secondary,
    onTertiaryContainer = WaveyyPalette.Foreground,
    error = WaveyyPalette.Destructive,
    onError = WaveyyPalette.Foreground,
    errorContainer = WaveyyPalette.Destructive,
    onErrorContainer = WaveyyPalette.Foreground,
    background = WaveyyPalette.Background,
    onBackground = WaveyyPalette.Foreground,
    surface = WaveyyPalette.Background,
    onSurface = WaveyyPalette.Foreground,
    surfaceVariant = WaveyyPalette.Secondary,
    onSurfaceVariant = WaveyyPalette.MutedForeground,
    surfaceDim = WaveyyPalette.Background,
    surfaceBright = WaveyyPalette.Secondary,
    surfaceContainerLowest = WaveyyPalette.Background,
    surfaceContainerLow = WaveyyPalette.Background,
    surfaceContainer = WaveyyPalette.Secondary,
    surfaceContainerHigh = WaveyyPalette.Secondary,
    surfaceContainerHighest = WaveyyPalette.Secondary,
    outline = WaveyyPalette.Secondary,
    outlineVariant = WaveyyPalette.PrimaryForeground,
    scrim = Color.Black,
    inverseSurface = WaveyyPalette.Foreground,
    inverseOnSurface = WaveyyPalette.PrimaryForeground,
    inversePrimary = WaveyyPalette.PrimaryForeground
)

/**
 * Light scheme fallback. The web app ships no light mode; this exists only so
 * light `@ThemePreviews` render something sane. Baseline M3 light roles.
 */
val WaveyyLightColorScheme = lightColorScheme(
    primary = WaveyyPalette.PrimaryForeground,
    onPrimary = WaveyyPalette.Foreground,
    secondary = WaveyyPalette.Secondary,
    onSecondary = WaveyyPalette.Foreground,
    background = WaveyyPalette.Foreground,
    onBackground = WaveyyPalette.PrimaryForeground,
    surface = WaveyyPalette.Foreground,
    onSurface = WaveyyPalette.PrimaryForeground
)
