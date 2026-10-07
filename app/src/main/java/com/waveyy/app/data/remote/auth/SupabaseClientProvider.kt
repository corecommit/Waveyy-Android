package com.waveyy.app.data.remote.auth

import com.waveyy.app.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Lazily built Supabase client from `BuildConfig` credentials. Built on
 * first use so misconfiguration surfaces at the call site, not at startup.
 *
 * Guest mode: with placeholder values (or an unreachable project URL) the
 * client still constructs — every repository call wraps network use in
 * `runCatching`, and [AuthRepository.currentUser] never throws — so the app
 * launches and browses anonymously.
 */
@Singleton
class SupabaseClientProvider @Inject constructor() {

    /** True when both URL and key look configured (non-blank). */
    val isConfigured: Boolean
        get() = BuildConfig.SUPABASE_URL.isNotBlank() &&
            BuildConfig.SUPABASE_ANON_KEY.isNotBlank()

    /** Shared client with Auth + PostgREST installed. */
    val client: SupabaseClient by lazy {
        createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY
        ) {
            install(Auth)
            install(Postgrest)
        }
    }
}
