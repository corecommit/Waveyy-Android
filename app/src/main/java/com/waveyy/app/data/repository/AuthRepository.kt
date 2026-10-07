package com.waveyy.app.data.repository

import com.waveyy.app.data.local.datastore.UserPrefs
import com.waveyy.app.data.remote.auth.GoogleOAuth
import com.waveyy.app.data.remote.auth.SupabaseClientProvider
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

/** Signed-in user snapshot. */
data class AuthUser(
    val id: String,
    val email: String,
    val displayName: String,
    val avatarUrl: String?,
    val discordConnected: Boolean
)

/** Optional public `profiles` row used for username login resolution. */
@Serializable
data class ProfileRow(
    val id: String? = null,
    val username: String? = null,
    val email: String? = null
)

/**
 * Auth port of `components/providers/auth-provider.jsx`: password auth with
 * username resolution (DataStore map → optional `profiles` lookup → email),
 * signup with profile upsert, Google OAuth via Custom Tabs, sign-out, and a
 * session flow. Guests may still play (callers never gate on a session).
 */
@Singleton
class AuthRepository @Inject constructor(
    private val clientProvider: SupabaseClientProvider,
    private val userPrefs: UserPrefs
) {

    private val client get() = clientProvider.client
    private val auth get() = client.auth

    /** Current session as a user flow (null when signed out). */
    val sessionFlow: Flow<AuthUser?> = auth.sessionStatus.map { status ->
        (status as? SessionStatus.Authenticated)?.session?.user?.toAuthUser()
    }

    /**
     * One-shot current user, or null. Never throws: with placeholder or
     * unreachable Supabase values the app stays in guest mode instead of
     * crashing the caller (notably the fire-and-forget history hookup).
     */
    fun currentUser(): AuthUser? {
        return runCatching { auth.currentSessionOrNull()?.user?.toAuthUser() }.getOrNull()
    }

    /**
     * Signs in with an email or username. Usernames resolve through the local
     * map first, then the optional public `profiles` table.
     */
    suspend fun signInWithPassword(identifier: String, password: String): Result<AuthUser> {
        return runCatching {
            val email = if (identifier.contains("@")) {
                identifier.trim()
            } else {
                resolveEmailFromUsername(normalizeUsername(identifier))
                    ?: throw IllegalArgumentException("Username not found")
            }
            auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            currentUser() ?: throw IllegalStateException("Sign-in produced no session")
        }
    }

    /**
     * Signs up with username + email + password, writes the local username
     * map, and best-effort upserts a `profiles` row (ignored when the table
     * does not exist yet).
     */
    suspend fun signUpWithPassword(username: String, email: String, password: String): Result<AuthUser> {
        return runCatching {
            val normalized = normalizeUsername(username)
            require(normalized.matches(USERNAME_REGEX)) { "Username must be 3-30 letters, digits or _" }
            auth.signUpWith(Email) {
                this.email = email.trim()
                this.password = password
                data = buildJsonObject {
                    put("username", normalized)
                    put("display_name", username.trim())
                }
            }
            userPrefs.putUsername(normalized, email.trim())
            runCatching {
                client.from("profiles").insert(
                    ProfileRow(username = normalized, email = email.trim())
                )
            }
            currentUser()
        }.mapCatching { user ->
            // Email confirmation may be required before a session exists.
            user ?: throw IllegalStateException("Check your email to confirm the account")
        }
    }

    /** Builds the Google OAuth URL to open in a Custom Tab. */
    fun googleOAuthUrl(): String {
        return auth.getOAuthUrl(GoogleOAuth, redirectUrl = AUTH_CALLBACK_URL)
    }

    /**
     * Links Google to the current account (or starts a fresh Google sign-in
     * when signed out). Returns the URL to open, or null when no browser
     * step is required.
     */
    suspend fun connectGoogle(): String? {
        return if (auth.currentSessionOrNull() != null) {
            auth.linkIdentity(GoogleOAuth, redirectUrl = AUTH_CALLBACK_URL)
        } else {
            googleOAuthUrl()
        }
    }

    /** Exchanges an OAuth `code` for a session (PKCE). */
    suspend fun exchangeCode(code: String): Result<Unit> {
        return runCatching {
            auth.exchangeCodeForSession(code)
            Unit
        }
    }

    /** Signs out. */
    suspend fun signOut(): Result<Unit> {
        return runCatching {
            auth.signOut()
            Unit
        }
    }

    private suspend fun resolveEmailFromUsername(username: String): String? {
        userPrefs.usernameMap()[username]?.let { return it }
        return runCatching {
            client.from("profiles").select().decodeList<ProfileRow>()
                .firstOrNull { it.username == username }?.email
        }.getOrNull()
    }

    private fun UserInfo.toAuthUser(): AuthUser {
        val metadata = userMetadata
        val username = metadata?.get("username")?.jsonPrimitive?.contentOrNull
        val displayName = metadata?.get("display_name")?.jsonPrimitive?.contentOrNull
            ?: username
            ?: email?.substringBefore("@")
            ?: "User"
        return AuthUser(
            id = id,
            email = email.orEmpty(),
            displayName = displayName,
            avatarUrl = metadata?.get("avatar_url")?.jsonPrimitive?.contentOrNull,
            discordConnected = identities.orEmpty().any { it.provider == "discord" }
        )
    }

    companion object {
        /** OAuth redirect, registered on the callback activity. */
        const val AUTH_CALLBACK_URL = "com.waveyy.app://auth/callback"

        private val USERNAME_REGEX = Regex("^[a-zA-Z0-9_]{3,30}$")

        /** Matches `normalizeUsername` in `auth-provider.jsx`. */
        fun normalizeUsername(value: String): String {
            return value.trim().lowercase().replace(Regex("\\s+"), "_")
        }
    }
}
