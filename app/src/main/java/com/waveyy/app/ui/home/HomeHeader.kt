package com.waveyy.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Box
import timber.log.Timber

/**
 * Home header row: "Home" title plus three circular action buttons
 * (discover, search, settings). Title is top-aligned; buttons sit in a
 * right-aligned row with at least 24.dp breathing room. Matches the
 * reference layout.
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
    LaunchedEffect(Unit) {
        Timber.tag("HomeHeader").d("RENDERED: gap=8dp, buttonSize=40dp, iconSize=20dp")
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
            .padding(top = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "Home",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.weight(1f))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 4.dp)
        ) {
            HeaderIconButton(
                image = Icons.Outlined.Explore,
                description = "Discover",
                onClick = onExplore
            )
            HeaderIconButton(
                image = Icons.Outlined.Search,
                description = "Search",
                onClick = onSearch
            )
            HeaderIconButton(
                image = Icons.Outlined.Settings,
                description = "Settings",
                onClick = onSettings
            )
        }
    }
}

/** 40.dp circular header button with a 20.dp icon on a subtle container. */
@Composable
private fun HeaderIconButton(
    image: ImageVector,
    description: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = image,
            contentDescription = description,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurface
        )
    }
}
