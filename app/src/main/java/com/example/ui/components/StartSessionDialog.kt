package com.example.ui.components

import android.view.MotionEvent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.BlockList
import com.example.data.model.PlantType
import com.example.ui.theme.CrimsonStrict
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IndigoPrimary
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class SessionModeType {
    NORMAL,
    STRICT
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalComposeUiApi::class)
@Composable
fun StartSessionDialog(
    availableLists: List<BlockList>,
    onDismiss: () -> Unit,
    onStartSession: (
        title: String,
        durationMinutes: Int,
        isStrictMode: Boolean,
        activeLists: List<String>,
        autoLaunchMinimal: Boolean
    ) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedMinutes by remember { mutableIntStateOf(25) }
    var customMinutesText by remember { mutableStateOf("") }
    var isCustomDuration by remember { mutableStateOf(false) }
    var modeType by remember { mutableStateOf(SessionModeType.NORMAL) }
    var isAcknowledged by remember { mutableStateOf(false) }
    var autoLaunchMinimal by remember { mutableStateOf(true) }

    val isStrictMode = modeType == SessionModeType.STRICT

    // Multi-selected block lists
    val selectedListIds = remember {
        mutableStateOf(availableLists.filter { it.isEnabled }.map { it.id }.toSet())
    }

    // 3-second hold button logic
    var isHolding by remember { mutableStateOf(false) }
    var holdProgress by remember { mutableFloatStateOf(0f) }

    val presetDurations = listOf(15, 25, 45, 60, 90, 120)

    val effectiveMinutes = if (isCustomDuration) {
        customMinutesText.toIntOrNull()?.coerceIn(1, 1440) ?: 25
    } else {
        selectedMinutes
    }

    val calculatedEndTime = System.currentTimeMillis() + (effectiveMinutes * 60 * 1000L)
    val endTimeString = SimpleDateFormat("EEE, hh:mm a", Locale.getDefault()).format(Date(calculatedEndTime))

    LaunchedEffect(isHolding) {
        if (isHolding) {
            val totalSteps = 60
            for (i in 1..totalSteps) {
                if (!isHolding) {
                    holdProgress = 0f
                    break
                }
                delay(50)
                holdProgress = i / totalSteps.toFloat()
            }
            if (isHolding && holdProgress >= 1f) {
                // Strict mode hold completed!
                val activeNames = availableLists.filter { selectedListIds.value.contains(it.id) }.map { it.name }
                onStartSession(
                    if (title.isBlank()) "Strict Focus ($effectiveMinutes min)" else title,
                    effectiveMinutes,
                    true,
                    activeNames,
                    autoLaunchMinimal
                )
            }
        } else {
            holdProgress = 0f
        }
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
                    .padding(24.dp)
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
                                .background(
                                    when (modeType) {
                                        SessionModeType.STRICT -> CrimsonStrict.copy(alpha = 0.15f)
                                        SessionModeType.NORMAL -> IndigoPrimary.copy(alpha = 0.15f)
                                    },
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = when (modeType) {
                                    SessionModeType.STRICT -> "🔒"
                                    SessionModeType.NORMAL -> "🛡️"
                                },
                                fontSize = 18.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "New Focus Session",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Mode Selector
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
                            .background(if (modeType == SessionModeType.NORMAL) IndigoPrimary else Color.Transparent)
                            .clickable { modeType = SessionModeType.NORMAL }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "🛡️ Standard Focus",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (modeType == SessionModeType.NORMAL) Color.White else Color(0xFF94A3B8)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (modeType == SessionModeType.STRICT) CrimsonStrict else Color.Transparent)
                            .clickable { modeType = SessionModeType.STRICT }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "🔒 Strict Mode Lock",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (modeType == SessionModeType.STRICT) Color.White else Color(0xFF94A3B8)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Session Name
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Session Goal / Name (Optional)") },
                    placeholder = { Text("e.g. Exam Study Sprint") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Duration Picker
                Text(
                    text = "Duration",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presetDurations.forEach { mins ->
                        val isSelected = !isCustomDuration && selectedMinutes == mins
                        val label = if (mins < 60) "${mins}m" else "${mins / 60}h"

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) {
                                if (isStrictMode) CrimsonStrict else IndigoPrimary
                            } else {
                                Color(0xFF1E293B)
                            },
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    isCustomDuration = false
                                    selectedMinutes = mins
                                }
                        ) {
                            Text(
                                text = label,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isSelected) Color.White else Color(0xFF94A3B8),
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }

                    // Custom duration chip
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isCustomDuration) {
                            if (isStrictMode) CrimsonStrict else IndigoPrimary
                        } else {
                            Color(0xFF1E293B)
                        },
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { isCustomDuration = true }
                    ) {
                        Text(
                            text = "Custom",
                            fontWeight = FontWeight.SemiBold,
                            color = if (isCustomDuration) Color.White else Color(0xFF94A3B8),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }

                if (isCustomDuration) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = customMinutesText,
                        onValueChange = { customMinutesText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Enter minutes (1 - 1440)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Block Lists to apply
                Text(
                    text = "Enforce Block Lists",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    availableLists.forEach { list ->
                        val isChecked = selectedListIds.value.contains(list.id)
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isChecked) Color(list.colorHex).copy(alpha = 0.2f) else Color(0xFF1E293B),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isChecked) Color(list.colorHex) else Color.Transparent
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    val current = selectedListIds.value.toMutableSet()
                                    if (isChecked) current.remove(list.id) else current.add(list.id)
                                    selectedListIds.value = current
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isChecked) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color(list.colorHex),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                Text(
                                    text = list.name,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isChecked) Color(list.colorHex) else Color(0xFF94A3B8)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Strict Mode Warning & Controls
                AnimatedVisibility(visible = isStrictMode) {
                    Column {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = CrimsonStrict.copy(alpha = 0.12f)
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CrimsonStrict.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = "Warning",
                                        tint = CrimsonStrict,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "STRICT MODE COMMITMENT",
                                        fontWeight = FontWeight.Bold,
                                        color = CrimsonStrict,
                                        fontSize = 13.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "• Cannot be stopped or ended early.\n• Enforces across device reboots.\n• Clock manipulation will not bypass timer.",
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp,
                                    color = Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Acknowledgment Checkbox
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { isAcknowledged = !isAcknowledged }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isAcknowledged,
                                onCheckedChange = { isAcknowledged = it },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = CrimsonStrict
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "I agree to lock until $endTimeString.",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Auto-launch Minimalist Launcher during session
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF1E293B))
                        .clickable { autoLaunchMinimal = !autoLaunchMinimal }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = autoLaunchMinimal,
                        onCheckedChange = { autoLaunchMinimal = it },
                        colors = CheckboxDefaults.colors(checkedColor = IndigoPrimary)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Launch Minimal Launcher during session",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Restricts home screen to your 5 custom essential apps",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Action Buttons
                if (isStrictMode) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (isAcknowledged) CrimsonStrict.copy(alpha = 0.2f) else Color(0xFF1E293B)
                                )
                                .border(
                                    1.dp,
                                    if (isAcknowledged) CrimsonStrict else Color.Transparent,
                                    RoundedCornerShape(14.dp)
                                )
                                .pointerInteropFilter { event ->
                                    if (!isAcknowledged) return@pointerInteropFilter false
                                    when (event.action) {
                                        MotionEvent.ACTION_DOWN -> {
                                            isHolding = true
                                            true
                                        }
                                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                                            isHolding = false
                                            true
                                        }
                                        else -> false
                                    }
                                }
                                .testTag("hold_strict_mode_button"),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(holdProgress)
                                    .height(54.dp)
                                    .background(CrimsonStrict)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = if (isAcknowledged) Color.White else Color(0xFF94A3B8),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (!isAcknowledged) "Check agreement above" else if (isHolding) "Locking in... (${(holdProgress * 100).toInt()}%)" else "HOLD 3 SECONDS TO LOCK",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (isAcknowledged) Color.White else Color(0xFF94A3B8)
                                )
                            }
                        }
                    }
                } else {
                    Button(
                        onClick = {
                            val activeNames = availableLists.filter { selectedListIds.value.contains(it.id) }.map { it.name }
                            onStartSession(
                                if (title.isBlank()) "Focus Session ($effectiveMinutes min)" else title,
                                effectiveMinutes,
                                false,
                                activeNames,
                                autoLaunchMinimal
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("start_normal_focus_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Start Focus Session ($effectiveMinutes min)",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}
