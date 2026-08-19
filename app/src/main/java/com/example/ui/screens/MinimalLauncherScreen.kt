package com.example.ui.screens

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tune
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BlockedTarget
import com.example.data.model.TargetType
import com.example.service.ActiveSessionState
import com.example.service.FocusSessionManager
import com.example.ui.theme.CrimsonStrict
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IndigoPrimary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class EssentialAppShortcut(
    val label: String,
    val packageName: String
)

private val FOCUS_QUOTES = listOf(
    "“Simplicity is the ultimate sophistication.” — Leonardo da Vinci",
    "“You have power over your mind - not outside events. Realize this, and you will find strength.” — Marcus Aurelius",
    "“Focus is a muscle. The more you practice, the stronger it gets.”",
    "“Subtract the obvious and add the meaningful.” — John Maeda",
    "“It is not a daily increase, but a daily decrease. Hack away at the inessentials.” — Bruce Lee",
    "“Deep work is the ability to focus without distraction on a cognitively demanding task.” — Cal Newport",
    "“The successful warrior is the average man, with laser-like focus.” — Bruce Lee",
    "“Where your attention goes, your time goes. Where your time goes, your life goes.”"
)

enum class MinimalClockStyle(val displayName: String) {
    MODERN_CLEAN("Modern Sans"),
    MINIMAL_MONO("Digital Mono"),
    EDITORIAL_SERIF("Editorial Serif")
}

@Composable
fun MinimalLauncherScreen(
    sessionState: ActiveSessionState,
    allTargets: List<BlockedTarget>,
    onExitLauncher: () -> Unit,
    onStartFocusSession: () -> Unit
) {
    val context = LocalContext.current
    val sessionManager = remember { FocusSessionManager.getInstance(context) }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()

    // Preferences & Local State
    var customEssentialPkgs by remember { mutableStateOf(sessionManager.getCustomEssentialApps()) }
    var showConfigureEssentialAppsDialog by remember { mutableStateOf(false) }
    var isAppDrawerOpen by remember { mutableStateOf(false) }
    var drawerSearchQuery by remember { mutableStateOf("") }
    var showAppBlockedDialog by remember { mutableStateOf<String?>(null) }
    var showScratchpadDialog by remember { mutableStateOf(false) }
    var showPreferencesDialog by remember { mutableStateOf(false) }

    // Launcher Customization Preferences
    var clockStyle by remember { mutableStateOf(MinimalClockStyle.MODERN_CLEAN) }
    var userIntention by remember {
        val prefs = context.getSharedPreferences("minimal_launcher_prefs", Context.MODE_PRIVATE)
        mutableStateOf(prefs.getString("user_intention", "") ?: "")
    }
    var isEditingIntention by remember { mutableStateOf(false) }
    var intentionDraft by remember { mutableStateOf(userIntention) }
    var currentQuoteIndex by remember { mutableIntStateOf(0) }
    var scratchpadNotes by remember {
        val prefs = context.getSharedPreferences("minimal_launcher_prefs", Context.MODE_PRIVATE)
        mutableStateOf(prefs.getString("scratchpad_notes", "") ?: "")
    }

    // Exit lock state during active session
    var showExitLockedDialog by remember { mutableStateOf(false) }
    var showEmergencyExitDialog by remember { mutableStateOf(false) }
    // Refresh the counter whenever the active-session state changes (session start, end,
    // Pomodoro transition) so the display is never stale after a re-composition.
    var remainingExits by remember { mutableStateOf(sessionManager.getRemainingEmergencyExits()) }
    LaunchedEffect(sessionState.isActive) {
        remainingExits = sessionManager.getRemainingEmergencyExits()
    }

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

    suspend fun queryAllAndroidApps(ctx: Context): List<Pair<String, String>> = withContext(Dispatchers.IO) {
        val pm = ctx.packageManager
        val appsMap = mutableMapOf<String, String>()

        try {
            val launcherIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolveInfos = pm.queryIntentActivities(launcherIntent, PackageManager.MATCH_ALL)
            for (info in resolveInfos) {
                val pkg = info.activityInfo.packageName
                if (!pkg.isNullOrBlank()) {
                    val name = info.loadLabel(pm).toString()
                    if (name.isNotBlank()) {
                        appsMap[pkg] = name
                    }
                }
            }
        } catch (_: Exception) {}

        try {
            val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            for (appInfo in installedApps) {
                if (!appsMap.containsKey(appInfo.packageName)) {
                    if (pm.getLaunchIntentForPackage(appInfo.packageName) != null) {
                        val name = pm.getApplicationLabel(appInfo).toString()
                        if (name.isNotBlank()) {
                            appsMap[appInfo.packageName] = name
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        appsMap.map { (pkg, name) -> Pair(name, pkg) }
            .distinctBy { it.second }
            .sortedBy { it.first.lowercase(Locale.getDefault()) }
    }

    LaunchedEffect(Unit) {
        installedAppsList = queryAllAndroidApps(context)
    }

    LaunchedEffect(isAppDrawerOpen, showConfigureEssentialAppsDialog) {
        if (isAppDrawerOpen || showConfigureEssentialAppsDialog) {
            installedAppsList = queryAllAndroidApps(context)
        }
    }

    // Essential shortcuts mapped from user selection
    val essentialAppsShortcuts = remember(customEssentialPkgs, installedAppsList) {
        customEssentialPkgs.map { pkg ->
            val label = installedAppsList.firstOrNull { it.second == pkg }?.first
                ?: pkg.substringAfterLast('.').replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            EssentialAppShortcut(label = label, packageName = pkg)
        }.take(6)
    }

    val blockedPackages = remember(allTargets, sessionState.isActive) {
        if (!sessionState.isActive) emptySet()
        else allTargets.filter { it.targetType == TargetType.APP && it.isEnabled }.map { it.identifier.lowercase() }.toSet()
    }

    // Back handling
    BackHandler(enabled = showConfigureEssentialAppsDialog) {
        showConfigureEssentialAppsDialog = false
    }

    BackHandler(enabled = showEmergencyExitDialog) {
        showEmergencyExitDialog = false
    }

    BackHandler(enabled = showExitLockedDialog) {
        showExitLockedDialog = false
    }

    BackHandler(enabled = showScratchpadDialog) {
        showScratchpadDialog = false
    }

    BackHandler(enabled = showPreferencesDialog) {
        showPreferencesDialog = false
    }

    BackHandler(enabled = isAppDrawerOpen) {
        keyboardController?.hide()
        focusManager.clearFocus()
        isAppDrawerOpen = false
    }

    BackHandler(enabled = !isAppDrawerOpen && !showConfigureEssentialAppsDialog && !showEmergencyExitDialog && !showExitLockedDialog && !showScratchpadDialog && !showPreferencesDialog) {
        handleExitRequest()
    }

    // Animated Ambient Glow for Active Focus Mode
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseGlowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF070A11),
                        Color(0xFF0B101C),
                        Color(0xFF070A11)
                    )
                )
            )
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        if (isAppDrawerOpen) {
            // ==========================================
            // MINIMALIST APP DRAWER / SEARCH SCREEN
            // ==========================================
            Column(modifier = Modifier.fillMaxSize()) {
                // Drawer Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = Color(0xFF131C31),
                        shape = RoundedCornerShape(100.dp),
                        border = BorderStroke(1.dp, Color(0xFF1E293B)),
                        modifier = Modifier.clickable {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                            isAppDrawerOpen = false
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Back", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }

                    Text(
                        text = if (sessionState.isActive) "FOCUS DRAWER" else "ALL APPS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        color = if (sessionState.isActive) CrimsonStrict else Color(0xFF94A3B8)
                    )

                    Surface(
                        color = if (sessionState.isActive) CrimsonStrict.copy(alpha = 0.15f) else Color(0xFF131C31),
                        shape = RoundedCornerShape(100.dp),
                        border = BorderStroke(1.dp, if (sessionState.isActive) CrimsonStrict.copy(alpha = 0.4f) else Color(0xFF1E293B)),
                        modifier = Modifier.clickable {
                            isAppDrawerOpen = false
                            handleExitRequest()
                        }
                    ) {
                        Text(
                            text = if (sessionState.isActive) "🔒 Locked" else "Exit",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (sessionState.isActive) CrimsonStrict else Color(0xFF94A3B8),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                if (sessionState.isActive) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CrimsonStrict.copy(alpha = 0.12f)),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, CrimsonStrict.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = null,
                                tint = CrimsonStrict,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Strict Focus Active: Blocked apps cannot be opened until session ends.",
                                fontSize = 12.sp,
                                color = Color.White,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                // Sleek Search Field
                OutlinedTextField(
                    value = drawerSearchQuery,
                    onValueChange = { drawerSearchQuery = it },
                    placeholder = { Text("Search any app...", color = Color(0xFF64748B), fontSize = 14.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = IndigoPrimary) },
                    trailingIcon = {
                        if (drawerSearchQuery.isNotEmpty()) {
                            IconButton(onClick = { drawerSearchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color.Gray, modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        keyboardController?.hide()
                        focusManager.clearFocus()
                    }),
                    shape = RoundedCornerShape(14.dp),
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

                Spacer(modifier = Modifier.height(16.dp))

                // Allowed apps during active session vs all apps
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
                    else base.filter {
                        it.first.contains(drawerSearchQuery, ignoreCase = true) ||
                        it.second.contains(drawerSearchQuery, ignoreCase = true)
                    }
                }

                val listState = rememberLazyListState()

                Row(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        state = listState,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        items(filteredInstalled, key = { it.second }) { app ->
                            val isBlocked = blockedPackages.contains(app.second.lowercase())
                            val isPinned = customEssentialPkgs.contains(app.second)

                            Surface(
                                color = if (isBlocked) Color(0xFF120E18) else Color(0xFF0F172A),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, if (isBlocked) CrimsonStrict.copy(alpha = 0.25f) else Color(0xFF1E293B)),
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
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        // Minimalist Letter Icon Avatar
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .background(
                                                    if (isBlocked) CrimsonStrict.copy(alpha = 0.15f)
                                                    else IndigoPrimary.copy(alpha = 0.15f),
                                                    RoundedCornerShape(10.dp)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = app.first.take(1).uppercase(Locale.getDefault()),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = if (isBlocked) CrimsonStrict else IndigoPrimary
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(14.dp))

                                        Column {
                                            Text(
                                                text = app.first,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (isBlocked) Color(0xFF94A3B8) else Color.White,
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

                                    Row(verticalAlignment = Alignment.CenterVertically) {
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
                                        } else if (!sessionState.isActive) {
                                            IconButton(
                                                onClick = {
                                                    val updated = if (isPinned) {
                                                        customEssentialPkgs.filter { it != app.second }
                                                    } else {
                                                        if (customEssentialPkgs.size < 6) customEssentialPkgs + app.second
                                                        else customEssentialPkgs
                                                    }
                                                    sessionManager.saveCustomEssentialApps(updated)
                                                    customEssentialPkgs = updated
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = if (isPinned) Icons.Default.PushPin else Icons.Default.Add,
                                                    contentDescription = if (isPinned) "Unpin" else "Pin to Home",
                                                    tint = if (isPinned) IndigoPrimary else Color(0xFF64748B),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // A-Z Quick Jump Alphabet Rail
                    if (drawerSearchQuery.isBlank() && filteredInstalled.size > 10) {
                        val alphabet = remember { ('A'..'Z').toList() }
                        Column(
                            modifier = Modifier
                                .padding(start = 6.dp)
                                .fillMaxHeight(),
                            verticalArrangement = Arrangement.SpaceAround,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            alphabet.forEach { letter ->
                                Text(
                                    text = letter.toString(),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF64748B),
                                    modifier = Modifier
                                        .clickable {
                                            val index = filteredInstalled.indexOfFirst {
                                                it.first.startsWith(letter, ignoreCase = true)
                                            }
                                            if (index >= 0) {
                                                scope.launch {
                                                    listState.animateScrollToItem(index)
                                                }
                                            }
                                        }
                                        .padding(vertical = 1.dp, horizontal = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // ==========================================
            // MAIN MINIMALIST LAUNCHER SCREEN
            // ==========================================
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // TOP BAR: Exit button, Emergency Exit counter, and Status indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Exit / Lock indicator
                    Surface(
                        color = Color(0xFF111A2E),
                        shape = RoundedCornerShape(100.dp),
                        border = BorderStroke(1.dp, Color(0xFF1E293B)),
                        modifier = Modifier.clickable(onClick = handleExitRequest)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (sessionState.isActive) Icons.Default.Lock else Icons.Default.ArrowBack,
                                contentDescription = "Exit Minimal Launcher",
                                tint = if (sessionState.isActive) CrimsonStrict else Color(0xFF94A3B8),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (sessionState.isActive) "Locked" else "Exit",
                                fontSize = 12.sp,
                                color = if (sessionState.isActive) CrimsonStrict else Color(0xFFCBD5E1),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Preferences & Theme Trigger
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = Color(0xFF111A2E),
                            shape = RoundedCornerShape(100.dp),
                            border = BorderStroke(1.dp, Color(0xFF1E293B)),
                            modifier = Modifier.clickable { showPreferencesDialog = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = "Preferences",
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        // Emergency Exit Trigger
                        Surface(
                            color = Color(0xFF111A2E),
                            shape = RoundedCornerShape(100.dp),
                            border = BorderStroke(1.dp, Color(0xFF1E293B)),
                            modifier = Modifier
                                .clickable { showEmergencyExitDialog = true }
                                .testTag("minimal_emergency_exit_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LockOpen,
                                    contentDescription = "Emergency Exit",
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

                        // Status Pill (Standby vs Active Session)
                        Surface(
                            color = if (sessionState.isActive) CrimsonStrict.copy(alpha = 0.15f) else Color(0xFF111A2E),
                            shape = RoundedCornerShape(100.dp),
                            border = BorderStroke(1.dp, if (sessionState.isActive) CrimsonStrict.copy(alpha = 0.5f) else Color(0xFF1E293B))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .background(
                                            if (sessionState.isActive) CrimsonStrict else EmeraldSuccess,
                                            CircleShape
                                        )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (sessionState.isActive) "ACTIVE" else "STANDBY",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.sp,
                                    color = if (sessionState.isActive) CrimsonStrict else EmeraldSuccess
                                )
                            }
                        }
                    }
                }

                // ==========================================
                // MIDDLE: HERO CLOCK, DATE, INTENTION & QUOTE
                // ==========================================
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                ) {
                    // Large Digital Clock
                    val clockFontFamily = when (clockStyle) {
                        MinimalClockStyle.MODERN_CLEAN -> FontFamily.Default
                        MinimalClockStyle.MINIMAL_MONO -> FontFamily.Monospace
                        MinimalClockStyle.EDITORIAL_SERIF -> FontFamily.Serif
                    }

                    Text(
                        text = currentTime.ifEmpty { "12:00" },
                        fontSize = 76.sp,
                        fontWeight = FontWeight.ExtraLight,
                        fontFamily = clockFontFamily,
                        color = Color.White,
                        letterSpacing = (-2).sp,
                        lineHeight = 76.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = currentDate.ifEmpty { "Focus Day" },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = IndigoPrimary,
                        letterSpacing = 0.5.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Active Focus Session Live Countdown Card (if active)
                    if (sessionState.isActive) {
                        val mins = sessionState.remainingSeconds / 60
                        val secs = sessionState.remainingSeconds % 60
                        val totalDurationSec = (sessionState.durationMinutes * 60).coerceAtLeast(1)
                        val progress = (sessionState.remainingSeconds.toFloat() / totalDurationSec.toFloat()).coerceIn(0f, 1f)

                        Card(
                            colors = CardDefaults.cardColors(containerColor = CrimsonStrict.copy(alpha = pulseGlowAlpha)),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, CrimsonStrict.copy(alpha = 0.6f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onStartFocusSession() }
                                .padding(vertical = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator(
                                            progress = { progress },
                                            modifier = Modifier.size(34.dp),
                                            color = CrimsonStrict,
                                            trackColor = CrimsonStrict.copy(alpha = 0.2f),
                                            strokeWidth = 3.dp
                                        )
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = null,
                                            tint = CrimsonStrict,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column {
                                        Text(
                                            text = String.format(Locale.getDefault(), "%02d:%02d REMAINING", mins, secs),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.sp,
                                            color = Color.White
                                        )
                                        Text(
                                            text = if (sessionState.isStrictMode) "Strict Lock Active • Tap to view" else "Focus Flow Active • Tap to view",
                                            fontSize = 11.sp,
                                            color = Color(0xFFE2E8F0)
                                        )
                                    }
                                }

                                Surface(
                                    color = CrimsonStrict.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "VIEW",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Intention Prompt Widget (Hallmark of Minimalist Launchers)
                    Surface(
                        color = Color(0xFF101728),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFF1E293B)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable {
                                intentionDraft = userIntention
                                isEditingIntention = true
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = IndigoPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            if (userIntention.isBlank()) {
                                Text(
                                    text = "Set current intention / key task...",
                                    fontSize = 12.sp,
                                    color = Color(0xFF64748B),
                                    fontStyle = FontStyle.Italic
                                )
                            } else {
                                Text(
                                    text = "Goal: $userIntention",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFE2E8F0),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Stoic Quote of the Day
                    Text(
                        text = FOCUS_QUOTES[currentQuoteIndex % FOCUS_QUOTES.size],
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8),
                        lineHeight = 16.sp,
                        modifier = Modifier
                            .clickable { currentQuoteIndex++ }
                            .padding(vertical = 4.dp)
                    )
                }

                // ==========================================
                // ESSENTIAL APPS LIST (THE CORE LAUNCHER)
                // ==========================================
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ESSENTIAL APPS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 2.sp,
                            color = Color(0xFF64748B)
                        )

                        if (!sessionState.isActive) {
                            Surface(
                                color = Color(0xFF131C31),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0xFF1E293B)),
                                modifier = Modifier
                                    .clickable { showConfigureEssentialAppsDialog = true }
                                    .testTag("edit_essential_apps")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "Edit",
                                        tint = IndigoPrimary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        "Manage",
                                        fontSize = 11.sp,
                                        color = IndigoPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    if (essentialAppsShortcuts.isEmpty()) {
                        // Empty State - Guided Action
                        Surface(
                            color = Color(0xFF0F172A),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, Color(0xFF1E293B)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(20.dp)
                                    .fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = null,
                                    tint = IndigoPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No essential apps chosen",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Pin up to 6 apps you want accessible in minimal mode.",
                                    fontSize = 12.sp,
                                    color = Color(0xFF94A3B8),
                                    textAlign = TextAlign.Center
                                )
                                if (!sessionState.isActive) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Button(
                                        onClick = { showConfigureEssentialAppsDialog = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.testTag("choose_essential_apps")
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Select Essential Apps", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    } else {
                        // High Contrast Minimal App List
                        essentialAppsShortcuts.forEach { app ->
                            val isBlocked = blockedPackages.contains(app.packageName.lowercase())

                            Surface(
                                color = Color.Transparent,
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
                                    .padding(vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        // Sleek minimal bullet
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .background(
                                                    if (isBlocked) CrimsonStrict else IndigoPrimary,
                                                    CircleShape
                                                )
                                        )

                                        Spacer(modifier = Modifier.width(14.dp))

                                        Text(
                                            text = app.label,
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = if (isBlocked) Color(0xFF64748B) else Color.White,
                                            letterSpacing = 0.3.sp,
                                            textDecoration = if (isBlocked) TextDecoration.LineThrough else TextDecoration.None
                                        )
                                    }

                                    if (isBlocked) {
                                        Surface(
                                            color = CrimsonStrict.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    Icons.Default.Lock,
                                                    contentDescription = "Blocked",
                                                    tint = CrimsonStrict,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    "BLOCKED",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = CrimsonStrict
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Add another app shortcut slot if less than 6
                        if (!sessionState.isActive && essentialAppsShortcuts.size < 6) {
                            Surface(
                                color = Color.Transparent,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showConfigureEssentialAppsDialog = true }
                                    .padding(vertical = 2.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = "Add app",
                                        tint = Color(0xFF475569),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "+ add shortcut",
                                        fontSize = 14.sp,
                                        color = Color(0xFF475569),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }

                // ==========================================
                // BOTTOM BAR: ALL APPS, NOTES & START FOCUS
                // ==========================================
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // All Apps Drawer Trigger
                    Surface(
                        color = Color(0xFF111A2E),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color(0xFF1E293B)),
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
                                text = if (sessionState.isActive) "Focus Drawer" else "All Apps",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    // Scratchpad / Quick Thoughts Trigger
                    Surface(
                        color = Color(0xFF111A2E),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color(0xFF1E293B)),
                        modifier = Modifier.clickable { showScratchpadDialog = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.EditNote,
                                contentDescription = "Quick Notes",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Start / Manage Focus Button
                    Button(
                        onClick = onStartFocusSession,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (sessionState.isActive) {
                                if (sessionState.isStrictMode) CrimsonStrict else IndigoPrimary
                            } else IndigoPrimary
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.height(48.dp)
                    ) {
                        val icon = if (sessionState.isActive) {
                            if (sessionState.isStrictMode) Icons.Default.Lock else Icons.Default.Shield
                        } else Icons.Default.PlayArrow

                        val mins = sessionState.remainingSeconds / 60
                        val secs = sessionState.remainingSeconds % 60
                        val timerString = String.format(Locale.getDefault(), "%02d:%02d", mins, secs)

                        val buttonText = if (sessionState.isActive) {
                            "Timer ($timerString)"
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

        // ==========================================
        // DIALOGS & OVERLAYS
        // ==========================================

        // Edit Intention Dialog
        if (isEditingIntention) {
            AlertDialog(
                onDismissRequest = { isEditingIntention = false },
                title = { Text("What is your focus right now?", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 18.sp) },
                text = {
                    Column {
                        Text("Define your primary intention to keep front and center.", fontSize = 13.sp, color = Color(0xFF94A3B8))
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = intentionDraft,
                            onValueChange = { intentionDraft = it },
                            placeholder = { Text("e.g. Finish writing proposal chapter 2", color = Color.Gray, fontSize = 13.sp) },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = IndigoPrimary,
                                unfocusedBorderColor = Color(0xFF1E293B),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            userIntention = intentionDraft.trim()
                            val prefs = context.getSharedPreferences("minimal_launcher_prefs", Context.MODE_PRIVATE)
                            prefs.edit().putString("user_intention", userIntention).apply()
                            isEditingIntention = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary)
                    ) {
                        Text("Save Goal", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        userIntention = ""
                        val prefs = context.getSharedPreferences("minimal_launcher_prefs", Context.MODE_PRIVATE)
                        prefs.edit().remove("user_intention").apply()
                        isEditingIntention = false
                    }) {
                        Text("Clear", color = Color.Gray)
                    }
                },
                containerColor = Color(0xFF111A2E)
            )
        }

        // Scratchpad / Quick Notes Dialog
        if (showScratchpadDialog) {
            var draftNotes by remember { mutableStateOf(scratchpadNotes) }
            AlertDialog(
                onDismissRequest = { showScratchpadDialog = false },
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.EditNote, contentDescription = null, tint = IndigoPrimary, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Distraction-Free Scratchpad", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                        }
                    }
                },
                text = {
                    Column {
                        Text(
                            "Jot down fleeting thoughts or tasks without getting distracted by other apps.",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = draftNotes,
                            onValueChange = { draftNotes = it },
                            placeholder = { Text("Write quick thoughts, ideas, or to-dos...", color = Color.Gray, fontSize = 13.sp) },
                            minLines = 6,
                            maxLines = 10,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = IndigoPrimary,
                                unfocusedBorderColor = Color(0xFF1E293B),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            scratchpadNotes = draftNotes
                            val prefs = context.getSharedPreferences("minimal_launcher_prefs", Context.MODE_PRIVATE)
                            prefs.edit().putString("scratchpad_notes", scratchpadNotes).apply()
                            showScratchpadDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary)
                    ) {
                        Text("Save Notes", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showScratchpadDialog = false }) {
                        Text("Close", color = Color.Gray)
                    }
                },
                containerColor = Color(0xFF111A2E)
            )
        }

        // Preferences Dialog
        if (showPreferencesDialog) {
            AlertDialog(
                onDismissRequest = { showPreferencesDialog = false },
                title = { Text("Launcher Customization", fontWeight = FontWeight.Bold, color = Color.White) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text("CLOCK TYPOGRAPHY", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = IndigoPrimary, letterSpacing = 1.sp)
                        MinimalClockStyle.values().forEach { style ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (clockStyle == style) IndigoPrimary.copy(alpha = 0.2f) else Color(0xFF1A2640))
                                    .clickable { clockStyle = style }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(style.displayName, color = Color.White, fontSize = 14.sp)
                                if (clockStyle == style) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = IndigoPrimary, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { showPreferencesDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary)
                    ) {
                        Text("Done", fontWeight = FontWeight.Bold)
                    }
                },
                containerColor = Color(0xFF111A2E)
            )
        }

        // Locked Exit Warning Dialog during Active Focus Session
        if (showExitLockedDialog) {
            AlertDialog(
                onDismissRequest = { showExitLockedDialog = false },
                icon = { Icon(Icons.Default.Lock, contentDescription = null, tint = CrimsonStrict, modifier = Modifier.size(28.dp)) },
                title = { Text("Focus Session Active", fontWeight = FontWeight.Bold, color = Color.White) },
                text = {
                    Text(
                        "A focus session is currently in progress (${sessionState.remainingSeconds / 60}m remaining). Would you like to stay focused or use an exit?",
                        color = Color(0xFFCBD5E1),
                        fontSize = 14.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showExitLockedDialog = false
                            showEmergencyExitDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonStrict)
                    ) {
                        Text("Exit Launcher (∞)", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { showExitLockedDialog = false }
                    ) {
                        Text("Stay Focused", color = Color(0xFFCBD5E1))
                    }
                },
                containerColor = Color(0xFF111A2E)
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
                containerColor = Color(0xFF111A2E)
            )
        }

        // Edit Essential Apps Dialog
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
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1D2A4A)),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color(0xFF2D3F68)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "EMERGENCY EXITS STATUS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = IndigoPrimary,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Unlimited Exits (∞)",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldSuccess
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Emergency exits are unlimited. You can unlock the launcher and conclude the current focus session anytime.",
                            fontSize = 12.sp,
                            color = Color(0xFFCBD5E1),
                            lineHeight = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onConfirmExit,
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonStrict),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Unlock & Exit Launcher", fontWeight = FontWeight.Bold, fontSize = 14.sp)
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
                val pm = context.packageManager
                val appsMap = mutableMapOf<String, String>()

                try {
                    val launcherIntent = Intent(Intent.ACTION_MAIN, null).apply {
                        addCategory(Intent.CATEGORY_LAUNCHER)
                    }
                    val resolveInfos = pm.queryIntentActivities(launcherIntent, PackageManager.MATCH_ALL)
                    for (info in resolveInfos) {
                        val pkg = info.activityInfo.packageName
                        if (!pkg.isNullOrBlank()) {
                            val name = info.loadLabel(pm).toString()
                            if (name.isNotBlank()) {
                                appsMap[pkg] = name
                            }
                        }
                    }
                } catch (_: Exception) {}

                try {
                    val installed = pm.getInstalledApplications(PackageManager.GET_META_DATA)
                    for (appInfo in installed) {
                        if (!appsMap.containsKey(appInfo.packageName)) {
                            if (pm.getLaunchIntentForPackage(appInfo.packageName) != null) {
                                val name = pm.getApplicationLabel(appInfo).toString()
                                if (name.isNotBlank()) {
                                    appsMap[appInfo.packageName] = name
                                }
                            }
                        }
                    }
                } catch (_: Exception) {}

                val result = appsMap.map { (pkg, name) -> Pair(name, pkg) }
                    .distinctBy { it.second }
                    .sortedBy { it.first.lowercase(Locale.getDefault()) }

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

private fun launchApp(context: Context, packageName: String) {
    try {
        val pm = context.packageManager
        var intent = pm.getLaunchIntentForPackage(packageName)
        if (intent == null) {
            val resolveInfo = pm.queryIntentActivities(
                Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                    setPackage(packageName)
                },
                PackageManager.MATCH_ALL
            ).firstOrNull()
            if (resolveInfo != null) {
                intent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                    component = ComponentName(
                        resolveInfo.activityInfo.packageName,
                        resolveInfo.activityInfo.name
                    )
                }
            }
        }
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            context.startActivity(intent)
        }
    } catch (e: Exception) {
        // Fallback gracefully
    }
}
