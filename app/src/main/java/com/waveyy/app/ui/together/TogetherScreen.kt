package com.waveyy.app.ui.together

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.waveyy.app.ui.components.WaveyyButton
import com.waveyy.app.ui.components.WaveyyInput
import com.waveyy.app.ui.components.WaveyyOutlineButton

/**
 * Listen Together: create a room, share its 8-char code, join by code, and
 * see who's in the room. Ports `app/(root)/together/page.jsx`.
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
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(24.dp))
        Text(
            text = "Listen Together",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = if (roomId == null) {
                "Create a room and share the code, or enter a friend's code to join."
            } else {
                "Share this code with friends so they can join."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))

        if (roomId == null) {
            IdleState(
                onCreateRoom = viewModel::createRoom,
                onJoin = viewModel::joinRoom
            )
        } else {
            ActiveRoomState(
                code = roomId,
                users = state.users,
                onCopyCode = {
                    copyToClipboard(context, "Waveyy room code", roomId)
                    Toast.makeText(context, "Code copied", Toast.LENGTH_SHORT).show()
                },
                onCopyLink = {
                    val link = viewModel.roomLink().orEmpty()
                    copyToClipboard(context, "Waveyy room link", link)
                    Toast.makeText(context, "Link copied", Toast.LENGTH_SHORT).show()
                },
                onLeave = viewModel::leaveRoom
            )
        }
    }
}

/** Idle state: Create Room button + code input for joining. */
@Composable
private fun IdleState(
    onCreateRoom: () -> Unit,
    onJoin: (String) -> Unit
) {
    var code by remember { mutableStateOf("") }
    val normalized = TogetherViewModel.normalizeCode(code)
    val canJoin = normalized.length >= 6

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        WaveyyButton(
            text = "Create Room",
            onClick = onCreateRoom,
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(Modifier.width(24.dp))
            Spacer(
                modifier = Modifier
                    .weight(1f)
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )
            Text(
                text = "  or  ",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(
                modifier = Modifier
                    .weight(1f)
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )
            Spacer(Modifier.width(24.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            WaveyyInput(
                value = code,
                onValueChange = { code = it.uppercase().filter { ch -> ch.isLetterOrDigit() }.take(8) },
                label = "Room code",
                placeholder = "e.g. K7NP2XRM",
                modifier = Modifier.weight(1f)
            )
            WaveyyButton(
                text = "Join",
                onClick = { onJoin(code) },
                enabled = canJoin
            )
        }
    }
}

/** Active room: big code display, copy actions, leave, participants. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ActiveRoomState(
    code: String,
    users: List<com.waveyy.app.domain.model.RoomUser>,
    onCopyCode: () -> Unit,
    onCopyLink: () -> Unit,
    onLeave: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .clickable(onClick = onCopyCode),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Room code",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = code,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 6.sp
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Tap to copy",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        WaveyyOutlineButton(
            text = "Copy link",
            onClick = onCopyLink,
            modifier = Modifier.fillMaxWidth()
        )

        WaveyyButton(
            text = "Leave Room",
            onClick = onLeave,
            modifier = Modifier.fillMaxWidth()
        )

        if (users.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "In this room (${users.size})",
                style = MaterialTheme.typography.titleSmall
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                users.forEach { user ->
                    AssistChip(onClick = {}, label = { Text(user.name) })
                }
            }
        }
    }
}

/** Copies [text] to the system clipboard under [label]. */
private fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(ClipboardManager::class.java)
    clipboard?.setPrimaryClip(ClipData.newPlainText(label, text))
}
