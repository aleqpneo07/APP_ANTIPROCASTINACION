package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddTask
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.FreeTimeSlot
import com.example.model.QuickInputParser
import com.example.model.TaskCategory
import com.example.model.TaskPriority
import com.example.ui.theme.ElectricIndigo
import com.example.ui.theme.NightBorderSubtle
import com.example.ui.theme.NightCardElevated
import com.example.ui.theme.NightCardSurface
import com.example.ui.theme.NocturnalAmber
import com.example.ui.theme.SyncSuccessGreen
import com.example.ui.theme.TextMoonlight
import com.example.ui.theme.TextMutedSlate
import com.example.ui.theme.TextStarlight

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NocturnalQuickAddCard(
    tomorrowHumanDate: String,
    freeSlots: List<FreeTimeSlot>,
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
    modifier: Modifier = Modifier
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    var rawTitle by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf(TaskCategory.UNIVERSIDAD) }
    var selectedPriority by rememberSaveable { mutableStateOf(TaskPriority.ALTA) }
    var startTimeInput by rememberSaveable { mutableStateOf("") }
    var endTimeInput by rememberSaveable { mutableStateOf("") }
    var durationInput by rememberSaveable { mutableStateOf("60") }
    var syncWithCalendar by rememberSaveable { mutableStateOf(true) }
    var showAdvancedOptions by rememberSaveable { mutableStateOf(true) }

    val liveParsed by remember(rawTitle) {
        derivedStateOf { QuickInputParser.parse(rawTitle) }
    }

    val submitTask = {
        if (rawTitle.isNotBlank()) {
            onAddTask(
                rawTitle,
                notes,
                selectedCategory,
                selectedPriority,
                startTimeInput.takeIf { it.isNotBlank() },
                endTimeInput.takeIf { it.isNotBlank() },
                durationInput.toIntOrNull(),
                syncWithCalendar
            )
            rawTitle = ""
            notes = ""
            startTimeInput = ""
            endTimeInput = ""
            keyboardController?.hide()
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("nocturnal_quick_add_card"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = NightCardElevated),
        border = BorderStroke(1.dp, ElectricIndigo.copy(alpha = 0.45f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(ElectricIndigo.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = ElectricIndigo,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Añadir tarea para mañana",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = TextMoonlight
                        )
                        Text(
                            text = "Asignación: $tomorrowHumanDate",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMutedSlate
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    onClick = { showAdvancedOptions = !showAdvancedOptions },
                    shape = RoundedCornerShape(10.dp),
                    color = if (showAdvancedOptions) ElectricIndigo.copy(alpha = 0.14f) else NightCardSurface,
                    border = BorderStroke(
                        1.dp,
                        if (showAdvancedOptions) ElectricIndigo.copy(alpha = 0.35f) else NightBorderSubtle
                    ),
                    modifier = Modifier.testTag("toggle_advanced_composer_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (showAdvancedOptions) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = if (showAdvancedOptions) ElectricIndigo else TextMutedSlate,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (showAdvancedOptions) "Compacto" else "Opciones",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                            color = if (showAdvancedOptions) ElectricIndigo else TextMutedSlate
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Main Quick Input TextField with Keyboard Shortcut support
            OutlinedTextField(
                value = rawTitle,
                onValueChange = { rawTitle = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("quick_task_title_input"),
                placeholder = {
                    Text(
                        text = stringResource(R.string.quick_add_placeholder),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMutedSlate
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submitTask() }),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ElectricIndigo,
                    unfocusedBorderColor = NightBorderSubtle,
                    focusedContainerColor = Color(0xFF0A101E),
                    unfocusedContainerColor = Color(0xFF0A101E),
                    focusedTextColor = TextMoonlight,
                    unfocusedTextColor = TextMoonlight
                )
            )

            // Quick Keyboard Shortcut Token Bar (1-Tap Insertion)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Atajos:",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMutedSlate
                )
                listOf(
                    "#uni",
                    "#gym",
                    "#proyectos",
                    "#personal",
                    "!alta",
                    "!media",
                    "@09:00-10:30",
                    "@17:00-18:00",
                    "+45m"
                ).forEach { token ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF0E1629),
                        border = BorderStroke(1.dp, NightBorderSubtle),
                        modifier = Modifier.clickable {
                            rawTitle = if (rawTitle.isBlank()) "$token " else "${rawTitle.trimEnd()} $token "
                        }
                    ) {
                        Text(
                            text = token,
                            style = MaterialTheme.typography.labelSmall,
                            color = ElectricIndigo,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            // Live Parsed Preview if inline tags detected
            if (liveParsed.detectedCategory != null ||
                liveParsed.detectedPriority != null ||
                liveParsed.detectedStartTime != null ||
                liveParsed.detectedDurationMinutes != null
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = ElectricIndigo.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, ElectricIndigo.copy(alpha = 0.35f))
                ) {
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Detectado:",
                            style = MaterialTheme.typography.labelSmall,
                            color = NocturnalAmber
                        )
                        liveParsed.detectedCategory?.let {
                            SmallBadgePill(text = it.displayName, accentColor = it.color)
                        }
                        liveParsed.detectedPriority?.let {
                            SmallBadgePill(text = "Prio ${it.displayName}", accentColor = it.color)
                        }
                        liveParsed.detectedStartTime?.let { st ->
                            val endLabel = liveParsed.detectedEndTime ?: "…"
                            SmallBadgePill(text = "⏰ $st-$endLabel", accentColor = ElectricIndigo)
                        }
                    }
                }
            }

            // Expandable Selectors: Category, Priority, Time-Blocking, Free Slots, Calendar Sync
            AnimatedVisibility(visible = showAdvancedOptions) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = NightBorderSubtle)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Optional Notes Input
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("quick_task_notes_input"),
                        label = { Text("Notas / Descripción (Opcional)", style = MaterialTheme.typography.labelSmall) },
                        placeholder = { Text("Detalles adicionales que se sincronizarán en Google Calendar…") },
                        maxLines = 2,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Category Selection Row
                    Text(
                        text = "Categoría (con color en Google Calendar)",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextStarlight
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TaskCategory.entries.forEach { category ->
                            val isSelected = selectedCategory == category
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedCategory = category },
                                label = {
                                    Text(
                                        text = category.displayName,
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = category.color.copy(alpha = 0.22f),
                                    selectedLabelColor = category.color,
                                    containerColor = Color(0xFF0A101E),
                                    labelColor = TextMutedSlate
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    selectedBorderColor = category.color,
                                    borderColor = NightBorderSubtle
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Priority Selection Row
                    Text(
                        text = "Prioridad de ejecución",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextStarlight
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TaskPriority.entries.forEach { priority ->
                            val isSelected = selectedPriority == priority
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedPriority = priority },
                                modifier = Modifier.weight(1f),
                                label = {
                                    Text(
                                        text = priority.displayName,
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = priority.color.copy(alpha = 0.22f),
                                    selectedLabelColor = priority.color,
                                    containerColor = Color(0xFF0A101E),
                                    labelColor = TextMutedSlate
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    selectedBorderColor = priority.color,
                                    borderColor = NightBorderSubtle
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Time Blocking Inputs (Start, End, Duration)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = ElectricIndigo,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Time Blocking (Opcional)",
                                style = MaterialTheme.typography.labelLarge,
                                color = TextStarlight
                            )
                        }
                        if (startTimeInput.isNotBlank() || endTimeInput.isNotBlank()) {
                            TextButton(
                                onClick = {
                                    startTimeInput = ""
                                    endTimeInput = ""
                                }
                            ) {
                                Text("Limpiar hora (Todo el día)", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = startTimeInput,
                            onValueChange = { startTimeInput = it.take(5) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("quick_start_time_input"),
                            label = { Text("Inicio (HH:mm)", style = MaterialTheme.typography.labelSmall) },
                            placeholder = { Text("09:00") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = endTimeInput,
                            onValueChange = { endTimeInput = it.take(5) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("quick_end_time_input"),
                            label = { Text("Fin (HH:mm)", style = MaterialTheme.typography.labelSmall) },
                            placeholder = { Text("10:30") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = durationInput,
                            onValueChange = { durationInput = it.filter { ch -> ch.isDigit() }.take(3) },
                            modifier = Modifier
                                .width(104.dp)
                                .testTag("quick_duration_input"),
                            label = { Text("Minutos", style = MaterialTheme.typography.labelSmall) },
                            placeholder = { Text("60") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    // 1-Tap Free Slot Suggestions ("Huecos Libres de Mañana")
                    if (freeSlots.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Huecos libres detectados mañana (toca para asignar bloque):",
                            style = MaterialTheme.typography.labelSmall,
                            color = SyncSuccessGreen
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            freeSlots.take(6).forEach { slot ->
                                val suggestedEnd = QuickInputParser.addMinutesToHHmm(
                                    slot.startTimeHHmm,
                                    (durationInput.toIntOrNull() ?: 60).coerceAtMost(slot.durationMinutes)
                                )
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = SyncSuccessGreen.copy(alpha = 0.12f),
                                    border = BorderStroke(1.dp, SyncSuccessGreen.copy(alpha = 0.4f)),
                                    modifier = Modifier.clickable {
                                        startTimeInput = slot.startTimeHHmm
                                        endTimeInput = suggestedEnd
                                    }
                                ) {
                                    Text(
                                        text = "Libre ${slot.startTimeHHmm}–${slot.endTimeHHmm} (${slot.periodLabel})",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SyncSuccessGreen,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Sync with Google Calendar Toggle
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF0A101E),
                        border = BorderStroke(1.dp, NightBorderSubtle)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EventAvailable,
                                    contentDescription = null,
                                    tint = if (syncWithCalendar) SyncSuccessGreen else TextMutedSlate,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Sincronizar con Google Calendar (syncWithCalendar)",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = TextMoonlight
                                    )
                                    Text(
                                        text = if (startTimeInput.isNotBlank()) {
                                            "Se creará con bloque exacto ($startTimeInput - ${endTimeInput.ifBlank { "…" }})"
                                        } else {
                                            "Sin hora fija: se creará como 'Todo el día' o bloque por defecto"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextMutedSlate
                                    )
                                }
                            }
                            Switch(
                                checked = syncWithCalendar,
                                onCheckedChange = { syncWithCalendar = it },
                                modifier = Modifier.testTag("sync_with_calendar_switch"),
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(0xFF061510),
                                    checkedTrackColor = SyncSuccessGreen
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Submit Button
            Button(
                onClick = submitTask,
                enabled = rawTitle.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("submit_add_tomorrow_task_btn"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElectricIndigo,
                    contentColor = Color(0xFF080C16)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.AddTask,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Añadir al Plan de Mañana",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}
