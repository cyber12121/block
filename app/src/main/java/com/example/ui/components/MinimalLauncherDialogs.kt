package com.example.ui.components

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeveloperMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CrimsonStrict
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IndigoPrimary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.Locale

@Composable
fun EmergencyExitDialog(
    remainingExits: Int,
    isDeveloper: Boolean = false,
    onDismiss: () -> Unit,
    onConfirmExit: () -> Unit
) {
    val hasExitsLeft = isDeveloper || remainingExits > 0

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF111A2E),
            border = BorderStroke(1.dp, Color(0xFF1E293B)),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clickable(enabled = false) {}
        ) {
            Column(
                modifier = Modifier
                    .padding(22.dp)
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LockOpen, contentDescription = null, tint = CrimsonStrict, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Emergency Exit", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDeveloper) Color(0xFF06281E) else Color(0xFF1D2A4A)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(
                        1.dp,
                        if (isDeveloper) EmeraldSuccess.copy(alpha = 0.5f) else Color(0xFF2D3F68)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = if (isDeveloper) "DEVELOPER MODE STATUS" else "GOOGLE ACCOUNT EXITS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDeveloper) EmeraldSuccess else IndigoPrimary,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "$remainingExits / 10 Remaining Today",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (remainingExits <= 2) CrimsonStrict else Color(0xFF38BDF8)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (remainingExits > 0)
                                "You have 10 emergency exits per day. Unlocking now will use 1 exit (resets at midnight)."
                            else
                                "You have reached today's 10 exits quota. Focus sessions must be completed.",
                            fontSize = 12.sp,
                            color = Color(0xFFCBD5E1),
                            lineHeight = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onConfirmExit,
                    enabled = hasExitsLeft,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CrimsonStrict,
                        disabledContainerColor = Color(0xFF334155)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (hasExitsLeft) "Unlock & Exit Launcher" else "0 Exits Left (Limit Reached)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
fun EditEssentialAppsDialog(
    installedApps: List<Pair<String, String>>,
    initialSelectedPackages: List<String>,
    onDismiss: () -> Unit,
    onSave: (List<String>) -> Unit
) {
    val context = LocalContext.current
    var appsList by remember { mutableStateOf(installedApps) }
    var isLoading by remember { mutableStateOf(installedApps.isEmpty()) }
    var selectedPkgs by remember { mutableStateOf(initialSelectedPackages.take(6).toSet()) }
    var searchQuery by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        if (appsList.isEmpty()) {
            withContext(Dispatchers.IO) {
                val cached = com.example.util.InstalledAppsCache.loadApps(context)
                val result = cached.map { Pair(it.appName, it.packageName) }
                appsList = result
                isLoading = false
            }
        }
    }

    val filteredApps = remember(appsList, searchQuery) {
        if (searchQuery.isBlank()) appsList
        else appsList.filter {
            it.first.contains(searchQuery, ignoreCase = true) ||
            it.second.contains(searchQuery, ignoreCase = true)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF111A2E),
            border = BorderStroke(1.dp, Color(0xFF1E293B)),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .clickable(enabled = false) {}
        ) {
            Column(
                modifier = Modifier
                    .padding(22.dp)
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Manage Essential Apps", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
                        Text(
                            text = "${selectedPkgs.size} / 6 selected • ${appsList.size} apps available",
                            fontSize = 12.sp,
                            color = IndigoPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search all installed apps...", color = Color.Gray, fontSize = 13.sp) },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color.Gray, modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        keyboardController?.hide()
                        focusManager.clearFocus()
                    }),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = IndigoPrimary,
                        unfocusedBorderColor = Color(0xFF1E293B),
                        focusedContainerColor = Color(0xFF0F172A),
                        unfocusedContainerColor = Color(0xFF0F172A),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = IndigoPrimary, strokeWidth = 3.dp)
                    }
                } else if (filteredApps.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No matching apps found", color = Color(0xFF64748B), fontSize = 14.sp)
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                    ) {
                        items(filteredApps, key = { it.second }) { app ->
                            val isChecked = selectedPkgs.contains(app.second)
                            Surface(
                                color = if (isChecked) IndigoPrimary.copy(alpha = 0.18f) else Color(0xFF0F172A),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, if (isChecked) IndigoPrimary.copy(alpha = 0.5f) else Color(0xFF1E293B)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (isChecked) {
                                            selectedPkgs = selectedPkgs - app.second
                                        } else if (selectedPkgs.size < 6) {
                                            selectedPkgs = selectedPkgs + app.second
                                        }
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
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
                                                .background(
                                                    if (isChecked) IndigoPrimary.copy(alpha = 0.3f) else Color(0xFF1E293B),
                                                    RoundedCornerShape(8.dp)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = app.first.take(1).uppercase(Locale.getDefault()),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = if (isChecked) Color.White else IndigoPrimary
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = app.first,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color.White,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = app.second,
                                                fontSize = 11.sp,
                                                color = Color(0xFF64748B),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = { checked ->
                                            if (checked && selectedPkgs.size < 6) {
                                                selectedPkgs = selectedPkgs + app.second
                                            } else if (!checked) {
                                                selectedPkgs = selectedPkgs - app.second
                                            }
                                        },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = IndigoPrimary,
                                            uncheckedColor = Color(0xFF64748B)
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { onSave(selectedPkgs.toList()) },
                    colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("save_essential_apps")
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save Essential Apps", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

data class StrictDurationOption(val label: String, val minutes: Int)

data class StrictLevelOption(
    val level: Int,
    val title: String,
    val subtitle: String,
    val badge: String,
    val exitsDesc: String
)

@Composable
fun MinimalStrictLockSetupDialog(
    onDismiss: () -> Unit,
    onStartLock: (minutes: Int, level: Int) -> Unit
) {
    val durationOptions = listOf(
        StrictDurationOption("1 Min", 1),
        StrictDurationOption("1 Hour", 60),
        StrictDurationOption("2 Hours", 120),
        StrictDurationOption("3 Hours", 180),
        StrictDurationOption("4 Hours", 240),
        StrictDurationOption("6 Hours", 360),
        StrictDurationOption("8 Hours", 480),
        StrictDurationOption("12 Hours", 720)
    )

    val strictLevels = listOf(
        StrictLevelOption(
            level = 1,
            title = "Level 1: Soft Strict",
            subtitle = "Mindful Focus • 15s Reflection Exit",
            badge = "ACCOUNTABILITY",
            exitsDesc = "Unlimited daily exits with a short 15-second reflection timer."
        ),
        StrictLevelOption(
            level = 2,
            title = "Level 2: Standard Strict",
            subtitle = "Guarded Mode • 1 Exit Per Day",
            badge = "RECOMMENDED",
            exitsDesc = "1 Emergency Exit per day with a mandatory 1-minute reflection timer."
        ),
        StrictLevelOption(
            level = 3,
            title = "Level 3: Ultra Strict",
            subtitle = "Iron Lockdown • Zero Early Exits",
            badge = "UNBYPASSABLE",
            exitsDesc = "Completely locks Minimal Launcher until the timer reaches zero."
        )
    )

    var selectedOption by remember { mutableStateOf(durationOptions[0]) }
    var selectedLevel by remember { mutableIntStateOf(2) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = CrimsonStrict,
                modifier = Modifier.size(28.dp)
            )
        },
        title = {
            Text("Minimalist Strict Lock 🔒", fontWeight = FontWeight.Bold, color = Color.White)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = "Lock Minimalist Launcher for a fixed duration to eliminate phone distractions. Select your strictness level below:",
                    color = Color(0xFFCBD5E1),
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )

                Text(
                    text = "1. SELECT STRICT LEVEL",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = IndigoPrimary,
                    letterSpacing = 1.sp
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    strictLevels.forEach { lvl ->
                        val isSelected = selectedLevel == lvl.level
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) CrimsonStrict.copy(alpha = 0.16f) else Color(0xFF131D33),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) CrimsonStrict else Color(0xFF223252)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedLevel = lvl.level }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = lvl.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = if (isSelected) Color.White else Color(0xFFE2E8F0)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            color = if (isSelected) CrimsonStrict else Color(0xFF1E293B),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = lvl.badge,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = lvl.subtitle,
                                        fontSize = 11.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { selectedLevel = lvl.level },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = CrimsonStrict,
                                        unselectedColor = Color(0xFF64748B)
                                    )
                                )
                            }
                        }
                    }
                }

                Text(
                    text = "2. SELECT LOCK DURATION",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = IndigoPrimary,
                    letterSpacing = 1.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    durationOptions.forEach { opt ->
                        val isSelected = selectedOption.minutes == opt.minutes
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) CrimsonStrict else Color(0xFF1A2640),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) CrimsonStrict else Color(0xFF2A3A5E)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedOption = opt }
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = opt.label,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                val activeLevelInfo = strictLevels.first { it.level == selectedLevel }
                Surface(
                    color = CrimsonStrict.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, CrimsonStrict.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Summary: ${activeLevelInfo.title} (${selectedOption.label})",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 12.sp
                        )
                        Text(
                            text = activeLevelInfo.exitsDesc,
                            color = Color(0xFFCBD5E1),
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onStartLock(selectedOption.minutes, selectedLevel) },
                colors = ButtonDefaults.buttonColors(containerColor = CrimsonStrict)
            ) {
                Text("Lock Launcher (${selectedOption.label})", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFFCBD5E1))
            }
        },
        containerColor = Color(0xFF111A2E)
    )
}

@Composable
fun MinimalStrictLockStatusDialog(
    remainingMillis: Long,
    exitsRemaining: Int,
    strictLevel: Int = 2,
    isDeveloper: Boolean,
    onDismiss: () -> Unit,
    onDisarm: () -> Unit
) {
    val totalSeconds = remainingMillis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    val formattedTime = when {
        hours > 0 -> "${hours}h ${minutes}m ${seconds}s"
        else -> "${minutes}m ${seconds}s"
    }

    val levelTitle = when (strictLevel) {
        1 -> "Level 1: Soft Strict (15s Reflection)"
        3 -> "Level 3: Ultra Strict (Zero Exits)"
        else -> "Level 2: Standard Strict (1 Exit / Day)"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = CrimsonStrict,
                modifier = Modifier.size(28.dp)
            )
        },
        title = {
            Text("Minimalist Strict Lock Active 🔒", fontWeight = FontWeight.Bold, color = Color.White)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    color = Color(0xFF1A2640),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("REMAINING LOCK TIME", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8), letterSpacing = 1.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(formattedTime, fontSize = 24.sp, fontWeight = FontWeight.Black, color = CrimsonStrict)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(levelTitle, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF06B6D4))
                    }
                }

                Surface(
                    color = if (strictLevel == 3 || (exitsRemaining <= 0 && !isDeveloper)) CrimsonStrict.copy(alpha = 0.15f) else EmeraldSuccess.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(
                        1.dp,
                        if (strictLevel == 3 || (exitsRemaining <= 0 && !isDeveloper)) CrimsonStrict.copy(alpha = 0.4f) else EmeraldSuccess.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (strictLevel == 3 || (exitsRemaining <= 0 && !isDeveloper)) Icons.Default.Lock else Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (strictLevel == 3 || (exitsRemaining <= 0 && !isDeveloper)) CrimsonStrict else EmeraldSuccess,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = when {
                                strictLevel == 3 -> "Level 3: Zero Exits Allowed (Locked until countdown ends)"
                                strictLevel == 1 -> "Level 1: Flexible Exits (15-second reflection timer)"
                                exitsRemaining <= 0 -> "Emergency Exit: 1/1 USED TODAY (Locked until timer ends)"
                                else -> "Emergency Exit Available: $exitsRemaining / 1 Remaining Today"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Text(
                    text = when (strictLevel) {
                        1 -> "Level 1 allows mindful exits with a quick 15-second reflection delay."
                        3 -> "Level 3 Ultra Strict completely locks the Minimal Launcher until the timer reaches zero."
                        else -> "Level 2 allows 1 emergency exit per day with a mandatory 1-minute reflection timer."
                    },
                    fontSize = 11.sp,
                    color = Color(0xFFCBD5E1)
                )
            }
        },
        confirmButton = {
            if (isDeveloper) {
                Button(
                    onClick = onDisarm,
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonStrict)
                ) {
                    Text("Disarm Lock (Dev)", fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary)
                ) {
                    Text("Close", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            if (isDeveloper) {
                OutlinedButton(onClick = onDismiss) {
                    Text("Close", color = Color(0xFFCBD5E1))
                }
            }
        },
        containerColor = Color(0xFF111A2E)
    )
}

@Composable
fun MinimalStrictUseExitDialog(
    remainingFormatted: String,
    exitsRemaining: Int,
    strictLevel: Int = 2,
    onDismiss: () -> Unit,
    onConfirmExit: () -> Unit
) {
    val initialSeconds = if (strictLevel == 1) 15 else 60
    var countdownSeconds by remember { mutableIntStateOf(initialSeconds) }

    LaunchedEffect(Unit) {
        while (countdownSeconds > 0) {
            delay(1000L)
            countdownSeconds -= 1
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Timer,
                contentDescription = null,
                tint = Color(0xFFF59E0B),
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(if (strictLevel == 1) "15-Second Reflection Timer ⏳" else "1-Minute Exit Reflection Timer ⏳", fontWeight = FontWeight.Bold, color = Color.White)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    color = Color(0xFF1A2640),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (countdownSeconds > 0) "MANDATORY WAIT TIMER" else "REFLECTED & READY",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (countdownSeconds > 0) Color(0xFFF59E0B) else EmeraldSuccess,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (countdownSeconds > 0) "${countdownSeconds}s" else "EXIT UNLOCKED",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Black,
                            color = if (countdownSeconds > 0) Color(0xFFF59E0B) else EmeraldSuccess
                        )
                    }
                }

                Text(
                    text = "Minimalist Strict Lock is active ($remainingFormatted remaining).",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )

                Surface(
                    color = IndigoPrimary.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, IndigoPrimary.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Shield, contentDescription = null, tint = Color(0xFF06B6D4), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (strictLevel == 1) "Level 1: Flexible Exit with Mindful Delay"
                            else "Daily Exit Quota: $exitsRemaining / 1 Exit Remaining Today",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Text(
                    text = if (strictLevel == 1) "A 15-second reflection timer prevents reflexive app hopping while still allowing flexible exits."
                    else "A 1-minute reflection timer enforces mindful decisions before leaving Minimalist space. Exiting now will consume your 1 daily exit.",
                    color = Color(0xFFCBD5E1),
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirmExit,
                enabled = countdownSeconds <= 0,
                colors = ButtonDefaults.buttonColors(
                    containerColor = CrimsonStrict,
                    disabledContainerColor = CrimsonStrict.copy(alpha = 0.35f)
                )
            ) {
                Text(
                    text = if (countdownSeconds > 0) "Wait ${countdownSeconds}s..." else if (strictLevel == 1) "Confirm Exit" else "Confirm Exit (Use 1 Daily Exit)",
                    fontWeight = FontWeight.Bold,
                    color = if (countdownSeconds > 0) Color.LightGray else Color.White
                )
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Stay in Minimalist", color = Color(0xFFCBD5E1))
            }
        },
        containerColor = Color(0xFF111A2E)
    )
}

@Composable
fun MinimalStrictLockedDialog(
    remainingFormatted: String,
    strictLevel: Int = 2,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = CrimsonStrict,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text("Minimalist Strict Lock Active 🔒", fontWeight = FontWeight.Bold, color = Color.White)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Minimalist Launcher is strictly locked for another $remainingFormatted.",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
                Text(
                    text = if (strictLevel == 3) {
                        "Level 3 Ultra Strict is active. No early exits are permitted under any circumstances until the lock countdown reaches zero."
                    } else {
                        "You have ALREADY USED your 1 daily emergency exit for today. Minimalist Launcher is locked until the timer ends."
                    },
                    color = Color(0xFFCBD5E1),
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = CrimsonStrict)
            ) {
                Text("Got It (Stay Focused)", fontWeight = FontWeight.Bold)
            }
        },
        containerColor = Color(0xFF111A2E)
    )
}

@Composable
fun DevExitConfirmDialog(
    remainingFormatted: String,
    onDismiss: () -> Unit,
    onConfirmExit: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.DeveloperMode,
                contentDescription = null,
                tint = IndigoPrimary,
                modifier = Modifier.size(28.dp)
            )
        },
        title = {
            Text("Exit Minimal Launcher", fontWeight = FontWeight.Bold, color = Color.White)
        },
        text = {
            Text(
                text = "Minimalist Strict Lock is active ($remainingFormatted remaining).\n\nDo you want to exit the launcher now?",
                color = Color(0xFFCBD5E1),
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirmExit,
                colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary)
            ) {
                Text("Exit Launcher", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFFCBD5E1))
            }
        },
        containerColor = Color(0xFF111A2E)
    )
}
