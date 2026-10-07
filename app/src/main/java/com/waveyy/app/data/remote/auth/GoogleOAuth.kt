package com.waveyy.app.data.remote.auth

import io.github.jan.supabase.auth.providers.OAuthProvider

/**
 * Google as an OAuth *authorization-code* provider. The SDK only ships Google
 * as an ID-token provider (`IDTokenProvider`, for native Credential Manager
 * flows), so this object drives the Custom Tabs web flow instead — matching
 * the web app's Google OAuth — with PKCE managed by the SDK.
 */
object GoogleOAuth : OAuthProvider() {
    override val name: String = "google"
}
