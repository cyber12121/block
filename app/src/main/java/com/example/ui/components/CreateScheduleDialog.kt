package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.BlockList
import com.example.ui.theme.CrimsonStrict
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IndigoPrimary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateScheduleDialog(
    availableLists: List<BlockList>,
    onDismiss: () -> Unit,
    onCreateSchedule: (
        name: String,
        startHour: Int,
        startMinute: Int,
        endHour: Int,
        endMinute: Int,
        daysOfWeek: String,
        isStrictMode: Boolean,
        isUltraStrict: Boolean,
        activeListNames: String
    ) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var startHour by remember { mutableIntStateOf(9) }
    var startMinute by remember { mutableIntStateOf(0) }
    var endHour by remember { mutableIntStateOf(17) }
    var endMinute by remember { mutableIntStateOf(0) }
    var isStrictMode by remember { mutableStateOf(false) }
    var isUltraStrict by remember { mutableStateOf(false) }

    // Dialog state for interactive Clock Time Picker
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    // Day numbers: 1=Sun, 2=Mon, 3=Tue, 4=Wed, 5=Thu, 6=Fri, 7=Sat
    val selectedDays = remember { mutableStateOf(setOf(2, 3, 4, 5, 6)) } // Mon-Fri default
    val selectedListIds = remember { mutableStateOf(availableLists.filter { it.isEnabled }.map { it.id }.toSet()) }
    var isAppBlockingEnforced by remember { mutableStateOf(true) }
    var isDropdownExpanded by remember { mutableStateOf(false) }

    val daysList = listOf(
        Pair(2, "Mon"),
        Pair(3, "Tue"),
        Pair(4, "Wed"),
        Pair(5, "Thu"),
        Pair(6, "Fri"),
        Pair(7, "Sat"),
        Pair(1, "Sun")
    )

    val templates = listOf(
        ScheduleTemplate("💼 Work Sprint", 9, 0, 17, 0, setOf(2, 3, 4, 5, 6), false),
        ScheduleTemplate("🌙 Night Rest", 22, 30, 7, 0, setOf(1, 2, 3, 4, 5, 6, 7), true),
        ScheduleTemplate("📚 Deep Study", 14, 0, 18, 0, setOf(2, 3, 4, 5, 6), false),
        ScheduleTemplate("🌅 Morning Flow", 6, 30, 9, 0, setOf(2, 3, 4, 5, 6), false),
        ScheduleTemplate("🧘 Weekend Detox", 10, 0, 20, 0, setOf(1, 7), true)
    )

    fun formatTime(hour: Int, minute: Int): String {
        val h = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
        val amPm = if (hour >= 12) "PM" else "AM"
        return String.format("%02d:%02d %s", h, minute, amPm)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(28.dp)),
            color = Color(0xFF0B1222),
            border = BorderStroke(1.dp, Color(0xFF1E2D4A)),
            tonalElevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .background(IndigoPrimary.copy(alpha = 0.25f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = CyanAccent,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Create Auto Schedule",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Automate custom focus windows & block rules",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color(0xFF16233B), CircleShape)
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Quick Templates
                Text(
                    text = "QUICK PRESETS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF64748B),
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    templates.forEach { tmpl ->
                        val isSelected = name == tmpl.name
                        Surface(
                            shape = RoundedCornerShape(100.dp),
                            color = if (isSelected) IndigoPrimary.copy(alpha = 0.3f) else Color(0xFF131F37),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) CyanAccent else Color(0xFF263554)
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(100.dp))
                                .clickable {
                                    name = tmpl.name
                                    startHour = tmpl.startHour
                                    startMinute = tmpl.startMinute
                                    endHour = tmpl.endHour
                                    endMinute = tmpl.endMinute
                                    selectedDays.value = tmpl.days
                                    isStrictMode = tmpl.isStrictMode
                                }
                        ) {
                            Text(
                                text = tmpl.name,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else Color(0xFFCBD5E1),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Schedule Name Input
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Schedule Name") },
                    placeholder = { Text("e.g. Work Sprint, Night Sleep Guard") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanAccent,
                        unfocusedBorderColor = Color(0xFF263554),
                        focusedContainerColor = Color(0xFF0F172A),
                        unfocusedContainerColor = Color(0xFF0F172A),
                        focusedLabelColor = CyanAccent,
                        unfocusedLabelColor = Color(0xFF94A3B8),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = CyanAccent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Time Range Section with Clock Dial Tapping
                Text(
                    text = "ACTIVE TIME WINDOW",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF64748B),
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Start Time Box
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                        border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { showStartTimePicker = true }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "STARTS AT",
                                    fontSize = 10.sp,
                                    color = Color(0xFF94A3B8),
                                    fontWeight = FontWeight.Bold
                                )
                                Icon(
                                    imageVector = Icons.Default.AccessTime,
                                    contentDescription = "Clock",
                                    tint = CyanAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = formatTime(startHour, startMinute),
                                fontSize = 17.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Tap to change",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF38BDF8)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(Color(0xFF16233B), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "to",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    // End Time Box
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                        border = BorderStroke(1.dp, IndigoPrimary.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { showEndTimePicker = true }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "ENDS AT",
                                    fontSize = 10.sp,
                                    color = Color(0xFF94A3B8),
                                    fontWeight = FontWeight.Bold
                                )
                                Icon(
                                    imageVector = Icons.Default.AccessTime,
                                    contentDescription = "Clock",
                                    tint = IndigoPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = formatTime(endHour, endMinute),
                                fontSize = 17.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Tap to change",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF818CF8)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Repeat Days
                Text(
                    text = "REPEAT ON DAYS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF64748B),
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    daysList.forEach { (dayNum, label) ->
                        val isSelected = selectedDays.value.contains(dayNum)
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) IndigoPrimary else Color(0xFF0F172A))
                                .border(
                                    1.dp,
                                    if (isSelected) CyanAccent.copy(alpha = 0.6f) else Color(0xFF263554),
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    val current = selectedDays.value.toMutableSet()
                                    if (isSelected) {
                                        if (current.size > 1) current.remove(dayNum)
                                    } else {
                                        current.add(dayNum)
                                    }
                                    selectedDays.value = current
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else Color(0xFF94A3B8)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Strict Mode Switch
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isStrictMode) CrimsonStrict.copy(alpha = 0.12f) else Color(0xFF0F172A)
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (isStrictMode) CrimsonStrict.copy(alpha = 0.4f) else Color(0xFF263554)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .padding(14.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = if (isStrictMode) CrimsonStrict else Color(0xFF94A3B8),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Normal Blocker Enforcement",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = "Enforces session restrictions and prevents disabling during the active schedule",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 10.sp,
                                    lineHeight = 14.sp
                                )
                            }
                        }

                        Switch(
                            checked = isStrictMode,
                            onCheckedChange = { checked ->
                                isStrictMode = checked
                                if (checked) {
                                    isUltraStrict = false
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = CrimsonStrict,
                                uncheckedTrackColor = Color(0xFF1E2D4A)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Ultra Strict Blocker Card (Clean, multi-row layout to prevent text cramming)
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isUltraStrict) Color(0xFF3F0F17) else Color(0xFF0F172A)
                    ),
                    border = BorderStroke(1.dp, if (isUltraStrict) CrimsonStrict else Color(0xFF263554)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .padding(14.dp)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = if (isUltraStrict) CrimsonStrict else Color(0xFF94A3B8),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Strict Blocker 🔒",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = CrimsonStrict,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "NO EXIT",
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Switch(
                                checked = isUltraStrict,
                                onCheckedChange = { checked ->
                                    isUltraStrict = checked
                                    if (checked) {
                                        isStrictMode = false
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = CrimsonStrict,
                                    uncheckedTrackColor = Color(0xFF1E2D4A)
                                )
                            )
                        }

                        Text(
                            text = "Automated Schedule only: Cannot be exited during the locked window under ANY circumstance. Unlocks ONLY when the schedule window expires.",
                            color = Color(0xFFCBD5E1),
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Block Lists & App Blocking Dropdown Selector
                Text(
                    text = "ENFORCE BLOCK LISTS & APP BLOCKING",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF64748B),
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Dropdown Header Card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    border = BorderStroke(1.dp, if (isDropdownExpanded) CyanAccent else Color(0xFF263554)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { isDropdownExpanded = !isDropdownExpanded }
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
                                    .background(CyanAccent.copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhoneAndroid,
                                    contentDescription = null,
                                    tint = CyanAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Enforced App & Site Lists",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                val activeNamesList = availableLists.filter { selectedListIds.value.contains(it.id) }.map { it.name }.toMutableList()
                                if (isAppBlockingEnforced) {
                                    activeNamesList.add(0, "Installed Apps Blocker")
                                }
                                val summary = if (activeNamesList.isEmpty()) {
                                    "No lists or apps selected"
                                } else {
                                    activeNamesList.joinToString(", ")
                                }
                                Text(
                                    text = summary,
                                    fontSize = 11.sp,
                                    color = Color(0xFF94A3B8),
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                        }

                        Icon(
                            imageVector = if (isDropdownExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                            contentDescription = "Toggle Dropdown",
                            tint = CyanAccent,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Animated Dropdown Body
                AnimatedVisibility(visible = isDropdownExpanded) {
                    Column(
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .fillMaxWidth()
                            .background(Color(0xFF0F172A), RoundedCornerShape(16.dp))
                            .border(1.dp, Color(0xFF1E2D4A), RoundedCornerShape(16.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Featured Installed Apps Blocker Toggle
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (isAppBlockingEnforced) IndigoPrimary.copy(alpha = 0.18f) else Color(0xFF131D31),
                                    RoundedCornerShape(12.dp)
                                )
                                .border(
                                    1.dp,
                                    if (isAppBlockingEnforced) CyanAccent.copy(alpha = 0.5f) else Color(0xFF263554),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { isAppBlockingEnforced = !isAppBlockingEnforced }
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(CyanAccent.copy(alpha = 0.2f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PhoneAndroid,
                                        contentDescription = "Installed Apps",
                                        tint = CyanAccent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Installed Apps Blocker",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Enforce blocking on installed mobile apps",
                                        fontSize = 10.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                            }

                            Switch(
                                checked = isAppBlockingEnforced,
                                onCheckedChange = { isAppBlockingEnforced = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = IndigoPrimary,
                                    uncheckedTrackColor = Color(0xFF1E2D4A)
                                )
                            )
                        }

                        HorizontalDivider(color = Color(0xFF1E2D4A), thickness = 1.dp)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "CATEGORY BLOCK LISTS",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF64748B),
                                letterSpacing = 0.5.sp
                            )
                            val allSelected = selectedListIds.value.size == availableLists.size
                            Text(
                                text = if (allSelected) "Deselect All" else "Select All",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyanAccent,
                                modifier = Modifier.clickable {
                                    selectedListIds.value = if (allSelected) emptySet() else availableLists.map { it.id }.toSet()
                                }
                            )
                        }

                        // Category list items
                        availableLists.forEach { list ->
                            val isChecked = selectedListIds.value.contains(list.id)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isChecked) Color(list.colorHex).copy(alpha = 0.12f) else Color.Transparent)
                                    .clickable {
                                        val current = selectedListIds.value.toMutableSet()
                                        if (isChecked) current.remove(list.id) else current.add(list.id)
                                        selectedListIds.value = current
                                    }
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .background(
                                                if (isChecked) Color(list.colorHex) else Color(0xFF1E2D4A),
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isChecked) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = list.name,
                                            fontSize = 12.sp,
                                            fontWeight = if (isChecked) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isChecked) Color.White else Color(0xFFCBD5E1)
                                        )
                                        if (list.description.isNotBlank()) {
                                            Text(
                                                text = list.description,
                                                fontSize = 10.sp,
                                                color = Color(0xFF64748B),
                                                maxLines = 1,
                                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                // Save & Activate Action Button
                val isFormValid = name.isNotBlank() && selectedDays.value.isNotEmpty()
                Button(
                    onClick = {
                        if (isFormValid) {
                            val daysString = selectedDays.value.sorted().joinToString(",")
                            val activeListNamesList = availableLists.filter { selectedListIds.value.contains(it.id) }.map { it.name }.toMutableList()
                            if (isAppBlockingEnforced && !activeListNamesList.contains("Installed Apps Blocker")) {
                                activeListNamesList.add(0, "Installed Apps Blocker")
                            }
                            val activeNames = activeListNamesList.joinToString(", ")
                            onCreateSchedule(
                                name,
                                startHour,
                                startMinute,
                                endHour,
                                endMinute,
                                daysString,
                                isStrictMode,
                                isUltraStrict,
                                activeNames
                            )
                        }
                    },
                    enabled = isFormValid,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = IndigoPrimary,
                        disabledContainerColor = Color(0xFF16233B)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("submit_create_schedule_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Save & Activate Schedule",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }

    // Interactive Clock Time Picker Dialog for Start Time
    if (showStartTimePicker) {
        ClockTimePickerDialog(
            title = "Set Start Time",
            initialHour = startHour,
            initialMinute = startMinute,
            onDismiss = { showStartTimePicker = false },
            onTimeSelected = { hour, minute ->
                startHour = hour
                startMinute = minute
                showStartTimePicker = false
            }
        )
    }

    // Interactive Clock Time Picker Dialog for End Time
    if (showEndTimePicker) {
        ClockTimePickerDialog(
            title = "Set End Time",
            initialHour = endHour,
            initialMinute = endMinute,
            onDismiss = { showEndTimePicker = false },
            onTimeSelected = { hour, minute ->
                endHour = hour
                endMinute = minute
                showEndTimePicker = false
            }
        )
    }
}


data class ScheduleTemplate(
    val name: String,
    val startHour: Int,
    val startMinute: Int,
    val endHour: Int,
    val endMinute: Int,
    val days: Set<Int>,
    val isStrictMode: Boolean
)
