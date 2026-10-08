package com.waveyy.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.waveyy.app.data.repository.AuthRepository
import com.waveyy.app.data.repository.AuthUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Account-menu backing state. */
@HiltViewModel
class UserPanelViewModel @Inject constructor(
    private val repository: AuthRepository
) : ViewModel() {
    /** Current session (null for guests). */
    val session = repository.sessionFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = null
    )

    /** Signs out. */
    fun signOut() {
        viewModelScope.launch { repository.signOut() }
    }
}

/**
 * Account menu: avatar/person button opening Profile, Library, Settings,
 * and Sign in/out items. Ports `components/page/user-panel.jsx` (Discord
 * entry skipped on Android per spec).
 *
 * @param onProfile navigate-to-profile handler.
 * @param onLibrary navigate-to-library handler.
 * @param onSettings navigate-to-settings handler.
 * @param onSignIn navigate-to-login handler.
 * @param modifier layout modifier.
 * @param viewModel backing state (activity-scoped when called outside a
 *   destination).
 */
@Composable
fun WaveyyUserPanel(
    onProfile: () -> Unit,
    onLibrary: () -> Unit,
    onSettings: () -> Unit,
    onSignIn: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: UserPanelViewModel = hiltViewModel()
) {
    var expanded by remember { mutableStateOf(false) }
    val session: AuthUser? by viewModel.session.collectAsState()
    // Tight anchor box so the menu opens directly below the avatar on the
    // right side instead of drifting across the screen.
    Box(modifier = modifier.wrapContentSize(Alignment.TopEnd)) {
        IconButton(onClick = { expanded = true }) {
            Icon(imageVector = Icons.Filled.Person, contentDescription = "Account")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        if (session != null) {
            DropdownMenuItem(
                text = { Text(session?.displayName ?: "Profile") },
                leadingIcon = { Icon(imageVector = Icons.Filled.Person, contentDescription = null) },
                onClick = {
                    expanded = false
                    onProfile()
                }
            )
            DropdownMenuItem(
                text = { Text("Library") },
                leadingIcon = { Icon(imageVector = Icons.Filled.LibraryMusic, contentDescription = null) },
                onClick = {
                    expanded = false
                    onLibrary()
                }
            )
            DropdownMenuItem(
                text = { Text("Settings") },
                leadingIcon = { Icon(imageVector = Icons.Filled.Settings, contentDescription = null) },
                onClick = {
                    expanded = false
                    onSettings()
                }
            )
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text("Sign out") },
                leadingIcon = { Icon(imageVector = Icons.Filled.Logout, contentDescription = null) },
                onClick = {
                    expanded = false
                    viewModel.signOut()
                }
            )
        } else {
            DropdownMenuItem(
                text = { Text("Sign in") },
                leadingIcon = { Icon(imageVector = Icons.Filled.Person, contentDescription = null) },
                onClick = {
                    expanded = false
                    onSignIn()
                }
            )
            DropdownMenuItem(
                text = { Text("Settings") },
                leadingIcon = { Icon(imageVector = Icons.Filled.Settings, contentDescription = null) },
                onClick = {
                    expanded = false
                    onSettings()
                }
            )
        }
        }
    }
}
