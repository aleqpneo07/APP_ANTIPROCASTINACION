package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.ViewTimeline
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.ViewTimeline
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.NightPlannerTab
import com.example.ui.NightPlannerUiState
import com.example.ui.NightPlannerViewModel
import com.example.ui.components.EditTaskBottomSheet
import com.example.ui.components.GoogleCalendarSetupScreen
import com.example.ui.components.PlanTomorrowScreen
import com.example.ui.components.TimelineAndConflictsScreen
import com.example.ui.components.TodayExecutionScreen
import com.example.ui.theme.ConflictWarningRed
import com.example.ui.theme.ElectricIndigo
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NightBgDeep
import com.example.ui.theme.NightBgSurface
import com.example.ui.theme.NightBorderSubtle
import com.example.ui.theme.NocturnalAmber
import com.example.ui.theme.SyncSuccessGreen
import com.example.ui.theme.TextMoonlight
import com.example.ui.theme.TextMutedSlate
import com.example.ui.theme.TextStarlight

class MainActivity : ComponentActivity() {

    private val viewModel: NightPlannerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        checkOAuthRedirectIntent(intent)

        setContent {
            MyApplicationTheme {
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                NightPlannerAppRoot(
                    state = state,
                    viewModel = viewModel
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        checkOAuthRedirectIntent(intent)
    }

    private fun checkOAuthRedirectIntent(intent: Intent?) {
        val data = intent?.data ?: return
        val scheme = data.scheme.orEmpty()
        if (scheme == "com.aistudio.nightplanner.vxkpmq" || scheme == "nightplanner") {
            viewModel.handleOAuthRedirect(data)
        }
    }
}

private data class NavTabSpec(
    val tab: NightPlannerTab,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val badgeCount: Int = 0,
    val badgeColor: Color = ElectricIndigo,
    val testTag: String
)

@Composable
fun NightPlannerAppRoot(
    state: NightPlannerUiState,
    viewModel: NightPlannerViewModel
) {
    // BackHandler on secondary tabs returns to primary Plan Tomorrow screen
    if (state.activeTab != NightPlannerTab.PLAN_TOMORROW) {
        BackHandler {
            viewModel.selectTab(NightPlannerTab.PLAN_TOMORROW)
        }
    }

    val navItems = listOf(
        NavTabSpec(
            tab = NightPlannerTab.PLAN_TOMORROW,
            label = "Mañana",
            selectedIcon = Icons.Filled.NightsStay,
            unselectedIcon = Icons.Outlined.NightsStay,
            badgeCount = state.tomorrowTasks.size,
            badgeColor = ElectricIndigo,
            testTag = "nav_tab_plan_tomorrow"
        ),
        NavTabSpec(
            tab = NightPlannerTab.EXECUTE_TODAY,
            label = "Hoy",
            selectedIcon = Icons.Filled.WbSunny,
            unselectedIcon = Icons.Outlined.WbSunny,
            badgeCount = state.todayPendingCount,
            badgeColor = NocturnalAmber,
            testTag = "nav_tab_today"
        ),
        NavTabSpec(
            tab = NightPlannerTab.TIMELINE_CONFLICTS,
            label = "Agenda 24h",
            selectedIcon = Icons.Filled.ViewTimeline,
            unselectedIcon = Icons.Outlined.ViewTimeline,
            badgeCount = state.overlapWarnings.size,
            badgeColor = ConflictWarningRed,
            testTag = "nav_tab_timeline"
        ),
        NavTabSpec(
            tab = NightPlannerTab.GOOGLE_CALENDAR_GUIDE,
            label = "Calendar",
            selectedIcon = Icons.Filled.CalendarMonth,
            unselectedIcon = Icons.Outlined.CalendarMonth,
            badgeCount = 0,
            badgeColor = SyncSuccessGreen,
            testTag = "nav_tab_calendar"
        )
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(NightBgDeep)
    ) {
        val isWideScreen = maxWidth >= 700.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = NightBgDeep,
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                NightPlannerTopBar(
                    activeTab = state.activeTab,
                    isCalendarConnected = state.oAuthSettings.isConnected,
                    onQuickSwitchTab = { viewModel.selectTab(it) }
                )
            },
            bottomBar = {
                if (!isWideScreen) {
                    NavigationBar(
                        containerColor = NightBgSurface,
                        tonalElevation = 8.dp
                    ) {
                        navItems.forEach { item ->
                            val isSelected = state.activeTab == item.tab
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = { viewModel.selectTab(item.tab) },
                                modifier = Modifier.testTag(item.testTag),
                                icon = {
                                    BadgedBox(
                                        badge = {
                                            if (item.badgeCount > 0) {
                                                Badge(
                                                    containerColor = item.badgeColor,
                                                    contentColor = Color(0xFF080C16)
                                                ) {
                                                    Text(item.badgeCount.toString())
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                            contentDescription = item.label
                                        )
                                    }
                                },
                                label = {
                                    Text(
                                        text = item.label,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = ElectricIndigo,
                                    selectedTextColor = ElectricIndigo,
                                    indicatorColor = ElectricIndigo.copy(alpha = 0.16f),
                                    unselectedIconColor = TextMutedSlate,
                                    unselectedTextColor = TextMutedSlate
                                )
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isWideScreen) {
                    NavigationRail(
                        containerColor = NightBgSurface,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        Spacer(modifier = Modifier.height(12.dp))
                        navItems.forEach { item ->
                            val isSelected = state.activeTab == item.tab
                            NavigationRailItem(
                                selected = isSelected,
                                onClick = { viewModel.selectTab(item.tab) },
                                modifier = Modifier.testTag(item.testTag),
                                icon = {
                                    Icon(
                                        imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.label
                                    )
                                },
                                label = { Text(item.label) }
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 840.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Status Feedback Banner
                    AnimatedVisibility(
                        visible = !state.statusBannerMessage.isNullOrBlank(),
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        StatusNotificationBanner(
                            message = state.statusBannerMessage.orEmpty(),
                            isError = state.isStatusError,
                            onDismiss = { viewModel.clearStatusBanner() }
                        )
                    }

                    // Active Screen Content
                    Box(modifier = Modifier.fillMaxSize()) {
                        when (state.activeTab) {
                            NightPlannerTab.PLAN_TOMORROW -> {
                                PlanTomorrowScreen(
                                    state = state,
                                    onAddTask = viewModel::addTaskForTomorrow,
                                    onToggleComplete = viewModel::toggleTaskCompletion,
                                    onEditTask = viewModel::openEditTask,
                                    onDeleteTask = viewModel::deleteTask,
                                    onSelectCategoryFilter = viewModel::setTomorrowCategoryFilter,
                                    onConfirmAndSyncTomorrow = viewModel::confirmAndSyncTomorrow,
                                    onRefreshCalendarEvents = {
                                        viewModel.refreshTomorrowGoogleCalendarEvents(silent = false)
                                    },
                                    onNavigateToOAuthSetup = {
                                        viewModel.selectTab(NightPlannerTab.GOOGLE_CALENDAR_GUIDE)
                                    }
                                )
                            }

                            NightPlannerTab.EXECUTE_TODAY -> {
                                TodayExecutionScreen(
                                    state = state,
                                    onToggleComplete = viewModel::toggleTaskCompletion,
                                    onEditTask = viewModel::openEditTask,
                                    onDeleteTask = viewModel::deleteTask,
                                    onSelectCategoryFilter = viewModel::setTodayCategoryFilter,
                                    onMigratePendingToTomorrow = viewModel::migratePendingTodayToTomorrow
                                )
                            }

                            NightPlannerTab.TIMELINE_CONFLICTS -> {
                                TimelineAndConflictsScreen(
                                    state = state,
                                    onAssignTaskToFreeSlot = viewModel::assignTaskToTimeWindow,
                                    onRefreshCalendarEvents = {
                                        viewModel.refreshTomorrowGoogleCalendarEvents(silent = false)
                                    }
                                )
                            }

                            NightPlannerTab.GOOGLE_CALENDAR_GUIDE -> {
                                GoogleCalendarSetupScreen(
                                    state = state,
                                    onGeneratePkceAuthUrl = viewModel::generatePkceAuthUrl,
                                    onSaveManualOAuthToken = viewModel::saveManualOAuthToken,
                                    onUpdateClientId = viewModel::updateOAuthClientId,
                                    onUpdateUnscheduledPolicy = viewModel::updateUnscheduledPolicy,
                                    onDisconnect = viewModel::disconnectGoogleCalendar
                                )
                            }
                        }
                    }
                }
            }
        }

        // Edit Task Bottom Sheet if active
        state.editingTask?.let { editing ->
            EditTaskBottomSheet(
                task = editing,
                onDismiss = viewModel::dismissEditTask,
                onSave = viewModel::saveEditedTask
            )
        }
    }
}

@Composable
private fun NightPlannerTopBar(
    activeTab: NightPlannerTab,
    isCalendarConnected: Boolean,
    onQuickSwitchTab: (NightPlannerTab) -> Unit
) {
    Surface(
        color = NightBgSurface,
        border = BorderStroke(1.dp, NightBorderSubtle)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(ElectricIndigo.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.NightsStay,
                        contentDescription = "NightPlanner Logo",
                        tint = NocturnalAmber,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "NightPlanner",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = TextMoonlight
                        )
                        if (isCalendarConnected) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.CloudDone,
                                contentDescription = "Google Calendar Conectado",
                                tint = SyncSuccessGreen,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Text(
                        text = "Planificación Nocturna · Google Calendar v3",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMutedSlate
                    )
                }
            }

            // Quick Mode Pill Switcher (Mañana vs Hoy)
            Surface(
                shape = RoundedCornerShape(50),
                color = NightBgDeep,
                border = BorderStroke(1.dp, NightBorderSubtle)
            ) {
                Row(
                    modifier = Modifier.padding(3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isPlan = activeTab == NightPlannerTab.PLAN_TOMORROW
                    val isToday = activeTab == NightPlannerTab.EXECUTE_TODAY

                    Surface(
                        onClick = { onQuickSwitchTab(NightPlannerTab.PLAN_TOMORROW) },
                        shape = RoundedCornerShape(50),
                        color = if (isPlan) ElectricIndigo else Color.Transparent,
                        modifier = Modifier.testTag("top_switch_plan_tomorrow")
                    ) {
                        Text(
                            text = "🌙 Mañana",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isPlan) Color(0xFF080C16) else TextStarlight,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }

                    Surface(
                        onClick = { onQuickSwitchTab(NightPlannerTab.EXECUTE_TODAY) },
                        shape = RoundedCornerShape(50),
                        color = if (isToday) NocturnalAmber else Color.Transparent,
                        modifier = Modifier.testTag("top_switch_today")
                    ) {
                        Text(
                            text = "☀️ Hoy",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isToday) Color(0xFF1A1100) else TextStarlight,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusNotificationBanner(
    message: String,
    isError: Boolean,
    onDismiss: () -> Unit
) {
    val accent = if (isError) ConflictWarningRed else SyncSuccessGreen
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("status_notification_banner"),
        shape = RoundedCornerShape(14.dp),
        color = accent.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = if (isError) Icons.Default.ErrorOutline else Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = TextMoonlight
                )
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cerrar aviso",
                    tint = TextStarlight,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
