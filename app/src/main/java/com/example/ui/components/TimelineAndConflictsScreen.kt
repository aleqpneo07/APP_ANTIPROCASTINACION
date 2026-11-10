package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.NightTaskEntity
import com.example.ui.NightPlannerUiState
import com.example.ui.theme.CalendarImportedPurple
import com.example.ui.theme.ConflictWarningRed
import com.example.ui.theme.ElectricIndigo
import com.example.ui.theme.NightBorderSubtle
import com.example.ui.theme.NightCardElevated
import com.example.ui.theme.NightCardSurface
import com.example.ui.theme.NocturnalAmber
import com.example.ui.theme.SyncSuccessGreen
import com.example.ui.theme.TextMoonlight
import com.example.ui.theme.TextMutedSlate
import com.example.ui.theme.TextStarlight

private data class TimelineBlockItem(
    val startTimeHHmm: String,
    val endTimeHHmm: String,
    val title: String,
    val subtitle: String,
    val accentColor: Color,
    val isExternalGoogleEvent: Boolean,
    val hasConflict: Boolean
)

@Composable
fun TimelineAndConflictsScreen(
    state: NightPlannerUiState,
    onAssignTaskToFreeSlot: (task: NightTaskEntity, startHHmm: String, durationMin: Int) -> Unit,
    onRefreshCalendarEvents: () -> Unit,
    modifier: Modifier = Modifier
) {
    val conflictTaskIds = state.overlapWarnings.map { it.taskId }.toSet()
    val syncedTaskEventIds = state.tomorrowTasks
        .mapNotNull { it.googleEventId?.takeIf { id -> id.isNotBlank() } }
        .toSet()

    val timelineBlocks = buildList {
        state.importedTomorrowEvents
            .filter { ext ->
                ext.id !in syncedTaskEventIds &&
                    !ext.isAllDay &&
                    ext.startTimeHHmm != null &&
                    ext.endTimeHHmm != null
            }
            .forEach { ext ->
                add(
                    TimelineBlockItem(
                        startTimeHHmm = ext.startTimeHHmm!!,
                        endTimeHHmm = ext.endTimeHHmm!!,
                        title = ext.title,
                        subtitle = "Google Calendar (Importado modo lectura)",
                        accentColor = CalendarImportedPurple,
                        isExternalGoogleEvent = true,
                        hasConflict = false
                    )
                )
            }

        state.tomorrowTasks
            .filter { !it.startTime.isNullOrBlank() && !it.endTime.isNullOrBlank() }
            .forEach { task ->
                add(
                    TimelineBlockItem(
                        startTimeHHmm = task.startTime!!,
                        endTimeHHmm = task.endTime!!,
                        title = task.title,
                        subtitle = "${task.categoryEnum.displayName} · Prioridad ${task.priorityEnum.displayName}",
                        accentColor = task.categoryEnum.color,
                        isExternalGoogleEvent = false,
                        hasConflict = task.id in conflictTaskIds
                    )
                )
            }
    }.sortedBy { it.startTimeHHmm }

    val unscheduledTasks = state.tomorrowTasks.filter {
        it.startTime.isNullOrBlank() || it.endTime.isNullOrBlank()
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("timeline_conflicts_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Header Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = NightCardElevated),
                border = BorderStroke(1.dp, NightBorderSubtle)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Time Blocking & Prevención de Solapamientos",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = TextMoonlight
                            )
                            Text(
                                text = "Mañana · ${state.tomorrowHumanEs}",
                                style = MaterialTheme.typography.bodySmall,
                                color = NocturnalAmber
                            )
                        }
                        OutlinedButton(
                            onClick = onRefreshCalendarEvents,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Sincronizar Lectura", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (state.overlapWarnings.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = SyncSuccessGreen.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, SyncSuccessGreen.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Verified,
                                    contentDescription = null,
                                    tint = SyncSuccessGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Cero solapamientos horarios. Tus bloques de estudio/gimnasio encajan con tu Google Calendar.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SyncSuccessGreen
                                )
                            }
                        }
                    } else {
                        state.overlapWarnings.forEach { warning ->
                            val conflictingTask = state.tomorrowTasks.find { it.id == warning.taskId }
                            val firstFreeSlot = state.freeTimeSlots.firstOrNull {
                                it.durationMinutes >= (conflictingTask?.durationMinutes ?: 60)
                            } ?: state.freeTimeSlots.firstOrNull()

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                shape = RoundedCornerShape(12.dp),
                                color = ConflictWarningRed.copy(alpha = 0.14f),
                                border = BorderStroke(1.dp, ConflictWarningRed.copy(alpha = 0.5f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.EventBusy,
                                            contentDescription = null,
                                            tint = ConflictWarningRed,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Conflicto (${warning.overlapWindow}): '${warning.taskTitle}' choca con '${warning.conflictingWithTitle}'",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                            color = Color(0xFFFECDD3)
                                        )
                                    }
                                    if (conflictingTask != null && firstFreeSlot != null) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Button(
                                            onClick = {
                                                onAssignTaskToFreeSlot(
                                                    conflictingTask,
                                                    firstFreeSlot.startTimeHHmm,
                                                    conflictingTask.durationMinutes ?: 60
                                                )
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = ConflictWarningRed,
                                                contentColor = Color(0xFF28050C)
                                            ),
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AutoFixHigh,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Reubicar en hueco libre (${firstFreeSlot.startTimeHHmm})",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Detected Free Slots ("Huecos Libres") Section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = NightCardSurface),
                border = BorderStroke(1.dp, NightBorderSubtle)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Huecos Libres Disponibles Mañana (08:00 – 22:00)",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = SyncSuccessGreen
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Calculados restando tus eventos de Google Calendar y tareas ya bloqueadas.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMutedSlate
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    state.freeTimeSlots.forEach { slot ->
                        val hours = slot.durationMinutes / 60
                        val mins = slot.durationMinutes % 60
                        val durLabel = if (hours > 0) "${hours}h ${mins}m" else "${mins}m"

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF0A101E),
                            border = BorderStroke(1.dp, SyncSuccessGreen.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Schedule,
                                        contentDescription = null,
                                        tint = SyncSuccessGreen,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "${slot.startTimeHHmm} – ${slot.endTimeHHmm}",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = TextMoonlight
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "(${slot.periodLabel})",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextMutedSlate
                                    )
                                }
                                SmallBadgePill(
                                    text = "Libre: $durLabel",
                                    accentColor = SyncSuccessGreen
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Unscheduled Tasks Quick-Assign to Free Slot
        if (unscheduledTasks.isNotEmpty() && state.freeTimeSlots.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = NightCardSurface),
                    border = BorderStroke(1.dp, ElectricIndigo.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Tareas sin hora fija (${unscheduledTasks.size}) · Asignación rápida a Hueco Libre",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = ElectricIndigo
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        unscheduledTasks.forEach { task ->
                            val taskDur = task.durationMinutes ?: 60
                            val suggestedSlot = state.freeTimeSlots.firstOrNull {
                                it.durationMinutes >= taskDur
                            } ?: state.freeTimeSlots.first()
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = task.title,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = TextMoonlight
                                    )
                                    Text(
                                        text = "${task.categoryEnum.displayName} · ${task.durationMinutes ?: 60} min",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextMutedSlate
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                OutlinedButton(
                                    onClick = {
                                        onAssignTaskToFreeSlot(
                                            task,
                                            suggestedSlot.startTimeHHmm,
                                            task.durationMinutes ?: 60
                                        )
                                    },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        text = "Asignar ${suggestedSlot.startTimeHHmm}",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Chronological Timeline Blocks
        item {
            Text(
                text = "Línea de Tiempo Cronológica de Mañana (${timelineBlocks.size} bloques)",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = TextMoonlight
            )
        }

        items(timelineBlocks) { block ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = NightCardSurface),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (block.hasConflict) ConflictWarningRed else block.accentColor.copy(alpha = 0.45f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = block.accentColor.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, block.accentColor.copy(alpha = 0.45f))
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = block.startTimeHHmm,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = block.accentColor
                            )
                            Text(
                                text = block.endTimeHHmm,
                                style = MaterialTheme.typography.labelSmall,
                                color = TextStarlight
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(block.accentColor)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = block.title,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = TextMoonlight
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = block.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMutedSlate
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
