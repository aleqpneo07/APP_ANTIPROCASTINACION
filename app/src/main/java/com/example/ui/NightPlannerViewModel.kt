package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.NightPlannerRepository
import com.example.data.local.NightPlannerDatabase
import com.example.data.local.OAuthPreferencesRepository
import com.example.data.local.OAuthSettingsState
import com.example.data.remote.GoogleCalendarService
import com.example.model.ExternalCalendarEvent
import com.example.model.FreeTimeSlot
import com.example.model.NightTaskEntity
import com.example.model.OverlapWarning
import com.example.model.QuickInputParser
import com.example.model.TaskCategory
import com.example.model.TaskPriority
import com.example.util.DateTimeHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class NightPlannerTab(val titleEs: String) {
    PLAN_TOMORROW("Planificar Mañana"),
    EXECUTE_TODAY("Hoy"),
    TIMELINE_CONFLICTS("Agenda 24h"),
    GOOGLE_CALENDAR_GUIDE("Google Calendar")
}

data class NightPlannerUiState(
    val activeTab: NightPlannerTab = NightPlannerTab.PLAN_TOMORROW,
    val todayDateIso: String = DateTimeHelper.todayIso(),
    val tomorrowDateIso: String = DateTimeHelper.tomorrowIso(),
    val todayHumanEs: String = DateTimeHelper.formatHumanDateEs(DateTimeHelper.todayIso()),
    val tomorrowHumanEs: String = DateTimeHelper.formatHumanDateEs(DateTimeHelper.tomorrowIso()),
    val todayTasks: List<NightTaskEntity> = emptyList(),
    val tomorrowTasks: List<NightTaskEntity> = emptyList(),
    val selectedTodayCategoryFilter: TaskCategory? = null,
    val selectedTomorrowCategoryFilter: TaskCategory? = null,
    val importedTomorrowEvents: List<ExternalCalendarEvent> = emptyList(),
    val overlapWarnings: List<OverlapWarning> = emptyList(),
    val freeTimeSlots: List<FreeTimeSlot> = emptyList(),
    val oAuthSettings: OAuthSettingsState = OAuthSettingsState(),
    val isSyncing: Boolean = false,
    val isLoadingCalendarEvents: Boolean = false,
    val statusBannerMessage: String? = null,
    val isStatusError: Boolean = false,
    val editingTask: NightTaskEntity? = null
) {
    val filteredTodayTasks: List<NightTaskEntity>
        get() = if (selectedTodayCategoryFilter == null) {
            todayTasks
        } else {
            todayTasks.filter { it.categoryEnum == selectedTodayCategoryFilter }
        }

    val filteredTomorrowTasks: List<NightTaskEntity>
        get() = if (selectedTomorrowCategoryFilter == null) {
            tomorrowTasks
        } else {
            tomorrowTasks.filter { it.categoryEnum == selectedTomorrowCategoryFilter }
        }

    val todayCompletedCount: Int
        get() = todayTasks.count { it.completed }

    val todayPendingCount: Int
        get() = todayTasks.count { !it.completed }

    val todayProgressFraction: Float
        get() = if (todayTasks.isEmpty()) 0f else todayCompletedCount.toFloat() / todayTasks.size.toFloat()

    val tomorrowPendingSyncCount: Int
        get() = tomorrowTasks.count { it.syncWithCalendar && it.googleEventId.isNullOrBlank() }

    val tomorrowTotalPlannedMinutes: Int
        get() = tomorrowTasks.sumOf { task ->
            when {
                !task.startTime.isNullOrBlank() && !task.endTime.isNullOrBlank() ->
                    QuickInputParser.minutesBetween(task.startTime, task.endTime)
                task.durationMinutes != null -> task.durationMinutes
                else -> 0
            }
        }
}

private data class LocalViewControlState(
    val activeTab: NightPlannerTab = NightPlannerTab.PLAN_TOMORROW,
    val selectedTodayCategory: TaskCategory? = null,
    val selectedTomorrowCategory: TaskCategory? = null,
    val importedEvents: List<ExternalCalendarEvent> = emptyList(),
    val isSyncing: Boolean = false,
    val isLoadingCalendar: Boolean = false,
    val statusMessage: String? = null,
    val isError: Boolean = false,
    val editingTask: NightTaskEntity? = null
)

class NightPlannerViewModel(application: Application) : AndroidViewModel(application) {

    private val database = NightPlannerDatabase.getInstance(application)
    private val dao = database.nightTaskDao()
    private val oAuthPrefs = OAuthPreferencesRepository(application)
    private val googleCalendarService = GoogleCalendarService(oAuthPrefs)
    private val repository = NightPlannerRepository(dao, oAuthPrefs, googleCalendarService)

    private val todayIso = DateTimeHelper.todayIso()
    private val tomorrowIso = DateTimeHelper.tomorrowIso()

    private val controlState = MutableStateFlow(LocalViewControlState())

    val uiState: StateFlow<NightPlannerUiState> = combine(
        repository.observeTasksForDate(todayIso),
        repository.observeTasksForDate(tomorrowIso),
        repository.oAuthSettingsFlow,
        controlState
    ) { todayList, tomorrowList, oAuth, ctrl ->
        val overlaps = DateTimeHelper.detectScheduleOverlaps(
            tasks = tomorrowList,
            externalEvents = ctrl.importedEvents
        )
        val freeSlots = DateTimeHelper.calculateFreeTimeSlots(
            tasks = tomorrowList,
            externalEvents = ctrl.importedEvents
        )
        NightPlannerUiState(
            activeTab = ctrl.activeTab,
            todayDateIso = todayIso,
            tomorrowDateIso = tomorrowIso,
            todayHumanEs = DateTimeHelper.formatHumanDateEs(todayIso),
            tomorrowHumanEs = DateTimeHelper.formatHumanDateEs(tomorrowIso),
            todayTasks = todayList,
            tomorrowTasks = tomorrowList,
            selectedTodayCategoryFilter = ctrl.selectedTodayCategory,
            selectedTomorrowCategoryFilter = ctrl.selectedTomorrowCategory,
            importedTomorrowEvents = ctrl.importedEvents,
            overlapWarnings = overlaps,
            freeTimeSlots = freeSlots,
            oAuthSettings = oAuth,
            isSyncing = ctrl.isSyncing,
            isLoadingCalendarEvents = ctrl.isLoadingCalendar,
            statusBannerMessage = ctrl.statusMessage,
            isStatusError = ctrl.isError,
            editingTask = ctrl.editingTask
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = NightPlannerUiState()
    )

    init {
        viewModelScope.launch {
            NightPlannerDatabase.seedInitialDataIfEmpty(dao)
            val currentSettings = oAuthPrefs.getCurrentSettings()
            if (currentSettings.isConnected) {
                refreshTomorrowGoogleCalendarEvents(silent = true)
            }
        }
    }

    fun selectTab(tab: NightPlannerTab) {
        controlState.update { it.copy(activeTab = tab) }
    }

    fun setTodayCategoryFilter(category: TaskCategory?) {
        controlState.update { it.copy(selectedTodayCategory = category) }
    }

    fun setTomorrowCategoryFilter(category: TaskCategory?) {
        controlState.update { it.copy(selectedTomorrowCategory = category) }
    }

    fun clearStatusBanner() {
        controlState.update { it.copy(statusMessage = null, isError = false) }
    }

    private fun postStatus(message: String, isError: Boolean = false) {
        controlState.update { it.copy(statusMessage = message, isError = isError) }
    }

    /**
     * Adds a task to Tomorrow ("Planificar Mañana") supporting inline keyboard shortcuts
     * (`#uni`, `#gym`, `!alta`, `@09:00-10:30`, `+60m`) as well as explicit form selectors.
     */
    fun addTaskForTomorrow(
        rawTitleInput: String,
        notes: String = "",
        explicitCategory: TaskCategory,
        explicitPriority: TaskPriority,
        explicitStartTime: String?,
        explicitEndTime: String?,
        explicitDurationMinutes: Int?,
        syncWithCalendar: Boolean
    ) {
        val parsed = QuickInputParser.parse(rawTitleInput)
        val finalTitle = parsed.cleanTitle.ifBlank { rawTitleInput.trim() }
        if (finalTitle.isBlank()) {
            postStatus("Escribe un título para la tarea de mañana.", isError = true)
            return
        }

        val finalCategory = parsed.detectedCategory ?: explicitCategory
        val finalPriority = parsed.detectedPriority ?: explicitPriority
        val finalStart = parsed.detectedStartTime ?: explicitStartTime?.takeIf { it.isNotBlank() }
        var finalEnd = parsed.detectedEndTime ?: explicitEndTime?.takeIf { it.isNotBlank() }
        val finalDuration = parsed.detectedDurationMinutes ?: explicitDurationMinutes

        if (finalStart != null && finalEnd == null && finalDuration != null && finalDuration > 0) {
            finalEnd = QuickInputParser.addMinutesToHHmm(finalStart, finalDuration)
        }

        val newTask = NightTaskEntity(
            title = finalTitle,
            notes = notes.trim(),
            targetDateIso = tomorrowIso,
            category = finalCategory.name,
            priority = finalPriority.name,
            startTime = finalStart,
            endTime = finalEnd,
            durationMinutes = finalDuration,
            syncWithCalendar = syncWithCalendar,
            completed = false
        )

        viewModelScope.launch {
            val (saved, syncErr) = repository.createTask(newTask, syncImmediatelyIfConnected = true)
            if (syncErr != null) {
                postStatus(
                    "Tarea guardada localmente. Pendiente de sincronizar con Calendar: $syncErr",
                    isError = true
                )
            } else if (!saved.googleEventId.isNullOrBlank()) {
                postStatus("Tarea añadida para mañana y creada en Google Calendar (ID: ${saved.googleEventId.take(10)}…).")
            } else {
                postStatus("Tarea añadida al plan de mañana (${finalCategory.displayName}).")
            }
        }
    }

    /**
     * Toggles task completion (`completed`) in local SQLite and immediately patches the
     * Google Calendar event (`events.patch`) with the "✅ " prefix and Basil green `colorId`.
     */
    fun toggleTaskCompletion(task: NightTaskEntity) {
        val newCompleted = !task.completed
        val updated = task.copy(
            completed = newCompleted,
            completedAtMillis = if (newCompleted) System.currentTimeMillis() else null
        )
        viewModelScope.launch {
            val err = repository.updateTaskAndSync(updated)
            if (err != null) {
                postStatus("Estado actualizado localmente (error al sincronizar con Calendar: $err)", isError = true)
            } else if (newCompleted && !task.googleEventId.isNullOrBlank()) {
                postStatus("¡Tarea completada! Evento actualizado con prefijo '✅' en Google Calendar.")
            }
        }
    }

    fun openEditTask(task: NightTaskEntity) {
        controlState.update { it.copy(editingTask = task) }
    }

    fun dismissEditTask() {
        controlState.update { it.copy(editingTask = null) }
    }

    fun saveEditedTask(updatedTask: NightTaskEntity) {
        controlState.update { it.copy(editingTask = null) }
        viewModelScope.launch {
            val err = repository.updateTaskAndSync(updatedTask)
            if (err != null) {
                postStatus("Cambios guardados localmente. Error al actualizar Calendar: $err", isError = true)
            } else if (updatedTask.syncWithCalendar && uiState.value.oAuthSettings.isConnected) {
                postStatus("Tarea y evento de Google Calendar actualizados correctamente (events.patch).")
            } else {
                postStatus("Tarea actualizada correctamente.")
            }
        }
    }

    fun deleteTask(task: NightTaskEntity) {
        controlState.update {
            if (it.editingTask?.id == task.id) it.copy(editingTask = null) else it
        }
        viewModelScope.launch {
            val err = repository.deleteTaskAndSync(task)
            if (err != null) {
                postStatus("Tarea eliminada localmente (aviso Calendar: $err)", isError = true)
            } else if (!task.googleEventId.isNullOrBlank()) {
                postStatus("Tarea eliminada de NightPlanner y de tu Google Calendar (events.delete).")
            } else {
                postStatus("Tarea eliminada.")
            }
        }
    }

    /**
     * Resolves an overlap or schedules an unscheduled task into a specific free slot with 1 tap.
     */
    fun assignTaskToTimeWindow(task: NightTaskEntity, startHHmm: String, durationMin: Int = 60) {
        val actualDuration = task.durationMinutes?.takeIf { it in 15..240 } ?: durationMin
        val endHHmm = QuickInputParser.addMinutesToHHmm(startHHmm, actualDuration)
        val updated = task.copy(
            startTime = startHHmm,
            endTime = endHHmm,
            durationMinutes = actualDuration
        )
        viewModelScope.launch {
            val err = repository.updateTaskAndSync(updated)
            if (err == null) {
                postStatus("Bloque horario asignado: $startHHmm – $endHHmm para '${task.title.take(28)}'.")
            } else {
                postStatus("Horario asignado localmente ($startHHmm – $endHHmm). Calendar: $err", isError = true)
            }
        }
    }

    /**
     * "Confirmar y Sincronizar Día Siguiente":
     * Batch-syncs all tomorrow's tasks with `syncWithCalendar = true` to the user's primary Google Calendar.
     */
    fun confirmAndSyncTomorrow() {
        viewModelScope.launch {
            controlState.update { it.copy(isSyncing = true, statusMessage = null) }
            val summary = repository.confirmAndSyncDateTasks(tomorrowIso)
            // Also refresh tomorrow's external events after syncing
            if (uiState.value.oAuthSettings.isConnected) {
                val extResult = repository.fetchTomorrowExternalEvents(tomorrowIso)
                if (extResult.isSuccess) {
                    controlState.update { it.copy(importedEvents = extResult.getOrDefault(emptyList())) }
                }
            }
            controlState.update { it.copy(isSyncing = false) }

            when {
                summary.totalCandidates == 0 -> {
                    postStatus(summary.firstErrorMessage ?: "No hay tareas para sincronizar mañana.", isError = true)
                }
                summary.failedCount == 0 -> {
                    postStatus(
                        "¡Sincronización completada! ${summary.syncedSuccessfully} de ${summary.totalCandidates} tareas enviadas a tu Google Calendar principal."
                    )
                }
                summary.syncedSuccessfully > 0 -> {
                    postStatus(
                        "Sincronizadas ${summary.syncedSuccessfully}/${summary.totalCandidates} tareas. Error en ${summary.failedCount}: ${summary.firstErrorMessage}",
                        isError = true
                    )
                }
                else -> {
                    postStatus(
                        summary.firstErrorMessage ?: "No se pudo sincronizar con Google Calendar.",
                        isError = true
                    )
                }
            }
        }
    }

    /**
     * "Migrar pendientes a mañana":
     * Moves all incomplete tasks from Today to Tomorrow with a single click and patches their
     * Google Calendar events if synced.
     */
    fun migratePendingTodayToTomorrow() {
        viewModelScope.launch {
            controlState.update { it.copy(isSyncing = true) }
            val (migratedCount, calendarPatchedCount) = repository.migratePendingTasksToTomorrow(
                todayIso = todayIso,
                tomorrowIso = tomorrowIso
            )
            controlState.update { it.copy(isSyncing = false) }

            if (migratedCount == 0) {
                postStatus("¡Todo al día! No tienes tareas pendientes hoy para migrar.")
            } else {
                val calSuffix = if (calendarPatchedCount > 0) {
                    " y $calendarPatchedCount eventos actualizados en Google Calendar."
                } else {
                    "."
                }
                postStatus("Se migraron $migratedCount tareas pendientes al plan de mañana$calSuffix")
            }
        }
    }

    /**
     * Reads existing events from the user's primary Google Calendar for tomorrow (`listTomorrowEvents`)
     * to prevent overlaps and calculate available time slots.
     */
    fun refreshTomorrowGoogleCalendarEvents(silent: Boolean = false) {
        viewModelScope.launch {
            controlState.update { it.copy(isLoadingCalendar = true) }
            val result = repository.fetchTomorrowExternalEvents(tomorrowIso)
            controlState.update { it.copy(isLoadingCalendar = false) }

            if (result.isSuccess) {
                val events = result.getOrDefault(emptyList())
                controlState.update { it.copy(importedEvents = events) }
                if (!silent) {
                    postStatus("Importados ${events.size} eventos existentes de tu Google Calendar para mañana.")
                }
            } else if (!silent) {
                postStatus(
                    result.exceptionOrNull()?.message ?: "Error al leer eventos de Google Calendar.",
                    isError = true
                )
            }
        }
    }

    // --- OAuth 2.0 & Calendar Settings Actions ---

    suspend fun generatePkceAuthUrl(): String {
        val currentClientId = oAuthPrefs.getCurrentSettings().clientId
        return googleCalendarService.buildPkceAuthorizationUrl(currentClientId)
    }

    fun handleOAuthRedirect(uri: Uri) {
        viewModelScope.launch {
            controlState.update {
                it.copy(
                    activeTab = NightPlannerTab.GOOGLE_CALENDAR_GUIDE,
                    isSyncing = true
                )
            }
            val res = googleCalendarService.handleOAuthRedirectUri(uri)
            controlState.update { it.copy(isSyncing = false) }
            if (res.isSuccess) {
                postStatus(res.getOrThrow())
                refreshTomorrowGoogleCalendarEvents(silent = false)
            } else {
                postStatus(res.exceptionOrNull()?.message ?: "Error en callback OAuth 2.0", isError = true)
            }
        }
    }

    fun saveManualOAuthToken(
        accessToken: String,
        refreshToken: String,
        email: String
    ) {
        if (accessToken.isBlank()) {
            postStatus("Introduce un Access Token OAuth 2.0 válido.", isError = true)
            return
        }
        viewModelScope.launch {
            val cleanedAccess = accessToken.removePrefix("Bearer ").removePrefix("bearer ").trim()
            oAuthPrefs.saveOAuthTokens(
                accessToken = cleanedAccess,
                refreshToken = refreshToken.takeIf { it.isNotBlank() },
                expiresInSeconds = 3600L,
                email = email.ifBlank { "Cuenta principal (primary)" }
            )
            postStatus("Token OAuth 2.0 guardado. Sincronizando eventos de mañana con Google Calendar…")
            refreshTomorrowGoogleCalendarEvents(silent = false)
        }
    }

    fun updateOAuthClientId(newClientId: String) {
        viewModelScope.launch {
            oAuthPrefs.updateClientId(newClientId)
            postStatus("Client ID de OAuth 2.0 actualizado.")
        }
    }

    fun updateUnscheduledPolicy(
        asAllDay: Boolean,
        defaultStartHour: Int,
        defaultDurationMin: Int
    ) {
        viewModelScope.launch {
            oAuthPrefs.updateUnscheduledPolicy(asAllDay, defaultStartHour, defaultDurationMin)
            postStatus("Preferencia de eventos sin hora específica guardada.")
        }
    }

    fun disconnectGoogleCalendar() {
        viewModelScope.launch {
            oAuthPrefs.disconnectAccount()
            controlState.update { it.copy(importedEvents = emptyList()) }
            postStatus("Cuenta de Google Calendar desconectada.")
        }
    }
}
