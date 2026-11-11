package com.example.data

import com.example.data.local.NightTaskDao
import com.example.data.local.OAuthPreferencesRepository
import com.example.data.local.OAuthSettingsState
import com.example.data.remote.GoogleCalendarService
import com.example.model.ExternalCalendarEvent
import com.example.model.NightTaskEntity
import com.example.model.SyncState
import kotlinx.coroutines.flow.Flow

data class BatchSyncSummary(
    val totalCandidates: Int,
    val syncedSuccessfully: Int,
    val failedCount: Int,
    val firstErrorMessage: String? = null
)

class NightPlannerRepository(
    private val dao: NightTaskDao,
    private val oAuthPrefs: OAuthPreferencesRepository,
    val googleCalendarService: GoogleCalendarService
) {
    val oAuthSettingsFlow: Flow<OAuthSettingsState> = oAuthPrefs.settingsFlow

    fun observeAllTasks(): Flow<List<NightTaskEntity>> = dao.observeAllTasks()

    fun observeTasksForDate(dateIso: String): Flow<List<NightTaskEntity>> =
        dao.observeTasksForDate(dateIso)

    suspend fun getTasksForDateOnce(dateIso: String): List<NightTaskEntity> =
        dao.getTasksForDateOnce(dateIso)

    /**
     * Creates a new task locally in SQLite (Room) and, if `syncImmediatelyIfConnected` is true
     * and the user has connected Google Calendar, creates the event in Google Calendar right away
     * and stores the returned `eventId`.
     */
    suspend fun createTask(
        task: NightTaskEntity,
        syncImmediatelyIfConnected: Boolean = true
    ): Pair<NightTaskEntity, String?> {
        val initialSyncState = if (task.syncWithCalendar) {
            SyncState.PENDING_SYNC.name
        } else {
            SyncState.LOCAL_ONLY.name
        }
        val insertedId = dao.insertTask(task.copy(syncState = initialSyncState))
        val savedTask = task.copy(id = insertedId, syncState = initialSyncState)

        val settings = oAuthPrefs.getCurrentSettings()
        if (task.syncWithCalendar && syncImmediatelyIfConnected && settings.isConnected) {
            val createResult = googleCalendarService.createCalendarEvent(savedTask, settings)
            return if (createResult.isSuccess) {
                val remoteEvent = createResult.getOrThrow()
                val updated = savedTask.copy(
                    googleEventId = remoteEvent.id,
                    googleHtmlLink = remoteEvent.htmlLink,
                    syncState = SyncState.SYNCED.name
                )
                dao.updateTask(updated)
                updated to null
            } else {
                val errored = savedTask.copy(syncState = SyncState.SYNC_ERROR.name)
                dao.updateTask(errored)
                errored to createResult.exceptionOrNull()?.message
            }
        }
        return savedTask to null
    }

    /**
     * Updates an existing task (title, time block, category, priority, or completion status).
     * If the task is synced with Google Calendar, calls `updateCalendarEvent` (`events.patch`)
     * or `createCalendarEvent` if it didn't have an `eventId` yet.
     */
    suspend fun updateTaskAndSync(task: NightTaskEntity): String? {
        val settings = oAuthPrefs.getCurrentSettings()

        if (!task.syncWithCalendar) {
            // If user disabled calendar sync on a previously synced task, delete the remote event
            if (!task.googleEventId.isNullOrBlank() && settings.isConnected) {
                googleCalendarService.deleteCalendarEvent(task.googleEventId)
            }
            dao.updateTask(
                task.copy(
                    googleEventId = null,
                    googleHtmlLink = null,
                    syncState = SyncState.LOCAL_ONLY.name
                )
            )
            return null
        }

        // Save immediately locally for zero-latency UI
        dao.updateTask(task.copy(syncState = SyncState.PENDING_SYNC.name))

        if (!settings.isConnected) {
            return null
        }

        val remoteResult = if (!task.googleEventId.isNullOrBlank()) {
            googleCalendarService.updateCalendarEvent(task.googleEventId, task, settings)
        } else {
            googleCalendarService.createCalendarEvent(task, settings)
        }

        return if (remoteResult.isSuccess) {
            val dto = remoteResult.getOrThrow()
            dao.updateTask(
                task.copy(
                    googleEventId = dto.id,
                    googleHtmlLink = dto.htmlLink,
                    syncState = SyncState.SYNCED.name
                )
            )
            null
        } else {
            dao.updateTask(task.copy(syncState = SyncState.SYNC_ERROR.name))
            remoteResult.exceptionOrNull()?.message
        }
    }

    /**
     * Deletes a task from local SQLite and, if it has a `googleEventId`, deletes it from Google Calendar
     * via `events.delete`.
     */
    suspend fun deleteTaskAndSync(task: NightTaskEntity): String? {
        dao.deleteTaskById(task.id)
        val settings = oAuthPrefs.getCurrentSettings()
        if (!task.googleEventId.isNullOrBlank() && settings.isConnected) {
            val delResult = googleCalendarService.deleteCalendarEvent(task.googleEventId)
            if (delResult.isFailure) {
                return delResult.exceptionOrNull()?.message
            }
        }
        return null
    }

    /**
     * Batch synchronization triggered by "Confirmar y Sincronizar Día Siguiente".
     * Sends all tasks for `targetDateIso` with `syncWithCalendar = true` to the user's `primary`
     * Google Calendar, creating new events or patching existing ones, and storing `googleEventId`.
     */
    suspend fun confirmAndSyncDateTasks(targetDateIso: String): BatchSyncSummary {
        val settings = oAuthPrefs.getCurrentSettings()
        val allTasks = dao.getTasksForDateOnce(targetDateIso)
        val candidates = allTasks.filter { it.syncWithCalendar }

        if (candidates.isEmpty()) {
            return BatchSyncSummary(0, 0, 0, "No hay tareas marcadas con sincronización para esta fecha.")
        }
        if (!settings.isConnected) {
            return BatchSyncSummary(
                totalCandidates = candidates.size,
                syncedSuccessfully = 0,
                failedCount = candidates.size,
                firstErrorMessage = "Conecta primero tu cuenta de Google Calendar (OAuth 2.0) en la pestaña 'Google Calendar'."
            )
        }

        var successCount = 0
        var failCount = 0
        var firstError: String? = null

        for (task in candidates) {
            val result = if (task.googleEventId.isNullOrBlank()) {
                googleCalendarService.createCalendarEvent(task, settings)
            } else {
                googleCalendarService.updateCalendarEvent(task.googleEventId, task, settings)
            }

            if (result.isSuccess) {
                val dto = result.getOrThrow()
                dao.updateTask(
                    task.copy(
                        googleEventId = dto.id,
                        googleHtmlLink = dto.htmlLink,
                        syncState = SyncState.SYNCED.name
                    )
                )
                successCount++
            } else {
                dao.updateTask(task.copy(syncState = SyncState.SYNC_ERROR.name))
                failCount++
                if (firstError == null) {
                    firstError = result.exceptionOrNull()?.message
                }
            }
        }

        return BatchSyncSummary(
            totalCandidates = candidates.size,
            syncedSuccessfully = successCount,
            failedCount = failCount,
            firstErrorMessage = firstError
        )
    }

    /**
     * "Migrar pendientes a mañana":
     * Moves all incomplete tasks from `todayIso` to `tomorrowIso` in a single operation
     * and patches their Google Calendar events to tomorrow's date if connected.
     */
    suspend fun migratePendingTasksToTomorrow(
        todayIso: String,
        tomorrowIso: String
    ): Pair<Int, Int> {
        val todayTasks = dao.getTasksForDateOnce(todayIso)
        val pending = todayTasks.filter { !it.completed }
        if (pending.isEmpty()) return 0 to 0

        val migratedCount = dao.migrateIncompleteTasks(todayIso, tomorrowIso)
        val settings = oAuthPrefs.getCurrentSettings()
        var calendarUpdatedCount = 0

        if (settings.isConnected) {
            val updatedTomorrowTasks = dao.getTasksForDateOnce(tomorrowIso)
            for (task in updatedTomorrowTasks) {
                if (task.syncWithCalendar && !task.googleEventId.isNullOrBlank()) {
                    val patchRes = googleCalendarService.updateCalendarEvent(
                        eventId = task.googleEventId,
                        task = task,
                        settings = settings
                    )
                    if (patchRes.isSuccess) {
                        dao.updateTask(task.copy(syncState = SyncState.SYNCED.name))
                        calendarUpdatedCount++
                    }
                }
            }
        }
        return migratedCount to calendarUpdatedCount
    }

    suspend fun fetchTomorrowExternalEvents(targetDateIso: String): Result<List<ExternalCalendarEvent>> {
        return googleCalendarService.listTomorrowEvents(targetDateIso)
    }
}
