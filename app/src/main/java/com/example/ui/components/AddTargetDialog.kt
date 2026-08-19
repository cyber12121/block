package com.example.ui.components

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.BlockList
import com.example.data.model.TargetType
import com.example.ui.theme.CrimsonStrict
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IndigoPrimary

data class PresetTargetItem(
    val name: String,
    val identifier: String,
    val type: TargetType,
    val category: String,
    val emoji: String
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddTargetDialog(
    targetList: BlockList,
    onDismiss: () -> Unit,
    onAddBulkTargets: (type: TargetType, items: List<String>, category: String) -> Unit
) {
    var isQuickCatalogMode by remember { mutableStateOf(true) }
    var selectedType by remember { mutableStateOf(TargetType.WEBSITE) }
    var customInputText by remember { mutableStateOf("") }
    var customCategory by remember { mutableStateOf("Custom") }

    // Multi-selected presets
    val selectedPresets = remember { mutableStateListOf<PresetTargetItem>() }

    val presetCatalog = remember {
        listOf(
            // Social Media
            PresetTargetItem("YouTube", "youtube.com", TargetType.WEBSITE, "Social", "▶️"),
            PresetTargetItem("YouTube App", "com.google.android.youtube", TargetType.APP, "Social", "📱"),
            PresetTargetItem("Instagram", "instagram.com", TargetType.WEBSITE, "Social", "📸"),
            PresetTargetItem("Instagram App", "com.instagram.android", TargetType.APP, "Social", "📱"),
            PresetTargetItem("TikTok", "tiktok.com", TargetType.WEBSITE, "Social", "🎵"),
            PresetTargetItem("TikTok App", "com.zhiliaoapp.musically", TargetType.APP, "Social", "📱"),
            PresetTargetItem("Twitter / X", "twitter.com", TargetType.WEBSITE, "Social", "🐦"),
            PresetTargetItem("X App", "com.twitter.android", TargetType.APP, "Social", "📱"),
            PresetTargetItem("Facebook", "facebook.com", TargetType.WEBSITE, "Social", "📘"),
            PresetTargetItem("Reddit", "reddit.com", TargetType.WEBSITE, "Social", "🤖"),
            PresetTargetItem("Reddit App", "com.reddit.frontpage", TargetType.APP, "Social", "📱"),
            PresetTargetItem("Snapchat", "com.snapchat.android", TargetType.APP, "Social", "👻"),
            PresetTargetItem("Pinterest", "pinterest.com", TargetType.WEBSITE, "Social", "📌"),
            PresetTargetItem("Threads", "threads.net", TargetType.WEBSITE, "Social", "🧵"),

            // Entertainment & Streaming
            PresetTargetItem("Netflix", "netflix.com", TargetType.WEBSITE, "Entertainment", "🍿"),
            PresetTargetItem("Netflix App", "com.netflix.mediaclient", TargetType.APP, "Entertainment", "📱"),
            PresetTargetItem("Twitch", "twitch.tv", TargetType.WEBSITE, "Entertainment", "🟣"),
            PresetTargetItem("Twitch App", "tv.twitch.android.app", TargetType.APP, "Entertainment", "📱"),
            PresetTargetItem("Disney+", "disneyplus.com", TargetType.WEBSITE, "Entertainment", "✨"),
            PresetTargetItem("Prime Video", "primevideo.com", TargetType.WEBSITE, "Entertainment", "🎬"),
            PresetTargetItem("Spotify", "com.spotify.music", TargetType.APP, "Entertainment", "🎧"),

            // Gaming & Chat
            PresetTargetItem("Roblox", "com.roblox.client", TargetType.APP, "Gaming", "🧱"),
            PresetTargetItem("Discord", "com.discord", TargetType.APP, "Gaming", "💬"),
            PresetTargetItem("Steam", "steampowered.com", TargetType.WEBSITE, "Gaming", "🎮"),
            PresetTargetItem("Chess.com", "chess.com", TargetType.WEBSITE, "Gaming", "♟️"),

            // Doomscroll Keywords
            PresetTargetItem("Shorts Keyword", "shorts", TargetType.KEYWORD, "Doomscroll", "⚡"),
            PresetTargetItem("Reels Keyword", "reels", TargetType.KEYWORD, "Doomscroll", "⚡"),
            PresetTargetItem("Trending Keyword", "trending", TargetType.KEYWORD, "Doomscroll", "🔥"),
            PresetTargetItem("Explore Feed", "explore", TargetType.KEYWORD, "Doomscroll", "🔍"),
            PresetTargetItem("Gossip", "gossip", TargetType.KEYWORD, "Doomscroll", "📰")
        )
    }

    // Categories in Catalog
    val catalogCategories = listOf("All", "Social", "Entertainment", "Gaming", "Doomscroll")
    var selectedCategoryFilter by remember { mutableStateOf("All") }

    val filteredPresets = remember(selectedCategoryFilter) {
        if (selectedCategoryFilter == "All") presetCatalog else presetCatalog.filter { it.category == selectedCategoryFilter }
    }

    // Custom parsed items
    val customParsedItems = remember(customInputText) {
        customInputText.split("\n", ",", ";")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF0F172A)),
            color = Color(0xFF0F172A),
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(22.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(targetList.colorHex).copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "🛡️", fontSize = 20.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Add Multiple Block Rules",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Target List: ${targetList.name}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(targetList.colorHex),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Mode Tabs: Preset Multi-Picker vs Custom Text Input
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1E293B), RoundedCornerShape(12.dp))
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isQuickCatalogMode) IndigoPrimary else Color.Transparent)
                            .clickable { isQuickCatalogMode = true }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ListAlt,
                                contentDescription = null,
                                tint = if (isQuickCatalogMode) Color.White else Color(0xFF94A3B8),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Quick Multi-Picker",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isQuickCatalogMode) Color.White else Color(0xFF94A3B8)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (!isQuickCatalogMode) IndigoPrimary else Color.Transparent)
                            .clickable { isQuickCatalogMode = false }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ContentPaste,
                                contentDescription = null,
                                tint = if (!isQuickCatalogMode) Color.White else Color(0xFF94A3B8),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Custom Bulk Input",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (!isQuickCatalogMode) Color.White else Color(0xFF94A3B8)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (isQuickCatalogMode) {
                    // Category Filter Chips + Select All Option
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FlowRow(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            catalogCategories.forEach { cat ->
                                val isSelected = selectedCategoryFilter == cat
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) CyanAccent.copy(alpha = 0.2f) else Color(0xFF1E293B),
                                    border = BorderStroke(1.dp, if (isSelected) CyanAccent else Color.Transparent),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { selectedCategoryFilter = cat }
                                ) {
                                    Text(
                                        text = cat,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isSelected) CyanAccent else Color(0xFF94A3B8),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }

                        // Select All in Category button
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = IndigoPrimary.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, IndigoPrimary.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    val currentCategoryItems = filteredPresets
                                    val allSelected = currentCategoryItems.all { item -> selectedPresets.any { it.identifier == item.identifier } }
                                    if (allSelected) {
                                        // Deselect all in this category
                                        val toRemove = currentCategoryItems.map { it.identifier }.toSet()
                                        selectedPresets.removeAll { toRemove.contains(it.identifier) }
                                    } else {
                                        // Select all in this category
                                        currentCategoryItems.forEach { item ->
                                            if (selectedPresets.none { it.identifier == item.identifier }) {
                                                selectedPresets.add(item)
                                            }
                                        }
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(imageVector = Icons.Default.SelectAll, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Toggle All",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CyanAccent
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Preset Chips Grid
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        filteredPresets.forEach { item ->
                            val isChecked = selectedPresets.any { it.identifier == item.identifier }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isChecked) IndigoPrimary.copy(alpha = 0.25f) else Color(0xFF1E293B),
                                border = BorderStroke(
                                    1.dp,
                                    if (isChecked) IndigoPrimary else Color(0xFF334155)
                                ),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        if (isChecked) {
                                            selectedPresets.removeAll { it.identifier == item.identifier }
                                        } else {
                                            selectedPresets.add(item)
                                        }
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = item.emoji, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = item.name,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (isChecked) Color.White else Color(0xFFCBD5E1)
                                    )
                                    if (isChecked) {
                                        Spacer(modifier = Modifier.width(6.dp))
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

                    Spacer(modifier = Modifier.height(16.dp))

                    // Live Selection Status Bar
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Selected: ${selectedPresets.size} rules to block",
                                fontWeight = FontWeight.Bold,
                                color = if (selectedPresets.isNotEmpty()) EmeraldSuccess else Color(0xFF94A3B8),
                                fontSize = 13.sp
                            )
                            if (selectedPresets.isNotEmpty()) {
                                Text(
                                    text = "Clear All",
                                    fontSize = 11.sp,
                                    color = CrimsonStrict,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .clickable { selectedPresets.clear() }
                                        .padding(4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Submit Button for Multi-Picker
                    Button(
                        onClick = {
                            if (selectedPresets.isNotEmpty()) {
                                // Group by type and add in bulk
                                val byType = selectedPresets.groupBy { it.type }
                                byType.forEach { (type, items) ->
                                    onAddBulkTargets(type, items.map { it.identifier }, "Multi-Block")
                                }
                                onDismiss()
                            }
                        },
                        enabled = selectedPresets.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("submit_multi_presets_button")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Add ${selectedPresets.size} Selected Targets",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                } else {
                    // Custom Text Bulk Input
                    // Type selector
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF1E293B), RoundedCornerShape(12.dp))
                            .padding(4.dp)
                    ) {
                        TargetType.values().forEach { type ->
                            val isSelected = selectedType == type
                            val icon = when (type) {
                                TargetType.WEBSITE -> Icons.Default.Language
                                TargetType.APP -> Icons.Default.PhoneAndroid
                                TargetType.KEYWORD -> Icons.Default.TextFields
                            }
                            val title = when (type) {
                                TargetType.WEBSITE -> "Websites"
                                TargetType.APP -> "Apps"
                                TargetType.KEYWORD -> "Keywords"
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) IndigoPrimary else Color.Transparent)
                                    .clickable { selectedType = type }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = if (isSelected) Color.White else Color(0xFF94A3B8),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = title,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isSelected) Color.White else Color(0xFF94A3B8)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    val placeholderText = when (selectedType) {
                        TargetType.WEBSITE -> "Paste multiple domains separated by commas or lines:\nyoutube.com\ninstagram.com, twitter.com\nreddit.com"
                        TargetType.APP -> "Paste package names separated by commas or lines:\ncom.instagram.android\ncom.google.android.youtube"
                        TargetType.KEYWORD -> "Paste keywords or phrases separated by commas:\nreels, shorts, doomscroll, gossip"
                    }

                    OutlinedTextField(
                        value = customInputText,
                        onValueChange = { customInputText = it },
                        label = { Text("Paste Multiple Targets", color = Color.White) },
                        placeholder = { Text(placeholderText, fontSize = 12.sp, color = Color(0xFF64748B)) },
                        minLines = 4,
                        maxLines = 8,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (customParsedItems.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            color = EmeraldSuccess.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Ready to add ${customParsedItems.size} ${if (customParsedItems.size == 1) "rule" else "rules"} in bulk",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldSuccess
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = customCategory,
                        onValueChange = { customCategory = it },
                        label = { Text("Category Tag", color = Color.White) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            if (customParsedItems.isNotEmpty()) {
                                onAddBulkTargets(selectedType, customParsedItems, customCategory)
                                onDismiss()
                            }
                        },
                        enabled = customParsedItems.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("submit_custom_bulk_targets_button")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (customParsedItems.size > 1) "Add ${customParsedItems.size} Rules in Bulk" else "Add Rule to List",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}
