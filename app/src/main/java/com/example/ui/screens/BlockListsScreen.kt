package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.runtime.mutableStateMapOf
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
import com.example.data.model.BlockList
import com.example.data.model.BlockedTarget
import com.example.data.model.TargetType
import com.example.service.ActiveSessionState
import com.example.ui.components.EditTargetDialog
import com.example.ui.theme.CrimsonStrict
import com.example.ui.theme.IndigoPrimary

@Composable
fun BlockListsScreen(
    sessionState: ActiveSessionState,
    blockLists: List<BlockList>,
    allTargets: List<BlockedTarget>,
    onToggleList: (BlockList) -> Unit,
    onToggleTarget: (BlockedTarget) -> Unit,
    onUpdateTarget: (BlockedTarget) -> Unit,
    onDeleteTarget: (BlockedTarget) -> Unit,
    onOpenAddTarget: (BlockList) -> Unit,
    onOpenCreateList: () -> Unit,
    onDeleteList: (BlockList) -> Unit
) {
    val expandedMap = remember { mutableStateMapOf<Long, Boolean>() }
    var targetToEdit by remember { mutableStateOf<BlockedTarget?>(null) }
    val isSessionStrict = sessionState.isActive && sessionState.isStrictMode

    Scaffold(
        floatingActionButton = {
            if (!isSessionStrict) {
                ExtendedFloatingActionButton(
                    onClick = onOpenCreateList,
                    containerColor = IndigoPrimary,
                    contentColor = Color.White,
                    icon = {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null)
                    },
                    text = {
                        Text(text = "New List", fontWeight = FontWeight.Bold)
                    },
                    modifier = Modifier
                        .padding(bottom = 16.dp)
                        .testTag("fab_create_list")
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Block Lists & Rules",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isSessionStrict) "Rules locked during Strict Mode" else "Tap any rule to edit, or paste multiple domains",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isSessionStrict) CrimsonStrict else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (isSessionStrict) {
                        Surface(
                            color = CrimsonStrict.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = CrimsonStrict,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "LOCKED",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CrimsonStrict
                                )
                            }
                        }
                    }
                }
            }

            items(blockLists) { list ->
                val isExpanded = expandedMap[list.id] ?: false
                val targets = allTargets.filter { it.listId == list.id }

                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // List Header Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { expandedMap[list.id] = !isExpanded }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(Color(list.colorHex).copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Shield,
                                        contentDescription = null,
                                        tint = Color(list.colorHex),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = list.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${targets.size} rules configured",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Switch(
                                    checked = list.isEnabled,
                                    onCheckedChange = { if (!isSessionStrict) onToggleList(list) },
                                    enabled = !isSessionStrict,
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = Color(list.colorHex)
                                    )
                                )
                                IconButton(onClick = { expandedMap[list.id] = !isExpanded }) {
                                    Icon(
                                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = "Expand"
                                    )
                                }
                            }
                        }

                        // Expanded Rules View
                        AnimatedVisibility(visible = isExpanded) {
                            Column(modifier = Modifier.padding(top = 12.dp)) {
                                if (list.description.isNotBlank()) {
                                    Text(
                                        text = list.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(bottom = 12.dp)
                                    )
                                }

                                if (targets.isEmpty()) {
                                    Text(
                                        text = "No rules added to this list yet.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                } else {
                                    targets.forEach { target ->
                                        TargetItemRow(
                                            target = target,
                                            isLocked = isSessionStrict,
                                            onToggle = { onToggleTarget(target) },
                                            onEdit = { if (!isSessionStrict) targetToEdit = target },
                                            onDelete = { onDeleteTarget(target) }
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                    }
                                }

                                if (!isSessionStrict) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Button(
                                            onClick = { onOpenAddTarget(list) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(list.colorHex)),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.height(38.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(text = "Add / Bulk Paste", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }

                                        if (!list.isDefault) {
                                            IconButton(onClick = { onDeleteList(list) }) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Delete list",
                                                    tint = CrimsonStrict
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
        }
    }

    targetToEdit?.let { target ->
        EditTargetDialog(
            target = target,
            onDismiss = { targetToEdit = null },
            onSaveTarget = { updated ->
                onUpdateTarget(updated)
                targetToEdit = null
            },
            onDeleteTarget = {
                onDeleteTarget(it)
                targetToEdit = null
            }
        )
    }
}

@Composable
fun TargetItemRow(
    target: BlockedTarget,
    isLocked: Boolean,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val icon = when (target.targetType) {
        TargetType.WEBSITE -> Icons.Default.Language
        TargetType.APP -> Icons.Default.PhoneAndroid
        TargetType.KEYWORD -> Icons.Default.TextFields
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isLocked, onClick = onEdit)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = target.displayName,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = target.identifier,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (!isLocked) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = IndigoPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
