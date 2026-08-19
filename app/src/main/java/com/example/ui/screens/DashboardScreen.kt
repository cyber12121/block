package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Fireplace
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
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
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IndigoPrimary
import com.example.util.PermissionUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    sessionState: ActiveSessionState,
    blockLists: List<BlockList>,
    allTargets: List<BlockedTarget>,
    totalMinutesToday: Int,
    blockedAttemptsCount: Int,
    onStartSessionClick: () -> Unit,
    onQuickStart: (minutes: Int, isStrict: Boolean) -> Unit,
    onOpenSessionView: () -> Unit,
    onEndNormalSession: () -> Unit,
    onToggleList: (BlockList) -> Unit,
    onNavigateToLists: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onOpenMinimalLauncher: () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var refreshTrigger by remember { mutableIntStateOf(0) }

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

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // 1. Top Bar / App Title
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(
                            text = "FocusGuard",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = (-0.2).sp
                        )
                        Text(
                            text = if (sessionState.isActive) "Enforcing Focus Rules" else "Protection Ready",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (sessionState.isActive) {
                                if (sessionState.isStrictMode) CrimsonStrict else EmeraldSuccess
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Shield Status Pill
                Surface(
                    color = if (sessionState.isActive) {
                        if (sessionState.isStrictMode) CrimsonStrict.copy(alpha = 0.15f) else EmeraldSuccess.copy(alpha = 0.15f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    shape = RoundedCornerShape(100.dp),
                    border = BorderStroke(
                        1.dp,
                        if (sessionState.isActive) {
                            if (sessionState.isStrictMode) CrimsonStrict.copy(alpha = 0.5f) else EmeraldSuccess.copy(alpha = 0.5f)
                        } else {
                            Color.Transparent
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    if (sessionState.isActive) {
                                        if (sessionState.isStrictMode) CrimsonStrict else EmeraldSuccess
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    CircleShape
                                )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (sessionState.isActive) {
                                if (sessionState.isStrictMode) "STRICT" else "ACTIVE"
                            } else "STANDBY",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (sessionState.isActive) {
                                if (sessionState.isStrictMode) CrimsonStrict else EmeraldSuccess
                            } else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Permission Alert Banner if any permission missing
        if (hasMissingPermissions) {
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onNavigateToSettings)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(CrimsonStrict.copy(alpha = 0.15f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = null,
                                        tint = CrimsonStrict,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "${missingPermissions.size} Permission${if (missingPermissions.size > 1) "s" else ""} Missing",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = CrimsonStrict
                                    )
                                    Text(
                                        text = "Enable required permissions for full shielding",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Open Settings",
                                tint = CrimsonStrict
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Mini chips showing Green Tick vs Red Cross for all permissions
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            permissions.forEach { perm ->
                                val isGranted = perm.isGranted
                                Surface(
                                    color = if (isGranted) EmeraldSuccess.copy(alpha = 0.15f) else CrimsonStrict.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(1.dp, if (isGranted) EmeraldSuccess.copy(alpha = 0.4f) else CrimsonStrict.copy(alpha = 0.4f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (isGranted) Icons.Default.Check else Icons.Default.Close,
                                            contentDescription = null,
                                            tint = if (isGranted) EmeraldSuccess else CrimsonStrict,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = perm.title.split(" ").first(),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isGranted) EmeraldSuccess else CrimsonStrict
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Active Session Card or Quick Start Hero
        item {
            if (sessionState.isActive) {
                ActiveSessionBanner(
                    sessionState = sessionState,
                    onOpenSessionView = onOpenSessionView,
                    onEndNormalSession = onEndNormalSession
                )
            } else {
                QuickStartHeroCard(
                    onStartSessionClick = onStartSessionClick,
                    onQuickStart = onQuickStart
                )
            }
        }

        // Minimalist Olauncher Mode Launcher Banner
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenMinimalLauncher)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhoneAndroid,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Minimalist Focus Launcher",
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Text-based home screen without app icons",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Text(
                        text = "Open",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // 3. Today's Metrics / Stats Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Focus Minutes
                StatMetricCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Timer,
                    iconTint = IndigoPrimary,
                    title = "Focus Time",
                    value = "${totalMinutesToday}m",
                    subtitle = "Completed today"
                )

                // Blocks Prevented
                StatMetricCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Security,
                    iconTint = CyanAccent,
                    title = "Shield Blocks",
                    value = "$blockedAttemptsCount",
                    subtitle = "Distractions intercepted"
                )
            }
        }

        // 4. Quick Block Lists Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Active Protection Lists",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Manage All",
                    style = MaterialTheme.typography.labelMedium,
                    color = IndigoPrimary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clickable(onClick = onNavigateToLists)
                        .padding(4.dp)
                )
            }
        }

        // Block list items
        items(blockLists) { list ->
            val targetCount = allTargets.count { it.listId == list.id }
            BlockListRowCard(
                blockList = list,
                targetCount = targetCount,
                isSessionStrict = sessionState.isActive && sessionState.isStrictMode,
                onToggle = { onToggleList(list) },
                onClick = onNavigateToLists
            )
        }
    }
}

@Composable
fun ActiveSessionBanner(
    sessionState: ActiveSessionState,
    onOpenSessionView: () -> Unit,
    onEndNormalSession: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

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
        SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(sessionState.endTimeMillis))
    } else ""

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            if (sessionState.isStrictMode) CrimsonStrict.copy(alpha = 0.35f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenSessionView)
            .testTag("active_session_card")
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (sessionState.isStrictMode) Icons.Default.Lock else Icons.Default.Shield,
                        contentDescription = null,
                        tint = if (sessionState.isStrictMode) CrimsonStrict else IndigoPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (sessionState.isStrictMode) "STRICT MODE ACTIVE" else "FOCUS SESSION IN PROGRESS",
                        fontWeight = FontWeight.ExtraBold,
                        color = if (sessionState.isStrictMode) CrimsonStrict else IndigoPrimary,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp
                    )
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Timer & End Time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = timeFormatted,
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Light,
                        letterSpacing = (-0.5).sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (sessionState.isStrictMode) "Locked until $endFormatted" else "Scheduled end: $endFormatted",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (!sessionState.isStrictMode) {
                    Button(
                        onClick = onEndNormalSession,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = "End",
                            tint = CrimsonStrict,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "End", color = CrimsonStrict, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = if (sessionState.isStrictMode) CrimsonStrict else IndigoPrimary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            if (sessionState.isStrictMode) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = CrimsonStrict.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = CrimsonStrict,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Blocking cannot be disabled until $endFormatted",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = CrimsonStrict
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QuickStartHeroCard(
    onStartSessionClick: () -> Unit,
    onQuickStart: (minutes: Int, isStrict: Boolean) -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Ready to Focus?",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Block distractions & get into the flow",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(IndigoPrimary.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.FlashOn,
                        contentDescription = null,
                        tint = IndigoPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Start Button
            Button(
                onClick = onStartSessionClick,
                colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("dashboard_start_session_button")
            ) {
                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Configure Focus Session",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Quick launch pills: 25m Pomodoro, 30m Normal, 1h Strict
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onQuickStart(25, false) },
                    color = Color(0xFFEF4444).copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "25m Sprint",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFCA5A5),
                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 2.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onQuickStart(30, false) },
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "30m Normal",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 2.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }

                Surface(
                    modifier = Modifier
                        .weight(1.1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onQuickStart(60, true) },
                    color = CrimsonStrict.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, CrimsonStrict.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "1h Strict",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = CrimsonStrict,
                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 2.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    }
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
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(iconTint.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = value, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
            Text(text = title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
        }
    }
}

@Composable
fun BlockListRowCard(
    blockList: BlockList,
    targetCount: Int,
    isSessionStrict: Boolean,
    onToggle: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(Color(blockList.colorHex).copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = Color(blockList.colorHex),
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = blockList.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$targetCount rules active",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Switch(
                checked = blockList.isEnabled,
                onCheckedChange = { if (!isSessionStrict) onToggle() },
                enabled = !isSessionStrict,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(blockList.colorHex)
                )
            )
        }
    }
}
