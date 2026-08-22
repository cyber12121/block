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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DeveloperMode
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.runtime.mutableLongStateOf
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
import com.example.ui.components.DevExitConfirmDialog
import com.example.ui.components.EditEssentialAppsDialog
import com.example.ui.components.EmergencyExitDialog
import com.example.ui.components.MinimalStrictLockSetupDialog
import com.example.ui.components.MinimalStrictLockStatusDialog
import com.example.ui.components.MinimalStrictLockedDialog
import com.example.ui.components.MinimalStrictUseExitDialog
import com.example.ui.theme.CrimsonStrict
import com.example.ui.theme.CyanAccent
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

    // Minimalist Strict Lock state
    var showStrictLockSetupDialog by remember { mutableStateOf(false) }
    var showStrictLockStatusDialog by remember { mutableStateOf(false) }
    var showMinimalStrictLockedDialog by remember { mutableStateOf(false) }
    var showMinimalStrictUseExitDialog by remember { mutableStateOf(false) }
    var showDevExitConfirmDialog by remember { mutableStateOf(false) }
    var strictLockRemainingMillis by remember { mutableLongStateOf(sessionManager.getMinimalStrictLockRemainingMillis()) }

    fun formatRemainingTime(millis: Long): String {
        val totalSeconds = millis / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m ${seconds}s"
            else -> "${seconds}s"
        }
    }

    val handleExitRequest = {
        val isStrictActive = sessionManager.isMinimalStrictLockActive()
        val isDev = sessionManager.isDeveloperModeActive()
        val exitsRemaining = sessionManager.getMinimalStrictExitsRemaining()

        if (!isStrictActive) {
            sessionManager.stopMinimalStrictLock()
            sessionManager.setMinimalLauncherActive(false)
            onExitLauncher()
        } else if (isDev) {
            showDevExitConfirmDialog = true
        } else if (exitsRemaining > 0) {
            showMinimalStrictUseExitDialog = true
        } else {
            showMinimalStrictLockedDialog = true
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
            strictLockRemainingMillis = sessionManager.getMinimalStrictLockRemainingMillis()
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
    val isStrictLockActive = sessionManager.isMinimalStrictLockActive()
    val isAnyBlockingActive = sessionState.isActive || sessionManager.isAnyBlockingActive()
    val isDrawerRestricted = sessionState.isActive || isStrictLockActive

    val essentialAppsShortcuts = remember(customEssentialPkgs, installedAppsList) {
        customEssentialPkgs.map { pkg ->
            val label = installedAppsList.firstOrNull { it.second == pkg }?.first
                ?: pkg.substringAfterLast('.').replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            EssentialAppShortcut(label = label, packageName = pkg)
        }.take(6)
    }

    val blockedPackages = remember(allTargets, isAnyBlockingActive, customEssentialPkgs) {
        if (!isAnyBlockingActive) emptySet()
        else allTargets.filter { it.targetType == TargetType.APP && it.isEnabled }
            .map { it.identifier.lowercase() }
            .filter { !sessionManager.isEssentialApp(it) }
            .toSet()
    }

    // Back handling
    BackHandler(enabled = showConfigureEssentialAppsDialog) {
        showConfigureEssentialAppsDialog = false
    }

    BackHandler(enabled = showScratchpadDialog) {
        showScratchpadDialog = false
    }

    BackHandler(enabled = showPreferencesDialog) {
        showPreferencesDialog = false
    }

    BackHandler(enabled = showStrictLockSetupDialog) {
        showStrictLockSetupDialog = false
    }

    BackHandler(enabled = showStrictLockStatusDialog) {
        showStrictLockStatusDialog = false
    }

    BackHandler(enabled = showMinimalStrictLockedDialog) {
        showMinimalStrictLockedDialog = false
    }

    BackHandler(enabled = showMinimalStrictUseExitDialog) {
        showMinimalStrictUseExitDialog = false
    }

    BackHandler(enabled = showDevExitConfirmDialog) {
        showDevExitConfirmDialog = false
    }

    BackHandler(enabled = isAppDrawerOpen) {
        keyboardController?.hide()
        focusManager.clearFocus()
        isAppDrawerOpen = false
    }

    BackHandler(enabled = !isAppDrawerOpen && !showConfigureEssentialAppsDialog && !showScratchpadDialog && !showPreferencesDialog && !showStrictLockSetupDialog && !showStrictLockStatusDialog && !showMinimalStrictLockedDialog && !showMinimalStrictUseExitDialog && !showDevExitConfirmDialog) {
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
                        color = Color(0xFF131C31),
                        shape = RoundedCornerShape(100.dp),
                        border = BorderStroke(1.dp, Color(0xFF1E293B)),
                        modifier = Modifier.clickable {
                            isAppDrawerOpen = false
                            handleExitRequest()
                        }
                    ) {
                        Text(
                            text = "Exit",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF94A3B8),
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
                val allowedPkgsSet = remember(customEssentialPkgs, isDrawerRestricted) {
                    if (isDrawerRestricted) {
                        (customEssentialPkgs + context.packageName).map { it.lowercase() }.toSet()
                    } else emptySet()
                }

                val filteredInstalled = remember(installedAppsList, drawerSearchQuery, isDrawerRestricted, allowedPkgsSet) {
                    val base = if (isDrawerRestricted) {
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
                val isFocusActive = sessionState.isActive
                // ──────────────────────────────────────────
                // 1. TOP BAR: Balanced Status & Actions (No Clutter)
                // ──────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Status Badge (Left)
                    Surface(
                        color = if (isFocusActive) {
                            if (sessionState.isStrictMode) CrimsonStrict.copy(alpha = 0.12f) else IndigoPrimary.copy(alpha = 0.12f)
                        } else Color(0xFF10172A),
                        shape = RoundedCornerShape(100.dp),
                        border = BorderStroke(
                            1.dp,
                            if (isFocusActive) {
                                if (sessionState.isStrictMode) CrimsonStrict.copy(alpha = 0.4f) else IndigoPrimary.copy(alpha = 0.4f)
                            } else Color(0xFF1E293B)
                        ),
                        modifier = Modifier.clickable {
                            if (isFocusActive) onStartFocusSession()
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .background(
                                        if (isFocusActive) {
                                            if (sessionState.isStrictMode) CrimsonStrict else IndigoPrimary
                                        } else EmeraldSuccess,
                                        CircleShape
                                    )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isFocusActive) {
                                    if (sessionState.isUltraStrict) "STRICT BLOCKER 🔒" else if (sessionState.isStrictMode) "NORMAL BLOCKER" else "FOCUS ACTIVE"
                                } else "MINIMAL",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = if (isFocusActive) {
                                    if (sessionState.isStrictMode) CrimsonStrict else IndigoPrimary
                                } else EmeraldSuccess
                            )
                        }
                    }

                    // Action Controls (Right)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Strict Lock Button / Status Chip
                        if (strictLockRemainingMillis > 0) {
                            Surface(
                                color = CrimsonStrict.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(100.dp),
                                border = BorderStroke(1.dp, CrimsonStrict.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .clickable { showStrictLockStatusDialog = true }
                                    .testTag("minimal_strict_lock_status_chip")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Strict Lock Active",
                                        tint = CrimsonStrict,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "L${sessionManager.getMinimalStrictLevel()} • ${formatRemainingTime(strictLockRemainingMillis)}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        } else {
                            Surface(
                                color = Color(0xFF10172A),
                                shape = RoundedCornerShape(100.dp),
                                border = BorderStroke(1.dp, Color(0xFF1E293B)),
                                modifier = Modifier
                                    .clickable { showStrictLockSetupDialog = true }
                                    .testTag("minimal_strict_lock_enable_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Enable Strict Lock",
                                        tint = Color(0xFF94A3B8),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Strict Lock",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFFCBD5E1)
                                    )
                                }
                            }
                        }

                        // Quick Settings / Typography
                        Surface(
                            color = Color(0xFF10172A),
                            shape = CircleShape,
                            border = BorderStroke(1.dp, Color(0xFF1E293B)),
                            modifier = Modifier
                                .size(34.dp)
                                .clickable { showPreferencesDialog = true }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = "Preferences",
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }

                        // Exit Button
                        Surface(
                            color = Color(0xFF10172A),
                            shape = RoundedCornerShape(100.dp),
                            border = BorderStroke(1.dp, Color(0xFF1E293B)),
                            modifier = Modifier
                                .clickable(onClick = handleExitRequest)
                                .testTag("minimal_emergency_exit_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = "Exit Minimal Launcher",
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "Exit",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFCBD5E1)
                                )
                            }
                        }
                    }
                }

                // ==========================================
                // MIDDLE: HERO CLOCK, DATE, INTENTION & QUOTE
                // ==========================================
                // ──────────────────────────────────────────
                // 2. HERO SECTION: Clock, Date, Timer & Zen
                // ──────────────────────────────────────────
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val clockFontFamily = when (clockStyle) {
                        MinimalClockStyle.MODERN_CLEAN -> FontFamily.Default
                        MinimalClockStyle.MINIMAL_MONO -> FontFamily.Monospace
                        MinimalClockStyle.EDITORIAL_SERIF -> FontFamily.Serif
                    }

                    // Digital Clock
                    Text(
                        text = currentTime.ifEmpty { "12:00" },
                        fontSize = 68.sp,
                        fontWeight = FontWeight.Light,
                        fontFamily = clockFontFamily,
                        color = Color.White,
                        letterSpacing = (-2).sp,
                        lineHeight = 68.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Date
                    Text(
                        text = currentDate.ifEmpty { "Focus Day" },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = IndigoPrimary,
                        letterSpacing = 0.5.sp
                    )

                    // ── Active Session Sleek Countdown Strip (Single Source of Truth) ──
                    if (isFocusActive) {
                        val mins = sessionState.remainingSeconds / 60
                        val secs = sessionState.remainingSeconds % 60
                        val totalDurationSec = (sessionState.durationMinutes * 60).coerceAtLeast(1)
                        val progress = (sessionState.remainingSeconds.toFloat() / totalDurationSec.toFloat()).coerceIn(0f, 1f)

                        Spacer(modifier = Modifier.height(14.dp))

                        Surface(
                            color = Color(0xFF101728),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(
                                1.dp,
                                if (sessionState.isStrictMode) CrimsonStrict.copy(alpha = 0.5f) else IndigoPrimary.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onStartFocusSession() }
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
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
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (sessionState.isStrictMode) "Strict Lock In Progress" else "Focus Flow In Progress",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFFCBD5E1)
                                        )
                                    }

                                    Text(
                                        text = String.format(Locale.getDefault(), "%02d:%02d", mins, secs),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = if (sessionState.isStrictMode) CrimsonStrict else IndigoPrimary
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Minimalist Progress Bar
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(3.dp)
                                        .background(Color(0xFF1E293B), RoundedCornerShape(100.dp))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(progress)
                                            .height(3.dp)
                                            .background(
                                                if (sessionState.isStrictMode) CrimsonStrict else IndigoPrimary,
                                                RoundedCornerShape(100.dp)
                                            )
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // ── Intention & Zen Quote Unified Container ──
                    Surface(
                        color = Color(0xFF0F172A).copy(alpha = 0.6f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFF1E293B)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            // Intention row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        intentionDraft = userIntention
                                        isEditingIntention = true
                                    },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = IndigoPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                if (userIntention.isBlank()) {
                                    Text(
                                        text = "Set today's focus intention...",
                                        fontSize = 12.sp,
                                        color = Color(0xFF64748B),
                                        fontStyle = FontStyle.Italic
                                    )
                                } else {
                                    Text(
                                        text = userIntention,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFFE2E8F0),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Quote row
                            Text(
                                text = FOCUS_QUOTES[currentQuoteIndex % FOCUS_QUOTES.size],
                                fontSize = 11.sp,
                                color = Color(0xFF64748B),
                                lineHeight = 15.sp,
                                fontStyle = FontStyle.Italic,
                                modifier = Modifier
                                    .clickable { currentQuoteIndex++ }
                            )
                        }
                    }
                }

                // ==========================================
                // ESSENTIAL APPS LIST (THE CORE LAUNCHER)
                // ==========================================
                // ──────────────────────────────────────────
                // 3. ESSENTIAL APPS LIST (Core Launcher)
                // ──────────────────────────────────────────
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ESSENTIAL APPS (${essentialAppsShortcuts.size}/6)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.5.sp,
                            color = Color(0xFF64748B)
                        )

                        if (!sessionState.isActive && !isStrictLockActive) {
                            Text(
                                text = "Manage",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = IndigoPrimary,
                                modifier = Modifier
                                    .clickable { showConfigureEssentialAppsDialog = true }
                                    .testTag("edit_essential_apps")
                                    .padding(vertical = 4.dp, horizontal = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (essentialAppsShortcuts.isEmpty()) {
                        // Empty State Card
                        Surface(
                            color = Color(0xFF0F172A),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFF1E293B)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showConfigureEssentialAppsDialog = true }
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = null,
                                    tint = IndigoPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Choose up to 6 essential apps",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }
                    } else {
                        // Sleek Minimal App Items
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            essentialAppsShortcuts.forEach { app ->
                                Surface(
                                    color = Color(0xFF0D1424),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, Color(0xFF1A2640).copy(alpha = 0.6f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (app.packageName == context.packageName || app.label.equals("FocusGuard", ignoreCase = true)) {
                                                handleExitRequest()
                                            } else {
                                                launchApp(context, app.packageName)
                                            }
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 11.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            // Minimalist Dot Indicator
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .background(IndigoPrimary, CircleShape)
                                            )

                                            Spacer(modifier = Modifier.width(12.dp))

                                            Text(
                                                text = app.label,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = Color.White,
                                                letterSpacing = 0.2.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ──────────────────────────────────────────
                // 4. UNIFIED BOTTOM DOCK (Clean, Balanced & Consistent)
                // ──────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // App Drawer Trigger (Left Pill)
                    Surface(
                        color = Color(0xFF10172A),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color(0xFF1E293B)),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clickable { isAppDrawerOpen = true }
                            .testTag("open_minimal_drawer")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (sessionState.isActive) Icons.Default.Lock else Icons.Default.Search,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (sessionState.isActive) "Focus Drawer" else "All Apps",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }

                    // Scratchpad / Quick Notes (Center Icon Surface)
                    Surface(
                        color = Color(0xFF10172A),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color(0xFF1E293B)),
                        modifier = Modifier
                            .size(48.dp)
                            .clickable { showScratchpadDialog = true }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.EditNote,
                                contentDescription = "Quick Notes",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Focus Action (Right Button)
                    Button(
                        onClick = onStartFocusSession,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (sessionState.isActive) {
                                if (sessionState.isStrictMode) CrimsonStrict else IndigoPrimary
                            } else IndigoPrimary
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Icon(
                            imageVector = if (sessionState.isActive) Icons.Default.Shield else Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (sessionState.isActive) "Manage Focus" else "Start Focus",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
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

                        Spacer(modifier = Modifier.height(6.dp))
                        Text("STRICT LOCK SECURITY 🔒", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CrimsonStrict, letterSpacing = 1.sp)
                        Surface(
                            color = Color(0xFF1A2640),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showPreferencesDialog = false
                                    if (strictLockRemainingMillis > 0) {
                                        showStrictLockStatusDialog = true
                                    } else {
                                        showStrictLockSetupDialog = true
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (strictLockRemainingMillis > 0) "Strict Lock Active 🔒" else "Enable Minimalist Strict Lock",
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = if (strictLockRemainingMillis > 0) "Remaining: ${formatRemainingTime(strictLockRemainingMillis)} • 1 exit per day"
                                        else "Lock launcher for 1h, 2h, 3h. Allowed ONLY 1 EXIT PER DAY.",
                                        fontSize = 11.sp,
                                        color = Color(0xFFCBD5E1)
                                    )
                                }
                                Icon(Icons.Default.Lock, contentDescription = null, tint = if (strictLockRemainingMillis > 0) CrimsonStrict else Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
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

        // Minimalist Strict Lock Setup Dialog
        if (showStrictLockSetupDialog) {
            MinimalStrictLockSetupDialog(
                onDismiss = { showStrictLockSetupDialog = false },
                onStartLock = { minutes, level ->
                    sessionManager.startMinimalStrictLock(minutes, level)
                    strictLockRemainingMillis = sessionManager.getMinimalStrictLockRemainingMillis()
                    showStrictLockSetupDialog = false
                }
            )
        }

        // Minimalist Strict Lock Status Dialog
        if (showStrictLockStatusDialog) {
            MinimalStrictLockStatusDialog(
                remainingMillis = strictLockRemainingMillis,
                exitsRemaining = sessionManager.getMinimalStrictExitsRemaining(),
                strictLevel = sessionManager.getMinimalStrictLevel(),
                isDeveloper = sessionManager.isDeveloperModeActive(),
                onDismiss = { showStrictLockStatusDialog = false },
                onDisarm = {
                    sessionManager.stopMinimalStrictLock()
                    strictLockRemainingMillis = 0L
                    showStrictLockStatusDialog = false
                }
            )
        }

        // Emergency Exit Dialog for Minimalist Strict Lock (Level 1: 15s delay, Level 2: 60s delay + daily quota)
        if (showMinimalStrictUseExitDialog) {
            MinimalStrictUseExitDialog(
                remainingFormatted = formatRemainingTime(strictLockRemainingMillis),
                exitsRemaining = sessionManager.getMinimalStrictExitsRemaining(),
                strictLevel = sessionManager.getMinimalStrictLevel(),
                onDismiss = { showMinimalStrictUseExitDialog = false },
                onConfirmExit = {
                    val success = sessionManager.useMinimalStrictExit()
                    if (success) {
                        showMinimalStrictUseExitDialog = false
                        onExitLauncher()
                    }
                }
            )
        }

        // Blocked Exit Dialog for Minimalist Strict Lock
        if (showMinimalStrictLockedDialog) {
            MinimalStrictLockedDialog(
                remainingFormatted = formatRemainingTime(strictLockRemainingMillis),
                strictLevel = sessionManager.getMinimalStrictLevel(),
                onDismiss = { showMinimalStrictLockedDialog = false }
            )
        }

        // Developer Exit Confirmation Dialog
        if (showDevExitConfirmDialog) {
            DevExitConfirmDialog(
                remainingFormatted = formatRemainingTime(strictLockRemainingMillis),
                onDismiss = { showDevExitConfirmDialog = false },
                onConfirmExit = {
                    sessionManager.stopMinimalStrictLock()
                    sessionManager.setMinimalLauncherActive(false)
                    showDevExitConfirmDialog = false
                    onExitLauncher()
                }
            )
        }
    }
}


private fun launchApp(context: Context, packageName: String) {
    try {
        FocusSessionManager.getInstance(context).reportEssentialAppLaunched(packageName)
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
    } catch (_: Exception) {
        // Fallback gracefully
    }
}
