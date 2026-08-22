package com.example.ui.components

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.BlockList
import com.example.service.FocusSessionManager
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.EmeraldSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StartSessionDialog(
    availableLists: List<BlockList>,
    onDismiss: () -> Unit,
    onStartSession: (
        title: String,
        durationMinutes: Int,
        isStrictMode: Boolean,
        activeLists: List<String>,
        autoLaunchMinimal: Boolean,
        isPomodoro: Boolean,
        pomodoroRound: Int,
        pomodoroTotalRounds: Int,
        isUltraStrict: Boolean
    ) -> Unit
) {
    val context = LocalContext.current
    val sessionManager = remember { FocusSessionManager.getInstance(context) }

    var title by remember { mutableStateOf("") }
    var pomodoroWorkMinutes by remember { mutableIntStateOf(25) }
    var isCustomSprint by remember { mutableStateOf(false) }
    var customSprintText by remember { mutableStateOf("") }
    val pomodoroBreakMinutes = 5
    val pomodoroTotalRounds = 4

    var showShieldOptions by remember { mutableStateOf(false) }

    // Allowed Apps in Focus Mode (Max 3)
    var allowedAppPkgs by remember {
        mutableStateOf(sessionManager.getCustomEssentialApps().take(3))
    }
    var showAllowedAppPicker by remember { mutableStateOf(false) }
    var allowedAppSearchQuery by remember { mutableStateOf("") }
    var installedAppsList by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var maxLimitWarning by remember { mutableStateOf(false) }

    // Load installed apps asynchronously for the allowed app picker
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val intent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolveInfos = pm.queryIntentActivities(intent, 0)
            val list = resolveInfos.mapNotNull { ri ->
                val pkg = ri.activityInfo.packageName
                if (pkg == context.packageName) null
                else {
                    val label = ri.loadLabel(pm)?.toString() ?: pkg
                    label to pkg
                }
            }.distinctBy { it.second }.sortedBy { it.first.lowercase() }

            withContext(Dispatchers.Main) {
                installedAppsList = list
            }
        }
    }

    // Multi-selected block lists
    val selectedListIds = remember {
        mutableStateOf(availableLists.filter { it.isEnabled }.map { it.id }.toSet())
    }

    val effectiveMinutes = if (isCustomSprint) {
        customSprintText.toIntOrNull()?.coerceIn(5, 120) ?: 25
    } else {
        pomodoroWorkMinutes
    }

    val totalMins = (effectiveMinutes * pomodoroTotalRounds) + (pomodoroBreakMinutes * (pomodoroTotalRounds - 1))
    val calculatedEndTime = System.currentTimeMillis() + (totalMins * 60 * 1000L)
    val endTimeString = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(calculatedEndTime))

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(20.dp))
                .background(DarkSurface),
            color = DarkSurface,
            tonalElevation = 6.dp,
            border = BorderStroke(1.dp, DarkCardBorder)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Clean Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFFE11D48).copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                tint = Color(0xFFF43F5E),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Pomodoro Focus",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "4 Cycles • 5m Rest Intervals",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // 2. Goal Input (Clean & Optional)
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("What is your focus goal? (Optional)", color = Color(0xFF64748B), fontSize = 13.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFE11D48),
                        unfocusedBorderColor = DarkCardBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0xFF131D33),
                        unfocusedContainerColor = Color(0xFF131D33)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // 3. Pomodoro Sprint Duration Selector
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "FOCUS SPRINT DURATION",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8),
                        letterSpacing = 0.8.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(25 to "Classic", 45 to "Deep", 50 to "Sprint").forEach { (mins, label) ->
                            val isSelected = !isCustomSprint && pomodoroWorkMinutes == mins
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) Color(0xFFE11D48) else Color(0xFF1E293B),
                                border = BorderStroke(1.dp, if (isSelected) Color(0xFFFB7185) else DarkCardBorder),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        isCustomSprint = false
                                        pomodoroWorkMinutes = mins
                                    }
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "${mins}m",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = if (isSelected) Color.White else Color(0xFF94A3B8)
                                    )
                                    Text(
                                        text = label,
                                        fontSize = 10.sp,
                                        color = if (isSelected) Color.White.copy(alpha = 0.8f) else Color(0xFF64748B)
                                    )
                                }
                            }
                        }

                        // Custom sprint button
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isCustomSprint) Color(0xFFE11D48) else Color(0xFF1E293B),
                            border = BorderStroke(1.dp, if (isCustomSprint) Color(0xFFFB7185) else DarkCardBorder),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { isCustomSprint = true }
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Custom",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (isCustomSprint) Color.White else Color(0xFF94A3B8)
                                )
                                Text(
                                    text = "Mins",
                                    fontSize = 10.sp,
                                    color = if (isCustomSprint) Color.White.copy(alpha = 0.8f) else Color(0xFF64748B)
                                )
                            }
                        }
                    }

                    if (isCustomSprint) {
                        OutlinedTextField(
                            value = customSprintText,
                            onValueChange = { customSprintText = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Sprint minutes (5 - 120)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFE11D48),
                                unfocusedBorderColor = DarkCardBorder,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Quiet breakdown summary
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF131D33), RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "4 Cycles • 5m Breathers • Ends $endTimeString",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                        Text(
                            text = "Auto Flow",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldSuccess
                        )
                    }
                }

                // 4. SECTION: ALLOWED APPS ONLY IN FOCUS MODE (MAX 3 APPS)
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF131D33)),
                    border = BorderStroke(1.dp, if (allowedAppPkgs.isNotEmpty()) EmeraldSuccess.copy(alpha = 0.35f) else DarkCardBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("allowed_apps_section")
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showAllowedAppPicker = !showAllowedAppPicker },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(EmeraldSuccess.copy(alpha = 0.15f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = EmeraldSuccess,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Allowed Apps in Focus",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Only these apps stay accessible",
                                        fontSize = 10.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (allowedAppPkgs.size == 3) EmeraldSuccess.copy(alpha = 0.2f) else Color(0xFF1E293B)
                                ) {
                                    Text(
                                        text = "${allowedAppPkgs.size}/3 max",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (allowedAppPkgs.size == 3) EmeraldSuccess else CyanAccent,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                    )
                                }
                                Icon(
                                    imageVector = if (showAllowedAppPicker) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Selected Apps Chips
                        if (allowedAppPkgs.isEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF0F172A),
                                border = BorderStroke(1.dp, Color(0xFF334155)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { showAllowedAppPicker = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        tint = CyanAccent,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Select up to 3 essential apps to allow",
                                        fontSize = 11.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                            }
                        } else {
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                allowedAppPkgs.forEach { pkg ->
                                    val appName = installedAppsList.firstOrNull { it.second.equals(pkg, ignoreCase = true) }?.first
                                        ?: pkg.substringAfterLast('.').replaceFirstChar { it.uppercase() }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = EmeraldSuccess.copy(alpha = 0.15f),
                                        border = BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.4f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(start = 8.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = appName,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = Color.White
                                            )
                                            IconButton(
                                                onClick = {
                                                    allowedAppPkgs = allowedAppPkgs.filter { it != pkg }
                                                    maxLimitWarning = false
                                                },
                                                modifier = Modifier.size(18.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Remove",
                                                    tint = Color(0xFF94A3B8),
                                                    modifier = Modifier.size(12.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                if (allowedAppPkgs.size < 3) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFF1E293B),
                                        border = BorderStroke(1.dp, DarkCardBorder),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { showAllowedAppPicker = true }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = null,
                                                tint = CyanAccent,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Text(
                                                text = "Add App",
                                                fontSize = 11.sp,
                                                color = CyanAccent,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Expandable App Picker Sheet
                        AnimatedVisibility(visible = showAllowedAppPicker) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (maxLimitWarning) {
                                    Text(
                                        text = "⚠️ Maximum 3 allowed apps reached. Remove one to add another.",
                                        fontSize = 11.sp,
                                        color = Color(0xFFFBBF24),
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                OutlinedTextField(
                                    value = allowedAppSearchQuery,
                                    onValueChange = { allowedAppSearchQuery = it },
                                    placeholder = { Text("Search installed apps...", color = Color(0xFF64748B), fontSize = 11.sp) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Search,
                                            contentDescription = null,
                                            tint = Color(0xFF64748B),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(8.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = EmeraldSuccess,
                                        unfocusedBorderColor = DarkCardBorder,
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedContainerColor = Color(0xFF0F172A),
                                        unfocusedContainerColor = Color(0xFF0F172A)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(46.dp)
                                )

                                val filteredApps = installedAppsList.filter { (name, pkg) ->
                                    allowedAppSearchQuery.isBlank() ||
                                            name.contains(allowedAppSearchQuery, ignoreCase = true) ||
                                            pkg.contains(allowedAppSearchQuery, ignoreCase = true)
                                }.take(15)

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(160.dp)
                                        .verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    if (filteredApps.isEmpty()) {
                                        Text(
                                            text = if (installedAppsList.isEmpty()) "Loading apps..." else "No matching apps found",
                                            fontSize = 11.sp,
                                            color = Color(0xFF64748B),
                                            modifier = Modifier.padding(8.dp)
                                        )
                                    } else {
                                        filteredApps.forEach { (name, pkg) ->
                                            val isAllowed = allowedAppPkgs.contains(pkg)
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (isAllowed) EmeraldSuccess.copy(alpha = 0.15f) else Color.Transparent)
                                                    .clickable {
                                                        if (isAllowed) {
                                                            allowedAppPkgs = allowedAppPkgs.filter { it != pkg }
                                                            maxLimitWarning = false
                                                        } else {
                                                            if (allowedAppPkgs.size < 3) {
                                                                allowedAppPkgs = allowedAppPkgs + pkg
                                                                maxLimitWarning = false
                                                            } else {
                                                                maxLimitWarning = true
                                                            }
                                                        }
                                                    }
                                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Apps,
                                                        contentDescription = null,
                                                        tint = if (isAllowed) EmeraldSuccess else Color(0xFF94A3B8),
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Column {
                                                        Text(
                                                            text = name,
                                                            fontSize = 12.sp,
                                                            fontWeight = if (isAllowed) FontWeight.Bold else FontWeight.Medium,
                                                            color = if (isAllowed) Color.White else Color(0xFF94A3B8)
                                                        )
                                                        Text(
                                                            text = pkg,
                                                            fontSize = 9.sp,
                                                            color = Color(0xFF64748B),
                                                            maxLines = 1
                                                        )
                                                    }
                                                }

                                                if (isAllowed) {
                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = EmeraldSuccess
                                                    ) {
                                                        Text(
                                                            text = "Allowed",
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color.Black,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                } else {
                                                    Text(
                                                        text = if (allowedAppPkgs.size >= 3) "Max reached" else "+ Allow",
                                                        fontSize = 10.sp,
                                                        color = if (allowedAppPkgs.size >= 3) Color(0xFF475569) else CyanAccent
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 5. Clean Protection Shield Card (Category Blocklists)
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF131D33)),
                    border = BorderStroke(1.dp, DarkCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showShieldOptions = !showShieldOptions },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = CyanAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Distraction Shield",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color.White
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${selectedListIds.value.size} categories active",
                                    fontSize = 11.sp,
                                    color = Color(0xFF94A3B8)
                                )
                                Icon(
                                    imageVector = if (showShieldOptions) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Expandable block list picker
                        AnimatedVisibility(visible = showShieldOptions) {
                            Column(
                                modifier = Modifier.padding(top = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                availableLists.forEach { list ->
                                    val isChecked = selectedListIds.value.contains(list.id)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isChecked) Color(0xFF1E293B) else Color.Transparent)
                                            .clickable {
                                                val current = selectedListIds.value.toMutableSet()
                                                if (isChecked) current.remove(list.id) else current.add(list.id)
                                                selectedListIds.value = current
                                            }
                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = list.name,
                                            fontSize = 12.sp,
                                            color = if (isChecked) Color.White else Color(0xFF94A3B8)
                                        )
                                        if (isChecked) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = CyanAccent,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 6. Action Button: Start Pomodoro Focus
                Button(
                    onClick = {
                        sessionManager.saveCustomEssentialApps(allowedAppPkgs.take(3))
                        val activeNames = availableLists.filter { selectedListIds.value.contains(it.id) }.map { it.name }
                        val sessionTitle = if (title.isBlank()) "Pomodoro Flow (Round 1/4)" else title
                        onStartSession(
                            sessionTitle,
                            effectiveMinutes,
                            true, // isStrictMode = true
                            activeNames,
                            false,
                            true, // isPomodoro = true
                            1,
                            pomodoroTotalRounds,
                            false // isUltraStrict = false
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("start_pomodoro_focus_button")
                ) {
                    Text(text = "🍅", fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Start Pomodoro Flow (${effectiveMinutes}m)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}
