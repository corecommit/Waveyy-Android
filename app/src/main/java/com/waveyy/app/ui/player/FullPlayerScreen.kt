package com.waveyy.app.ui.player

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import com.waveyy.app.domain.model.Song
import com.waveyy.app.playback.PlaybackConnection
import com.waveyy.app.ui.components.AdaptiveImage
import com.waveyy.app.ui.library.LibraryViewModel
import com.waveyy.app.ui.library.LikeSongButton
import com.waveyy.app.ui.library.MusicViewModel
import com.waveyy.app.ui.library.sharedMusicViewModel
import com.waveyy.app.ui.theme.rememberAlbumAccent
import kotlinx.coroutines.launch

/**
 * Full-screen Now Playing: header row, square artwork, title/artist,
 * accent slider, transport pill, secondary actions. Everything fits on
 * one screen (no scrolling, no swipe gestures). Ports the player route;
 * queue and lyrics open as bottom sheets.
 *
 * @param viewModel player logic (shared instance).
 * @param onClose minimize/back handler.
 * @param onArtist artist-page navigation (artist id).
 * @param onFullLyrics full-lyrics navigation (song id).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullPlayerScreen(
    viewModel: PlayerViewModel = sharedPlayerViewModel(),
    onClose: () -> Unit = {},
    onArtist: (String) -> Unit = {},
    onFullLyrics: (String) -> Unit = {}
) {
    val connection = viewModel.connection
    val state by connection.state.collectAsState()
    val queue by viewModel.queue.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val libraryVm: LibraryViewModel = hiltViewModel()
    val libraryState by libraryVm.state.collectAsState()
    val musicVm: MusicViewModel = sharedMusicViewModel()
    val musicLiked by musicVm.likedSongIds.collectAsStateWithLifecycle()
    var showQueue by remember { mutableStateOf(false) }
    var showInfo by remember { mutableStateOf(false) }
    var showMetadata by remember { mutableStateOf(false) }
    var showPlaylistPicker by remember { mutableStateOf(false) }
    var showSleepDialog by remember { mutableStateOf(false) }
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showLyricsProvider by remember { mutableStateOf(false) }
    var speedValue by remember { mutableStateOf(1f) }
    val currentSong = queue.getOrNull(state.mediaIndex)
        ?: queue.firstOrNull { it.id == state.mediaId }
    val lyricsState by viewModel.lyrics.collectAsState()
    LaunchedEffect(currentSong?.id) {
        currentSong?.let { viewModel.loadLyrics(it) }
    }
    val accent = rememberAlbumAccent(currentSong?.imageUrl, currentSong?.id)
    val vmError by viewModel.playerError.collectAsState()
    val snackbarHost = remember { SnackbarHostState() }
    LaunchedEffect(vmError, state.error) {
        val message = vmError ?: state.error ?: return@LaunchedEffect
        val result = snackbarHost.showSnackbar(message, actionLabel = "Retry")
        viewModel.clearPlayerError()
        connection.clearError()
        if (result == SnackbarResult.ActionPerformed) {
            currentSong?.let { viewModel.playSong(it) }
        }
    }
    val fraction = if (state.durationMs > 0L) {
        (state.positionMs.toFloat() / state.durationMs).coerceIn(0f, 1f)
    } else {
        0f
    }

    Box(modifier = Modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        accent.copy(alpha = 0.5f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(imageVector = Icons.Filled.KeyboardArrowDown, contentDescription = "Minimize")
            }
            Spacer(Modifier.weight(1f))
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = "NOW PLAYING",
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { showInfo = true }) {
                Icon(imageVector = Icons.Filled.MoreVert, contentDescription = "More")
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            AdaptiveImage(
                imageUrl = currentSong?.imageUrl.orEmpty(),
                contentDescription = currentSong?.name,
                modifier = Modifier
                    .fillMaxSize()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(28.dp)),
                contentScale = ContentScale.Crop
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),        // gap (i): album art → title
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = currentSong?.name ?: "Nothing playing",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                // Reserve 2 lines of height always so the layout is stable on
                // 1-line and 2-line titles alike.
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 72.dp, max = 72.dp)
            )
            if (currentSong != null) {
                IconButton(onClick = { showPlaylistPicker = true }) {
                    Icon(
                        imageVector = Icons.Outlined.AddCircleOutline,
                        contentDescription = "Add to playlist",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                LikeSongButton(song = currentSong)
            }
        }
        val artistId = currentSong?.primaryArtistId.orEmpty()
        Text(
            text = currentSong?.artist ?: "",
            style = MaterialTheme.typography.titleMedium,
            color = if (artistId.isNotBlank()) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .padding(top = 2.dp)
                .clickable(enabled = artistId.isNotBlank()) { onArtist(artistId) }
        )

        Spacer(Modifier.height(16.dp))

        Slider(
            value = fraction,
            onValueChange = { scope.launch { connection.seekTo((it * state.durationMs).toLong()) } },
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                inactiveTickColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                activeTickColor = MaterialTheme.colorScheme.primary
            )
        )

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = formatDuration(state.positionMs / 1000L),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = formatDuration(state.durationMs / 1000L),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { scope.launch { connection.toggleShuffle() } }) {
                Icon(
                    imageVector = Icons.Filled.Shuffle,
                    contentDescription = "Shuffle",
                    tint = if (state.shuffleEnabled) accent
                           else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = { viewModel.rewind() },
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.SkipPrevious,
                    contentDescription = "Previous",
                    modifier = Modifier.size(36.dp)
                )
            }
            FilledIconButton(
                onClick = { scope.launch { connection.togglePlayPause() } },
                modifier = Modifier.size(72.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(
                    imageVector = if (state.isPlaying) Icons.Filled.Pause
                                  else Icons.Filled.PlayArrow,
                    contentDescription = if (state.isPlaying) "Pause" else "Play",
                    modifier = Modifier.size(36.dp)
                )
            }
            IconButton(
                onClick = { viewModel.advance() },
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.SkipNext,
                    contentDescription = "Next",
                    modifier = Modifier.size(36.dp)
                )
            }
            IconButton(onClick = { scope.launch { connection.cycleRepeatMode() } }) {
                Icon(
                    imageVector = if (state.repeatMode == Player.REPEAT_MODE_ONE) {
                        Icons.Filled.RepeatOne
                    } else {
                        Icons.Filled.Repeat
                    },
                    contentDescription = "Repeat",
                    tint = if (state.repeatMode == Player.REPEAT_MODE_OFF) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        accent
                    }
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            IconButton(onClick = { showMetadata = true }) {
                Icon(imageVector = Icons.Filled.Info, contentDescription = "Song info")
            }
            IconButton(onClick = { showQueue = true }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                    contentDescription = "Queue"
                )
            }
            IconButton(onClick = {
                currentSong?.let { showPlaylistPicker = true }
            }) {
                Icon(imageVector = Icons.Filled.PlaylistAdd, contentDescription = "Add to playlist")
            }
            val lyricsReady = lyricsState is LyricsUiState.Ready ||
                lyricsState is LyricsUiState.Plain ||
                lyricsState is LyricsUiState.Instrumental
            IconButton(
                onClick = { currentSong?.let { onFullLyrics(it.id) } },
                enabled = lyricsReady && currentSong != null
            ) {
                Icon(
                    imageVector = Icons.Filled.Lyrics,
                    contentDescription = "Lyrics",
                    tint = if (lyricsReady && currentSong != null) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    }
                )
            }
        }
    }
        SnackbarHost(
            hostState = snackbarHost,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp)
                .padding(bottom = 88.dp)
        )
    }
    if (showQueue) {
        ModalBottomSheet(
            onDismissRequest = { showQueue = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            QueueSheet(
                queue = queue,
                currentId = state.mediaId,
                onMove = viewModel::moveQueueItem,
                onRemove = viewModel::removeQueueItem,
                onClear = viewModel::clearQueue,
                onPlay = { index ->
                    scope.launch { connection.playSongs(queue, index) }
                    showQueue = false
                }
            )
        }
    }
    if (showInfo && currentSong != null) {
        val infoSong = currentSong
        SongInfoSheet(
            song = infoSong,
            isLiked = infoSong.id in musicLiked,
            onDismiss = { showInfo = false },
            onAction = { action ->
                if (action == SongAction.ARTISTS) {
                    val artistId = infoSong.primaryArtistId
                    showInfo = false
                    if (artistId.isNotBlank()) onArtist(artistId)
                } else {
                    handleInfoAction(
                        action = action,
                        song = infoSong,
                        viewModel = viewModel,
                        libraryVm = libraryVm,
                        musicVm = musicVm,
                        connection = connection,
                        scope = scope,
                        context = context,
                        onPlaylistPicker = { showPlaylistPicker = true },
                        onSleepDialog = { showSleepDialog = true },
                        onSpeedDialog = { showSpeedDialog = true },
                        onLyricsProvider = { showLyricsProvider = true }
                    )
                }
            }
        )
    }
    if (showMetadata && currentSong != null) {
        SongMetadataSheet(
            song = currentSong,
            onDismiss = { showMetadata = false }
        )
    }
    if (showPlaylistPicker && currentSong != null) {
        PlaylistPickerDialog(
            song = currentSong,
            libraryVm = libraryVm,
            onDismiss = { showPlaylistPicker = false }
        )
    }
    if (showSleepDialog) {
        SleepTimerDialog(
            onDismiss = { showSleepDialog = false },
            onSelect = { minutes ->
                showSleepDialog = false
                if (minutes == null) {
                    viewModel.setSleepEndOfSong()
                } else {
                    viewModel.setSleepTimer(minutes)
                }
                Toast.makeText(context, "Sleep timer set", Toast.LENGTH_SHORT).show()
            }
        )
    }
    if (showSpeedDialog) {
        SpeedDialog(
            initial = speedValue,
            onDismiss = { showSpeedDialog = false },
            onSelect = { speed ->
                speedValue = speed
                showSpeedDialog = false
                viewModel.setPlaybackSpeed(speed)
            }
        )
    }
    if (showLyricsProvider) {
        LyricsProviderDialog(
            viewModel = viewModel,
            onDismiss = { showLyricsProvider = false }
        )
    }
}

/** Routes info-sheet actions to player/library/connection operations. */
private fun handleInfoAction(
    action: SongAction,
    song: Song,
    viewModel: PlayerViewModel,
    libraryVm: LibraryViewModel,
    musicVm: MusicViewModel,
    connection: PlaybackConnection,
    scope: kotlinx.coroutines.CoroutineScope,
    context: android.content.Context,
    onPlaylistPicker: () -> Unit,
    onSleepDialog: () -> Unit,
    onSpeedDialog: () -> Unit,
    onLyricsProvider: () -> Unit
) {
    fun toast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
    when (action) {
        SongAction.LIKE -> musicVm.toggleLike(song)
        SongAction.DOWNLOAD -> scope.launch { viewModel.downloadSong(song) }
        SongAction.ADD_TO_PLAYLIST -> onPlaylistPicker()
        SongAction.PLAY_NEXT -> scope.launch { connection.playNextSong(song) }
        SongAction.ADD_TO_QUEUE -> viewModel.appendSong(song)
        SongAction.ARTISTS -> toast("Coming soon")
        SongAction.SONG_RADIO -> viewModel.startRadio(song)
        SongAction.START_RADIO -> viewModel.startRadio(song)
        SongAction.LYRICS_PROVIDER -> onLyricsProvider()
        SongAction.SLEEP_TIMER -> onSleepDialog()
        SongAction.SPEED_PITCH -> onSpeedDialog()
        SongAction.SHARE -> viewModel.shareSong(song)
    }
}

/** Formats seconds as m:ss. */
internal fun formatDuration(totalSeconds: Long): String {
    if (totalSeconds <= 0L) return "0:00"
    val minutes = totalSeconds / 60L
    val seconds = (totalSeconds % 60L).toString().padStart(2, '0')
    return "$minutes:$seconds"
}
