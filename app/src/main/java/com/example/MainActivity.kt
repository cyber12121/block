package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.MainViewModel
import com.example.ui.MainViewModelFactory
import com.example.ui.components.AddTargetDialog
import com.example.ui.components.CreateListDialog
import com.example.ui.components.CreateScheduleDialog
import com.example.ui.components.StartSessionDialog
import com.example.ui.components.StrictModeInteractionGuard
import com.example.ui.screens.AppsScreen
import com.example.ui.screens.BlockListsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.GardenScreen
import com.example.ui.screens.InsightsScreen
import com.example.ui.screens.MinimalLauncherScreen
import com.example.ui.screens.SchedulesScreen
import androidx.compose.ui.platform.LocalContext
import com.example.service.FocusSessionManager
import com.example.ui.screens.SessionScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.FocusGuardTheme
import com.example.ui.theme.IndigoPrimary

enum class AppTab(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    DASHBOARD("Dashboard", Icons.Default.Dashboard),
    APPS("Apps", Icons.Default.Apps),
    BLOCK_LISTS("Rules", Icons.Default.Block),
    SCHEDULES("Schedules", Icons.Default.Schedule),
    INSIGHTS("Insights", Icons.Default.Insights),
    SETTINGS("Security", Icons.Default.Security)
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as FocusGuardApp
        val factory = MainViewModelFactory(app.repository, app.sessionManager)
        val viewModel = ViewModelProvider(this, factory)[MainViewModel::class.java]

        // NOTE: The Minimal Launcher is intentionally NOT auto-opened here.
        // It used to relaunch itself on top of FocusGuard whenever the user left
        // the app (onUserLeaveHint / LAUNCH_MINIMAL_MODE), which immediately
        // covered any app the user had just launched from it. It is now opened
        // only manually (Dashboard banner / "All Apps" area), so users decide
        // when they want it.
        setContent {
            FocusGuardTheme {
                MainAppContent(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainAppContent(viewModel: MainViewModel) {
    val sessionState by viewModel.sessionState.collectAsStateWithLifecycle()
    val blockLists by viewModel.blockLists.collectAsStateWithLifecycle()
    val allTargets by viewModel.allTargets.collectAsStateWithLifecycle()
    val allSchedules by viewModel.allSchedules.collectAsStateWithLifecycle()
    val totalMinutes by viewModel.totalMinutes.collectAsStateWithLifecycle()
    val completedSessionsCount by viewModel.completedSessionsCount.collectAsStateWithLifecycle()
    val totalBlockedAttempts by viewModel.totalBlockedAttempts.collectAsStateWithLifecycle()
    val recentStats by viewModel.recentStats.collectAsStateWithLifecycle()

    val allGardenPlants by viewModel.allGardenPlants.collectAsStateWithLifecycle()
    val bloomedPlantsCount by viewModel.bloomedPlantsCount.collectAsStateWithLifecycle()
    val witheredPlantsCount by viewModel.witheredPlantsCount.collectAsStateWithLifecycle()

    val isStartSessionOpen by viewModel.isStartSessionDialogOpen.collectAsStateWithLifecycle()
    val isCreateListOpen by viewModel.isCreateListDialogOpen.collectAsStateWithLifecycle()
    val isCreateScheduleOpen by viewModel.isCreateScheduleDialogOpen.collectAsStateWithLifecycle()
    val selectedListForAddTarget by viewModel.selectedListForAddTarget.collectAsStateWithLifecycle()

    var currentTab by remember { mutableStateOf(AppTab.DASHBOARD) }
    var isLiveSessionFullscreen by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val sessionManager = remember { FocusSessionManager.getInstance(context) }
    // Always start on the regular UI; the Minimal Launcher only appears when the
    // user opens it manually.
    var isMinimalLauncherFullscreen by remember { mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(isMinimalLauncherFullscreen) {
        sessionManager.setMinimalLauncherActive(isMinimalLauncherFullscreen)
    }

    val isStrictActive = sessionState.isActive && sessionState.isStrictMode

    StrictModeInteractionGuard(
        isStrictActive = isStrictActive,
        remainingSeconds = sessionState.remainingSeconds,
        onEmergencyUnlockRequested = { viewModel.forceEmergencyUnlock() }
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                if (!isLiveSessionFullscreen && !isMinimalLauncherFullscreen) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.background,
                        tonalElevation = 0.dp
                    ) {
                        AppTab.values().forEach { tab ->
                            val isSelected = currentTab == tab
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = { currentTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = tab.title,
                                        modifier = Modifier.size(22.dp)
                                    )
                                },
                                alwaysShowLabel = false,
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.onBackground,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    indicatorColor = MaterialTheme.colorScheme.surfaceVariant
                                ),
                                modifier = Modifier.testTag("tab_${tab.name.lowercase()}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isMinimalLauncherFullscreen) {
                    MinimalLauncherScreen(
                        sessionState = sessionState,
                        allTargets = allTargets,
                        onExitLauncher = { isMinimalLauncherFullscreen = false },
                        onStartFocusSession = {
                            if (sessionState.isActive) {
                                isLiveSessionFullscreen = true
                            } else {
                                viewModel.openStartSessionDialog()
                            }
                        }
                    )
                } else if (isLiveSessionFullscreen && sessionState.isActive) {
                    SessionScreen(
                        sessionState = sessionState,
                        onBack = { isLiveSessionFullscreen = false },
                        onEndNormalSession = {
                            viewModel.endCurrentSession(earlyUnlocked = false)
                            isLiveSessionFullscreen = false
                        },
                        onEmergencyUnlock = {
                            viewModel.forceEmergencyUnlock()
                            isLiveSessionFullscreen = false
                        },
                        onTransitionPomodoro = { nextIsBreak, nextRound, mins ->
                            val activeNames = blockLists.filter { it.isEnabled }.map { it.name }
                            viewModel.transitionPomodoroStage(
                                nextIsBreak = nextIsBreak,
                                nextRound = nextRound,
                                durationMinutes = mins,
                                isStrict = sessionState.isStrictMode,
                                activeListNames = activeNames
                            )
                        }
                    )
                } else {
                    when (currentTab) {
                        AppTab.DASHBOARD -> {
                            DashboardScreen(
                                sessionState = sessionState,
                                blockLists = blockLists,
                                allTargets = allTargets,
                                totalMinutesToday = recentStats.firstOrNull()?.totalFocusMinutes ?: (totalMinutes ?: 0),
                                blockedAttemptsCount = totalBlockedAttempts ?: 0,
                                onStartSessionClick = { viewModel.openStartSessionDialog() },
                                onQuickStart = { minutes, isStrict ->
                                    val enabledNames = blockLists.filter { it.isEnabled }.map { it.name }
                                    viewModel.startFocusSession(
                                        title = if (isStrict) "Strict Focus ($minutes min)" else "Quick Focus ($minutes min)",
                                        durationMinutes = minutes,
                                        isStrictMode = isStrict,
                                        activeListNames = enabledNames
                                    )
                                },
                                onOpenSessionView = { isLiveSessionFullscreen = true },
                                onEndNormalSession = { viewModel.endCurrentSession(earlyUnlocked = false) },
                                onToggleList = { viewModel.toggleBlockList(it) },
                                onNavigateToLists = { currentTab = AppTab.BLOCK_LISTS },
                                onNavigateToSettings = { currentTab = AppTab.SETTINGS },
                                onOpenMinimalLauncher = { isMinimalLauncherFullscreen = true }
                            )
                        }
                        AppTab.APPS -> {
                            AppsScreen(
                                sessionState = sessionState,
                                blockLists = blockLists,
                                allTargets = allTargets,
                                onToggleTarget = { viewModel.toggleTarget(it) },
                                onAddBulkTargets = { listId, type, items, category ->
                                    viewModel.addBulkTargets(listId, type, items, category)
                                },
                                onDeleteTarget = { viewModel.deleteTarget(it) }
                            )
                        }
                        AppTab.BLOCK_LISTS -> {
                            BlockListsScreen(
                                sessionState = sessionState,
                                blockLists = blockLists,
                                allTargets = allTargets,
                                onToggleList = { viewModel.toggleBlockList(it) },
                                onToggleTarget = { viewModel.toggleTarget(it) },
                                onUpdateTarget = { viewModel.updateTarget(it) },
                                onDeleteTarget = { viewModel.deleteTarget(it) },
                                onOpenAddTarget = { viewModel.openAddTargetDialog(it) },
                                onOpenCreateList = { viewModel.openCreateListDialog() },
                                onDeleteList = { viewModel.deleteBlockList(it) }
                            )
                        }
                        AppTab.SCHEDULES -> {
                            SchedulesScreen(
                                sessionState = sessionState,
                                schedules = allSchedules,
                                onToggleSchedule = { viewModel.toggleSchedule(it) },
                                onDeleteSchedule = { viewModel.deleteSchedule(it) },
                                onOpenCreateSchedule = { viewModel.openCreateScheduleDialog() }
                            )
                        }
                        AppTab.INSIGHTS -> {
                            InsightsScreen(
                                totalMinutes = totalMinutes ?: 0,
                                completedSessionsCount = completedSessionsCount,
                                totalBlockedAttempts = totalBlockedAttempts ?: 0,
                                recentStats = recentStats
                            )
                        }
                        AppTab.SETTINGS -> {
                            SettingsScreen(
                                sessionState = sessionState,
                                onEmergencyUnlock = { viewModel.forceEmergencyUnlock() }
                            )
                        }
                    }
                }
            }
        }
    }

    // Dialogs
    if (isStartSessionOpen) {
        StartSessionDialog(
            availableLists = blockLists,
            onDismiss = { viewModel.closeStartSessionDialog() },
            onStartSession = { title, durationMinutes, isStrict, activeLists, autoLaunchMinimal ->
                viewModel.startFocusSession(
                    title = title,
                    durationMinutes = durationMinutes,
                    isStrictMode = isStrict,
                    activeListNames = activeLists
                )
                if (autoLaunchMinimal) {
                    isMinimalLauncherFullscreen = true
                }
            }
        )
    }

    if (isCreateListOpen) {
        CreateListDialog(
            onDismiss = { viewModel.closeCreateListDialog() },
            onCreateList = { name, description, iconName, colorHex ->
                viewModel.createBlockList(name, description, iconName, colorHex)
            }
        )
    }

    if (isCreateScheduleOpen) {
        CreateScheduleDialog(
            availableLists = blockLists,
            onDismiss = { viewModel.closeCreateScheduleDialog() },
            onCreateSchedule = { name, startHour, startMinute, endHour, endMinute, daysOfWeek, isStrict, activeNames ->
                viewModel.createSchedule(
                    name = name,
                    startHour = startHour,
                    startMinute = startMinute,
                    endHour = endHour,
                    endMinute = endMinute,
                    daysOfWeek = daysOfWeek,
                    isStrictMode = isStrict,
                    activeListNames = activeNames
                )
            }
        )
    }

    selectedListForAddTarget?.let { list ->
        AddTargetDialog(
            targetList = list,
            onDismiss = { viewModel.closeAddTargetDialog() },
            onAddBulkTargets = { type, items, category ->
                viewModel.addBulkTargets(list.id, type, items, category)
            }
        )
    }
}
