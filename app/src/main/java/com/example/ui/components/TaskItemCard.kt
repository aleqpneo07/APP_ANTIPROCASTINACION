package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.NightTaskEntity
import com.example.model.OverlapWarning
import com.example.model.SyncState
import com.example.ui.theme.ConflictWarningRed
import com.example.ui.theme.ElectricIndigo
import com.example.ui.theme.NightBorderSubtle
import com.example.ui.theme.NightCardSurface
import com.example.ui.theme.SyncSuccessGreen
import com.example.ui.theme.TextMoonlight
import com.example.ui.theme.TextMutedSlate
import com.example.ui.theme.TextStarlight

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TaskItemCard(
    task: NightTaskEntity,
    overlapWarning: OverlapWarning? = null,
    onToggleComplete: (NightTaskEntity) -> Unit,
    onEditClick: (NightTaskEntity) -> Unit,
    onDeleteClick: (NightTaskEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val category = task.categoryEnum
    val priority = task.priorityEnum
    val syncState = task.syncStateEnum

    val cardAlpha by animateFloatAsState(
        targetValue = if (task.completed) 0.72f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "cardAlpha"
    )
    val checkScale by animateFloatAsState(
        targetValue = if (task.completed) 1.05f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "checkScale"
    )
    val borderColor by animateColorAsState(
        targetValue = when {
            overlapWarning != null && !task.completed -> ConflictWarningRed.copy(alpha = 0.75f)
            task.completed -> SyncSuccessGreen.copy(alpha = 0.35f)
            else -> NightBorderSubtle
        },
        label = "borderColor"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .alpha(cardAlpha)
            .testTag("task_item_card_${task.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = NightCardSurface),
        border = BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            // Left Category Color Accent Strip
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(category.color)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, top = 12.dp, bottom = 12.dp, end = 6.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Custom Animated Completion Checkbox (48dp touch target)
                Box(
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .scale(checkScale)
                        .clip(CircleShape)
                        .clickable { onToggleComplete(task) }
                        .testTag("task_checkbox_${task.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        modifier = Modifier.size(26.dp),
                        shape = CircleShape,
                        color = if (task.completed) SyncSuccessGreen else Color.Transparent,
                        border = BorderStroke(
                            width = 2.dp,
                            color = if (task.completed) SyncSuccessGreen else category.color
                        )
                    ) {
                        if (task.completed) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Marcar como pendiente",
                                    tint = Color(0xFF061510),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Main Content Column
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onEditClick(task) }
                ) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            textDecoration = if (task.completed) TextDecoration.LineThrough else TextDecoration.None
                        ),
                        color = if (task.completed) TextMutedSlate else TextMoonlight,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (task.notes.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = task.notes,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMutedSlate,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Metadata Pills FlowRow
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Category Pill
                        SmallBadgePill(
                            text = category.displayName,
                            accentColor = category.color
                        )

                        // Priority Pill
                        SmallBadgePill(
                            text = "Prio ${priority.displayName}",
                            accentColor = priority.color
                        )

                        // Time Block Pill
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF0B1120),
                            border = BorderStroke(1.dp, NightBorderSubtle)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = ElectricIndigo,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = task.formattedTimeBlock,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextStarlight
                                )
                            }
                        }

                        // Google Calendar Sync Status Pill
                        CalendarSyncBadge(
                            syncWithCalendar = task.syncWithCalendar,
                            syncState = syncState,
                            googleEventId = task.googleEventId
                        )
                    }

                    // Overlap Warning Banner if detected
                    if (overlapWarning != null && !task.completed) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = ConflictWarningRed.copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, ConflictWarningRed.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.WarningAmber,
                                    contentDescription = "Solapamiento horario",
                                    tint = ConflictWarningRed,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Solapa (${overlapWarning.overlapWindow}) con: ${overlapWarning.conflictingWithTitle}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                    color = Color(0xFFFECDD3)
                                )
                            }
                        }
                    }
                }

                // Edit & Delete Action Buttons
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = { onEditClick(task) },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("edit_task_button_${task.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "Editar tarea",
                            tint = TextMutedSlate,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = { onDeleteClick(task) },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("delete_task_button_${task.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = "Eliminar tarea",
                            tint = ConflictWarningRed.copy(alpha = 0.85f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SmallBadgePill(
    text: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = accentColor.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(accentColor)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = accentColor
            )
        }
    }
}

@Composable
private fun CalendarSyncBadge(
    syncWithCalendar: Boolean,
    syncState: SyncState,
    googleEventId: String?
) {
    val (icon, label, tint) = when {
        !syncWithCalendar -> Triple(
            Icons.Outlined.CloudOff,
            "Solo local",
            TextMutedSlate
        )
        !googleEventId.isNullOrBlank() && syncState == SyncState.SYNCED -> Triple(
            Icons.Default.CloudDone,
            "GCal ID: ${googleEventId.take(6)}…",
            SyncSuccessGreen
        )
        syncState == SyncState.SYNC_ERROR -> Triple(
            Icons.Default.ErrorOutline,
            "Reintentar Sync",
            ConflictWarningRed
        )
        else -> Triple(
            Icons.Default.CloudQueue,
            "Sync Calendar activo",
            ElectricIndigo
        )
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = tint.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, tint.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = tint
            )
        }
    }
}
