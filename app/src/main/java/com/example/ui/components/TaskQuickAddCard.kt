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
import androidx.compose.runtime.LaunchedEffect
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
import com.example.ui.TaskDayFilter
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TaskQuickAddCard(
    todayIso: String,
    tomorrowIso: String,
    afterTomorrowIso: String,
    selectedDayFilter: TaskDayFilter,
    freeSlotsToday: List<FreeTimeSlot>,
    freeSlotsTomorrow: List<FreeTimeSlot>,
    prefillTitle: String?,
    prefillCategory: TaskCategory?,
    onPrefillConsumed: () -> Unit,
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
    modifier: Modifier = Modifier
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    var rawTitle by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }
    var chosenDateIso by rememberSaveable { mutableStateOf(todayIso) }
    var selectedCategory by rememberSaveable { mutableStateOf(TaskCategory.UNIVERSIDAD) }
    var selectedPriority by rememberSaveable { mutableStateOf(TaskPriority.ALTA) }
    var startTimeInput by rememberSaveable { mutableStateOf("") }
    var endTimeInput by rememberSaveable { mutableStateOf("") }
    var durationInput by rememberSaveable { mutableStateOf("60") }
    var syncWithCalendar by rememberSaveable { mutableStateOf(true) }
    var showAdvancedOptions by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(selectedDayFilter) {
        when (selectedDayFilter) {
            TaskDayFilter.TODAY -> chosenDateIso = todayIso
            TaskDayFilter.TOMORROW -> chosenDateIso = tomorrowIso
            TaskDayFilter.ALL -> Unit
        }
    }

    LaunchedEffect(prefillTitle) {
        if (!prefillTitle.isNullOrBlank()) {
            rawTitle = prefillTitle
            if (prefillCategory != null) selectedCategory = prefillCategory
            showAdvancedOptions = true
            onPrefillConsumed()
        }
    }

    val liveParsed by remember(rawTitle) {
        derivedStateOf { QuickInputParser.parse(rawTitle) }
    }

    val activeFreeSlots = when (chosenDateIso) {
        tomorrowIso -> freeSlotsTomorrow
        todayIso -> freeSlotsToday
        else -> emptyList()
    }

    val chosenDateLabel = when (chosenDateIso) {
        todayIso -> "Hoy"
        tomorrowIso -> "Mañana"
        afterTomorrowIso -> "Pasado mañana"
        else -> chosenDateIso
    }

    val submitTask = {
        if (rawTitle.isNotBlank()) {
            onAddTask(
                chosenDateIso,
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
            .testTag("task_quick_add_card"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = NightCardElevated),
        border = BorderStroke(1.dp, ElectricIndigo.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                            .background(ElectricIndigo.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = ElectricIndigoBright,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Proponer tarea o actividad",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = TextMoonlight
                        )
                        Text(
                            text = "Se guardará para $chosenDateLabel",
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
                            tint = if (showAdvancedOptions) ElectricIndigoBright else TextMutedSlate,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (showAdvancedOptions) "Compacto" else "Opciones",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                            color = if (showAdvancedOptions) ElectricIndigoBright else TextMutedSlate
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Target Date Chips (Hoy / Mañana / Pasado)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    todayIso to "Para Hoy",
                    tomorrowIso to "Para Mañana",
                    afterTomorrowIso to "Pasado"
                ).forEach { (dateIso, label) ->
                    val selected = chosenDateIso == dateIso
                    FilterChip(
                        selected = selected,
                        onClick = { chosenDateIso = dateIso },
                        modifier = Modifier.weight(1f),
                        label = {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ElectricIndigo.copy(alpha = 0.2f),
                            selectedLabelColor = ElectricIndigoBright,
                            containerColor = NightBgDeep,
                            labelColor = TextMutedSlate
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selected,
                            selectedBorderColor = ElectricIndigo.copy(alpha = 0.5f),
                            borderColor = NightBorderSubtle
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Main Quick Input TextField
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
                    focusedContainerColor = NightBgDeep,
                    unfocusedContainerColor = NightBgDeep,
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
                    "@10:00",
                    "@18:00-19:30",
                    "+45m",
                    "+90m"
                ).forEach { token ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = NightCardSurface,
                        border = BorderStroke(1.dp, NightBorderSubtle),
                        modifier = Modifier.clickable {
                            rawTitle = if (rawTitle.isBlank()) "$token " else "${rawTitle.trimEnd()} $token "
                        }
                    ) {
                        Text(
                            text = token,
                            style = MaterialTheme.typography.labelSmall,
                            color = ElectricIndigoBright,
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
                            SmallBadgePill(text = "$st–$endLabel", accentColor = ElectricIndigoBright)
                        }
                        liveParsed.detectedDurationMinutes?.let { dur ->
                            SmallBadgePill(text = "${dur} min", accentColor = SyncSuccessGreen)
                        }
                    }
                }
            }

            // Expandable Details: Notes, Category, Priority, Time-Blocking, Free Slots, Calendar Sync
            AnimatedVisibility(visible = showAdvancedOptions) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = NightBorderSubtle)
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("quick_task_notes_input"),
                        label = { Text("Notas / Descripción (Opcional)", style = MaterialTheme.typography.labelSmall) },
                        placeholder = { Text("Detalles o entrega (se envía a Google Calendar)…") },
                        maxLines = 2,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Categoría",
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
                                    containerColor = NightBgDeep,
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

                    Text(
                        text = "Prioridad",
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
                                    containerColor = NightBgDeep,
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

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = ElectricIndigoBright,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Horario y tiempo estimado",
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
                                Text("Limpiar hora", style = MaterialTheme.typography.labelSmall)
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
                            label = { Text("Inicio", style = MaterialTheme.typography.labelSmall) },
                            placeholder = { Text("17:00") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = endTimeInput,
                            onValueChange = { endTimeInput = it.take(5) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("quick_end_time_input"),
                            label = { Text("Fin", style = MaterialTheme.typography.labelSmall) },
                            placeholder = { Text("18:30") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = durationInput,
                            onValueChange = { durationInput = it.filter { ch -> ch.isDigit() }.take(3) },
                            modifier = Modifier
                                .width(100.dp)
                                .testTag("quick_duration_input"),
                            label = { Text("Minutos", style = MaterialTheme.typography.labelSmall) },
                            placeholder = { Text("60") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    // Free Slot Suggestions for chosen date (accounting for uni classes + Google Calendar)
                    if (activeFreeSlots.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Huecos libres ($chosenDateLabel, sin clases ni Calendar):",
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
                            activeFreeSlots.take(6).forEach { slot ->
                                val suggestedEnd = QuickInputParser.addMinutesToHHmm(
                                    slot.startTimeHHmm,
                                    (durationInput.toIntOrNull() ?: 60).coerceAtMost(slot.durationMinutes)
                                )
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = SyncSuccessGreen.copy(alpha = 0.12f),
                                    border = BorderStroke(1.dp, SyncSuccessGreen.copy(alpha = 0.35f)),
                                    modifier = Modifier.clickable {
                                        startTimeInput = slot.startTimeHHmm
                                        endTimeInput = suggestedEnd
                                    }
                                ) {
                                    Text(
                                        text = "${slot.startTimeHHmm}–${slot.endTimeHHmm} (${slot.periodLabel})",
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
                        color = NightBgDeep,
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
                                        text = "Enviar a Google Calendar",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = TextMoonlight
                                    )
                                    Text(
                                        text = if (startTimeInput.isNotBlank()) {
                                            "Bloque $startTimeInput – ${endTimeInput.ifBlank { "…" }}"
                                        } else {
                                            "Sin hora fija: todo el día o bloque por defecto"
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
                    .testTag("submit_add_task_btn"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElectricIndigo,
                    contentColor = Color.White
                )
            ) {
                Icon(
                    imageVector = Icons.Default.AddTask,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Añadir para $chosenDateLabel",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}
