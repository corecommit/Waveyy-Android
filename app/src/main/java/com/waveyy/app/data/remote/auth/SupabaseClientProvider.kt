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
 */
@Singleton
class SupabaseClientProvider @Inject constructor() {

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
