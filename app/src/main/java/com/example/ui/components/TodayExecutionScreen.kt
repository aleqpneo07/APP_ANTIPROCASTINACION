package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Forward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.NightTaskEntity
import com.example.model.TaskCategory
import com.example.ui.NightPlannerUiState
import com.example.ui.theme.ElectricIndigo
import com.example.ui.theme.NightBorderSubtle
import com.example.ui.theme.NightCardElevated
import com.example.ui.theme.NightCardSurface
import com.example.ui.theme.NocturnalAmber
import com.example.ui.theme.SyncSuccessGreen
import com.example.ui.theme.TextMoonlight
import com.example.ui.theme.TextMutedSlate
import com.example.ui.theme.TextStarlight

@Composable
fun TodayExecutionScreen(
    state: NightPlannerUiState,
    onToggleComplete: (NightTaskEntity) -> Unit,
    onEditTask: (NightTaskEntity) -> Unit,
    onDeleteTask: (NightTaskEntity) -> Unit,
    onSelectCategoryFilter: (TaskCategory?) -> Unit,
    onMigratePendingToTomorrow: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = state.todayProgressFraction,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "todayProgress"
    )
    val percentageInt = (animatedProgress * 100).toInt()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("today_execution_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Daily Execution Progress Header Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = NightCardElevated),
                border = BorderStroke(1.dp, ElectricIndigo.copy(alpha = 0.4f))
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.WbSunny,
                                contentDescription = null,
                                tint = NocturnalAmber,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Modo Ejecución Diaria · Hoy",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = TextMoonlight
                                )
                                Text(
                                    text = state.todayHumanEs,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = NocturnalAmber
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = SyncSuccessGreen.copy(alpha = 0.16f),
                            border = BorderStroke(1.dp, SyncSuccessGreen.copy(alpha = 0.45f))
                        ) {
                            Text(
                                text = "$percentageInt% Completado",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = SyncSuccessGreen,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(50))
                            .testTag("today_progress_bar"),
                        color = SyncSuccessGreen,
                        trackColor = Color(0xFF0A101E),
                        strokeCap = StrokeCap.Round
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = SyncSuccessGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${state.todayCompletedCount} completadas · ${state.todayPendingCount} pendientes",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextStarlight
                            )
                        }
                        Text(
                            text = "Marcar añade '✅' en Calendar",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMutedSlate
                        )
                    }
                }
            }
        }

        // 2. Category Filter Bar
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Filtrar checklist por categoría",
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
                    FilterChip(
                        selected = state.selectedTodayCategoryFilter == null,
                        onClick = { onSelectCategoryFilter(null) },
                        label = { Text("Todas (${state.todayTasks.size})") },
                        modifier = Modifier.testTag("filter_today_all")
                    )
                    TaskCategory.entries.forEach { cat ->
                        val count = state.todayTasks.count { it.categoryEnum == cat }
                        FilterChip(
                            selected = state.selectedTodayCategoryFilter == cat,
                            onClick = {
                                onSelectCategoryFilter(
                                    if (state.selectedTodayCategoryFilter == cat) null else cat
                                )
                            },
                            label = { Text("${cat.displayName} ($count)") },
                            modifier = Modifier.testTag("filter_today_${cat.name.lowercase()}")
                        )
                    }
                }
            }
        }

        // 3. Today's Tasks Checklist
        if (state.filteredTodayTasks.isEmpty()) {
            item {
                EmptyPlanStateCard(
                    title = stringResource(R.string.empty_today_title),
                    subtitle = stringResource(R.string.empty_today_subtitle)
                )
            }
        } else {
            items(
                items = state.filteredTodayTasks,
                key = { it.id }
            ) { task ->
                TaskItemCard(
                    task = task,
                    overlapWarning = null,
                    onToggleComplete = onToggleComplete,
                    onEditClick = onEditTask,
                    onDeleteClick = onDeleteTask
                )
            }
        }

        // 4. End-of-Day 1-Click Rollover Card: "Migrar pendientes a mañana"
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = NightCardSurface),
                border = BorderStroke(1.dp, NocturnalAmber.copy(alpha = 0.45f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "¿Terminaste el día con tareas sin completar?",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = NocturnalAmber
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Transfiere con un solo clic las ${state.todayPendingCount} tarea(s) pendiente(s) de hoy directamente al plan de mañana (${state.tomorrowDateIso}) y actualiza sus eventos en Google Calendar.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextStarlight
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onMigratePendingToTomorrow,
                        enabled = state.todayPendingCount > 0 && !state.isSyncing,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("migrate_pending_tomorrow_btn"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NocturnalAmber,
                            contentColor = Color(0xFF1C1300)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Forward,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${stringResource(R.string.btn_migrate_pending)} (${state.todayPendingCount})",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
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
