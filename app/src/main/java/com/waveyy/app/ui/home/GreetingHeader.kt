package com.waveyy.app.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.util.Calendar

/**
 * Time-based greeting header with the username, mirroring the web home
 * header (Good morning/afternoon/evening + name, large headline with a
 * muted secondary line).
 *
 * @param username display name, or Guest.
 * @param modifier layout modifier.
 */
@Composable
fun GreetingHeader(
    username: String?,
    modifier: Modifier = Modifier
) {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greeting = when {
        hour < 12 -> "Good morning"
        hour < 17 -> "Good afternoon"
        hour < 21 -> "Good evening"
        else -> "Good night"
    }
    Column(modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            text = greeting,
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = username?.takeIf { it.isNotBlank() } ?: "Guest",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Preview-friendly greeting resolver (hour in 0..23). */
fun greetingForHour(hour: Int): String {
    return when {
        hour < 12 -> "Good morning"
        hour < 17 -> "Good afternoon"
        hour < 21 -> "Good evening"
        else -> "Good night"
    }
}
