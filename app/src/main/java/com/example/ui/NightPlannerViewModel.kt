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
import com.example.model.UniClassSession
import com.example.model.UniSubject
import com.example.model.UniversityScheduleRepository
import com.example.util.DateTimeHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class NightPlannerTab(val titleEs: String) {
    TASKS("Tareas"),
    UNIVERSITY_SCHEDULE("Horario"),
    TIMELINE_CONFLICTS("Agenda"),
    GOOGLE_CALENDAR_GUIDE("Calendar")
}

enum class TaskDayFilter(val label: String) {
    TODAY("Hoy"),
    TOMORROW("Mañana"),
    ALL("Todas")
}

data class NightPlannerUiState(
    val activeTab: NightPlannerTab = NightPlannerTab.TASKS,
    val selectedDayFilter: TaskDayFilter = TaskDayFilter.TODAY,
    val selectedCategoryFilter: TaskCategory? = null,
    val todayDateIso: String = DateTimeHelper.todayIso(),
    val tomorrowDateIso: String = DateTimeHelper.tomorrowIso(),
    val afterTomorrowDateIso: String = DateTimeHelper.addDaysToIso(DateTimeHelper.todayIso(), 2),
    val todayHumanEs: String = DateTimeHelper.formatHumanDateEs(DateTimeHelper.todayIso()),
    val tomorrowHumanEs: String = DateTimeHelper.formatHumanDateEs(DateTimeHelper.tomorrowIso()),
    val afterTomorrowHumanEs: String = DateTimeHelper.formatHumanDateEs(
        DateTimeHelper.addDaysToIso(DateTimeHelper.todayIso(), 2)
    ),
    val allTasks: List<NightTaskEntity> = emptyList(),
    val todayTasks: List<NightTaskEntity> = emptyList(),
    val tomorrowTasks: List<NightTaskEntity> = emptyList(),
    val importedTodayEvents: List<ExternalCalendarEvent> = emptyList(),
    val importedTomorrowEvents: List<ExternalCalendarEvent> = emptyList(),
    val todayUniClasses: List<UniClassSession> = UniversityScheduleRepository.sessionsForDateIso(
        DateTimeHelper.todayIso()
    ),
    val tomorrowUniClasses: List<UniClassSession> = UniversityScheduleRepository.sessionsForDateIso(
        DateTimeHelper.tomorrowIso()
    ),
    val overlapWarningsToday: List<OverlapWarning> = emptyList(),
    val overlapWarningsTomorrow: List<OverlapWarning> = emptyList(),
    val freeTimeSlotsToday: List<FreeTimeSlot> = emptyList(),
    val freeTimeSlotsTomorrow: List<FreeTimeSlot> = emptyList(),
    val includeUniInFreeSlots: Boolean = true,
    val composerPrefillTitle: String? = null,
    val composerPrefillCategory: TaskCategory? = null,
    val oAuthSettings: OAuthSettingsState = OAuthSettingsState(),
    val isSyncing: Boolean = false,
    val isLoadingCalendarEvents: Boolean = false,
    val statusBannerMessage: String? = null,
    val isStatusError: Boolean = false,
    val editingTask: NightTaskEntity? = null
) {
    val activeDateIso: String
        get() = when (selectedDayFilter) {
            TaskDayFilter.TOMORROW -> tomorrowDateIso
            TaskDayFilter.TODAY, TaskDayFilter.ALL -> todayDateIso
        }

    val activeHumanDateEs: String
        get() = when (selectedDayFilter) {
            TaskDayFilter.TODAY -> todayHumanEs
            TaskDayFilter.TOMORROW -> tomorrowHumanEs
            TaskDayFilter.ALL -> "Todas tus actividades"
        }

    val tasksForSelectedFilter: List<NightTaskEntity>
        get() = when (selectedDayFilter) {
            TaskDayFilter.TODAY -> todayTasks
            TaskDayFilter.TOMORROW -> tomorrowTasks
            TaskDayFilter.ALL -> allTasks
        }

    val filteredTasks: List<NightTaskEntity>
        get() = if (selectedCategoryFilter == null) {
            tasksForSelectedFilter
        } else {
            tasksForSelectedFilter.filter { it.categoryEnum == selectedCategoryFilter }
        }

    val selectedCompletedCount: Int
        get() = tasksForSelectedFilter.count { it.completed }

    val selectedPendingCount: Int
        get() = tasksForSelectedFilter.count { !it.completed }

    val selectedProgressFraction: Float
        get() = if (tasksForSelectedFilter.isEmpty()) 0f else {
            selectedCompletedCount.toFloat() / tasksForSelectedFilter.size.toFloat()
        }

    val selectedTotalMinutes: Int
        get() = tasksForSelectedFilter.sumOf { task ->
            when {
                !task.startTime.isNullOrBlank() && !task.endTime.isNullOrBlank() ->
                    QuickInputParser.minutesBetween(task.startTime, task.endTime)
                task.durationMinutes != null -> task.durationMinutes
                else -> 0
            }
        }

    val todayPendingCount: Int
        get() = todayTasks.count { !it.completed }

    val totalPendingCount: Int
        get() = allTasks.count { !it.completed }

    val activeExternalEvents: List<ExternalCalendarEvent>
        get() = when (selectedDayFilter) {
            TaskDayFilter.TOMORROW -> importedTomorrowEvents
            TaskDayFilter.TODAY, TaskDayFilter.ALL -> importedTodayEvents
        }

    val activeUniClasses: List<UniClassSession>
        get() = when (selectedDayFilter) {
            TaskDayFilter.TOMORROW -> tomorrowUniClasses
            TaskDayFilter.TODAY, TaskDayFilter.ALL -> todayUniClasses
        }

    val activeOverlapWarnings: List<OverlapWarning>
        get() = when (selectedDayFilter) {
            TaskDayFilter.TODAY -> overlapWarningsToday
            TaskDayFilter.TOMORROW -> overlapWarningsTomorrow
            TaskDayFilter.ALL -> (overlapWarningsToday + overlapWarningsTomorrow).distinctBy { it.taskId }
        }

    val activeFreeTimeSlots: List<FreeTimeSlot>
        get() = when (selectedDayFilter) {
            TaskDayFilter.TOMORROW -> freeTimeSlotsTomorrow
            TaskDayFilter.TODAY, TaskDayFilter.ALL -> freeTimeSlotsToday
        }

    val totalOverlapCount: Int
        get() = (overlapWarningsToday + overlapWarningsTomorrow).distinctBy { it.taskId }.size
}

private data class LocalViewControlState(
    val activeTab: NightPlannerTab = NightPlannerTab.TASKS,
    val selectedDayFilter: TaskDayFilter = TaskDayFilter.TODAY,
    val selectedCategory: TaskCategory? = null,
    val importedTodayEvents: List<ExternalCalendarEvent> = emptyList(),
    val importedTomorrowEvents: List<ExternalCalendarEvent> = emptyList(),
    val includeUniInFreeSlots: Boolean = true,
    val composerPrefillTitle: String? = null,
    val composerPrefillCategory: TaskCategory? = null,
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
    private val afterTomorrowIso = DateTimeHelper.addDaysToIso(todayIso, 2)

    private val controlState = MutableStateFlow(LocalViewControlState())

    val uiState: StateFlow<NightPlannerUiState> = combine(
        repository.observeAllTasks(),
        repository.oAuthSettingsFlow,
        controlState
    ) { allTasks, oAuth, ctrl ->
        val todayList = allTasks.filter { it.targetDateIso == todayIso }
        val tomorrowList = allTasks.filter { it.targetDateIso == tomorrowIso }

        val todayUniVirtual = if (ctrl.includeUniInFreeSlots) {
            UniversityScheduleRepository.toVirtualExternalEvents(todayIso)
        } else {
            emptyList()
        }
        val tomorrowUniVirtual = if (ctrl.includeUniInFreeSlots) {
            UniversityScheduleRepository.toVirtualExternalEvents(tomorrowIso)
        } else {
            emptyList()
        }

        val overlapsToday = DateTimeHelper.detectScheduleOverlaps(
            tasks = todayList,
            externalEvents = ctrl.importedTodayEvents + todayUniVirtual
        )
        val overlapsTomorrow = DateTimeHelper.detectScheduleOverlaps(
            tasks = tomorrowList,
            externalEvents = ctrl.importedTomorrowEvents + tomorrowUniVirtual
        )

        val freeToday = DateTimeHelper.calculateFreeTimeSlots(
            tasks = todayList,
            externalEvents = ctrl.importedTodayEvents + todayUniVirtual
        )
        val freeTomorrow = DateTimeHelper.calculateFreeTimeSlots(
            tasks = tomorrowList,
            externalEvents = ctrl.importedTomorrowEvents + tomorrowUniVirtual
        )

        NightPlannerUiState(
            activeTab = ctrl.activeTab,
            selectedDayFilter = ctrl.selectedDayFilter,
            selectedCategoryFilter = ctrl.selectedCategory,
            todayDateIso = todayIso,
            tomorrowDateIso = tomorrowIso,
            afterTomorrowDateIso = afterTomorrowIso,
            todayHumanEs = DateTimeHelper.formatHumanDateEs(todayIso),
            tomorrowHumanEs = DateTimeHelper.formatHumanDateEs(tomorrowIso),
            afterTomorrowHumanEs = DateTimeHelper.formatHumanDateEs(afterTomorrowIso),
            allTasks = allTasks,
            todayTasks = todayList,
            tomorrowTasks = tomorrowList,
            importedTodayEvents = ctrl.importedTodayEvents,
            importedTomorrowEvents = ctrl.importedTomorrowEvents,
            todayUniClasses = UniversityScheduleRepository.sessionsForDateIso(todayIso),
            tomorrowUniClasses = UniversityScheduleRepository.sessionsForDateIso(tomorrowIso),
            overlapWarningsToday = overlapsToday,
            overlapWarningsTomorrow = overlapsTomorrow,
            freeTimeSlotsToday = freeToday,
            freeTimeSlotsTomorrow = freeTomorrow,
            includeUniInFreeSlots = ctrl.includeUniInFreeSlots,
            composerPrefillTitle = ctrl.composerPrefillTitle,
            composerPrefillCategory = ctrl.composerPrefillCategory,
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
                refreshGoogleCalendarEvents(silent = true)
            }
        }
    }

    fun selectTab(tab: NightPlannerTab) {
        controlState.update { it.copy(activeTab = tab) }
    }

    fun selectDayFilter(filter: TaskDayFilter) {
        controlState.update { it.copy(selectedDayFilter = filter) }
    }

    fun setCategoryFilter(category: TaskCategory?) {
        controlState.update { it.copy(selectedCategory = category) }
    }

    fun toggleIncludeUniInFreeSlots() {
        controlState.update { it.copy(includeUniInFreeSlots = !it.includeUniInFreeSlots) }
    }

    fun prefillTaskFromUniClass(subject: UniSubject, isPractice: Boolean) {
        val prefix = if (isPractice) "Práctica ${subject.abbr}" else "Tarea / AAD ${subject.abbr}"
        controlState.update {
            it.copy(
                activeTab = NightPlannerTab.TASKS,
                composerPrefillTitle = "$prefix #uni ",
                composerPrefillCategory = TaskCategory.UNIVERSIDAD
            )
        }
    }

    fun consumeComposerPrefill() {
        controlState.update {
            it.copy(composerPrefillTitle = null, composerPrefillCategory = null)
        }
    }

    fun clearStatusBanner() {
        controlState.update { it.copy(statusMessage = null, isError = false) }
    }

    private fun postStatus(message: String, isError: Boolean = false) {
        controlState.update { it.copy(statusMessage = message, isError = isError) }
    }

    /**
     * Adds a task/activity for any chosen date (`todayIso`, `tomorrowIso`, etc.), supporting inline
     * shortcuts (`#uni`, `#gym`, `!alta`, `@09:00-10:30`, `+60m`) as well as explicit form selectors.
     */
    fun addTaskForDate(
        targetDateIso: String,
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
            postStatus("Escribe un título para la tarea o actividad.", isError = true)
            return
        }

        val finalCategory = parsed.detectedCategory ?: explicitCategory
        val finalPriority = parsed.detectedPriority ?: explicitPriority
        val finalStart = (parsed.detectedStartTime ?: explicitStartTime?.takeIf { it.isNotBlank() })
            ?.let { QuickInputParser.normalizeHHmm(it) }
        var finalEnd = (parsed.detectedEndTime ?: explicitEndTime?.takeIf { it.isNotBlank() })
            ?.let { QuickInputParser.normalizeHHmm(it) }
        var finalDuration = parsed.detectedDurationMinutes ?: explicitDurationMinutes

        if (finalStart != null && finalEnd == null && finalDuration != null && finalDuration > 0) {
            finalEnd = QuickInputParser.addMinutesToHHmm(finalStart, finalDuration)
        } else if (finalStart != null && finalEnd != null) {
            val diff = QuickInputParser.minutesBetween(finalStart, finalEnd)
            if (diff > 0) finalDuration = diff
        }

        val newTask = NightTaskEntity(
            title = finalTitle,
            notes = notes.trim(),
            targetDateIso = targetDateIso,
            category = finalCategory.name,
            priority = finalPriority.name,
            startTime = finalStart,
            endTime = finalEnd,
            durationMinutes = finalDuration,
            syncWithCalendar = syncWithCalendar,
            completed = false
        )

        // Ensure the user immediately sees the date where they placed the task
        controlState.update { ctrl ->
            val updatedFilter = when {
                ctrl.selectedDayFilter == TaskDayFilter.ALL -> TaskDayFilter.ALL
                targetDateIso == todayIso -> TaskDayFilter.TODAY
                targetDateIso == tomorrowIso -> TaskDayFilter.TOMORROW
                else -> TaskDayFilter.ALL
            }
            ctrl.copy(selectedDayFilter = updatedFilter)
        }

        val dateLabel = when (targetDateIso) {
            todayIso -> "hoy"
            tomorrowIso -> "mañana"
            else -> targetDateIso
        }

        viewModelScope.launch {
            val (saved, syncErr) = repository.createTask(newTask, syncImmediatelyIfConnected = true)
            if (syncErr != null) {
                postStatus(
                    "Actividad guardada localmente para $dateLabel. Pendiente de Calendar: $syncErr",
                    isError = true
                )
            } else if (!saved.googleEventId.isNullOrBlank()) {
                postStatus("Actividad añadida para $dateLabel y sincronizada con Google Calendar.")
            } else {
                postStatus("Actividad añadida para $dateLabel (${finalCategory.displayName}).")
            }
        }
    }

    fun toggleTaskCompletion(task: NightTaskEntity) {
        val newCompleted = !task.completed
        val updated = task.copy(
            completed = newCompleted,
            completedAtMillis = if (newCompleted) System.currentTimeMillis() else null
        )
        viewModelScope.launch {
            val err = repository.updateTaskAndSync(updated)
            if (err != null) {
                postStatus("Estado actualizado localmente (error al sincronizar Calendar: $err)", isError = true)
            } else if (newCompleted && !task.googleEventId.isNullOrBlank()) {
                postStatus("¡Actividad completada! Evento marcado con '✅' en Google Calendar.")
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
        val normStart = updatedTask.startTime?.takeIf { it.isNotBlank() }?.let { QuickInputParser.normalizeHHmm(it) }
        var normEnd = updatedTask.endTime?.takeIf { it.isNotBlank() }?.let { QuickInputParser.normalizeHHmm(it) }
        var normDuration = updatedTask.durationMinutes

        if (normStart != null && normEnd == null && normDuration != null && normDuration > 0) {
            normEnd = QuickInputParser.addMinutesToHHmm(normStart, normDuration)
        } else if (normStart != null && normEnd != null) {
            val diff = QuickInputParser.minutesBetween(normStart, normEnd)
            if (diff > 0) normDuration = diff
        }

        val normalizedTask = updatedTask.copy(
            startTime = normStart,
            endTime = normEnd,
            durationMinutes = normDuration
        )

        viewModelScope.launch {
            val err = repository.updateTaskAndSync(normalizedTask)
            if (err != null) {
                postStatus("Cambios guardados localmente. Error al actualizar Calendar: $err", isError = true)
            } else if (normalizedTask.syncWithCalendar && uiState.value.oAuthSettings.isConnected) {
                postStatus("Tarea y evento de Google Calendar actualizados correctamente.")
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
                postStatus("Tarea eliminada de la app y de tu Google Calendar.")
            } else {
                postStatus("Tarea eliminada.")
            }
        }
    }

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
                postStatus("Horario asignado: $startHHmm – $endHHmm para '${task.title.take(28)}'.")
            } else {
                postStatus("Horario asignado localmente ($startHHmm – $endHHmm). Calendar: $err", isError = true)
            }
        }
    }

    fun confirmAndSyncActiveDateTasks() {
        val targetDate = uiState.value.activeDateIso
        viewModelScope.launch {
            controlState.update { it.copy(isSyncing = true, statusMessage = null) }
            val summary = repository.confirmAndSyncDateTasks(targetDate)
            if (uiState.value.oAuthSettings.isConnected) {
                val todayRes = repository.fetchTomorrowExternalEvents(todayIso)
                val tomorrowRes = repository.fetchTomorrowExternalEvents(tomorrowIso)
                controlState.update {
                    it.copy(
                        importedTodayEvents = todayRes.getOrDefault(it.importedTodayEvents),
                        importedTomorrowEvents = tomorrowRes.getOrDefault(it.importedTomorrowEvents)
                    )
                }
            }
            controlState.update { it.copy(isSyncing = false) }

            when {
                summary.totalCandidates == 0 -> {
                    postStatus(summary.firstErrorMessage ?: "No hay tareas marcadas para sincronizar en esta fecha.", isError = true)
                }
                summary.failedCount == 0 -> {
                    postStatus(
                        "¡Sincronización completada! ${summary.syncedSuccessfully} de ${summary.totalCandidates} tareas enviadas a Google Calendar."
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

    fun migratePendingTodayToTomorrow() {
        viewModelScope.launch {
            controlState.update { it.copy(isSyncing = true) }
            val (migratedCount, calendarPatchedCount) = repository.migratePendingTasksToTomorrow(
                todayIso = todayIso,
                tomorrowIso = tomorrowIso
            )
            controlState.update { it.copy(isSyncing = false) }

            if (migratedCount == 0) {
                postStatus("¡Todo al día! No tienes tareas pendientes hoy para mover.")
            } else {
                val calSuffix = if (calendarPatchedCount > 0) {
                    " y $calendarPatchedCount eventos actualizados en Google Calendar."
                } else {
                    "."
                }
                postStatus("Se movieron $migratedCount tareas pendientes a mañana$calSuffix")
            }
        }
    }

    fun refreshGoogleCalendarEvents(silent: Boolean = false) {
        viewModelScope.launch {
            controlState.update { it.copy(isLoadingCalendar = true) }
            val todayRes = repository.fetchTomorrowExternalEvents(todayIso)
            val tomorrowRes = repository.fetchTomorrowExternalEvents(tomorrowIso)
            controlState.update { it.copy(isLoadingCalendar = false) }

            if (todayRes.isSuccess || tomorrowRes.isSuccess) {
                val todayEvs = todayRes.getOrDefault(emptyList())
                val tomorrowEvs = tomorrowRes.getOrDefault(emptyList())
                controlState.update {
                    it.copy(
                        importedTodayEvents = todayEvs,
                        importedTomorrowEvents = tomorrowEvs
                    )
                }
                if (!silent) {
                    val total = todayEvs.size + tomorrowEvs.size
                    postStatus("Importados $total eventos de Google Calendar (Hoy y Mañana).")
                }
            } else if (!silent) {
                val msg = todayRes.exceptionOrNull()?.message
                    ?: tomorrowRes.exceptionOrNull()?.message
                    ?: "Error al leer eventos de Google Calendar."
                postStatus(msg, isError = true)
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
                refreshGoogleCalendarEvents(silent = false)
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
            postStatus("Token OAuth 2.0 guardado. Sincronizando eventos con Google Calendar…")
            refreshGoogleCalendarEvents(silent = false)
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
            controlState.update {
                it.copy(
                    importedTodayEvents = emptyList(),
                    importedTomorrowEvents = emptyList()
                )
            }
            postStatus("Cuenta de Google Calendar desconectada.")
        }
    }
}
