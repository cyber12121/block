package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.BlockedTarget
import com.example.data.model.TargetType
import com.example.service.ActiveSessionState
import com.example.service.FocusSessionManager
import com.example.ui.theme.CrimsonStrict
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IndigoPrimary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class EssentialAppShortcut(
    val label: String,
    val packageName: String
)

@Composable
fun MinimalLauncherScreen(
    sessionState: ActiveSessionState,
    allTargets: List<BlockedTarget>,
    onExitLauncher: () -> Unit,
    onStartFocusSession: () -> Unit
) {
    val context = LocalContext.current
    val sessionManager = remember { FocusSessionManager.getInstance(context) }

    // Custom 5 essential apps state
    var customEssentialPkgs by remember { mutableStateOf(sessionManager.getCustomEssentialApps()) }
    var showConfigureEssentialAppsDialog by remember { mutableStateOf(false) }

    // Exit lock state during active session
    var showExitLockedDialog by remember { mutableStateOf(false) }
    var showEmergencyExitDialog by remember { mutableStateOf(false) }
    var remainingExits by remember { mutableStateOf(sessionManager.getRemainingEmergencyExits()) }

    val handleExitRequest = {
        if (sessionState.isActive) {
            showExitLockedDialog = true
        } else {
            onExitLauncher()
        }
    }

    // Live clock state
    var currentTime by remember { mutableStateOf("") }
    var currentDate by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val dateFormat = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
        while (true) {
            val now = Date()
            currentTime = timeFormat.format(now)
            currentDate = dateFormat.format(now)
            delay(1000)
        }
    }

    var installedAppsList by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }

    LaunchedEffect(context) {
        withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val intent = Intent(Intent.ACTION_MAIN, null).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
            val list = pm.queryIntentActivities(intent, 0).map {
                Pair(it.loadLabel(pm).toString(), it.activityInfo.packageName)
            }.sortedBy { it.first.lowercase() }
            installedAppsList = list
        }
    }

    // Derived 5 essential shortcuts mapped to installed app labels
    val essentialAppsShortcuts = remember(customEssentialPkgs, installedAppsList) {
        val mapped = customEssentialPkgs.map { pkg ->
            val label = installedAppsList.firstOrNull { it.second == pkg }?.first
                ?: pkg.substringAfterLast('.').replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            EssentialAppShortcut(label = label, packageName = pkg)
        }.toMutableList()

        // Ensure FocusGuard app itself is always available in essential list if not already present
        val fgPkg = context.packageName
        if (mapped.none { it.packageName == fgPkg }) {
            mapped.add(0, EssentialAppShortcut("FocusGuard", fgPkg))
        }
        mapped.take(6)
    }

    var isAppDrawerOpen by remember { mutableStateOf(false) }
    var drawerSearchQuery by remember { mutableStateOf("") }
    var showAppBlockedDialog by remember { mutableStateOf<String?>(null) }

    val blockedPackages = remember(allTargets, sessionState.isActive) {
        if (!sessionState.isActive) emptySet()
        else allTargets.filter { it.targetType == TargetType.APP && it.isEnabled }.map { it.identifier.lowercase() }.toSet()
    }

    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current

    // Handle System Back gesture safely for dialogs and drawer
    BackHandler(enabled = showConfigureEssentialAppsDialog) {
        showConfigureEssentialAppsDialog = false
    }

    BackHandler(enabled = showEmergencyExitDialog) {
        showEmergencyExitDialog = false
    }

    BackHandler(enabled = showExitLockedDialog) {
        showExitLockedDialog = false
    }

    BackHandler(enabled = isAppDrawerOpen) {
        keyboardController?.hide()
        focusManager.clearFocus()
        isAppDrawerOpen = false
    }

    BackHandler(enabled = !isAppDrawerOpen && !showConfigureEssentialAppsDialog && !showEmergencyExitDialog && !showExitLockedDialog) {
        handleExitRequest()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070A10))
            .padding(24.dp)
    ) {
        if (isAppDrawerOpen) {
            // Minimalist App Drawer View
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = Color(0xFF131B2E),
                        shape = RoundedCornerShape(100.dp),
                        modifier = Modifier.clickable { isAppDrawerOpen = false }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Back", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }

                    Text(
                        text = if (sessionState.isActive) "RESTRICTED FOCUS DRAWER" else "MINIMAL APP DRAWER",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        color = if (sessionState.isActive) CrimsonStrict else Color.Gray
                    )

                    Surface(
                        color = CrimsonStrict.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(100.dp),
                        modifier = Modifier.clickable {
                            isAppDrawerOpen = false
                            handleExitRequest()
                        }
                    ) {
                        Text(
                            text = if (sessionState.isActive) "🔒 Locked" else "Exit",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CrimsonStrict,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (sessionState.isActive) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CrimsonStrict.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = CrimsonStrict, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Focus Session Active: Only your 5 essential apps + FocusGuard are accessible.",
                                fontSize = 12.sp,
                                color = Color.White,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = drawerSearchQuery,
                    onValueChange = { drawerSearchQuery = it },
                    placeholder = { Text("Search apps...", color = Color.Gray, fontSize = 14.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
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
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                // During active session, restrict visible apps to custom 5 essential apps + FocusGuard
                val allowedPkgsSet = remember(customEssentialPkgs, sessionState.isActive) {
                    if (sessionState.isActive) {
                        (customEssentialPkgs + context.packageName).map { it.lowercase() }.toSet()
                    } else emptySet()
                }

                val filteredInstalled = remember(installedAppsList, drawerSearchQuery, sessionState.isActive, allowedPkgsSet) {
                    val base = if (sessionState.isActive) {
                        installedAppsList.filter { allowedPkgsSet.contains(it.second.lowercase()) }
                    } else {
                        installedAppsList
                    }
                    if (drawerSearchQuery.isBlank()) base
                    else base.filter { it.first.contains(drawerSearchQuery, ignoreCase = true) }
                }

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredInstalled, key = { it.second }) { app ->
                        val isBlocked = blockedPackages.contains(app.second.lowercase())
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isBlocked) {
                                        showAppBlockedDialog = app.first
                                    } else if (app.second == context.packageName || app.first.equals("FocusGuard", ignoreCase = true)) {
                                        isAppDrawerOpen = false
                                        handleExitRequest()
                                    } else {
                                        launchApp(context, app.second)
                                    }
                                }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = app.first,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isBlocked) Color.Gray else Color.White
                            )

                            if (isBlocked) {
                                Surface(
                                    color = CrimsonStrict.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "BLOCKED",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CrimsonStrict,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Main Minimal Launcher View
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Header: Exit Button, Dev Options & Shield Status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = Color(0xFF131B2E),
                        shape = RoundedCornerShape(100.dp),
                        modifier = Modifier.clickable(onClick = handleExitRequest)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (sessionState.isActive) Icons.Default.Lock else Icons.Default.ArrowBack,
                                contentDescription = "Exit Minimal Launcher",
                                tint = if (sessionState.isActive) CrimsonStrict else Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (sessionState.isActive) "Launcher Locked" else "Exit Launcher",
                                fontSize = 12.sp,
                                color = if (sessionState.isActive) CrimsonStrict else Color.Gray,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Surface(
                        color = if (remainingExits > 0) Color(0xFF1E293B) else CrimsonStrict.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(100.dp),
                        modifier = Modifier
                            .clickable { showEmergencyExitDialog = true }
                            .testTag("minimal_emergency_exit_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (remainingExits > 0) Icons.Default.LockOpen else Icons.Default.Lock,
                                contentDescription = "Emergency Exit",
                                tint = if (remainingExits > 0) CrimsonStrict else Color.Gray,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Emergency Exit ($remainingExits/5)",
                                fontSize = 11.sp,
                                color = if (remainingExits > 0) Color.White else Color.Gray,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Surface(
                        color = if (sessionState.isActive) CrimsonStrict.copy(alpha = 0.15f) else Color(0xFF1E293B),
                        shape = RoundedCornerShape(100.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(if (sessionState.isActive) CrimsonStrict else EmeraldSuccess, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (sessionState.isActive) "🔒 FOCUS LOCK" else "STANDBY",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (sessionState.isActive) CrimsonStrict else EmeraldSuccess
                            )
                        }
                    }
                }

                // Middle: Time, Date & Daily Reflection Quote
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp)
                ) {
                    Text(
                        text = currentTime.ifEmpty { "12:00" },
                        fontSize = 72.sp,
                        fontWeight = FontWeight.Light,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White,
                        letterSpacing = (-2).sp
                    )

                    Text(
                        text = currentDate.ifEmpty { "Focus Mode" },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = IndigoPrimary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "“Simplicity is about subtracting the obvious and adding the meaningful.”",
                        fontSize = 13.sp,
                        color = Color(0xFF94A3B8),
                        lineHeight = 18.sp
                    )
                }

                // Essential App Text Shortcuts List (Up to 5 custom apps + FocusGuard)
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.padding(vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "MY 5 ESSENTIAL APPS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp,
                            color = Color(0xFF64748B)
                        )

                        if (!sessionState.isActive) {
                            Surface(
                                color = Color(0xFF1E293B),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.clickable { showConfigureEssentialAppsDialog = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = IndigoPrimary, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Edit 5 Apps", fontSize = 11.sp, color = IndigoPrimary, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    essentialAppsShortcuts.forEach { app ->
                        val isBlocked = blockedPackages.contains(app.packageName.lowercase())

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isBlocked) {
                                        showAppBlockedDialog = app.label
                                    } else if (app.packageName == context.packageName || app.label.equals("FocusGuard", ignoreCase = true)) {
                                        handleExitRequest()
                                    } else {
                                        launchApp(context, app.packageName)
                                    }
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = app.label,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isBlocked) Color.Gray else Color.White,
                                letterSpacing = 0.5.sp
                            )

                            if (isBlocked) {
                                Icon(Icons.Default.Lock, contentDescription = "Blocked", tint = CrimsonStrict, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }

                // Bottom Bar: All Apps Drawer Trigger & Start Session
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = Color(0xFF1E293B),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .clickable { isAppDrawerOpen = true }
                            .testTag("open_minimal_drawer")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (sessionState.isActive) Icons.Default.Lock else Icons.Default.Search,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (sessionState.isActive) "Allowed Apps" else "All Apps",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    Button(
                        onClick = onStartFocusSession,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (sessionState.isActive) {
                                if (sessionState.isStrictMode) CrimsonStrict else IndigoPrimary
                            } else IndigoPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(44.dp)
                    ) {
                        val icon = if (sessionState.isActive) {
                            if (sessionState.isStrictMode) Icons.Default.Lock else Icons.Default.Shield
                        } else Icons.Default.PlayArrow

                        val mins = sessionState.remainingSeconds / 60
                        val secs = sessionState.remainingSeconds % 60
                        val timerString = String.format(Locale.getDefault(), "%02d:%02d", mins, secs)

                        val buttonText = if (sessionState.isActive) {
                            "Session ($timerString)"
                        } else {
                            "Start Focus"
                        }

                        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = buttonText, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }

        // Locked Exit Warning Dialog during Active Focus Session
        if (showExitLockedDialog) {
            AlertDialog(
                onDismissRequest = { showExitLockedDialog = false },
                icon = { Icon(Icons.Default.Lock, contentDescription = null, tint = CrimsonStrict, modifier = Modifier.size(28.dp)) },
                title = { Text("Focus Lock Active", fontWeight = FontWeight.Bold, color = Color.White) },
                text = {
                    Text(
                        "Minimal Launcher is locked to keep you focused. You cannot exit until your focus session timer ends (${sessionState.remainingSeconds / 60}m remaining).",
                        color = Color(0xFFCBD5E1),
                        fontSize = 14.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { showExitLockedDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonStrict)
                    ) {
                        Text("Stay Focused", fontWeight = FontWeight.Bold)
                    }
                },
                containerColor = Color(0xFF0F172A)
            )
        }

        // Blocked App Dialog
        showAppBlockedDialog?.let { appName ->
            AlertDialog(
                onDismissRequest = { showAppBlockedDialog = null },
                icon = { Icon(Icons.Default.Lock, contentDescription = null, tint = CrimsonStrict) },
                title = { Text("$appName is Blocked", fontWeight = FontWeight.Bold, color = Color.White) },
                text = { Text("$appName is currently restricted by your active Focus Session rule to help you stay in deep flow.", color = Color(0xFFCBD5E1)) },
                confirmButton = {
                    Button(
                        onClick = { showAppBlockedDialog = null },
                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonStrict)
                    ) {
                        Text("Stay Focused")
                    }
                },
                containerColor = Color(0xFF0F172A)
            )
        }

        // Edit 5 Essential Apps Dialog
        if (showConfigureEssentialAppsDialog) {
            EditEssentialAppsDialog(
                installedApps = installedAppsList,
                initialSelectedPackages = customEssentialPkgs,
                onDismiss = { showConfigureEssentialAppsDialog = false },
                onSave = { newPkgs ->
                    sessionManager.saveCustomEssentialApps(newPkgs)
                    customEssentialPkgs = newPkgs
                    showConfigureEssentialAppsDialog = false
                }
            )
        }

        // Emergency Exit Dialog
        if (showEmergencyExitDialog) {
            EmergencyExitDialog(
                remainingExits = remainingExits,
                onDismiss = { showEmergencyExitDialog = false },
                onConfirmExit = {
                    if (sessionManager.useEmergencyExit()) {
                        remainingExits = sessionManager.getRemainingEmergencyExits()
                        showEmergencyExitDialog = false
                        onExitLauncher()
                    }
                }
            )
        }
    }
}

@Composable
fun EmergencyExitDialog(
    remainingExits: Int,
    onDismiss: () -> Unit,
    onConfirmExit: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF0F172A),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clickable(enabled = false) {}
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
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

                Spacer(modifier = Modifier.height(12.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "EMERGENCY EXITS REMAINING",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = IndigoPrimary,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$remainingExits / 5 Exits Left",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (remainingExits > 0) EmeraldSuccess else CrimsonStrict
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (remainingExits > 0) {
                                "You are granted a strict limit of 5 Emergency Exits total to bypass focus mode or exit the launcher. Using an exit now will reduce your balance to ${remainingExits - 1}."
                            } else {
                                "You have exhausted all 5 Emergency Exits! Emergency unlock is permanently disabled for remaining focus sessions."
                            },
                            fontSize = 12.sp,
                            color = Color(0xFFCBD5E1),
                            lineHeight = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                if (remainingExits > 0) {
                    Button(
                        onClick = onConfirmExit,
                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonStrict),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                    ) {
                        Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Use Emergency Exit ($remainingExits Left)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                } else {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                    ) {
                        Text("No Exits Remaining (Stay Focused)", fontWeight = FontWeight.Bold, color = Color.Gray, fontSize = 13.sp)
                    }
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
    var selectedPkgs by remember { mutableStateOf(initialSelectedPackages.take(5).toSet()) }
    var searchQuery by remember { mutableStateOf("") }
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current

    val filteredApps = remember(installedApps, searchQuery) {
        if (searchQuery.isBlank()) installedApps
        else installedApps.filter { it.first.contains(searchQuery, ignoreCase = true) }
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
            color = Color(0xFF0F172A),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .clickable(enabled = false) {}
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Select 5 Essential Apps", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
                        Text("${selectedPkgs.size} / 5 selected", fontSize = 12.sp, color = IndigoPrimary, fontWeight = FontWeight.Bold)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Filter installed apps...", color = Color.Gray, fontSize = 13.sp) },
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
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                ) {
                    items(filteredApps, key = { it.second }) { app ->
                        val isChecked = selectedPkgs.contains(app.second)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isChecked) IndigoPrimary.copy(alpha = 0.15f) else Color(0xFF1E293B))
                                .clickable {
                                    if (isChecked) {
                                        selectedPkgs = selectedPkgs - app.second
                                    } else if (selectedPkgs.size < 5) {
                                        selectedPkgs = selectedPkgs + app.second
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = app.first, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color.White)
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    if (checked && selectedPkgs.size < 5) {
                                        selectedPkgs = selectedPkgs + app.second
                                    } else if (!checked) {
                                        selectedPkgs = selectedPkgs - app.second
                                    }
                                },
                                colors = CheckboxDefaults.colors(checkedColor = IndigoPrimary)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { onSave(selectedPkgs.toList()) },
                    enabled = selectedPkgs.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save 5 Essential Apps", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

private fun launchApp(context: Context, packageName: String) {
    try {
        val pm = context.packageManager
        val intent = pm.getLaunchIntentForPackage(packageName)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    } catch (e: Exception) {
        // Fallback gracefully
    }
}
