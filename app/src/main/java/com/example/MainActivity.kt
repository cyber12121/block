package com.example

import android.content.Intent
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
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Shield
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
import com.example.data.auth.AuthManager
import com.example.ui.components.MandatoryLoginGateScreen
import com.example.ui.screens.SessionScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.FocusGuardTheme
import com.example.ui.theme.IndigoPrimary

enum class AppTab(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    DASHBOARD("Home", Icons.Default.Home),
    APPS("Apps", Icons.Default.Apps),
    BLOCK_LISTS("Lists", Icons.Default.Language),
    SCHEDULES("Schedules", Icons.Default.CalendarMonth),
    INSIGHTS("Insights", Icons.Default.BarChart),
    SETTINGS("Security", Icons.Default.Shield)
}

class MainActivity : ComponentActivity() {

    companion object {
        const val EXTRA_OPEN_MINIMAL_LAUNCHER = "extra_open_minimal_launcher"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (intent?.getBooleanExtra(EXTRA_OPEN_MINIMAL_LAUNCHER, false) == true) {
            val app = application as FocusGuardApp
            app.sessionManager.setMinimalLauncherActive(true)
        }

        val app = application as FocusGuardApp
        val factory = MainViewModelFactory(app.repository, app.sessionManager, app)
        val viewModel = ViewModelProvider(this, factory)[MainViewModel::class.java]

        setContent {
            FocusGuardTheme {
                MainAppContent(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(EXTRA_OPEN_MINIMAL_LAUNCHER, false)) {
            val app = application as FocusGuardApp
            app.sessionManager.setMinimalLauncherActive(true)
        }
    }
}

@Composable
fun MainAppContent(viewModel: MainViewModel) {
    val sessionState by viewModel.sessionState.collectAsStateWithLifecycle()
    val activeSchedulesState by viewModel.activeSchedulesState.collectAsStateWithLifecycle()
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
    val authManager = remember { AuthManager.getInstance(context) }
    val currentUser by authManager.currentUser.collectAsStateWithLifecycle()
    val isDeveloperMode by authManager.isDeveloperMode.collectAsStateWithLifecycle()
    val isAuthorized = isDeveloperMode || currentUser != null

    val sessionManager = remember { FocusSessionManager.getInstance(context) }
    // Restore the Minimal Launcher state from SharedPrefs so it survives process death
    // and accessibility-service-triggered relaunches (the service bounces the user back
    // to MainActivity, and without this the composable always started with false).
    var isMinimalLauncherFullscreen by remember { mutableStateOf(sessionManager.isMinimalLauncherActive()) }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                if (sessionManager.isMinimalLauncherActive()) {
                    isMinimalLauncherFullscreen = true
                } else {
                    isMinimalLauncherFullscreen = false
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    androidx.compose.runtime.LaunchedEffect(isMinimalLauncherFullscreen) {
        sessionManager.setMinimalLauncherActive(isMinimalLauncherFullscreen)
    }

    if (!isAuthorized) {
        MandatoryLoginGateScreen(authManager = authManager)
        return
    }

    val isStrictActive = sessionState.isActive && (sessionState.isStrictMode || sessionState.isUltraStrict)
    val isMinimalistLock by sessionManager.minimalStrictLockState.collectAsStateWithLifecycle()
    val pinActive = isStrictActive || isMinimalistLock

    val activity = (context as? android.app.Activity)
    androidx.compose.runtime.LaunchedEffect(pinActive) {
        if (pinActive) {
            try {
                activity?.startLockTask()
            } catch (_: Throwable) {}
        } else {
            try {
                activity?.stopLockTask()
            } catch (_: Throwable) {}
        }
    }

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
                        containerColor = androidx.compose.ui.graphics.Color(0xFF0B101B),
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
                                    selectedIconColor = androidx.compose.ui.graphics.Color.White,
                                    unselectedIconColor = androidx.compose.ui.graphics.Color(0xFF64748B),
                                    indicatorColor = androidx.compose.ui.graphics.Color(0xFF5B61F4)
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
                        onExitLauncher = {
                            sessionManager.stopMinimalStrictLock()
                            sessionManager.setMinimalLauncherActive(false)
                            isMinimalLauncherFullscreen = false
                        },
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
                                activeSchedulesState = activeSchedulesState,
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
                                onEmergencyUnlock = { viewModel.forceEmergencyUnlock() },
                                onToggleList = { viewModel.toggleBlockList(it) },
                                onNavigateToLists = { currentTab = AppTab.BLOCK_LISTS },
                                onNavigateToApps = { currentTab = AppTab.APPS },
                                onNavigateToSchedules = { currentTab = AppTab.SCHEDULES },
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
                                onOpenCreateSchedule = { viewModel.openCreateScheduleDialog() },
                                onEmergencyUnlock = { viewModel.forceEmergencyUnlock() }
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
                                onEmergencyUnlock = { viewModel.forceEmergencyUnlock() },
                                onOpenSessionView = { isLiveSessionFullscreen = true }
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
            onStartSession = { title, durationMinutes, isStrict, activeLists, autoLaunchMinimal, isPomodoro, pomodoroRound, pomodoroTotalRounds, isUltraStrict ->
                viewModel.startFocusSession(
                    title = title,
                    durationMinutes = durationMinutes,
                    isStrictMode = isStrict,
                    activeListNames = activeLists,
                    isPomodoro = isPomodoro,
                    pomodoroRound = pomodoroRound,
                    pomodoroTotalRounds = pomodoroTotalRounds,
                    isPomodoroBreak = false,
                    isUltraStrict = isUltraStrict
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
            onCreateSchedule = { name, startHour, startMinute, endHour, endMinute, daysOfWeek, isStrict, isUltraStrict, activeNames ->
                viewModel.createSchedule(
                    name = name,
                    startHour = startHour,
                    startMinute = startMinute,
                    endHour = endHour,
                    endMinute = endMinute,
                    daysOfWeek = daysOfWeek,
                    isStrictMode = isStrict,
                    isUltraStrict = isUltraStrict,
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
