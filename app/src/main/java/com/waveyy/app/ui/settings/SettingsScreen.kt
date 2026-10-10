package com.waveyy.app.ui.settings

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.waveyy.app.data.local.datastore.UserPrefs
import com.waveyy.app.ui.components.WaveyyBackButton
import com.waveyy.app.ui.player.rememberChromeBottomReserve
import com.waveyy.app.ui.update.sharedUpdateViewModel
import kotlinx.coroutines.launch

/**
 * Settings as sectioned cards with a top bar. Stream quality and lyrics
 * pickers open modal bottom sheets. Library cleanup rows show an undoable
 * Snackbar after each destructive action.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onSignIn: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    val quality by viewModel.audioQuality.collectAsState()
    val saavnEnabled by viewModel.saavnEnabled.collectAsState()
    val preferVideo by viewModel.preferVideo.collectAsState()
    val ytmusicEnabled by viewModel.ytmusicEnabled.collectAsState()
    val preferFast by viewModel.preferFast.collectAsState()
    val stopMusicOnClose by viewModel.stopMusicOnClose.collectAsState()
    val session by viewModel.session.collectAsState()
    val lyricsMode by viewModel.lyricsMode.collectAsState()
    val preferredProvider by viewModel.preferredLyricsProvider.collectAsState()
    val hapticsEnabled by viewModel.hapticsEnabled.collectAsState()
    val updateVm = sharedUpdateViewModel()
    val updateState by updateVm.state.collectAsState()
    val updateAvailable = updateState.latest
    val context = LocalContext.current
    val chromeBottom = rememberChromeBottomReserve()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var picker by remember { mutableStateOf<ActivePicker?>(null) }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { scaffoldPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(scaffoldPadding)
                .background(MaterialTheme.colorScheme.background)
                .statusBarsPadding(),
            contentPadding = PaddingValues(bottom = chromeBottom)
        ) {
            // ─── Top bar ───
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 24.dp, top = 12.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    WaveyyBackButton(onClick = onBack)
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // ─── Audio ───
            item {
                SettingsSectionCard(title = "Audio") {
                    SettingsClickRow(
                        label = "Stream quality",
                        value = quality,
                        onClick = { picker = ActivePicker.StreamQuality }
                    )
                }
            }

            // ─── Sources ───
            item {
                SettingsSectionCard(title = "Sources") {
                    SettingsToggleRow(
                        label = "Primary catalog",
                        subtitle = "JioSaavn — the main source for songs and charts",
                        checked = saavnEnabled,
                        onChecked = viewModel::setSaavnEnabled
                    )
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    SettingsToggleRow(
                        label = "YouTube Music",
                        subtitle = "Adds YT Music as a second search source",
                        checked = ytmusicEnabled,
                        onChecked = viewModel::setYtmusicEnabled
                    )
                    if (ytmusicEnabled) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        SettingsToggleRow(
                            label = "Prefer music videos",
                            subtitle = "When available, show the music-video version",
                            checked = preferVideo,
                            onChecked = viewModel::setPreferVideo
                        )
                    }
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    SettingsToggleRow(
                        label = "Faster search results",
                        subtitle = "Show the first source to respond instead of merging them",
                        checked = preferFast,
                        onChecked = viewModel::setPreferFast
                    )
                }
            }

            // ─── Playback ───
            item {
                SettingsSectionCard(title = "Playback") {
                    SettingsToggleRow(
                        label = "Keep playing in background",
                        subtitle = "Playback continues when you leave the app",
                        checked = !stopMusicOnClose,
                        onChecked = { keepPlaying -> viewModel.setStopMusicOnClose(!keepPlaying) }
                    )
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    SettingsClickRow(
                        label = "Battery optimization",
                        value = "Open settings",
                        onClick = {
                            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            runCatching { context.startActivity(intent) }
                        }
                    )
                }
            }

            // ─── Lyrics ───
            item {
                SettingsSectionCard(title = "Lyrics") {
                    SettingsClickRow(
                        label = "Loading style",
                        value = if (lyricsMode == UserPrefs.LYRICS_MODE_PREFERRED) {
                            "Reliable"
                        } else {
                            "Fast"
                        },
                        onClick = { picker = ActivePicker.LyricsMode }
                    )
                    if (lyricsMode == UserPrefs.LYRICS_MODE_PREFERRED) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        SettingsClickRow(
                            label = "Lyrics source",
                            value = preferredProvider,
                            onClick = { picker = ActivePicker.LyricsSource }
                        )
                    }
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    SettingsClickRow(
                        label = "Clear lyrics cache",
                        value = null,
                        onClick = viewModel::clearLyricsCache
                    )
                }
            }

            // ─── Appearance ───
            item {
                SettingsSectionCard(title = "Appearance") {
                    SettingsToggleRow(
                        label = "Haptic feedback",
                        subtitle = "Vibrate on taps, likes, and playback controls",
                        checked = hapticsEnabled,
                        onChecked = viewModel::setHapticsEnabled
                    )
                }
            }

            // ─── Library ───
            item {
                SettingsSectionCard(title = "Library") {
                    SettingsClickRow(
                        label = "Clear search history",
                        value = null,
                        onClick = {
                            scope.launch {
                                val previous = viewModel.captureAndClearSearchHistory()
                                val result = snackbarHostState.showSnackbar(
                                    message = "Search history cleared",
                                    actionLabel = "Undo",
                                    duration = SnackbarDuration.Short
                                )
                                if (result == SnackbarResult.ActionPerformed) {
                                    viewModel.restoreSearchHistory(previous)
                                }
                            }
                        }
                    )
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    SettingsClickRow(
                        label = "Clear listening history",
                        value = null,
                        onClick = {
                            scope.launch {
                                val previous = viewModel.captureAndClearListeningHistory()
                                val result = snackbarHostState.showSnackbar(
                                    message = "Listening history cleared",
                                    actionLabel = "Undo",
                                    duration = SnackbarDuration.Short
                                )
                                if (result == SnackbarResult.ActionPerformed) {
                                    viewModel.restoreListeningHistory(previous)
                                }
                            }
                        }
                    )
                }
            }

            // ─── Account ───
            item {
                SettingsSectionCard(title = "Account") {
                    SettingsClickRow(
                        label = if (session != null) "Sign out" else "Sign in",
                        value = session?.displayName ?: "Guest",
                        onClick = {
                            if (session != null) viewModel.signOut() else onSignIn()
                        }
                    )
                }
            }

            // ─── About ───
            item {
            SettingsSectionCard(title = "About") {
                SettingsClickRow(
                    label = "Waveyy",
                    value = if (updateAvailable != null) {
                        "Update available · v${updateAvailable.version}"
                    } else {
                        "v${viewModel.versionName}"
                    },
                    onClick = { if (updateAvailable != null) updateVm.showPopup() }
                )
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    SettingsClickRow(
                        label = "License",
                        value = "MIT",
                        onClick = { }
                    )
                }
            }
        }
    }

    // ─── Pickers ───
    when (val current = picker) {
        ActivePicker.StreamQuality -> SettingsPickerSheet(
            title = "Stream quality",
            options = AUDIO_QUALITY_OPTIONS.map { PickerOption(label = it, value = it) },
            currentValue = quality,
            onSelect = {
                viewModel.setAudioQuality(it)
                picker = null
            },
            onDismiss = { picker = null }
        )
        ActivePicker.LyricsMode -> SettingsPickerSheet(
            title = "Lyrics loading style",
            options = listOf(
                PickerOption(
                    label = "Fast",
                    value = UserPrefs.LYRICS_MODE_AUTO,
                    subtitle = "Use the first provider that responds"
                ),
                PickerOption(
                    label = "Reliable",
                    value = UserPrefs.LYRICS_MODE_PREFERRED,
                    subtitle = "Use one specific provider every time"
                )
            ),
            currentValue = lyricsMode,
            onSelect = {
                viewModel.setLyricsMode(it)
                picker = null
            },
            onDismiss = { picker = null }
        )
        ActivePicker.LyricsSource -> SettingsPickerSheet(
            title = "Lyrics source",
            options = LYRICS_PROVIDER_OPTIONS.map { option ->
                PickerOption(label = option, value = option)
            },
            currentValue = preferredProvider,
            onSelect = {
                viewModel.setPreferredLyricsProvider(it)
                picker = null
            },
            onDismiss = { picker = null }
        )
        null -> Unit
    }
}

/** Which picker sheet is currently open, if any. */
private enum class ActivePicker { StreamQuality, LyricsMode, LyricsSource }

/** Rounded card section with a small uppercase title above it. */
@Composable
private fun SettingsSectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Column(content = content)
        }
    }
}

/** Row with label + value on the right + chevron. Tappable. */
@Composable
private fun SettingsClickRow(
    label: String,
    value: String?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        if (value != null) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

/** Row with label + subtitle on the left, switch on the right. */
@Composable
private fun SettingsToggleRow(
    label: String,
    subtitle: String?,
    checked: Boolean,
    onChecked: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onChecked,
            colors = SwitchDefaults.colors(
                checkedThumbColor = com.waveyy.app.ui.theme.WaveyyPalette.DeepGreen,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                uncheckedBorderColor = MaterialTheme.colorScheme.outlineVariant
            )
        )
    }
}
