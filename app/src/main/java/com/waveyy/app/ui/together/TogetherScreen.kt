package com.waveyy.app.ui.together

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.waveyy.app.ui.components.WaveyyButton

/**
 * Listen Together: create a room, share its link, and see who's in it.
 * Ports `app/(root)/together/page.jsx`.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TogetherScreen(
    viewModel: TogetherViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val roomId = state.roomId

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Listen Together", style = MaterialTheme.typography.headlineMedium)
        Text(
            text = if (roomId == null) {
                "Create a room and invite friends to listen in real time."
            } else {
                "Share this link so friends can join the room."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (roomId == null) {
            WaveyyButton(
                text = "Create Room",
                onClick = viewModel::createRoom,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            Text(
                text = viewModel.roomLink().orEmpty(),
                style = MaterialTheme.typography.bodyMedium
            )
            WaveyyButton(
                text = "Copy Link",
                onClick = { copyLink(context, viewModel.roomLink().orEmpty()) },
                modifier = Modifier.fillMaxWidth()
            )
            WaveyyButton(
                text = "Leave Room",
                onClick = viewModel::leaveRoom,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "In this room (${state.users.size})",
                style = MaterialTheme.typography.titleSmall
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                state.users.forEach { user ->
                    AssistChip(onClick = {}, label = { Text(user.name) })
                }
            }
        }
    }
}

/** Copies [link] to the clipboard. */
private fun copyLink(context: Context, link: String) {
    val clipboard = context.getSystemService(ClipboardManager::class.java)
    clipboard?.setPrimaryClip(ClipData.newPlainText("Waveyy room", link))
}
