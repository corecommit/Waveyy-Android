package com.waveyy.app.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * App header: logo left, search center, account menu right. Ports
 * `components/page/header.jsx` composition (logo + search + user panel).
 *
 * @param query search text.
 * @param onQuery search text handler.
 * @param onSubmitSearch search submit handler (navigates to results).
 * @param onHome logo-tap handler.
 * @param onProfile profile handler.
 * @param onLibrary library handler.
 * @param onSettings settings handler.
 * @param onSignIn sign-in handler.
 * @param modifier layout modifier.
 */
@Composable
fun WaveyyTopBar(
    query: String,
    onQuery: (String) -> Unit,
    onSubmitSearch: (String) -> Unit,
    onHome: () -> Unit,
    onProfile: () -> Unit,
    onLibrary: () -> Unit,
    onSettings: () -> Unit,
    onSignIn: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        WaveyyLogo(onClick = onHome)
        WaveyySearchBar(
            query = query,
            onQuery = onQuery,
            onSubmit = onSubmitSearch,
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
        )
        WaveyyUserPanel(
            onProfile = onProfile,
            onLibrary = onLibrary,
            onSettings = onSettings,
            onSignIn = onSignIn
        )
    }
}
