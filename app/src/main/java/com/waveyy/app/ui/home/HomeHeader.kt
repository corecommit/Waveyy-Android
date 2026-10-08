package com.waveyy.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Home header row: "Home" title plus three circular action buttons
 * (discover, search, settings). Matches the reference layout.
 *
 * @param onExplore play-something-surprising handler.
 * @param onSearch open-search handler.
 * @param onSettings open-settings handler.
 * @param modifier layout modifier.
 */
@Composable
fun HomeHeader(
    onExplore: () -> Unit,
    onSearch: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Home",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        HeaderIconButton(image = Icons.Outlined.Explore, description = "Discover", onClick = onExplore)
        HeaderIconButton(image = Icons.Outlined.Search, description = "Search", onClick = onSearch)
        HeaderIconButton(image = Icons.Outlined.Settings, description = "Settings", onClick = onSettings)
    }
}

/** 48.dp circular header button on a subtle container. */
@Composable
private fun HeaderIconButton(
    image: ImageVector,
    description: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .padding(start = 8.dp)
            .size(48.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Icon(
            imageVector = image,
            contentDescription = description,
            tint = MaterialTheme.colorScheme.onSurface
        )
    }
}
