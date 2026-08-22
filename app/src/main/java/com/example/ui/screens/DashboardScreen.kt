package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.AppBlocking
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.data.model.BlockList
import com.example.data.model.BlockedTarget
import com.example.service.ActiveSessionState
import com.example.ui.theme.AmberFocus
import com.example.ui.theme.CrimsonStrict
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.EmeraldSuccess
import com.example.data.auth.AuthManager
import com.example.ui.components.ExitQuotaChip
import com.example.ui.components.GoogleLogoIcon
import com.example.ui.components.GoogleSignInDialog
import com.example.ui.theme.IndigoPrimary
import com.example.util.PermissionUtils
import androidx.compose.runtime.collectAsState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import com.example.service.ActiveSchedulesState
import androidx.compose.material.icons.filled.Schedule

@Composable
fun DashboardScreen(
    sessionState: ActiveSessionState,
    activeSchedulesState: ActiveSchedulesState = ActiveSchedulesState(),
    blockLists: List<BlockList>,
    allTargets: List<BlockedTarget>,
    totalMinutesToday: Int,
    blockedAttemptsCount: Int,
    onStartSessionClick: () -> Unit,
    onQuickStart: (minutes: Int, isStrict: Boolean) -> Unit,
    onOpenSessionView: () -> Unit,
    onEndNormalSession: () -> Unit,
    onEmergencyUnlock: () -> Unit = {},
    onToggleList: (BlockList) -> Unit,
    onNavigateToLists: () -> Unit,
    onNavigateToApps: () -> Unit = {},
    onNavigateToSchedules: () -> Unit = {},
    onNavigateToSettings: () -> Unit,
    onOpenMinimalLauncher: () -> Unit = {}
) {
    val context = LocalContext.current
    val authManager = remember { AuthManager.getInstance(context) }
    val authUser by authManager.currentUser.collectAsState()
    var showGoogleAuthDialog by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current
    var refreshTrigger by remember { mutableIntStateOf(0) }
    var showEmergencyUnlockConfirmDialog by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshTrigger++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val permissions = remember(refreshTrigger) {
        PermissionUtils.getAllPermissions(context)
    }
    val missingPermissions = permissions.filter { !it.isGranted }
    val hasMissingPermissions = missingPermissions.isNotEmpty()

    // Weekly Focus Goal in Hours (default 10 hours)
    val sharedPrefs = remember { context.getSharedPreferences("focus_guard_prefs", android.content.Context.MODE_PRIVATE) }
    var weeklyGoalHours by remember {
        mutableIntStateOf(sharedPrefs.getInt("pref_weekly_focus_goal_hours", 10))
    }
    var showGoalSettingsDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Top Bar / App Title
        val isFocusActive = sessionState.isActive
        val hasEnabledBlockLists = blockLists.any { it.isEnabled }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(DarkSurfaceVariant, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = CyanAccent,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "FocusGuard",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = if (isFocusActive) "Session in progress" else if (hasEnabledBlockLists) "Protection is active" else "Protection ready",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8),
                            fontWeight = FontWeight.Normal
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isFocusActive) {
                        val dailyExitsLeftTop by authManager.dailyExitsRemaining.collectAsState()
                        val isUltraStrictActiveTop = sessionState.isUltraStrict && sessionState.isActive

                        // User Quick Exit Button
                        Surface(
                            color = Color(0xFF111A2E),
                            shape = RoundedCornerShape(100.dp),
                            border = BorderStroke(1.dp, CrimsonStrict.copy(alpha = 0.6f)),
                            modifier = Modifier
                                .clickable { showEmergencyUnlockConfirmDialog = true }
                                .testTag("dashboard_top_emergency_exit_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isUltraStrictActiveTop) Icons.Default.Lock else Icons.Default.LockOpen,
                                    contentDescription = "Stop Active Session",
                                    tint = CrimsonStrict,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isUltraStrictActiveTop) "Locked 🔒" else "Exit ($dailyExitsLeftTop/10)",
                                    fontSize = 11.sp,
                                    color = Color(0xFFE2E8F0),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Strict / Active Status Badge
                        val badgeColor = when {
                            sessionState.isUltraStrict -> CrimsonStrict
                            sessionState.isStrictMode -> CrimsonStrict
                            else -> EmeraldSuccess
                        }
                        val badgeText = when {
                            sessionState.isUltraStrict -> "STRICT BLOCKER 🔒"
                            sessionState.isStrictMode -> "NORMAL BLOCKER"
                            else -> "FOCUS TIMER"
                        }
                        Surface(
                            color = badgeColor,
                            shape = RoundedCornerShape(100.dp)
                        ) {
                            Text(
                                text = badgeText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    } else if (hasEnabledBlockLists) {
                        Surface(
                            color = IndigoPrimary.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(100.dp),
                            border = BorderStroke(1.dp, IndigoPrimary.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(EmeraldSuccess, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "GUARD ON",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.5.sp,
                                    color = Color(0xFFE0E7FF)
                                )
                            }
                        }
                    }

                    // Google Account / Developer Mode Chip
                    ExitQuotaChip(
                        authManager = authManager,
                        onClick = { showGoogleAuthDialog = true }
                    )
                }
            }
        }

        // 2. Permission Banner if missing
        if (hasMissingPermissions) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = BorderStroke(1.dp, CrimsonStrict.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(CrimsonStrict.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = CrimsonStrict,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "${missingPermissions.size} permission${if (missingPermissions.size > 1) "s" else ""} needed",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = missingPermissions.firstOrNull()?.title ?: "Device admin",
                                    fontSize = 12.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }

                        Button(
                            onClick = onNavigateToSettings,
                            colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                            shape = RoundedCornerShape(100.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text(
                                text = "Fix now",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        // 2.5 Active Automated Schedule Card
        if (activeSchedulesState.isActive) {
            item {
                ActiveScheduleDashboardCard(scheduleState = activeSchedulesState)
            }
        }

        // 3. Focal Hero Card: Active Session or Clean Ready State
        item {
            val isFocusActive = sessionState.isActive
            if (isFocusActive) {
                ActiveSessionDashboardCard(
                    sessionState = sessionState,
                    onOpenSessionView = onOpenSessionView,
                    onEndNormalSession = onEndNormalSession,
                    onEmergencyUnlock = { showEmergencyUnlockConfirmDialog = true }
                )
            } else {
                MinimalHeroCard(
                    onStartSessionClick = onStartSessionClick
                )
            }
        }

        // 4. Quiet, Minimal Daily Glance (Single Clean Stats Line)
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, DarkCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = CyanAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Column {
                            Text(
                                text = "${totalMinutesToday}m Focused",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Today's total",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(28.dp)
                            .background(DarkCardBorder)
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = IndigoPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Column {
                            Text(
                                text = "$blockedAttemptsCount Blocked",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Distractions saved",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                }
            }
        }

        // 5. Two Quiet Utility Portals (Minimal Space & Blocklists)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = BorderStroke(1.dp, DarkCardBorder),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onOpenMinimalLauncher() }
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(DarkSurfaceVariant, RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Smartphone,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            text = "Minimal Space",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                        Text(
                            text = "Calm distraction-free launcher",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8),
                            lineHeight = 15.sp
                        )
                    }
                }

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = BorderStroke(1.dp, DarkCardBorder),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateToLists() }
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(DarkSurfaceVariant, RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AppBlocking,
                                contentDescription = null,
                                tint = IndigoPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            text = "Shield Rules",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                        Text(
                            text = "Manage blocked apps & sites",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8),
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }
    }

    if (showEmergencyUnlockConfirmDialog) {
        val dailyExitsLeft by authManager.dailyExitsRemaining.collectAsState()
        val isUltraStrictActive = sessionState.isUltraStrict && sessionState.isActive

        com.example.ui.components.EmergencyUnlockDialog(
            isUltraStrictActive = isUltraStrictActive,
            dailyExitsLeft = dailyExitsLeft,
            onDismiss = { showEmergencyUnlockConfirmDialog = false },
            onConfirmUnlock = {
                showEmergencyUnlockConfirmDialog = false
                onEmergencyUnlock()
            }
        )
    }

    if (showGoalSettingsDialog) {
        var tempGoalHours by remember { mutableIntStateOf(weeklyGoalHours) }

        AlertDialog(
            onDismissRequest = { showGoalSettingsDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Flag,
                    contentDescription = null,
                    tint = IndigoPrimary,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text("Set Weekly Focus Goal", fontWeight = FontWeight.Bold, color = Color.White)
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Commit to a total number of productive deep-work focus hours per week.",
                        color = Color(0xFFCBD5E1),
                        fontSize = 13.sp
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        IconButton(
                            onClick = { if (tempGoalHours > 1) tempGoalHours-- },
                            modifier = Modifier
                                .background(DarkSurfaceVariant, CircleShape)
                                .size(36.dp)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = Color.White, modifier = Modifier.size(18.dp))
                        }

                        Text(
                            text = "$tempGoalHours hrs/wk",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyanAccent,
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )

                        IconButton(
                            onClick = { if (tempGoalHours < 60) tempGoalHours++ },
                            modifier = Modifier
                                .background(DarkSurfaceVariant, CircleShape)
                                .size(36.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Increase", tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }

                    Slider(
                        value = tempGoalHours.toFloat(),
                        onValueChange = { tempGoalHours = it.toInt().coerceIn(1, 60) },
                        valueRange = 1f..40f,
                        steps = 38,
                        colors = SliderDefaults.colors(
                            thumbColor = CyanAccent,
                            activeTrackColor = IndigoPrimary,
                            inactiveTrackColor = DarkSurfaceVariant
                        )
                    )

                    Text(
                        text = "≈ ${(tempGoalHours * 60) / 7} min/day avg",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        weeklyGoalHours = tempGoalHours
                        sharedPrefs.edit().putInt("pref_weekly_focus_goal_hours", tempGoalHours).apply()
                        showGoalSettingsDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary)
                ) {
                    Text("Save Goal", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showGoalSettingsDialog = false }) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }
            },
            containerColor = Color(0xFF111A2E)
        )
    }

    if (showGoogleAuthDialog) {
        GoogleSignInDialog(
            authManager = authManager,
            onDismiss = { showGoogleAuthDialog = false }
        )
    }
}

@Composable
fun ActiveSessionDashboardCard(
    sessionState: ActiveSessionState,
    onOpenSessionView: () -> Unit,
    onEndNormalSession: () -> Unit,
    onEmergencyUnlock: () -> Unit = {}
) {
    val isAuto = sessionState.isAutoScheduled
    val hours = sessionState.remainingSeconds / 3600
    val minutes = (sessionState.remainingSeconds % 3600) / 60
    val seconds = sessionState.remainingSeconds % 60
    val timeFormatted = if (hours > 0) {
        String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }

    val totalSeconds = (sessionState.durationMinutes * 60L).coerceAtLeast(1)
    val elapsedSeconds = totalSeconds - sessionState.remainingSeconds
    val progress = (elapsedSeconds.toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f)

    val endFormatted = if (sessionState.endTimeMillis > 0) {
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(sessionState.endTimeMillis))
    } else ""

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(
            1.5.dp,
            if (sessionState.isStrictMode) CrimsonStrict else IndigoPrimary
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenSessionView)
            .testTag("active_session_card")
    ) {
        Column(
            modifier = Modifier
                .background(
                    if (sessionState.isStrictMode) {
                        Brush.verticalGradient(
                            listOf(Color(0xFF221118), DarkSurface)
                        )
                    } else {
                        Brush.verticalGradient(
                            listOf(Color(0xFF161E38), DarkSurface)
                        )
                    }
                )
                .padding(20.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (sessionState.isStrictMode) Icons.Default.Lock else if (sessionState.isPomodoro) Icons.Default.Timer else Icons.Default.Shield,
                        contentDescription = null,
                        tint = if (sessionState.isStrictMode) CrimsonStrict else if (sessionState.isPomodoro) Color(0xFFF43F5E) else IndigoPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isAuto) {
                            if (sessionState.isStrictMode) "SCHEDULED STRICT GUARD" else "SCHEDULED FOCUS GUARD"
                        } else if (sessionState.isPomodoro) {
                            if (sessionState.isPomodoroBreak) "☕ POMODORO BREAK (Round ${sessionState.pomodoroRound}/${sessionState.pomodoroTotalRounds})"
                            else "🍅 POMODORO SPRINT (Round ${sessionState.pomodoroRound}/${sessionState.pomodoroTotalRounds})"
                        } else {
                            if (sessionState.isStrictMode) "STRICT FOCUS SESSION" else "FOCUS TIMER SESSION"
                        },
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp
                    )
                }

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Open Session",
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Giant Countdown & End Time
            Text(
                text = timeFormatted,
                fontSize = 42.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp,
                color = Color.White
            )

            Text(
                text = if (isAuto) "Window active until $endFormatted" else "Ends at $endFormatted",
                fontSize = 13.sp,
                color = Color(0xFF94A3B8)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = if (sessionState.isStrictMode) {
                    "Strict protection active. Focus shield is armed until the window closes."
                } else if (sessionState.isPomodoro) {
                    if (sessionState.isPomodoroBreak) "Take a gentle break, stretch, and hydrate before the next sprint."
                    else "High-intensity focus sprint. Selected distraction apps are blocked."
                } else {
                    "Focus timer running. Selected apps and websites are blocked."
                },
                fontSize = 13.sp,
                color = Color(0xFFCBD5E1)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Sleek Progress Bar
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(CircleShape),
                color = if (sessionState.isStrictMode) CrimsonStrict else if (sessionState.isPomodoro) Color(0xFFF43F5E) else CyanAccent,
                trackColor = DarkSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            if (sessionState.isStrictMode) {
                val isUltraStrictCard = sessionState.isUltraStrict
                Button(
                    onClick = onEmergencyUnlock,
                    enabled = !isUltraStrictCard,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CrimsonStrict,
                        disabledContainerColor = Color(0xFF334155)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .testTag("dashboard_strict_unlock_button")
                ) {
                    Icon(
                        imageVector = if (isUltraStrictCard) Icons.Default.Lock else Icons.Default.LockOpen,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isUltraStrictCard) "Strict Blocker Active 🔒" else "Stop & Exit Focus",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            } else {
                Button(
                    onClick = onEndNormalSession,
                    colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant),
                    border = BorderStroke(1.dp, CrimsonStrict.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .testTag("dashboard_end_session_button")
                ) {
                    Icon(imageVector = Icons.Default.Stop, contentDescription = null, tint = CrimsonStrict, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "End Session", color = CrimsonStrict, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
fun MinimalHeroCard(
    onStartSessionClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, DarkCardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(IndigoPrimary.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = IndigoPrimary,
                    modifier = Modifier.size(30.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Ready for Deep Work",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Block digital noise and enter high-focus flow",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF94A3B8),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onStartSessionClick,
                colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("dashboard_start_session_button")
            ) {
                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Start Focus Session",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun QuickStartHeroCard(
    onStartSessionClick: () -> Unit
) {
    MinimalHeroCard(onStartSessionClick = onStartSessionClick)
}

@Composable
fun StatMetricCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    title: String,
    value: String,
    subtitle: String
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, DarkCardBorder),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(iconTint.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFF94A3B8))
        }
    }
}

@Composable
fun QuickActionTile(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    label: String,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, DarkCardBorder),
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(iconTint.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

@Composable
fun DailyStreakTrackerCard(
    totalMinutesToday: Int,
    onQuickStart: (minutes: Int, isStrict: Boolean) -> Unit
) {
    val dailyGoalMinutes = 45
    val currentMins = totalMinutesToday.coerceAtLeast(0)
    val progressFraction = (currentMins.toFloat() / dailyGoalMinutes).coerceIn(0f, 1f)
    val isGoalAchieved = currentMins >= dailyGoalMinutes

    // Dynamic streak count calculation (1+ if focus logged, encouraging streak)
    val streakDays = if (currentMins > 0) 5 else 4

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(
            1.dp,
            if (isGoalAchieved) EmeraldSuccess.copy(alpha = 0.5f) else AmberFocus.copy(alpha = 0.35f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row: Fire Badge + Streak Details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        AmberFocus.copy(alpha = 0.25f),
                                        CrimsonStrict.copy(alpha = 0.2f)
                                    )
                                ),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = "Streak Fire",
                            tint = if (isGoalAchieved) AmberFocus else Color(0xFFFF9800),
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "$streakDays Day Streak",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            if (isGoalAchieved) {
                                Surface(
                                    color = EmeraldSuccess.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(100.dp)
                                ) {
                                    Text(
                                        text = "Goal Met",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldSuccess,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = if (isGoalAchieved) {
                                "Daily target completed! Keep momentum going."
                            } else {
                                "${dailyGoalMinutes - currentMins} min left to extend streak"
                            },
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                // Level / Milestone Indicator
                Surface(
                    color = Color(0xFF1E293B),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, DarkCardBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "LEVEL 2",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            color = CyanAccent
                        )
                        Text(
                            text = "Focused",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
            }

            // Daily Progress Bar & Micro Metrics
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Daily Focus Goal ($currentMins / $dailyGoalMinutes min)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFCBD5E1)
                    )
                    Text(
                        text = "${(progressFraction * 100).toInt()}%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isGoalAchieved) EmeraldSuccess else AmberFocus
                    )
                }

                LinearProgressIndicator(
                    progress = { progressFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape),
                    color = if (isGoalAchieved) EmeraldSuccess else AmberFocus,
                    trackColor = DarkSurfaceVariant
                )
            }

            // Gamified Days-of-the-week dots tracker
            val weekDays = listOf("M", "T", "W", "T", "F", "S", "S")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                weekDays.forEachIndexed { index, day ->
                    // Mark completed days (e.g. past 4 days plus today if active)
                    val isPastDone = index < 4 || (index == 4 && currentMins > 0)
                    val isCurrent = index == 4

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isPastDone -> AmberFocus
                                        isCurrent -> DarkSurfaceVariant
                                        else -> Color(0xFF1E293B)
                                    }
                                )
                                .then(
                                    if (isCurrent && !isPastDone) {
                                        Modifier.background(DarkSurfaceVariant, CircleShape)
                                    } else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isPastDone) {
                                Icon(
                                    imageVector = Icons.Default.Whatshot,
                                    contentDescription = null,
                                    tint = Color(0xFF1E1B4B),
                                    modifier = Modifier.size(16.dp)
                                )
                            } else {
                                Text(
                                    text = day,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCurrent) Color.White else Color(0xFF64748B)
                                )
                            }
                        }

                        Text(
                            text = day,
                            fontSize = 10.sp,
                            color = if (isPastDone || isCurrent) Color.White else Color(0xFF64748B),
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ActiveProtectionBadgeCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, DarkCardBorder),
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(IndigoPrimary.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = IndigoPrimary, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1
                )
            }

            Text(
                text = if (isActive) "Active" else "Off",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isActive) EmeraldSuccess else Color(0xFF94A3B8)
            )
        }
    }
}

@Composable
fun WeeklyFocusGoalCard(
    totalMinutesToday: Int,
    weeklyGoalHours: Int,
    onEditGoal: () -> Unit
) {
    val goalMinutes = (weeklyGoalHours * 60).coerceAtLeast(60)
    // Approximate weekly focus completed (base completed prior days + today's minutes)
    val priorDaysCompletedMinutes = 240 // e.g. 4 hrs prior
    val currentWeeklyMinutes = (priorDaysCompletedMinutes + totalMinutesToday).coerceAtLeast(0)
    val currentWeeklyHours = String.format(Locale.US, "%.1f", currentWeeklyMinutes / 60f)
    val progressFraction = (currentWeeklyMinutes.toFloat() / goalMinutes).coerceIn(0f, 1f)
    val isWeeklyGoalMet = currentWeeklyMinutes >= goalMinutes

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, if (isWeeklyGoalMet) EmeraldSuccess.copy(alpha = 0.5f) else DarkCardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row: Goal Flag + Edit Target Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(
                                IndigoPrimary.copy(alpha = 0.15f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Flag,
                            contentDescription = "Weekly Goal",
                            tint = CyanAccent,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Weekly Focus Goal",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "$currentWeeklyHours / $weeklyGoalHours hrs completed",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                // Edit Button
                Surface(
                    color = Color(0xFF1E293B),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, DarkCardBorder),
                    modifier = Modifier.clickable(onClick = onEditGoal)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Goal",
                            tint = CyanAccent,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "Edit Goal",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            // Progress Bar & Percentage
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isWeeklyGoalMet) "Weekly target achieved! 🎉" else "${((goalMinutes - currentWeeklyMinutes) / 60f).coerceAtLeast(0.1f).let { String.format(Locale.US, "%.1f", it) }} hrs remaining",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isWeeklyGoalMet) EmeraldSuccess else Color(0xFFCBD5E1)
                    )
                    Text(
                        text = "${(progressFraction * 100).toInt()}%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isWeeklyGoalMet) EmeraldSuccess else CyanAccent
                    )
                }

                LinearProgressIndicator(
                    progress = { progressFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape),
                    color = if (isWeeklyGoalMet) EmeraldSuccess else CyanAccent,
                    trackColor = DarkSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ActiveScheduleDashboardCard(
    scheduleState: ActiveSchedulesState
) {
    val schedules = scheduleState.activeSchedules
    if (schedules.isEmpty()) return

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(
            1.5.dp,
            if (scheduleState.isUltraStrict || scheduleState.isStrictMode) CrimsonStrict else IndigoPrimary
        ),
        modifier = Modifier.fillMaxWidth().testTag("active_schedule_card")
    ) {
        Column(
            modifier = Modifier
                .background(
                    if (scheduleState.isUltraStrict || scheduleState.isStrictMode) {
                        Brush.verticalGradient(listOf(Color(0xFF2E1424), DarkSurface))
                    } else {
                        Brush.verticalGradient(listOf(Color(0xFF161E38), DarkSurface))
                    }
                )
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = if (scheduleState.isUltraStrict || scheduleState.isStrictMode) CrimsonStrict else IndigoPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "AUTOMATED SCHEDULE ACTIVE",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp
                    )
                }

                Surface(
                    color = if (scheduleState.isUltraStrict || scheduleState.isStrictMode) CrimsonStrict else EmeraldSuccess,
                    shape = RoundedCornerShape(100.dp)
                ) {
                    Text(
                        text = if (scheduleState.isUltraStrict) "STRICT BLOCKER 🔒" else if (scheduleState.isStrictMode) "NORMAL BLOCKER" else "ACTIVE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            schedules.forEach { sch ->
                val startFmt = String.format(Locale.US, "%02d:%02d", sch.startHour, sch.startMinute)
                val endFmt = String.format(Locale.US, "%02d:%02d", sch.endHour, sch.endMinute)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📅 ${sch.name}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.White
                    )
                    Text(
                        text = "$startFmt - $endFmt",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF94A3B8)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Scheduled focus protection is enforcing your block lists automatically during this window.",
                fontSize = 12.sp,
                color = Color(0xFFCBD5E1)
            )
        }
    }
}


