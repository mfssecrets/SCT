package com.example.data.supabase

import android.content.Context
import io.github.jan-tennert.supabase.SupabaseClient as SbClient
import io.github.jan-tennert.supabase.auth.Auth
import io.github.jan-tennert.supabase.auth.auth
import io.github.jan-tennert.supabase.createSupabaseClient
import io.github.jan-tennert.supabase.postgrest.Postgrest
import io.github.jan-tennert.supabase.postgrest.postgrest
import io.github.jan-tennert.supabase.realtime.Realtime
import io.github.jan-tennert.supabase.realtime.realtime
import io.github.jan-tennert.supabase.storage.Storage
import io.github.jan-tennert.supabase.storage.storage

/**
 * Singleton Supabase client for the entire application.
 * Uses BuildConfig values injected from .env via Secrets Gradle Plugin.
 */
object SupabaseClient {

    private var _client: SbClient? = null

    val client: SbClient
        get() {
            requireNotNull(_client) { "SupabaseClient not initialized. Call initialize() first." }
            return _client!!
        }

    fun initialize(context: Context) {
        if (_client != null) return
        _client = createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY
        ) {
            install(Auth)
            install(Postgrest)
            install(Realtime)
            install(Storage)
        }
    }

    val auth get() = client.auth
    val postgrest get() = client.postgrest
    val realtime get() = client.realtime
    val storage get() = client.storage
}
