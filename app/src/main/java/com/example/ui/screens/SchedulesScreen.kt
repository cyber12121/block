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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreTime
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Schedule
import com.example.service.ActiveSessionState
import com.example.ui.theme.CrimsonStrict
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IndigoPrimary
import java.util.Calendar

fun isScheduleActiveNow(schedule: Schedule): Boolean {
    if (!schedule.isEnabled) return false
    val cal = Calendar.getInstance()
    val currentDay = cal.get(Calendar.DAY_OF_WEEK)
    val currentHour = cal.get(Calendar.HOUR_OF_DAY)
    val currentMinute = cal.get(Calendar.MINUTE)
    val currentTotalMinutes = currentHour * 60 + currentMinute

    val days = schedule.daysOfWeek.split(",").mapNotNull { it.trim().toIntOrNull() }
    if (!days.contains(currentDay)) return false

    val startMinutes = schedule.startHour * 60 + schedule.startMinute
    val endMinutes = schedule.endHour * 60 + schedule.endMinute

    return if (startMinutes < endMinutes) {
        currentTotalMinutes in startMinutes until endMinutes
    } else {
        currentTotalMinutes >= startMinutes || currentTotalMinutes < endMinutes
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SchedulesScreen(
    sessionState: ActiveSessionState,
    schedules: List<Schedule>,
    onToggleSchedule: (Schedule) -> Unit,
    onDeleteSchedule: (Schedule) -> Unit,
    onOpenCreateSchedule: () -> Unit,
    onEmergencyUnlock: () -> Unit = {}
) {
    var lockedScheduleInfo by remember { mutableStateOf<Schedule?>(null) }
    val isSessionStrict = sessionState.isActive && sessionState.isStrictMode

    Scaffold(
        floatingActionButton = {
            if (!isSessionStrict) {
                ExtendedFloatingActionButton(
                    onClick = onOpenCreateSchedule,
                    containerColor = IndigoPrimary,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp),
                    elevation = androidx.compose.material3.FloatingActionButtonDefaults.elevation(6.dp),
                    modifier = Modifier
                        .padding(bottom = 16.dp)
                        .testTag("add_schedule_fab")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "New Schedule",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "New Schedule",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        },
        floatingActionButtonPosition = FabPosition.End,
        containerColor = Color.Transparent
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Automated Schedules",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Auto-start focus shields on recurring clock hours & days",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (sessionState.isActive) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = Color(0xFF111A2E),
                            shape = RoundedCornerShape(100.dp),
                            border = BorderStroke(1.dp, CrimsonStrict.copy(alpha = 0.6f)),
                            modifier = Modifier
                                .clickable { onEmergencyUnlock() }
                                .testTag("schedules_top_exit_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LockOpen,
                                    contentDescription = "Stop Active Session",
                                    tint = CrimsonStrict,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Exit (∞)",
                                    fontSize = 11.sp,
                                    color = Color(0xFFE2E8F0),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }



            // Active Scheduled Status Info Card
            item {
                val isAutoActive = sessionState.isActive && sessionState.isAutoScheduled
                val isAnyActive = sessionState.isActive
                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                val pulseAlpha by infiniteTransition.animateFloat(
                    initialValue = 0.6f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1200, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "pulseAlpha"
                )

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (isAnyActive) CrimsonStrict.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(
                                        if (isAnyActive) CrimsonStrict.copy(alpha = 0.15f) else IndigoPrimary.copy(alpha = 0.15f),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isAutoActive) Icons.Default.AutoAwesome else if (isAnyActive) Icons.Default.Lock else Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = if (isAnyActive) CrimsonStrict else IndigoPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (isAutoActive) "Scheduled Block Active Now" else if (isAnyActive) "Focus Timer Running" else "24/7 Clock Guard Armed",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (isAnyActive) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            color = CrimsonStrict.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = if (sessionState.isStrictMode) "STRICT" else "ACTIVE",
                                                color = CrimsonStrict,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Black,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = if (isAnyActive)
                                        "Focus block “${sessionState.title}” is active (${sessionState.remainingSeconds / 60}m remaining)."
                                    else
                                        "Schedules engage automatically when their clock time window arrives.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (isAnyActive) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = onEmergencyUnlock,
                                colors = ButtonDefaults.buttonColors(containerColor = CrimsonStrict),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(38.dp)
                                    .testTag("schedules_stop_active_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LockOpen,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Stop Active Schedule / Timer (∞)",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }

            // Schedules List
            if (schedules.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(52.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No Schedules Created Yet",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tap 'Set Up New Schedule' above to configure recurring focus blocks for work sprint, deep study, or sleep bedtime.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(schedules, key = { it.id }) { schedule ->
                    val isCurrentlyActive = isScheduleActiveNow(schedule)
                    ScheduleCard(
                        schedule = schedule,
                        isActiveNow = isCurrentlyActive,
                        isGlobalStrict = isSessionStrict,
                        onToggle = {
                            if (isCurrentlyActive || isSessionStrict) {
                                lockedScheduleInfo = schedule
                            } else {
                                onToggleSchedule(schedule)
                            }
                        },
                        onDelete = {
                            if (isCurrentlyActive || isSessionStrict) {
                                lockedScheduleInfo = schedule
                            } else {
                                onDeleteSchedule(schedule)
                            }
                        }
                    )
                }
            }
        }
    }

    // In-Window Lock Explanation Dialog
    lockedScheduleInfo?.let { schedule ->
        AlertDialog(
            onDismissRequest = { lockedScheduleInfo = null },
            icon = {
                Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = CrimsonStrict)
            },
            title = {
                Text("Schedule Locked In Active Window", fontWeight = FontWeight.Bold)
            },
            text = {
                Text("“${schedule.name}” is currently active and enforcing focus. To prevent distraction bypasses, schedules can only be turned off or deleted outside of their active schedule hours.")
            },
            confirmButton = {
                Button(
                    onClick = { lockedScheduleInfo = null },
                    colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary)
                ) {
                    Text("Understood")
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ScheduleCard(
    schedule: Schedule,
    isActiveNow: Boolean,
    isGlobalStrict: Boolean,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    fun formatHour(hour: Int, minute: Int): String {
        val h = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
        val amPm = if (hour >= 12) "PM" else "AM"
        return String.format("%02d:%02d %s", h, minute, amPm)
    }

    val activeDaysSet = schedule.daysOfWeek.split(",").map { it.trim() }.toSet()

    val allDays = listOf(
        Pair("2", "M"),
        Pair("3", "T"),
        Pair("4", "W"),
        Pair("5", "T"),
        Pair("6", "F"),
        Pair("7", "S"),
        Pair("1", "S")
    )

    val isLocked = isActiveNow || isGlobalStrict

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Title Row with Icon & Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(
                                if (isActiveNow) CyanAccent.copy(alpha = 0.2f) else if (schedule.isStrictMode) CrimsonStrict.copy(alpha = 0.18f) else IndigoPrimary.copy(alpha = 0.15f),
                                RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isActiveNow) Icons.Default.Shield else if (schedule.isStrictMode) Icons.Default.Lock else Icons.Default.AccessTime,
                            contentDescription = null,
                            tint = if (isActiveNow) CyanAccent else if (schedule.isStrictMode) CrimsonStrict else IndigoPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = schedule.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (isActiveNow) {
                                Surface(
                                    color = EmeraldSuccess.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "ACTIVE NOW",
                                        color = EmeraldSuccess,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = if (schedule.isEnabled) "Automated block armed" else "Schedule paused",
                                fontSize = 11.sp,
                                color = if (schedule.isEnabled) Color(0xFF94A3B8) else Color(0xFF64748B),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = schedule.isEnabled,
                        onCheckedChange = { onToggle() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = if (isActiveNow) CyanAccent else IndigoPrimary,
                            uncheckedTrackColor = Color(0xFF1D2A4A)
                        ),
                        modifier = Modifier.testTag("toggle_schedule_${schedule.id}")
                    )
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.Delete,
                            contentDescription = if (isLocked) "Locked" else "Delete",
                            tint = if (isLocked) CrimsonStrict else Color(0xFF64748B),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Time Window Clock Banner
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = null,
                            tint = CyanAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${formatHour(schedule.startHour, schedule.startMinute)}  ➔  ${formatHour(schedule.endHour, schedule.endMinute)}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    if (schedule.isStrictMode) {
                        Surface(
                            color = CrimsonStrict.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "STRICT",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = CrimsonStrict,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Days of the Week Mini-Pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                allDays.forEach { (dayId, initial) ->
                    val isDayActive = activeDaysSet.contains(dayId)
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(
                                color = if (isDayActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initial,
                            fontSize = 11.sp,
                            fontWeight = if (isDayActive) FontWeight.Bold else FontWeight.Normal,
                            color = if (isDayActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (schedule.activeListNames.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Guards: ${schedule.activeListNames}",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8),
                        maxLines = 1
                    )
                }
            }
        }
    }
}
