package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BlockList
import com.example.data.model.BlockedTarget
import com.example.data.model.TargetType
import com.example.service.ActiveSessionState
import com.example.ui.theme.AmberFocus
import com.example.ui.theme.CrimsonStrict
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IndigoPrimary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class InstalledAppItem(
    val appName: String,
    val packageName: String,
    val iconBitmap: Bitmap?,
    val category: String,
    val isSystemApp: Boolean
)

@Composable
fun AppsScreen(
    sessionState: ActiveSessionState,
    blockLists: List<BlockList>,
    allTargets: List<BlockedTarget>,
    onToggleTarget: (BlockedTarget) -> Unit,
    onAddBulkTargets: (listId: Long, type: TargetType, items: List<String>, category: String) -> Unit,
    onDeleteTarget: (BlockedTarget) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var selectedListId by remember(blockLists) {
        mutableStateOf(blockLists.firstOrNull()?.id ?: 1L)
    }

    var installedApps by remember { mutableStateOf<List<InstalledAppItem>>(emptyList()) }
    var isLoadingApps by remember { mutableStateOf(true) }

    val isSessionStrict = sessionState.isActive && sessionState.isStrictMode

    // Set of currently blocked package names
    val blockedAppTargets = remember(allTargets) {
        allTargets.filter { it.targetType == TargetType.APP && it.isEnabled }
    }
    val blockedPackagesMap = remember(allTargets) {
        allTargets.filter { it.targetType == TargetType.APP }.associateBy { it.identifier.lowercase() }
    }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val launcherIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolveInfos = pm.queryIntentActivities(launcherIntent, 0)
            val apps = mutableListOf<InstalledAppItem>()
            val seenPackages = mutableSetOf<String>()

            for (info in resolveInfos) {
                val pkg = info.activityInfo.packageName
                if (pkg == context.packageName || seenPackages.contains(pkg)) continue
                seenPackages.add(pkg)

                val name = info.loadLabel(pm).toString()
                val iconDrawable = try {
                    info.loadIcon(pm)
                } catch (_: Exception) {
                    null
                }
                val bitmap = iconDrawable?.let { drawableToBitmap(it) }

                var category = "Other"
                val pkgLower = pkg.lowercase()
                val nameLower = name.lowercase()

                if (pkgLower.contains("instagram") || pkgLower.contains("facebook") || pkgLower.contains("twitter") ||
                    pkgLower.contains("tiktok") || pkgLower.contains("snapchat") || pkgLower.contains("reddit") ||
                    pkgLower.contains("discord") || pkgLower.contains("telegram") || pkgLower.contains("whatsapp") ||
                    nameLower.contains("social") || nameLower.contains("chat")
                ) {
                    category = "Social"
                } else if (pkgLower.contains("youtube") || pkgLower.contains("netflix") || pkgLower.contains("spotify") ||
                    pkgLower.contains("twitch") || pkgLower.contains("disney") || pkgLower.contains("hulu") ||
                    pkgLower.contains("primevideo") || nameLower.contains("stream") || nameLower.contains("video")
                ) {
                    category = "Streaming"
                } else if (pkgLower.contains("game") || nameLower.contains("game") ||
                    info.activityInfo.applicationInfo.category == ApplicationInfo.CATEGORY_GAME
                ) {
                    category = "Games"
                } else if (pkgLower.contains("amazon") || pkgLower.contains("ebay") || pkgLower.contains("shopping") ||
                    pkgLower.contains("shein") || pkgLower.contains("temu") || pkgLower.contains("aliexpress")
                ) {
                    category = "Shopping"
                } else if (pkgLower.contains("chrome") || pkgLower.contains("browser") || pkgLower.contains("firefox") ||
                    pkgLower.contains("opera") || pkgLower.contains("edge")
                ) {
                    category = "Browsers"
                }

                val isSys = (info.activityInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                apps.add(InstalledAppItem(name, pkg, bitmap, category, isSys))
            }

            // Sort: Blocked first, then alphabetical
            apps.sortBy { it.appName.lowercase() }
            installedApps = apps
            isLoadingApps = false
        }
    }

    val filteredApps = remember(installedApps, searchQuery, selectedCategory, blockedPackagesMap) {
        installedApps.filter { app ->
            val matchesSearch = searchQuery.isBlank() ||
                app.appName.contains(searchQuery, ignoreCase = true) ||
                app.packageName.contains(searchQuery, ignoreCase = true)

            val matchesCategory = when (selectedCategory) {
                "All" -> true
                "Blocked" -> blockedPackagesMap.containsKey(app.packageName.lowercase())
                else -> app.category.equals(selectedCategory, ignoreCase = true)
            }

            matchesSearch && matchesCategory
        }
    }

    val categories = listOf("All", "Blocked", "Social", "Streaming", "Games", "Shopping", "Browsers", "Other")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Header
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Installed Apps Blocker",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Toggle distraction barriers for installed apps in real-time",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Surface(
                        color = if (blockedAppTargets.isNotEmpty()) CrimsonStrict.copy(alpha = 0.15f) else EmeraldSuccess.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(100.dp),
                        border = BorderStroke(1.dp, if (blockedAppTargets.isNotEmpty()) CrimsonStrict.copy(alpha = 0.5f) else EmeraldSuccess.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (blockedAppTargets.isNotEmpty()) Icons.Default.Block else Icons.Default.Shield,
                                contentDescription = null,
                                tint = if (blockedAppTargets.isNotEmpty()) CrimsonStrict else EmeraldSuccess,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${blockedAppTargets.size} Apps Blocked",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (blockedAppTargets.isNotEmpty()) CrimsonStrict else EmeraldSuccess
                            )
                        }
                    }
                }
            }
        }

        // 2. Strict Mode Warning if active
        if (isSessionStrict) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF261016)),
                    border = BorderStroke(1.dp, CrimsonStrict.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = CrimsonStrict,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Strict Mode Active: App blocking rules are locked to prevent bypassing.",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = CrimsonStrict
                        )
                    }
                }
            }
        }

        // 3. Quick Batch Actions Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "1-Tap Category Blocker",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val socialPackages = installedApps
                                    .filter { it.category == "Social" }
                                    .map { it.packageName }
                                if (socialPackages.isNotEmpty()) {
                                    onAddBulkTargets(selectedListId, TargetType.APP, socialPackages, "Social")
                                }
                            },
                            enabled = !isSessionStrict,
                            colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).height(38.dp)
                        ) {
                            Text("Block Social", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                val streamingPackages = installedApps
                                    .filter { it.category == "Streaming" }
                                    .map { it.packageName }
                                if (streamingPackages.isNotEmpty()) {
                                    onAddBulkTargets(selectedListId, TargetType.APP, streamingPackages, "Streaming")
                                }
                            },
                            enabled = !isSessionStrict,
                            colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).height(38.dp)
                        ) {
                            Text("Block Streaming", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        }

                        Button(
                            onClick = {
                                val gamePackages = installedApps
                                    .filter { it.category == "Games" }
                                    .map { it.packageName }
                                if (gamePackages.isNotEmpty()) {
                                    onAddBulkTargets(selectedListId, TargetType.APP, gamePackages, "Games")
                                }
                            },
                            enabled = !isSessionStrict,
                            colors = ButtonDefaults.buttonColors(containerColor = AmberFocus),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).height(38.dp)
                        ) {
                            Text("Block Games", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        }
                    }
                }
            }
        }

        // 4. Search Bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search installed apps or packages...", fontSize = 13.sp) },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = IndigoPrimary)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = IndigoPrimary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("app_search_input")
            )
        }

        // 5. Category Filter Chips
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(categories) { cat ->
                    val isSelected = selectedCategory == cat
                    val count = when (cat) {
                        "All" -> installedApps.size
                        "Blocked" -> blockedPackagesMap.size
                        else -> installedApps.count { it.category.equals(cat, ignoreCase = true) }
                    }

                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = cat },
                        label = {
                            Text(
                                text = "$cat ($count)",
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = IndigoPrimary.copy(alpha = 0.2f),
                            selectedLabelColor = IndigoPrimary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = if (isSelected) IndigoPrimary else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }
        }

        // 6. Loading or Empty State
        if (isLoadingApps) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = IndigoPrimary)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Scanning installed applications...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else if (filteredApps.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Apps,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No apps found matching '$searchQuery'",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Try a different search term or category filter",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            // 7. Installed App Rows
            items(filteredApps, key = { it.packageName }) { app ->
                val existingTarget = blockedPackagesMap[app.packageName.lowercase()]
                val isBlocked = existingTarget != null && existingTarget.isEnabled

                AppBlockItemRow(
                    app = app,
                    isBlocked = isBlocked,
                    isStrictActive = isSessionStrict,
                    onToggle = { enable ->
                        if (isSessionStrict) return@AppBlockItemRow
                        if (existingTarget != null) {
                            onToggleTarget(existingTarget)
                        } else if (enable) {
                            onAddBulkTargets(selectedListId, TargetType.APP, listOf(app.packageName), app.category)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun AppBlockItemRow(
    app: InstalledAppItem,
    isBlocked: Boolean,
    isStrictActive: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isBlocked) Color(0xFF1F1215) else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            if (isBlocked) CrimsonStrict.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.fillMaxWidth()
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
                // Real App Icon or Fallback
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (app.iconBitmap != null) {
                        Image(
                            bitmap = app.iconBitmap.asImageBitmap(),
                            contentDescription = app.appName,
                            modifier = Modifier.size(36.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.PhoneAndroid,
                            contentDescription = app.appName,
                            tint = IndigoPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = app.appName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = app.category,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Text(
                        text = app.packageName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Switch to Block / Unblock
            Switch(
                checked = isBlocked,
                onCheckedChange = { onToggle(it) },
                enabled = !isStrictActive,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = CrimsonStrict,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.testTag("toggle_app_${app.packageName}")
            )
        }
    }
}

private fun drawableToBitmap(drawable: Drawable): Bitmap? {
    return try {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            return drawable.bitmap
        }
        val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth.coerceAtMost(96) else 64
        val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight.coerceAtMost(96) else 64
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        bitmap
    } catch (_: Exception) {
        null
    }
}
