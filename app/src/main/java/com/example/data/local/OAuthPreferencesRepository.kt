package com.example.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.oAuthDataStore by preferencesDataStore(name = "nightplanner_oauth_prefs")

data class OAuthSettingsState(
    val clientId: String = DEFAULT_PROVISIONED_CLIENT_ID,
    val accessToken: String = "",
    val refreshToken: String = "",
    val tokenExpiryMillis: Long = 0L,
    val connectedEmail: String = "",
    val unscheduledAsAllDay: Boolean = true,
    val defaultBlockStartHour: Int = 9,
    val defaultBlockDurationMin: Int = 60,
    val lastSyncTimestampMillis: Long = 0L
) {
    val isConnected: Boolean
        get() = accessToken.isNotBlank()

    companion object {
        // Provisioned via AI Studio OAuth Setup for project powerful-asset-9fx4d
        const val DEFAULT_PROVISIONED_CLIENT_ID =
            "385055120883-le9a37e7j192nb2nf1p7embqsv11o621.apps.googleusercontent.com"
        const val CALENDAR_EVENTS_SCOPE = "https://www.googleapis.com/auth/calendar.events"
    }
}

class OAuthPreferencesRepository(private val context: Context) {

    private object Keys {
        val CLIENT_ID = stringPreferencesKey("oauth_client_id")
        val ACCESS_TOKEN = stringPreferencesKey("oauth_access_token")
        val REFRESH_TOKEN = stringPreferencesKey("oauth_refresh_token")
        val TOKEN_EXPIRY = longPreferencesKey("oauth_token_expiry_ms")
        val CONNECTED_EMAIL = stringPreferencesKey("oauth_connected_email")
        val UNSCHEDULED_ALL_DAY = booleanPreferencesKey("unscheduled_all_day")
        val DEFAULT_START_HOUR = intPreferencesKey("default_block_start_hour")
        val DEFAULT_DURATION_MIN = intPreferencesKey("default_block_duration_min")
        val LAST_SYNC_MS = longPreferencesKey("last_sync_timestamp_ms")
        val PKCE_VERIFIER = stringPreferencesKey("pkce_code_verifier")
    }

    val settingsFlow: Flow<OAuthSettingsState> = context.oAuthDataStore.data.map { prefs ->
        OAuthSettingsState(
            clientId = prefs[Keys.CLIENT_ID]?.takeIf { it.isNotBlank() }
                ?: OAuthSettingsState.DEFAULT_PROVISIONED_CLIENT_ID,
            accessToken = prefs[Keys.ACCESS_TOKEN] ?: "",
            refreshToken = prefs[Keys.REFRESH_TOKEN] ?: "",
            tokenExpiryMillis = prefs[Keys.TOKEN_EXPIRY] ?: 0L,
            connectedEmail = prefs[Keys.CONNECTED_EMAIL] ?: "",
            unscheduledAsAllDay = prefs[Keys.UNSCHEDULED_ALL_DAY] ?: true,
            defaultBlockStartHour = prefs[Keys.DEFAULT_START_HOUR] ?: 9,
            defaultBlockDurationMin = prefs[Keys.DEFAULT_DURATION_MIN] ?: 60,
            lastSyncTimestampMillis = prefs[Keys.LAST_SYNC_MS] ?: 0L
        )
    }

    suspend fun getCurrentSettings(): OAuthSettingsState = settingsFlow.first()

    suspend fun saveOAuthTokens(
        accessToken: String,
        refreshToken: String? = null,
        expiresInSeconds: Long = 3600L,
        email: String? = null
    ) {
        context.oAuthDataStore.edit { prefs ->
            prefs[Keys.ACCESS_TOKEN] = accessToken.trim()
            if (!refreshToken.isNullOrBlank()) {
                prefs[Keys.REFRESH_TOKEN] = refreshToken.trim()
            }
            prefs[Keys.TOKEN_EXPIRY] = System.currentTimeMillis() + (expiresInSeconds * 1000L)
            if (!email.isNullOrBlank()) {
                prefs[Keys.CONNECTED_EMAIL] = email.trim()
            }
        }
    }

    suspend fun updateClientId(clientId: String) {
        context.oAuthDataStore.edit { prefs ->
            prefs[Keys.CLIENT_ID] = clientId.trim().ifBlank {
                OAuthSettingsState.DEFAULT_PROVISIONED_CLIENT_ID
            }
        }
    }

    suspend fun updateUnscheduledPolicy(
        asAllDay: Boolean,
        defaultStartHour: Int,
        defaultDurationMin: Int
    ) {
        context.oAuthDataStore.edit { prefs ->
            prefs[Keys.UNSCHEDULED_ALL_DAY] = asAllDay
            prefs[Keys.DEFAULT_START_HOUR] = defaultStartHour.coerceIn(6, 22)
            prefs[Keys.DEFAULT_DURATION_MIN] = defaultDurationMin.coerceIn(15, 240)
        }
    }

    suspend fun recordSyncSuccess() {
        context.oAuthDataStore.edit { prefs ->
            prefs[Keys.LAST_SYNC_MS] = System.currentTimeMillis()
        }
    }

    suspend fun savePkceVerifier(verifier: String) {
        context.oAuthDataStore.edit { prefs ->
            prefs[Keys.PKCE_VERIFIER] = verifier
        }
    }

    suspend fun getPkceVerifier(): String? {
        return context.oAuthDataStore.data.first()[Keys.PKCE_VERIFIER]
    }

    suspend fun disconnectAccount() {
        context.oAuthDataStore.edit { prefs ->
            prefs.remove(Keys.ACCESS_TOKEN)
            prefs.remove(Keys.REFRESH_TOKEN)
            prefs.remove(Keys.TOKEN_EXPIRY)
            prefs.remove(Keys.CONNECTED_EMAIL)
        }
    }
}
