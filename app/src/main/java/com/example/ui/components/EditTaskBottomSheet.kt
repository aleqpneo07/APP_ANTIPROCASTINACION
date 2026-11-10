package com.example.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.NightTaskEntity
import com.example.model.TaskCategory
import com.example.model.TaskPriority
import com.example.ui.theme.ElectricIndigo
import com.example.ui.theme.NightCardElevated
import com.example.ui.theme.TextMoonlight
import com.example.ui.theme.TextMutedSlate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTaskBottomSheet(
    task: NightTaskEntity,
    onDismiss: () -> Unit,
    onSave: (NightTaskEntity) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var title by rememberSaveable(task.id) { mutableStateOf(task.title) }
    var notes by rememberSaveable(task.id) { mutableStateOf(task.notes) }
    var category by rememberSaveable(task.id) { mutableStateOf(task.categoryEnum) }
    var priority by rememberSaveable(task.id) { mutableStateOf(task.priorityEnum) }
    var startTime by rememberSaveable(task.id) { mutableStateOf(task.startTime.orEmpty()) }
    var endTime by rememberSaveable(task.id) { mutableStateOf(task.endTime.orEmpty()) }
    var durationStr by rememberSaveable(task.id) {
        mutableStateOf(task.durationMinutes?.toString().orEmpty())
    }
    var syncWithCalendar by rememberSaveable(task.id) { mutableStateOf(task.syncWithCalendar) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = NightCardElevated
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text(
                text = "Editar Tarea & Bloque de Tiempo",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = TextMoonlight
            )
            if (!task.googleEventId.isNullOrBlank()) {
                Text(
                    text = "Al guardar se enviará events.patch a Google Calendar (ID: ${task.googleEventId})",
                    style = MaterialTheme.typography.bodySmall,
                    color = ElectricIndigo
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Título de la tarea") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("edit_task_title_input"),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notas / Descripción (se envía a Google Calendar)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("edit_task_notes_input"),
                maxLines = 3,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Categoría",
                style = MaterialTheme.typography.labelLarge,
                color = TextMutedSlate
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TaskCategory.entries.forEach { cat ->
                    FilterChip(
                        selected = category == cat,
                        onClick = { category = cat },
                        label = { Text(cat.displayName) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Prioridad",
                style = MaterialTheme.typography.labelLarge,
                color = TextMutedSlate
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TaskPriority.entries.forEach { prio ->
                    FilterChip(
                        selected = priority == prio,
                        onClick = { priority = prio },
                        label = { Text(prio.displayName) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = startTime,
                    onValueChange = { startTime = it.take(5) },
                    label = { Text("Inicio (HH:mm)") },
                    placeholder = { Text("09:00") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = endTime,
                    onValueChange = { endTime = it.take(5) },
                    label = { Text("Fin (HH:mm)") },
                    placeholder = { Text("10:30") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = durationStr,
                    onValueChange = { durationStr = it.filter { ch -> ch.isDigit() }.take(3) },
                    label = { Text("Min") },
                    placeholder = { Text("60") },
                    modifier = Modifier.width(92.dp),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Sincronizar con Google Calendar",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = TextMoonlight
                    )
                    Text(
                        text = "Actualiza el evento mediante events.patch",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMutedSlate
                    )
                }
                Switch(
                    checked = syncWithCalendar,
                    onCheckedChange = { syncWithCalendar = it }
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Cancelar")
                }
                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            onSave(
                                task.copy(
                                    title = title.trim(),
                                    notes = notes.trim(),
                                    category = category.name,
                                    priority = priority.name,
                                    startTime = startTime.trim().ifBlank { null },
                                    endTime = endTime.trim().ifBlank { null },
                                    durationMinutes = durationStr.toIntOrNull(),
                                    syncWithCalendar = syncWithCalendar
                                )
                            )
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("save_edited_task_btn"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricIndigo,
                        contentColor = Color(0xFF080C16)
                    )
                ) {
                    Text("Guardar y Sincronizar", fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
