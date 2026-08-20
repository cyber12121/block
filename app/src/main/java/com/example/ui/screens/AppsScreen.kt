package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
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
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.example.ui.theme.CrimsonStrict
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IndigoPrimary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class InstalledAppItem(
    val appName: String,
    val packageName: String,
    val iconBitmap: Bitmap?,
    val category: String,
    val isSystemApp: Boolean
)

fun drawableToBitmap(drawable: Drawable): Bitmap {
    if (drawable is BitmapDrawable && drawable.bitmap != null) {
        return drawable.bitmap
    }
    val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 72
    val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 72
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
}

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
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var installedApps by remember { mutableStateOf<List<InstalledAppItem>>(emptyList()) }
    var isLoadingApps by remember { mutableStateOf(true) }

    val isSessionStrict = sessionState.isActive && sessionState.isStrictMode

    val blockedTargetsMap = remember(allTargets) {
        allTargets.filter { it.targetType == TargetType.APP }.associateBy { it.identifier.lowercase() }
    }
    val blockedAppCount = remember(allTargets) {
        allTargets.count { it.targetType == TargetType.APP && it.isEnabled }
    }

    val defaultListId = remember(blockLists) {
        blockLists.firstOrNull()?.id ?: 1L
    }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val launcherIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolveInfos = pm.queryIntentActivities(launcherIntent, PackageManager.MATCH_ALL)
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
                    category = "Video"
                } else if (pkgLower.contains("game") || nameLower.contains("game") ||
                    (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && info.activityInfo.applicationInfo.category == ApplicationInfo.CATEGORY_GAME)
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

            apps.sortBy { it.appName.lowercase() }
            installedApps = apps
            isLoadingApps = false
        }
    }

    val filteredApps = remember(installedApps, searchQuery, selectedCategory, blockedTargetsMap) {
        installedApps.filter { app ->
            val matchesSearch = searchQuery.isBlank() ||
                app.appName.contains(searchQuery, ignoreCase = true) ||
                app.packageName.contains(searchQuery, ignoreCase = true)

            val isBlocked = blockedTargetsMap[app.packageName.lowercase()]?.isEnabled == true
            val matchesCategory = when (selectedCategory) {
                "All" -> true
                "Blocked" -> isBlocked
                else -> app.category.equals(selectedCategory, ignoreCase = true)
            }

            matchesSearch && matchesCategory
        }
    }

    val socialCount = remember(installedApps) { installedApps.count { it.category == "Social" } }
    val videoCount = remember(installedApps) { installedApps.count { it.category == "Video" } }
    val gamesCount = remember(installedApps) { installedApps.count { it.category == "Games" } }

    val endFormatted = if (sessionState.endTimeMillis > 0) {
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(sessionState.endTimeMillis))
    } else ""

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Header Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, DarkCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "App Blocker",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Surface(
                            color = if (blockedAppCount > 0) CrimsonStrict.copy(alpha = 0.2f) else EmeraldSuccess.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(100.dp),
                            border = BorderStroke(1.dp, if (blockedAppCount > 0) CrimsonStrict.copy(alpha = 0.5f) else EmeraldSuccess.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = "$blockedAppCount blocked",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (blockedAppCount > 0) Color(0xFFF87171) else EmeraldSuccess,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Choose which apps FocusGuard should block",
                        fontSize = 13.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }
        }

        // 2. Strict Session Active Banner
        if (isSessionStrict) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF221118)),
                    border = BorderStroke(1.dp, CrimsonStrict.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(CrimsonStrict.copy(alpha = 0.2f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = CrimsonStrict,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Strict session active",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color(0xFFF87171)
                            )
                            Text(
                                text = "App rules can't be changed until $endFormatted",
                                fontSize = 12.sp,
                                color = Color(0xFFCBD5E1)
                            )
                        }
                    }
                }
            }
        }

        // 3. Search Bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        text = "Search installed apps",
                        color = Color(0xFF94A3B8),
                        fontSize = 14.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = DarkSurface,
                    unfocusedContainerColor = DarkSurface,
                    focusedBorderColor = IndigoPrimary,
                    unfocusedBorderColor = DarkCardBorder,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_installed_apps")
            )
        }

        // 4. Filter Chips Row
        item {
            val filterOptions = listOf(
                "All" to installedApps.size,
                "Blocked" to blockedAppCount,
                "Social" to socialCount,
                "Video" to videoCount,
                "Games" to gamesCount
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(filterOptions) { (label, count) ->
                    val isSelected = selectedCategory.equals(label, ignoreCase = true)
                    Surface(
                        color = if (isSelected) {
                            if (label == "Blocked") CrimsonStrict.copy(alpha = 0.25f) else IndigoPrimary.copy(alpha = 0.25f)
                        } else DarkSurface,
                        shape = RoundedCornerShape(100.dp),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) {
                                if (label == "Blocked") CrimsonStrict else IndigoPrimary
                            } else DarkCardBorder
                        ),
                        modifier = Modifier.clickable { selectedCategory = label }
                    ) {
                        Text(
                            text = "$label $count",
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) {
                                if (label == "Blocked") Color(0xFFF87171) else Color.White
                            } else Color(0xFF94A3B8),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                        )
                    }
                }
            }
        }

        // 5. Quick Block Section
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Quick block",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickCategoryTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.People,
                        label = "Social",
                        isStrictLocked = isSessionStrict,
                        onClick = {
                            if (!isSessionStrict) {
                                val pkgs = installedApps.filter { it.category == "Social" }.map { it.packageName }
                                if (pkgs.isNotEmpty()) {
                                    onAddBulkTargets(defaultListId, TargetType.APP, pkgs, "Social")
                                }
                            }
                        }
                    )
                    QuickCategoryTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.PlayCircle,
                        label = "Video",
                        isStrictLocked = isSessionStrict,
                        onClick = {
                            if (!isSessionStrict) {
                                val pkgs = installedApps.filter { it.category == "Video" }.map { it.packageName }
                                if (pkgs.isNotEmpty()) {
                                    onAddBulkTargets(defaultListId, TargetType.APP, pkgs, "Video")
                                }
                            }
                        }
                    )
                    QuickCategoryTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.SportsEsports,
                        label = "Games",
                        isStrictLocked = isSessionStrict,
                        onClick = {
                            if (!isSessionStrict) {
                                val pkgs = installedApps.filter { it.category == "Games" }.map { it.packageName }
                                if (pkgs.isNotEmpty()) {
                                    onAddBulkTargets(defaultListId, TargetType.APP, pkgs, "Games")
                                }
                            }
                        }
                    )
                }
            }
        }

        // 6. Loading state
        if (isLoadingApps) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = IndigoPrimary)
                }
            }
        }

        // 7. Installed Apps Items
        items(filteredApps, key = { it.packageName }) { app ->
            val existingTarget = blockedTargetsMap[app.packageName.lowercase()]
            val isBlocked = existingTarget?.isEnabled == true

            AppBlockerRowCard(
                app = app,
                isBlocked = isBlocked,
                isSessionStrict = isSessionStrict,
                onToggle = {
                    if (!isSessionStrict) {
                        if (existingTarget != null) {
                            onToggleTarget(existingTarget)
                        } else {
                            onAddBulkTargets(defaultListId, TargetType.APP, listOf(app.packageName), app.category)
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun QuickCategoryTile(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isStrictLocked: Boolean,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, DarkCardBorder),
        modifier = modifier.clickable(enabled = !isStrictLocked, onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isStrictLocked) Color(0xFF64748B) else IndigoPrimary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = if (isStrictLocked) Color(0xFF64748B) else Color.White
            )
        }
    }
}

@Composable
fun AppBlockerRowCard(
    app: InstalledAppItem,
    isBlocked: Boolean,
    isSessionStrict: Boolean,
    onToggle: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(
            1.dp,
            if (isBlocked && isSessionStrict) CrimsonStrict.copy(alpha = 0.3f) else DarkCardBorder
        ),
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
                // App Icon
                if (app.iconBitmap != null) {
                    Image(
                        bitmap = app.iconBitmap.asImageBitmap(),
                        contentDescription = app.appName,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(DarkSurfaceVariant, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = app.appName.take(1).uppercase(),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = IndigoPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = app.appName,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "${app.category} • ${app.packageName}",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8),
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Right lock / switch
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isSessionStrict && isBlocked) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Strictly Locked",
                        tint = CrimsonStrict,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }

                Switch(
                    checked = isBlocked,
                    onCheckedChange = { if (!isSessionStrict) onToggle() },
                    enabled = !isSessionStrict,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = CyanAccent,
                        uncheckedThumbColor = Color(0xFF94A3B8),
                        uncheckedTrackColor = DarkSurfaceVariant,
                        disabledCheckedThumbColor = Color.White,
                        disabledCheckedTrackColor = CrimsonStrict.copy(alpha = 0.6f),
                        disabledUncheckedThumbColor = Color(0xFF64748B),
                        disabledUncheckedTrackColor = DarkSurfaceVariant.copy(alpha = 0.5f)
                    )
                )
            }
        }
    }
}
