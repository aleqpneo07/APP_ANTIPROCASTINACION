package com.example.data.remote

import android.net.Uri
import android.util.Base64
import com.example.data.local.OAuthPreferencesRepository
import com.example.data.local.OAuthSettingsState
import com.example.model.ExternalCalendarEvent
import com.example.model.NightTaskEntity
import com.example.model.QuickInputParser
import com.example.util.DateTimeHelper
import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.concurrent.TimeUnit

// --- DTOs for Google Calendar API v3 ---

data class GoogleEventDateTimeDto(
    @Json(name = "dateTime") val dateTime: String? = null,
    @Json(name = "date") val date: String? = null,
    @Json(name = "timeZone") val timeZone: String? = null
)

data class GoogleCalendarEventRequest(
    @Json(name = "summary") val summary: String,
    @Json(name = "description") val description: String? = null,
    @Json(name = "colorId") val colorId: String? = null,
    @Json(name = "start") val start: GoogleEventDateTimeDto,
    @Json(name = "end") val end: GoogleEventDateTimeDto
)

data class GoogleCalendarEventResponse(
    @Json(name = "id") val id: String,
    @Json(name = "status") val status: String? = null,
    @Json(name = "htmlLink") val htmlLink: String? = null,
    @Json(name = "summary") val summary: String? = null,
    @Json(name = "description") val description: String? = null,
    @Json(name = "location") val location: String? = null,
    @Json(name = "colorId") val colorId: String? = null,
    @Json(name = "start") val start: GoogleEventDateTimeDto? = null,
    @Json(name = "end") val end: GoogleEventDateTimeDto? = null
)

data class GoogleCalendarEventsListResponse(
    @Json(name = "kind") val kind: String? = null,
    @Json(name = "summary") val summary: String? = null,
    @Json(name = "items") val items: List<GoogleCalendarEventResponse>? = emptyList()
)

data class GoogleOAuthTokenResponse(
    @Json(name = "access_token") val accessToken: String,
    @Json(name = "expires_in") val expiresIn: Long = 3600L,
    @Json(name = "refresh_token") val refreshToken: String? = null,
    @Json(name = "scope") val scope: String? = null,
    @Json(name = "token_type") val tokenType: String? = "Bearer"
)

interface GoogleCalendarRetrofitApi {

    @GET("calendar/v3/calendars/primary/events")
    suspend fun listPrimaryEvents(
        @Header("Authorization") bearerToken: String,
        @Query("timeMin") timeMinRfc3339: String,
        @Query("timeMax") timeMaxRfc3339: String,
        @Query("singleEvents") singleEvents: Boolean = true,
        @Query("orderBy") orderBy: String = "startTime",
        @Query("maxResults") maxResults: Int = 50
    ): Response<GoogleCalendarEventsListResponse>

    @POST("calendar/v3/calendars/primary/events")
    suspend fun insertPrimaryEvent(
        @Header("Authorization") bearerToken: String,
        @Body body: GoogleCalendarEventRequest
    ): Response<GoogleCalendarEventResponse>

    @PATCH("calendar/v3/calendars/primary/events/{eventId}")
    suspend fun patchPrimaryEvent(
        @Header("Authorization") bearerToken: String,
        @Path("eventId") eventId: String,
        @Body body: GoogleCalendarEventRequest
    ): Response<GoogleCalendarEventResponse>

    @DELETE("calendar/v3/calendars/primary/events/{eventId}")
    suspend fun deletePrimaryEvent(
        @Header("Authorization") bearerToken: String,
        @Path("eventId") eventId: String
    ): Response<Unit>
}

interface GoogleOAuthRetrofitApi {
    @FormUrlEncoded
    @POST("token")
    suspend fun exchangeAuthorizationCode(
        @Field("client_id") clientId: String,
        @Field("code") code: String,
        @Field("code_verifier") codeVerifier: String,
        @Field("redirect_uri") redirectUri: String,
        @Field("grant_type") grantType: String = "authorization_code"
    ): Response<GoogleOAuthTokenResponse>

    @FormUrlEncoded
    @POST("token")
    suspend fun refreshAccessToken(
        @Field("client_id") clientId: String,
        @Field("refresh_token") refreshToken: String,
        @Field("grant_type") grantType: String = "refresh_token"
    ): Response<GoogleOAuthTokenResponse>
}

/**
 * Full-featured Google Calendar API v3 Service (`googleCalendarService`)
 * implementing:
 * - `listTomorrowEvents`
 * - `createCalendarEvent`
 * - `updateCalendarEvent`
 * - `deleteCalendarEvent`
 * - OAuth 2.0 PKCE Authorization & Token Refresh management
 */
class GoogleCalendarService(
    private val oAuthPrefs: OAuthPreferencesRepository
) {
    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .addInterceptor(
            HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }
        )
        .build()

    private val calendarApi: GoogleCalendarRetrofitApi = Retrofit.Builder()
        .baseUrl("https://www.googleapis.com/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(GoogleCalendarRetrofitApi::class.java)

    private val oAuthApi: GoogleOAuthRetrofitApi = Retrofit.Builder()
        .baseUrl("https://oauth2.googleapis.com/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(GoogleOAuthRetrofitApi::class.java)

    companion object {
        const val REDIRECT_URI = "com.aistudio.nightplanner.vxkpmq:/oauth2redirect"
        const val COMPLETED_PREFIX = "✅ "
        const val COMPLETED_GOOGLE_COLOR_ID = "10" // Basil / Completed Green
    }

    /**
     * Ensures we have a valid Bearer token, refreshing automatically via `refresh_token` if needed.
     */
    private suspend fun getValidBearerHeader(): Result<String> {
        val settings = oAuthPrefs.getCurrentSettings()
        if (settings.accessToken.isBlank()) {
            return Result.failure(
                IllegalStateException("No hay sesión activa de Google Calendar. Conecta tu cuenta OAuth 2.0 en la pestaña 'Google Calendar'.")
            )
        }

        val now = System.currentTimeMillis()
        if (settings.tokenExpiryMillis in 1..now) {
            if (settings.refreshToken.isNotBlank()) {
                val refreshResp = runCatching {
                    oAuthApi.refreshAccessToken(
                        clientId = settings.clientId,
                        refreshToken = settings.refreshToken
                    )
                }.getOrNull()

                if (refreshResp != null && refreshResp.isSuccessful && refreshResp.body() != null) {
                    val body = refreshResp.body()!!
                    oAuthPrefs.saveOAuthTokens(
                        accessToken = body.accessToken,
                        refreshToken = body.refreshToken,
                        expiresInSeconds = body.expiresIn
                    )
                    return Result.success("Bearer ${body.accessToken}")
                }
            }
            return Result.failure(
                IllegalStateException("El Access Token de Google Calendar ha expirado. Renueva tu sesión en la pestaña 'Calendar'.")
            )
        }

        return Result.success("Bearer ${settings.accessToken}")
    }

    /**
     * 1. `listTomorrowEvents`:
     * Reads all existing events from the user's `primary` Google Calendar for the target date
     * (`mañana` by default) to prevent schedule overlaps and compute free slots.
     */
    suspend fun listTomorrowEvents(
        targetDateIso: String = DateTimeHelper.tomorrowIso()
    ): Result<List<ExternalCalendarEvent>> = withContext(Dispatchers.IO) {
        val bearerResult = getValidBearerHeader()
        if (bearerResult.isFailure) {
            return@withContext Result.failure(bearerResult.exceptionOrNull()!!)
        }
        val bearer = bearerResult.getOrThrow()
        val (timeMin, timeMax) = DateTimeHelper.getDayRfc3339Bounds(targetDateIso)

        runCatching {
            val response = calendarApi.listPrimaryEvents(
                bearerToken = bearer,
                timeMinRfc3339 = timeMin,
                timeMaxRfc3339 = timeMax
            )
            if (!response.isSuccessful) {
                val errBody = response.errorBody()?.string()?.take(240) ?: "HTTP ${response.code()}"
                throw IllegalStateException("Error al consultar Google Calendar (${response.code()}): $errBody")
            }

            val items = response.body()?.items.orEmpty()
            items
                .filter { it.status != "cancelled" }
                .map { dto ->
                    val startDt = dto.start?.dateTime
                    val endDt = dto.end?.dateTime
                    val isAllDay = startDt.isNullOrBlank() && !dto.start?.date.isNullOrBlank()
                    val isManagedByNightPlanner = dto.description?.contains("[NightPlanner]") == true
                    ExternalCalendarEvent(
                        id = dto.id,
                        title = dto.summary?.ifBlank { "(Sin título)" } ?: "(Sin título)",
                        dateIso = targetDateIso,
                        startTimeHHmm = DateTimeHelper.extractHHmmFromRfc3339(startDt),
                        endTimeHHmm = DateTimeHelper.extractHHmmFromRfc3339(endDt),
                        isAllDay = isAllDay,
                        location = dto.location,
                        htmlLink = dto.htmlLink,
                        isFromNightPlanner = isManagedByNightPlanner
                    )
                }
        }
    }

    /**
     * 2. `createCalendarEvent`:
     * Creates a new event in the user's `primary` Google Calendar.
     * - If the task has `startTime` and `endTime` (or `durationMinutes`), creates a timed block (`start.dateTime` & `end.dateTime`).
     * - If the task has no specific time, creates either an All-Day event (`start.date`) or uses the user's default block hour.
     */
    suspend fun createCalendarEvent(
        task: NightTaskEntity,
        settings: OAuthSettingsState
    ): Result<GoogleCalendarEventResponse> = withContext(Dispatchers.IO) {
        val bearerResult = getValidBearerHeader()
        if (bearerResult.isFailure) {
            return@withContext Result.failure(bearerResult.exceptionOrNull()!!)
        }
        val bearer = bearerResult.getOrThrow()
        val requestBody = buildEventRequestPayload(task, settings)

        runCatching {
            val response = calendarApi.insertPrimaryEvent(
                bearerToken = bearer,
                body = requestBody
            )
            if (!response.isSuccessful || response.body() == null) {
                val errBody = response.errorBody()?.string()?.take(240) ?: "HTTP ${response.code()}"
                throw IllegalStateException("No se pudo crear el evento en Google Calendar (${response.code()}): $errBody")
            }
            oAuthPrefs.recordSyncSuccess()
            response.body()!!
        }
    }

    /**
     * 3. `updateCalendarEvent`:
     * Updates an existing event in Google Calendar via `events.patch`.
     * Automatically adds/removes the "✅ " completion prefix and updates `colorId` when completed,
     * and updates the time block or date if edited or migrated to tomorrow.
     */
    suspend fun updateCalendarEvent(
        eventId: String,
        task: NightTaskEntity,
        settings: OAuthSettingsState
    ): Result<GoogleCalendarEventResponse> = withContext(Dispatchers.IO) {
        val bearerResult = getValidBearerHeader()
        if (bearerResult.isFailure) {
            return@withContext Result.failure(bearerResult.exceptionOrNull()!!)
        }
        val bearer = bearerResult.getOrThrow()
        val requestBody = buildEventRequestPayload(task, settings)

        runCatching {
            val response = calendarApi.patchPrimaryEvent(
                bearerToken = bearer,
                eventId = eventId,
                body = requestBody
            )
            if (!response.isSuccessful || response.body() == null) {
                val errBody = response.errorBody()?.string()?.take(240) ?: "HTTP ${response.code()}"
                throw IllegalStateException("No se pudo actualizar el evento en Calendar (${response.code()}): $errBody")
            }
            oAuthPrefs.recordSyncSuccess()
            response.body()!!
        }
    }

    /**
     * 4. `deleteCalendarEvent`:
     * Removes an event from the user's `primary` Google Calendar via `events.delete`.
     */
    suspend fun deleteCalendarEvent(
        eventId: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val bearerResult = getValidBearerHeader()
        if (bearerResult.isFailure) {
            return@withContext Result.failure(bearerResult.exceptionOrNull()!!)
        }
        val bearer = bearerResult.getOrThrow()

        runCatching {
            val response = calendarApi.deletePrimaryEvent(
                bearerToken = bearer,
                eventId = eventId
            )
            // 204 No Content or 410 Gone both mean the event is deleted
            if (!response.isSuccessful && response.code() != 410 && response.code() != 404) {
                val errBody = response.errorBody()?.string()?.take(240) ?: "HTTP ${response.code()}"
                throw IllegalStateException("No se pudo eliminar el evento de Calendar (${response.code()}): $errBody")
            }
            oAuthPrefs.recordSyncSuccess()
        }
    }

    /**
     * Builds the exact `GoogleCalendarEventRequest` respecting:
     * - Time-blocking (`start.dateTime` and `end.dateTime`) vs All-day (`start.date` and `end.date`)
     * - Completion prefix (`✅ `) and `colorId`
     */
    private fun buildEventRequestPayload(
        task: NightTaskEntity,
        settings: OAuthSettingsState
    ): GoogleCalendarEventRequest {
        val baseTitle = task.title.removePrefix(COMPLETED_PREFIX).trim()
        val finalSummary = if (task.completed) "$COMPLETED_PREFIX$baseTitle" else baseTitle
        val finalColorId = if (task.completed) {
            COMPLETED_GOOGLE_COLOR_ID
        } else {
            task.categoryEnum.googleColorId
        }

        val descriptionBuilder = StringBuilder()
        descriptionBuilder.append("[NightPlanner] Categoría: ${task.categoryEnum.displayName} | Prioridad: ${task.priorityEnum.displayName}")
        if (task.completed) {
            descriptionBuilder.append(" | Estado: COMPLETADA ✅")
        }
        if (task.notes.isNotBlank()) {
            descriptionBuilder.append("\n\nNotas:\n${task.notes}")
        }

        val tzId = DateTimeHelper.getLocalTimeZoneId()

        val resolvedStartHHmm = task.startTime?.takeIf { it.isNotBlank() }
        val resolvedEndHHmm = when {
            !task.endTime.isNullOrBlank() -> task.endTime
            resolvedStartHHmm != null -> {
                val dur = task.durationMinutes ?: settings.defaultBlockDurationMin
                QuickInputParser.addMinutesToHHmm(resolvedStartHHmm, dur)
            }
            else -> null
        }

        val (startDto, endDto) = if (resolvedStartHHmm != null && resolvedEndHHmm != null) {
            // Exact Time-Blocked Event
            val startRfc = DateTimeHelper.toRfc3339DateTime(task.targetDateIso, resolvedStartHHmm)
            val endRfc = DateTimeHelper.toRfc3339DateTime(task.targetDateIso, resolvedEndHHmm)
            GoogleEventDateTimeDto(dateTime = startRfc, timeZone = tzId) to
                GoogleEventDateTimeDto(dateTime = endRfc, timeZone = tzId)
        } else if (settings.unscheduledAsAllDay) {
            // All-Day Event (`start.date` and exclusive `end.date`)
            val nextDayIso = DateTimeHelper.addDaysToIso(task.targetDateIso, 1)
            GoogleEventDateTimeDto(date = task.targetDateIso) to
                GoogleEventDateTimeDto(date = nextDayIso)
        } else {
            // Configurable Default Time Block
            val defStart = "%02d:00".format(settings.defaultBlockStartHour.coerceIn(6, 22))
            val dur = task.durationMinutes ?: settings.defaultBlockDurationMin
            val defEnd = QuickInputParser.addMinutesToHHmm(defStart, dur)
            val startRfc = DateTimeHelper.toRfc3339DateTime(task.targetDateIso, defStart)
            val endRfc = DateTimeHelper.toRfc3339DateTime(task.targetDateIso, defEnd)
            GoogleEventDateTimeDto(dateTime = startRfc, timeZone = tzId) to
                GoogleEventDateTimeDto(dateTime = endRfc, timeZone = tzId)
        }

        return GoogleCalendarEventRequest(
            summary = finalSummary,
            description = descriptionBuilder.toString(),
            colorId = finalColorId,
            start = startDto,
            end = endDto
        )
    }

    /**
     * Generates a PKCE-secured OAuth 2.0 Authorization URL for `https://www.googleapis.com/auth/calendar.events`.
     */
    suspend fun buildPkceAuthorizationUrl(clientId: String): String {
        val verifierBytes = ByteArray(32)
        SecureRandom().nextBytes(verifierBytes)
        val codeVerifier = Base64.encodeToString(
            verifierBytes,
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
        )
        oAuthPrefs.savePkceVerifier(codeVerifier)

        val digest = MessageDigest.getInstance("SHA-256").digest(codeVerifier.toByteArray(Charsets.US_ASCII))
        val codeChallenge = Base64.encodeToString(
            digest,
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
        )

        return Uri.parse("https://accounts.google.com/o/oauth2/v2/auth")
            .buildUpon()
            .appendQueryParameter("client_id", clientId.trim())
            .appendQueryParameter("redirect_uri", REDIRECT_URI)
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("scope", OAuthSettingsState.CALENDAR_EVENTS_SCOPE)
            .appendQueryParameter("code_challenge", codeChallenge)
            .appendQueryParameter("code_challenge_method", "S256")
            .appendQueryParameter("access_type", "offline")
            .appendQueryParameter("prompt", "consent")
            .build()
            .toString()
    }

    /**
     * Handles the OAuth 2.0 redirect URI returning either an authorization `code` (PKCE) or `#access_token=`.
     */
    suspend fun handleOAuthRedirectUri(uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        // Check if implicit fragment contains access_token
        val fragment = uri.fragment.orEmpty()
        if (fragment.contains("access_token=")) {
            val params = fragment.split("&").associate {
                val kv = it.split("=", limit = 2)
                kv[0] to (kv.getOrNull(1) ?: "")
            }
            val token = params["access_token"].orEmpty()
            val expires = params["expires_in"]?.toLongOrNull() ?: 3600L
            if (token.isNotBlank()) {
                oAuthPrefs.saveOAuthTokens(accessToken = token, expiresInSeconds = expires)
                return@withContext Result.success("Token OAuth 2.0 recibido y vinculado con éxito.")
            }
        }

        // Check authorization code
        val code = uri.getQueryParameter("code")
        val error = uri.getQueryParameter("error")
        if (!error.isNullOrBlank()) {
            return@withContext Result.failure(IllegalStateException("Autorización OAuth cancelada o denegada: $error"))
        }
        if (code.isNullOrBlank()) {
            return@withContext Result.failure(IllegalArgumentException("No se encontró código de autorización en el redirect."))
        }

        val settings = oAuthPrefs.getCurrentSettings()
        val verifier = oAuthPrefs.getPkceVerifier().orEmpty()

        runCatching {
            val response = oAuthApi.exchangeAuthorizationCode(
                clientId = settings.clientId,
                code = code,
                codeVerifier = verifier,
                redirectUri = REDIRECT_URI
            )
            if (!response.isSuccessful || response.body() == null) {
                val err = response.errorBody()?.string()?.take(200) ?: "HTTP ${response.code()}"
                throw IllegalStateException("Fallo en intercambio de código OAuth: $err")
            }
            val tokenBody = response.body()!!
            oAuthPrefs.saveOAuthTokens(
                accessToken = tokenBody.accessToken,
                refreshToken = tokenBody.refreshToken,
                expiresInSeconds = tokenBody.expiresIn
            )
            "Cuenta de Google Calendar vinculada correctamente mediante OAuth 2.0."
        }
    }
}
