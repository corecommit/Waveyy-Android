package com.waveyy.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.waveyy.app.ui.theme.BricolageGrotesque
import com.waveyy.app.ui.theme.ThemePreviews
import com.waveyy.app.ui.theme.WaveyyTheme

/**
 * Waveyy wordmark: "Wave" in foreground with a blue "yy", mirroring
 * `components/page/logo.jsx` (`text-white` + `text-blue-500`, extrabold).
 *
 * @param onClick home navigation handler.
 * @param modifier layout modifier.
 * @param compact compact 18sp variant sized for the 80.dp top-bar slot.
 */
@Composable
fun WaveyyLogo(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    Row(
        modifier = modifier.clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(color = MaterialTheme.colorScheme.onBackground)) {
                    append("Wave")
                }
                withStyle(SpanStyle(color = WaveyyBlue)) {
                    append("yy")
                }
            },
            fontFamily = BricolageGrotesque,
            fontWeight = FontWeight.ExtraBold,
            fontSize = if (compact) 18.sp else 24.sp,
            modifier = Modifier.padding(start = 4.dp, top = 2.dp)
        )
    }
}

/** Brand blue for the "yy" suffix (`text-blue-500`). */
val WaveyyBlue = Color(0xFF3B82F6)

/** Preview of [WaveyyLogo]. */
@ThemePreviews
@Composable
private fun WaveyyLogoPreview() {
    WaveyyTheme {
        WaveyyLogo(onClick = {})
    }
}
