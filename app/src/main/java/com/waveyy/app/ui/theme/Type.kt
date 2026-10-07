package com.waveyy.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.waveyy.app.R

/**
 * Bricolage Grotesque, the web app's display/body face
 * (`Bricolage_Grotesque` in `app/layout.js`, weights 200-800).
 * Static TTF instances bundled in `res/font/`.
 */
val BricolageGrotesque = FontFamily(
    Font(R.font.bricolage_grotesque_w200, FontWeight.W200),
    Font(R.font.bricolage_grotesque_w300, FontWeight.W300),
    Font(R.font.bricolage_grotesque_w400, FontWeight.W400),
    Font(R.font.bricolage_grotesque_w500, FontWeight.W500),
    Font(R.font.bricolage_grotesque_w600, FontWeight.W600),
    Font(R.font.bricolage_grotesque_w700, FontWeight.W700),
    Font(R.font.bricolage_grotesque_w800, FontWeight.W800)
)

/**
 * Full M3 type scale on Bricolage Grotesque. Display/headline lean bold
 * (matching the web app's heavy display headers); body/label stay neutral.
 */
val WaveyyTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = BricolageGrotesque,
        fontWeight = FontWeight.W700,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp
    ),
    displayMedium = TextStyle(
        fontFamily = BricolageGrotesque,
        fontWeight = FontWeight.W700,
        fontSize = 45.sp,
        lineHeight = 52.sp,
        letterSpacing = 0.sp
    ),
    displaySmall = TextStyle(
        fontFamily = BricolageGrotesque,
        fontWeight = FontWeight.W600,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = BricolageGrotesque,
        fontWeight = FontWeight.W600,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = BricolageGrotesque,
        fontWeight = FontWeight.W600,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = BricolageGrotesque,
        fontWeight = FontWeight.W600,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = BricolageGrotesque,
        fontWeight = FontWeight.W500,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = BricolageGrotesque,
        fontWeight = FontWeight.W500,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
        fontFamily = BricolageGrotesque,
        fontWeight = FontWeight.W500,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = BricolageGrotesque,
        fontWeight = FontWeight.W400,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = BricolageGrotesque,
        fontWeight = FontWeight.W400,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily = BricolageGrotesque,
        fontWeight = FontWeight.W400,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),
    labelLarge = TextStyle(
        fontFamily = BricolageGrotesque,
        fontWeight = FontWeight.W500,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = BricolageGrotesque,
        fontWeight = FontWeight.W500,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = BricolageGrotesque,
        fontWeight = FontWeight.W500,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)
