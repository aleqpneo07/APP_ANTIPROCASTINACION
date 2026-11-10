package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
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
import com.example.ui.theme.CalendarImportedPurple
import com.example.ui.theme.ConflictWarningRed
import com.example.ui.theme.ElectricIndigo
import com.example.ui.theme.NightBgDeep
import com.example.ui.theme.NightBorderSubtle
import com.example.ui.theme.NightCardElevated
import com.example.ui.theme.NightCardSurface
import com.example.ui.theme.NocturnalAmber
import com.example.ui.theme.SyncSuccessGreen
import com.example.ui.theme.TextMoonlight
import com.example.ui.theme.TextMutedSlate
import com.example.ui.theme.TextStarlight

@Composable
fun PlanTomorrowScreen(
    state: NightPlannerUiState,
    onAddTask: (
        rawTitle: String,
        notes: String,
        category: TaskCategory,
        priority: TaskPriority,
        startTime: String?,
        endTime: String?,
        durationMinutes: Int?,
        syncWithCalendar: Boolean
    ) -> Unit,
    onToggleComplete: (NightTaskEntity) -> Unit,
    onEditTask: (NightTaskEntity) -> Unit,
    onDeleteTask: (NightTaskEntity) -> Unit,
    onSelectCategoryFilter: (TaskCategory?) -> Unit,
    onConfirmAndSyncTomorrow: () -> Unit,
    onRefreshCalendarEvents: () -> Unit,
    onNavigateToOAuthSetup: () -> Unit,
    modifier: Modifier = Modifier
) {
    val overlapMap = state.overlapWarnings.associateBy { it.taskId }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("plan_tomorrow_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Nocturnal Hero Banner Card
        item {
            NocturnalTomorrowHeroBanner(
                tomorrowHumanDate = state.tomorrowHumanEs,
                tomorrowIso = state.tomorrowDateIso,
                totalTasksCount = state.tomorrowTasks.size,
                totalPlannedMinutes = state.tomorrowTotalPlannedMinutes,
                freeSlotsCount = state.freeTimeSlots.size,
                isCalendarConnected = state.oAuthSettings.isConnected
            )
        }

        // 2. Existing Google Calendar Events Read-Only Preview & Overlap Detector
        item {
            ExistingCalendarEventsStripCard(
                isConnected = state.oAuthSettings.isConnected,
                isLoading = state.isLoadingCalendarEvents,
                externalEvents = state.importedTomorrowEvents,
                overlapCount = state.overlapWarnings.size,
                onRefresh = onRefreshCalendarEvents,
                onConnectClick = onNavigateToOAuthSetup
            )
        }

        // 3. Rapid Nocturnal Task Composer with Keyboard Shortcuts & Free Slots
        item {
            NocturnalQuickAddCard(
                tomorrowHumanDate = state.tomorrowHumanEs,
                freeSlots = state.freeTimeSlots,
                onAddTask = onAddTask
            )
        }

        // 4. Primary Batch Action: "Confirmar y Sincronizar Día Siguiente"
        item {
            ConfirmTomorrowSyncCard(
                totalTasks = state.tomorrowTasks.size,
                syncEnabledCount = state.tomorrowTasks.count { it.syncWithCalendar },
                alreadySyncedCount = state.tomorrowTasks.count { !it.googleEventId.isNullOrBlank() },
                isSyncing = state.isSyncing,
                onConfirmAndSync = onConfirmAndSyncTomorrow
            )
        }

        // 5. Category Filter & Section Header
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Bloques Planificados para Mañana (${state.filteredTomorrowTasks.size})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextMoonlight
                    )
                    if (state.overlapWarnings.isNotEmpty()) {
                        SmallBadgePill(
                            text = "${state.overlapWarnings.size} solapamiento(s)",
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
                        selected = state.selectedTomorrowCategoryFilter == null,
                        onClick = { onSelectCategoryFilter(null) },
                        label = { Text("Todas (${state.tomorrowTasks.size})") }
                    )
                    TaskCategory.entries.forEach { cat ->
                        val count = state.tomorrowTasks.count { it.categoryEnum == cat }
                        FilterChip(
                            selected = state.selectedTomorrowCategoryFilter == cat,
                            onClick = {
                                onSelectCategoryFilter(
                                    if (state.selectedTomorrowCategoryFilter == cat) null else cat
                                )
                            },
                            label = { Text("${cat.displayName} ($count)") }
                        )
                    }
                }
            }
        }

        // 6. Tomorrow's Task Cards or Empty State
        if (state.filteredTomorrowTasks.isEmpty()) {
            item {
                EmptyPlanStateCard(
                    title = stringResource(R.string.empty_tomorrow_title),
                    subtitle = stringResource(R.string.empty_tomorrow_subtitle)
                )
            }
        } else {
            items(
                items = state.filteredTomorrowTasks,
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

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun NocturnalTomorrowHeroBanner(
    tomorrowHumanDate: String,
    tomorrowIso: String,
    totalTasksCount: Int,
    totalPlannedMinutes: Int,
    freeSlotsCount: Int,
    isCalendarConnected: Boolean
) {
    val hours = totalPlannedMinutes / 60
    val mins = totalPlannedMinutes % 60
    val timeFormatted = if (hours > 0) "${hours}h ${mins}m" else "${mins}m"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = NightCardElevated),
        border = BorderStroke(1.dp, NightBorderSubtle)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = NocturnalAmber.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, NocturnalAmber.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.NightsStay,
                            contentDescription = null,
                            tint = NocturnalAmber,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Modo Planificación Nocturna",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = NocturnalAmber
                        )
                    }
                }

                SmallBadgePill(
                    text = if (isCalendarConnected) "Calendar Activo" else "Sin vincular",
                    accentColor = if (isCalendarConnected) SyncSuccessGreen else TextMutedSlate
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Mañana · $tomorrowHumanDate",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = TextMoonlight
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "$totalTasksCount bloques ($timeFormatted planificados) · $freeSlotsCount huecos libres",
                style = MaterialTheme.typography.bodyMedium,
                color = TextStarlight
            )
        }
    }
}

@Composable
private fun ExistingCalendarEventsStripCard(
    isConnected: Boolean,
    isLoading: Boolean,
    externalEvents: List<ExternalCalendarEvent>,
    overlapCount: Int,
    onRefresh: () -> Unit,
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
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.EventNote,
                        contentDescription = null,
                        tint = CalendarImportedPurple,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Eventos Existentes en Google Calendar (Mañana)",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = TextMoonlight
                        )
                        Text(
                            text = "Lectura en vivo (listTomorrowEvents) para prevenir solapamientos",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMutedSlate
                        )
                    }
                }

                if (isConnected) {
                    OutlinedButton(
                        onClick = onRefresh,
                        enabled = !isLoading,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("refresh_tomorrow_calendar_btn")
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Actualizar eventos de Google Calendar",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Importar", style = MaterialTheme.typography.labelSmall)
                    }
                } else {
                    OutlinedButton(
                        onClick = onConnectClick,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("connect_calendar_from_plan_btn")
                    ) {
                        Text("Conectar OAuth", style = MaterialTheme.typography.labelSmall)
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
                            "${ev.startTimeHHmm ?: "??:??"} – ${ev.endTimeHHmm ?: "??:??"}"
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = CalendarImportedPurple.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, CalendarImportedPurple.copy(alpha = 0.4f))
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                Text(
                                    text = ev.title,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = TextMoonlight
                                )
                                Text(
                                    text = "📅 $timeText",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CalendarImportedPurple
                                )
                            }
                        }
                    }
                }
            } else if (isConnected && !isLoading) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "No se encontraron eventos externos previos para mañana en tu Google Calendar principal. ¡Tienes la agenda libre!",
                    style = MaterialTheme.typography.bodySmall,
                    color = SyncSuccessGreen
                )
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
                        text = "Atención: Tienes $overlapCount bloque(s) con solapamiento horario. Revisa las tarjetas o la pestaña Agenda 24h.",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = ConflictWarningRed
                    )
                }
            }
        }
    }
}

@Composable
private fun ConfirmTomorrowSyncCard(
    totalTasks: Int,
    syncEnabledCount: Int,
    alreadySyncedCount: Int,
    isSyncing: Boolean,
    onConfirmAndSync: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = NightCardElevated),
        border = BorderStroke(1.dp, NightBorderSubtle)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Cierre de Planificación Nocturna",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = TextMoonlight
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "$syncEnabledCount de $totalTasks tareas marcadas para Google Calendar",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMutedSlate
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onConfirmAndSync,
                enabled = !isSyncing && syncEnabledCount > 0,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("confirm_sync_tomorrow_btn"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElectricIndigo,
                    contentColor = Color(0xFFFFFFFF)
                )
            ) {
                if (isSyncing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Sincronizando con Google Calendar…",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.CloudSync,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.btn_sync_tomorrow),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyPlanStateCard(
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
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(ElectricIndigo.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.NightsStay,
                    contentDescription = null,
                    tint = ElectricIndigo,
                    modifier = Modifier.size(28.dp)
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
