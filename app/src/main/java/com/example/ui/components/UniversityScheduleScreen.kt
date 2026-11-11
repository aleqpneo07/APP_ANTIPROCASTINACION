package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.LiveStatusKind
import com.example.model.ScheduleEntry
import com.example.model.UniBuilding
import com.example.model.UniClassSession
import com.example.model.UniDay
import com.example.model.UniLiveStatus
import com.example.model.UniSubject
import com.example.model.UniversityScheduleRepository
import com.example.ui.theme.ElectricIndigo
import com.example.ui.theme.ElectricIndigoBright
import com.example.ui.theme.NightBgDeep
import com.example.ui.theme.NightBgSurface
import com.example.ui.theme.NightBorderSubtle
import com.example.ui.theme.NightCardElevated
import com.example.ui.theme.NightCardSurface
import com.example.ui.theme.NocturnalAmber
import com.example.ui.theme.SyncSuccessGreen
import com.example.ui.theme.TextMoonlight
import com.example.ui.theme.TextMutedSlate
import com.example.ui.theme.TextStarlight
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UniversityScheduleScreen(
    onCreateTaskFromClass: (subject: UniSubject, isPractice: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val initialDay = remember {
        UniversityScheduleRepository.dayFromCalendarDayOfWeek(
            Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        )
    }
    var selectedDayName by rememberSaveable { mutableStateOf(initialDay?.name) }
    var onlyPractices by rememberSaveable { mutableStateOf(false) }
    var liveStatus by remember { mutableStateOf(UniversityScheduleRepository.resolveLiveStatus()) }

    LaunchedEffect(Unit) {
        while (true) {
            liveStatus = UniversityScheduleRepository.resolveLiveStatus()
            delay(30_000L)
        }
    }

    val selectedDay: UniDay? = remember(selectedDayName) {
        selectedDayName?.let { name -> UniDay.entries.find { it.name == name } }
    }

    val entries = remember(selectedDay, onlyPractices) {
        UniversityScheduleRepository.entriesForSelection(selectedDay, onlyPractices)
    }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .testTag("university_schedule_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Header Banner
        item(key = "uni_header") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = NightCardElevated),
                border = BorderStroke(1.dp, NightBorderSubtle)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(ElectricIndigo.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = null,
                            tint = ElectricIndigoBright,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Mi Horario",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = TextMoonlight
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = UniversityScheduleRepository.COURSE_SUBTITLE,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMutedSlate
                        )
                    }
                }
            }
        }

        // 2. Live Class Tracker Card
        item(key = "uni_live_tracker") {
            LiveScheduleTrackerCard(
                status = liveStatus,
                onClick = {
                    val target = liveStatus.targetDay
                    selectedDayName = target.name
                    val highlightStart = liveStatus.highlightedStartMinutes
                    coroutineScope.launch {
                        if (highlightStart != null) {
                            val updatedEntries = UniversityScheduleRepository.entriesForSelection(
                                target,
                                onlyPractices
                            )
                            val targetIdx = updatedEntries.indexOfFirst { entry ->
                                entry is ScheduleEntry.ClassCard &&
                                    entry.session.day == target &&
                                    entry.session.startMinutes == highlightStart
                            }
                            if (targetIdx >= 0) {
                                // Account for the 4 fixed items above entries
                                listState.animateScrollToItem(4 + targetIdx)
                            }
                        } else {
                            listState.animateScrollToItem(0)
                        }
                    }
                }
            )
        }

        // 3. Building Legend & Practices Filter Row
        item(key = "uni_legend_filter") {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FlowRow(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        UniBuilding.entries.forEach { building ->
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = NightCardSurface,
                                border = BorderStroke(1.dp, NightBorderSubtle)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(building.color)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = building.displayName,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextStarlight
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Practice-only toggle pill
                    Surface(
                        onClick = { onlyPractices = !onlyPractices },
                        shape = RoundedCornerShape(50),
                        color = if (onlyPractices) NocturnalAmber else NightCardSurface,
                        border = BorderStroke(
                            1.dp,
                            if (onlyPractices) NocturnalAmber else NocturnalAmber.copy(alpha = 0.55f)
                        ),
                        modifier = Modifier.testTag("uni_practice_filter_btn")
                    ) {
                        Text(
                            text = if (onlyPractices) "Solo Prácticas ✓" else "Prácticas",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (onlyPractices) Color(0xFF14110B) else NocturnalAmber,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                        )
                    }
                }
            }
        }

        // 4. Day Selector Pills (Todo | Lun | Mar | Mié | Jue | Vie)
        item(key = "uni_day_tabs") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DaySelectorTabPill(
                    label = "Todo",
                    subLabel = "${UniversityScheduleRepository.allSessions.size} clases",
                    isSelected = selectedDay == null,
                    onClick = { selectedDayName = null }
                )
                UniDay.entries.forEach { day ->
                    val count = UniversityScheduleRepository.sessionsForDay(day).size
                    DaySelectorTabPill(
                        label = day.shortLabel,
                        subLabel = "$count clases",
                        isSelected = selectedDay == day,
                        onClick = { selectedDayName = day.name }
                    )
                }
            }
        }

        // 5. Schedule Entries
        items(
            items = entries,
            key = { it.key }
        ) { entry ->
            when (entry) {
                is ScheduleEntry.DayDivider -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = entry.day.fullLabel.uppercase(),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = ElectricIndigoBright
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(1.dp)
                                .background(NightBorderSubtle)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "${entry.classCount} clases · ${entry.timeRangeLabel}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMutedSlate
                        )
                    }
                }

                is ScheduleEntry.DaySummary -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${entry.classCount} clases · ${entry.timeRangeLabel}",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextMutedSlate
                        )
                    }
                }

                is ScheduleEntry.OverlapNotice -> {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = NocturnalAmber.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, NocturnalAmber.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.WarningAmber,
                                contentDescription = null,
                                tint = NocturnalAmber,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = entry.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = NocturnalAmber
                            )
                        }
                    }
                }

                is ScheduleEntry.BreakIndicator -> {
                    val breakColor = NightBorderSubtle
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(NightBgSurface)
                            .drawBehind {
                                drawRoundRect(
                                    color = breakColor,
                                    cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx()),
                                    style = Stroke(
                                        width = 1.dp.toPx(),
                                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                                    )
                                )
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Coffee,
                                contentDescription = null,
                                tint = TextMutedSlate,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = entry.label,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = TextStarlight
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = entry.timeRange,
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMutedSlate
                            )
                        }
                    }
                }

                is ScheduleEntry.ClassCard -> {
                    val isTodayClass = liveStatus.targetDay == entry.session.day &&
                        liveStatus.kind != LiveStatusKind.WEEKEND
                    val isActiveNow = isTodayClass &&
                        liveStatus.kind == LiveStatusKind.ACTIVE_NOW &&
                        liveStatus.activeWindowStartMinutes == entry.session.startMinutes
                    val isNextClass = isTodayClass &&
                        liveStatus.kind == LiveStatusKind.NEXT_TODAY &&
                        liveStatus.nextStartMinutes == entry.session.startMinutes

                    UniClassCard(
                        session = entry.session,
                        isActiveNow = isActiveNow,
                        isNextClass = isNextClass,
                        onCreateTaskClick = {
                            onCreateTaskFromClass(entry.session.subject, entry.session.isPractice)
                        }
                    )
                }
            }
        }

        item(key = "uni_bottom_spacer") {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun LiveScheduleTrackerCard(
    status: UniLiveStatus,
    onClick: () -> Unit
) {
    val dotColor = when (status.kind) {
        LiveStatusKind.ACTIVE_NOW -> SyncSuccessGreen
        LiveStatusKind.NEXT_TODAY -> NocturnalAmber
        LiveStatusKind.WEEKEND, LiveStatusKind.DAY_FINISHED -> TextMutedSlate
    }
    val shouldPulse = status.kind == LiveStatusKind.ACTIVE_NOW || status.kind == LiveStatusKind.NEXT_TODAY

    val infiniteTransition = rememberInfiniteTransition(label = "livePulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.65f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseAlpha"
    )

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("uni_live_tracker_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = NightCardSurface),
        border = BorderStroke(
            1.dp,
            if (shouldPulse) dotColor.copy(alpha = 0.45f) else NightBorderSubtle
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(22.dp),
                contentAlignment = Alignment.Center
            ) {
                if (shouldPulse) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .scale(pulseScale)
                            .alpha(pulseAlpha)
                            .clip(CircleShape)
                            .background(dotColor)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(11.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = status.badgeText,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = dotColor
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = status.mainText,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = TextMoonlight,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = status.subText,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMutedSlate
                )
            }
        }
    }
}

@Composable
private fun DaySelectorTabPill(
    label: String,
    subLabel: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) ElectricIndigo else NightCardSurface,
        border = BorderStroke(1.dp, if (isSelected) ElectricIndigo else NightBorderSubtle)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                ),
                color = if (isSelected) Color.White else TextStarlight
            )
            Text(
                text = subLabel,
                style = MaterialTheme.typography.labelSmall,
                color = if (isSelected) Color.White.copy(alpha = 0.85f) else TextMutedSlate
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun UniClassCard(
    session: UniClassSession,
    isActiveNow: Boolean,
    isNextClass: Boolean,
    onCreateTaskClick: () -> Unit
) {
    val subjectColor = session.subject.color
    val borderColor = when {
        isActiveNow -> SyncSuccessGreen
        isNextClass -> NocturnalAmber
        session.isPractice -> subjectColor.copy(alpha = 0.55f)
        else -> NightBorderSubtle
    }

    val cardModifier = if (session.isPractice && !isActiveNow && !isNextClass) {
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(NightCardSurface)
            .drawBehind {
                drawRoundRect(
                    color = borderColor,
                    cornerRadius = CornerRadius(18.dp.toPx(), 18.dp.toPx()),
                    style = Stroke(
                        width = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f), 0f)
                    )
                )
            }
    } else {
        Modifier.fillMaxWidth()
    }

    Card(
        modifier = cardModifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (session.isPractice) NightCardElevated.copy(alpha = 0.8f) else NightCardSurface
        ),
        border = if (session.isPractice && !isActiveNow && !isNextClass) null else BorderStroke(
            width = if (isActiveNow || isNextClass) 1.5.dp else 1.dp,
            color = borderColor
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            // Left Subject Color Accent Strip
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(subjectColor)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    // Time Block
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(52.dp)
                    ) {
                        Text(
                            text = session.startHHmm,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = subjectColor
                        )
                        Text(
                            text = "▼",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMutedSlate.copy(alpha = 0.6f)
                        )
                        Text(
                            text = session.endHHmm,
                            style = MaterialTheme.typography.labelMedium,
                            color = TextMutedSlate
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Title + Active/Next Live Pill
                    Column(modifier = Modifier.weight(1f)) {
                        if (isActiveNow || isNextClass) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = (if (isActiveNow) SyncSuccessGreen else NocturnalAmber).copy(alpha = 0.16f),
                                border = BorderStroke(
                                    1.dp,
                                    (if (isActiveNow) SyncSuccessGreen else NocturnalAmber).copy(alpha = 0.45f)
                                ),
                                modifier = Modifier.padding(bottom = 4.dp)
                            ) {
                                Text(
                                    text = if (isActiveNow) "● EN CURSO" else "⏳ SIGUIENTE",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (isActiveNow) SyncSuccessGreen else NocturnalAmber,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(
                            text = session.subject.fullName,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = TextMoonlight
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Metadata FlowRow + Quick Task Action
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (session.isPractice) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = NocturnalAmber.copy(alpha = 0.18f),
                            border = BorderStroke(1.dp, NocturnalAmber.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "PRÁCTICAS",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = NocturnalAmber,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    SmallBadgePill(
                        text = session.subject.abbr,
                        accentColor = subjectColor
                    )

                    session.groupLabel?.let { group ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = NightBgDeep,
                            border = BorderStroke(1.dp, NightBorderSubtle)
                        ) {
                            Text(
                                text = group,
                                style = MaterialTheme.typography.labelSmall,
                                color = TextStarlight,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = NightBgDeep,
                        border = BorderStroke(1.dp, NightBorderSubtle)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Place,
                                contentDescription = null,
                                tint = TextMutedSlate,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = session.room,
                                style = MaterialTheme.typography.labelSmall,
                                color = TextStarlight
                            )
                        }
                    }

                    SmallBadgePill(
                        text = session.building.displayName,
                        accentColor = session.building.color
                    )

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = NightBgDeep,
                        border = BorderStroke(1.dp, NightBorderSubtle)
                    ) {
                        Text(
                            text = session.durationLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMutedSlate,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    // Quick "+ Tarea" button to create a práctica/AAD for this subject
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = ElectricIndigo.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, ElectricIndigo.copy(alpha = 0.35f)),
                        modifier = Modifier.clickable(onClick = onCreateTaskClick)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Añadir tarea de esta asignatura",
                                tint = ElectricIndigoBright,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Tarea",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = ElectricIndigoBright
                            )
                        }
                    }
                }
            }
        }
    }
}
