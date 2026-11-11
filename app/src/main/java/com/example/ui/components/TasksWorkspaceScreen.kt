package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.automirrored.filled.Forward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.ExternalCalendarEvent
import com.example.model.NightTaskEntity
import com.example.model.TaskCategory
import com.example.model.TaskPriority
import com.example.ui.NightPlannerUiState
import com.example.ui.TaskDayFilter
import com.example.ui.theme.CalendarImportedPurple
import com.example.ui.theme.ConflictWarningRed
import com.example.ui.theme.ElectricIndigo
import com.example.ui.theme.ElectricIndigoBright
import com.example.ui.theme.NightBgDeep
import com.example.ui.theme.NightBorderSubtle
import com.example.ui.theme.NightCardElevated
import com.example.ui.theme.NightCardSurface
import com.example.ui.theme.NocturnalAmber
import com.example.ui.theme.SyncSuccessGreen
import com.example.ui.theme.TextMoonlight
import com.example.ui.theme.TextMutedSlate
import com.example.ui.theme.TextStarlight
import com.example.util.DateTimeHelper

@Composable
fun TasksWorkspaceScreen(
    state: NightPlannerUiState,
    onSelectDayFilter: (TaskDayFilter) -> Unit,
    onSelectCategoryFilter: (TaskCategory?) -> Unit,
    onAddTask: (
        targetDateIso: String,
        rawTitle: String,
        notes: String,
        category: TaskCategory,
        priority: TaskPriority,
        startTime: String?,
        endTime: String?,
        durationMinutes: Int?,
        syncWithCalendar: Boolean
    ) -> Unit,
    onPrefillConsumed: () -> Unit,
    onToggleComplete: (NightTaskEntity) -> Unit,
    onEditTask: (NightTaskEntity) -> Unit,
    onDeleteTask: (NightTaskEntity) -> Unit,
    onConfirmAndSyncActiveDate: () -> Unit,
    onRefreshCalendarEvents: () -> Unit,
    onMigratePendingToTomorrow: () -> Unit,
    onNavigateToOAuthSetup: () -> Unit,
    modifier: Modifier = Modifier
) {
    val overlapMap = state.activeOverlapWarnings.associateBy { it.taskId }

    val animatedProgress by animateFloatAsState(
        targetValue = state.selectedProgressFraction,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "tasksProgress"
    )
    val percentageInt = (animatedProgress * 100).toInt()

    val hours = state.selectedTotalMinutes / 60
    val mins = state.selectedTotalMinutes % 60
    val timeFormatted = if (hours > 0) "${hours}h ${mins}m" else "${mins}m"

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("tasks_workspace_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Unified Summary & Flexible Date Switcher (Hoy / Mañana / Todas)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = NightCardElevated),
                border = BorderStroke(1.dp, NightBorderSubtle)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    // Segmented Day Filter Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            Triple(TaskDayFilter.TODAY, "Hoy", state.todayTasks.size),
                            Triple(TaskDayFilter.TOMORROW, "Mañana", state.tomorrowTasks.size),
                            Triple(TaskDayFilter.ALL, "Todas", state.allTasks.size)
                        ).forEach { (filter, label, count) ->
                            val isSelected = state.selectedDayFilter == filter
                            Surface(
                                onClick = { onSelectDayFilter(filter) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("day_filter_${filter.name.lowercase()}"),
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) ElectricIndigo else NightBgDeep,
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) ElectricIndigo else NightBorderSubtle
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 9.dp, horizontal = 8.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "$label ($count)",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        ),
                                        color = if (isSelected) Color.White else TextMutedSlate
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = state.activeHumanDateEs,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = TextMoonlight
                            )
                            Text(
                                text = "${state.selectedCompletedCount} completadas · ${state.selectedPendingCount} pendientes · $timeFormatted",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextStarlight
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = SyncSuccessGreen.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, SyncSuccessGreen.copy(alpha = 0.35f))
                        ) {
                            Text(
                                text = "$percentageInt%",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = SyncSuccessGreen,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(RoundedCornerShape(50))
                            .testTag("tasks_progress_bar"),
                        color = SyncSuccessGreen,
                        trackColor = NightBgDeep,
                        strokeCap = StrokeCap.Round
                    )
                }
            }
        }

        // 2. Quick Add Composer (works anytime for Hoy, Mañana, or Pasado)
        item {
            TaskQuickAddCard(
                todayIso = state.todayDateIso,
                tomorrowIso = state.tomorrowDateIso,
                afterTomorrowIso = state.afterTomorrowDateIso,
                selectedDayFilter = state.selectedDayFilter,
                freeSlotsToday = state.freeTimeSlotsToday,
                freeSlotsTomorrow = state.freeTimeSlotsTomorrow,
                prefillTitle = state.composerPrefillTitle,
                prefillCategory = state.composerPrefillCategory,
                onPrefillConsumed = onPrefillConsumed,
                onAddTask = onAddTask
            )
        }

        // 3. Google Calendar Live Preview & Sync Strip
        if (state.selectedDayFilter != TaskDayFilter.ALL) {
            item {
                CalendarSyncStripCard(
                    dayLabel = if (state.selectedDayFilter == TaskDayFilter.TOMORROW) "Mañana" else "Hoy",
                    isConnected = state.oAuthSettings.isConnected,
                    isLoading = state.isLoadingCalendarEvents,
                    isSyncing = state.isSyncing,
                    externalEvents = state.activeExternalEvents,
                    tasksToSyncCount = state.tasksForSelectedFilter.count { it.syncWithCalendar },
                    overlapCount = state.activeOverlapWarnings.size,
                    onRefresh = onRefreshCalendarEvents,
                    onSyncTasks = onConfirmAndSyncActiveDate,
                    onConnectClick = onNavigateToOAuthSetup
                )
            }
        }

        // 4. Category Filters & List Header
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Actividades y Tareas (${state.filteredTasks.size})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextMoonlight
                    )
                    if (state.activeOverlapWarnings.isNotEmpty()) {
                        SmallBadgePill(
                            text = "${state.activeOverlapWarnings.size} conflicto(s)",
                            accentColor = ConflictWarningRed
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = state.selectedCategoryFilter == null,
                        onClick = { onSelectCategoryFilter(null) },
                        label = { Text("Todas (${state.tasksForSelectedFilter.size})") }
                    )
                    TaskCategory.entries.forEach { cat ->
                        val count = state.tasksForSelectedFilter.count { it.categoryEnum == cat }
                        FilterChip(
                            selected = state.selectedCategoryFilter == cat,
                            onClick = {
                                onSelectCategoryFilter(
                                    if (state.selectedCategoryFilter == cat) null else cat
                                )
                            },
                            label = { Text("${cat.displayName} ($count)") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = cat.color.copy(alpha = 0.2f),
                                selectedLabelColor = cat.color
                            )
                        )
                    }
                }
            }
        }

        // 5. Task Cards (Grouped by Date when viewing ALL)
        if (state.filteredTasks.isEmpty()) {
            item {
                EmptyTasksStateCard(
                    title = stringResource(R.string.empty_tasks_title),
                    subtitle = stringResource(R.string.empty_tasks_subtitle)
                )
            }
        } else if (state.selectedDayFilter == TaskDayFilter.ALL) {
            val grouped = state.filteredTasks.groupBy { it.targetDateIso }
            grouped.forEach { (dateIso, tasksInDate) ->
                val dateHeading = when (dateIso) {
                    state.todayDateIso -> "Hoy · ${state.todayHumanEs}"
                    state.tomorrowDateIso -> "Mañana · ${state.tomorrowHumanEs}"
                    else -> DateTimeHelper.formatHumanDateEs(dateIso)
                }
                item(key = "header_$dateIso") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = dateHeading,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = ElectricIndigoBright
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(1.dp)
                                .background(NightBorderSubtle)
                        )
                    }
                }
                items(
                    items = tasksInDate,
                    key = { it.id }
                ) { task ->
                    TaskItemCard(
                        task = task,
                        overlapWarning = overlapMap[task.id],
                        onToggleComplete = onToggleComplete,
                        onEditClick = onEditTask,
                        onDeleteClick = onDeleteTask
                    )
                }
            }
        } else {
            items(
                items = state.filteredTasks,
                key = { it.id }
            ) { task ->
                TaskItemCard(
                    task = task,
                    overlapWarning = overlapMap[task.id],
                    onToggleComplete = onToggleComplete,
                    onEditClick = onEditTask,
                    onDeleteClick = onDeleteTask
                )
            }
        }

        // 6. Optional quick rollover when viewing Today with pending items
        if (state.selectedDayFilter == TaskDayFilter.TODAY && state.todayPendingCount > 0) {
            item {
                OutlinedButton(
                    onClick = onMigratePendingToTomorrow,
                    enabled = !state.isSyncing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("migrate_pending_tomorrow_btn"),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, NightBorderSubtle)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Forward,
                        contentDescription = null,
                        tint = NocturnalAmber,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Mover ${state.todayPendingCount} pendiente(s) a mañana",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextStarlight
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun CalendarSyncStripCard(
    dayLabel: String,
    isConnected: Boolean,
    isLoading: Boolean,
    isSyncing: Boolean,
    externalEvents: List<ExternalCalendarEvent>,
    tasksToSyncCount: Int,
    overlapCount: Int,
    onRefresh: () -> Unit,
    onSyncTasks: () -> Unit,
    onConnectClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = NightCardSurface),
        border = BorderStroke(1.dp, NightBorderSubtle)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.EventNote,
                        contentDescription = null,
                        tint = CalendarImportedPurple,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Google Calendar · $dayLabel",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = TextMoonlight
                        )
                        Text(
                            text = if (isConnected) {
                                "${externalEvents.size} evento(s) externos · $tasksToSyncCount tareas vinculables"
                            } else {
                                "Conecta tu cuenta para sincronizar actividades"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMutedSlate
                        )
                    }
                }

                if (isConnected) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(
                            onClick = onRefresh,
                            enabled = !isLoading,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Actualizar Calendar",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Button(
                            onClick = onSyncTasks,
                            enabled = !isSyncing && tasksToSyncCount > 0,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ElectricIndigo,
                                contentColor = Color.White
                            ),
                            modifier = Modifier.testTag("confirm_sync_active_btn")
                        ) {
                            if (isSyncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.CloudSync,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Sincronizar", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = onConnectClick,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("connect_calendar_from_tasks_btn")
                    ) {
                        Text("Vincular", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            if (externalEvents.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    externalEvents.forEach { ev ->
                        val timeText = if (ev.isAllDay) {
                            "Todo el día"
                        } else {
                            "${ev.startTimeHHmm ?: "??:??"}–${ev.endTimeHHmm ?: "??:??"}"
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = CalendarImportedPurple.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, CalendarImportedPurple.copy(alpha = 0.35f))
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                                Text(
                                    text = ev.title,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextMoonlight
                                )
                                Text(
                                    text = timeText,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CalendarImportedPurple
                                )
                            }
                        }
                    }
                }
            }

            if (overlapCount > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.WarningAmber,
                        contentDescription = null,
                        tint = ConflictWarningRed,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "$overlapCount actividad(es) chocan con tu horario o Calendar.",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = ConflictWarningRed
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyTasksStateCard(
    title: String,
    subtitle: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = NightCardSurface),
        border = BorderStroke(1.dp, NightBorderSubtle)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(ElectricIndigo.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.TaskAlt,
                    contentDescription = null,
                    tint = ElectricIndigoBright,
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = TextMoonlight,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextMutedSlate,
                textAlign = TextAlign.Center
            )
        }
    }
}
