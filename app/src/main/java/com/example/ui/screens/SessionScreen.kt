package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.service.ActiveSessionState
import com.example.service.sound.FocusSoundEngine
import com.example.service.sound.SoundPreset
import com.example.ui.theme.AmberFocus
import com.example.ui.theme.CrimsonStrict
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IndigoPrimary
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.collectAsState
import com.example.data.auth.AuthManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SessionScreen(
    sessionState: ActiveSessionState,
    onBack: () -> Unit,
    onEndNormalSession: () -> Unit,
    onEmergencyUnlock: () -> Unit,
    onTransitionPomodoro: (nextIsBreak: Boolean, nextRound: Int, mins: Int) -> Unit = { _, _, _ -> }
) {
    val context = LocalContext.current
    val authManager = remember { AuthManager.getInstance(context) }
    val isDeveloperMode by authManager.isDeveloperMode.collectAsState()
    val dailyExitsLeft by authManager.dailyExitsRemaining.collectAsState()
    val canExitStrict = isDeveloperMode || dailyExitsLeft > 0

    var showUnlockConfirmDialog by remember { mutableStateOf(false) }

    // Ambient Soundscape state
    var selectedSound by remember { mutableStateOf(FocusSoundEngine.currentPreset) }
    var isSoundPlaying by remember { mutableStateOf(FocusSoundEngine.isPlaying) }
    var soundVolume by remember { mutableFloatStateOf(FocusSoundEngine.volume) }

    DisposableEffect(Unit) {
        onDispose {
            // Keep sound playing if user leaves to background or stops manually
        }
    }

    val totalSeconds = (sessionState.durationMinutes * 60L).coerceAtLeast(1)
    val elapsedSeconds = totalSeconds - sessionState.remainingSeconds
    val progress = (elapsedSeconds.toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f)

    val hours = sessionState.remainingSeconds / 3600
    val minutes = (sessionState.remainingSeconds % 3600) / 60
    val seconds = sessionState.remainingSeconds % 60
    val timeFormatted = if (hours > 0) {
        String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }

    val endFormatted = if (sessionState.endTimeMillis > 0) {
        SimpleDateFormat("EEE, hh:mm a", Locale.getDefault()).format(Date(sessionState.endTimeMillis))
    } else ""

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    listOf(
                        Color(0xFF0B1120),
                        if (sessionState.isStrictMode) Color(0xFF1C0E14) else if (sessionState.isPomodoroBreak) Color(0xFF064E3B) else Color(0xFF111A2E),
                        Color(0xFF0B1120)
                    )
                )
            )
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Top navigation bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text(
                text = if (sessionState.isPomodoro) "🍅 Pomodoro Interval" else "Live Focus Session",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.size(48.dp))
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Pomodoro Cycle Status Pill
        if (sessionState.isPomodoro) {
            Surface(
                color = if (sessionState.isPomodoroBreak) EmeraldSuccess.copy(alpha = 0.2f) else CrimsonStrict.copy(alpha = 0.2f),
                shape = RoundedCornerShape(100.dp),
                border = BorderStroke(1.dp, if (sessionState.isPomodoroBreak) EmeraldSuccess else CrimsonStrict)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (sessionState.isPomodoroBreak) "☕ RECOVERY BREAK (Round ${sessionState.pomodoroRound}/${sessionState.pomodoroTotalRounds})"
                        else "🍅 FOCUS SPRINT (Round ${sessionState.pomodoroRound}/${sessionState.pomodoroTotalRounds})",
                        color = if (sessionState.isPomodoroBreak) EmeraldSuccess else Color(0xFFE98BA0),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Strict Mode Banner / Badge
        if (sessionState.isStrictMode) {
            Surface(
                color = CrimsonStrict.copy(alpha = 0.15f),
                shape = RoundedCornerShape(100.dp),
                border = BorderStroke(1.dp, CrimsonStrict.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Lock",
                        tint = CrimsonStrict,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "STRICT MODE ACTIVE",
                        color = CrimsonStrict,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Enforced until $endFormatted",
                color = CrimsonStrict,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Large Circular Countdown Timer Gauge
        Box(
            modifier = Modifier
                .size(230.dp)
                .scale(if (sessionState.isStrictMode) pulseScale else 1f),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                progress = { 1f },
                modifier = Modifier.fillMaxSize(),
                color = Color(0xFF1D2A4A),
                strokeWidth = 14.dp
            )
            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxSize(),
                color = if (sessionState.isStrictMode) CrimsonStrict else if (sessionState.isPomodoroBreak) EmeraldSuccess else IndigoPrimary,
                strokeWidth = 14.dp,
                strokeCap = StrokeCap.Round
            )

            // Inner content
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = sessionState.plantType.emoji,
                    fontSize = 32.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = timeFormatted,
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    color = Color.White
                )
                Text(
                    text = "${(progress * 100).toInt()}% elapsed",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF94A3B8)
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Session Title & Guarding Info
        Text(
            text = sessionState.title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        if (sessionState.activeListNames.isNotBlank() && !sessionState.isPomodoroBreak) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Guarding: ${sessionState.activeListNames}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF94A3B8),
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Pomodoro Next Stage Action (if Pomodoro session)
        if (sessionState.isPomodoro) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF16223E)),
                border = BorderStroke(1.dp, Color(0xFF26355C)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .padding(14.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (sessionState.isPomodoroBreak) "Ready to resume focus?" else "Need a quick breather?",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = if (sessionState.isPomodoroBreak) "Start next 25m sprint" else "Take 5m safe break",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }

                    Button(
                        onClick = {
                            if (sessionState.isPomodoroBreak) {
                                onTransitionPomodoro(false, sessionState.pomodoroRound + 1, 25)
                            } else {
                                onTransitionPomodoro(true, sessionState.pomodoroRound, 5)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (sessionState.isPomodoroBreak) IndigoPrimary else EmeraldSuccess
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = if (sessionState.isPomodoroBreak) "Focus Sprint 🛡️" else "Take Break ☕",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (sessionState.isPomodoroBreak) Color.White else Color(0xFF052E21)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
        }

        // Ambient Focus Soundscapes Control Card (Feature 4)
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF16223E)),
            border = BorderStroke(1.dp, if (isSoundPlaying) CyanAccent.copy(alpha = 0.5f) else Color(0xFF26355C)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Headphones,
                            contentDescription = null,
                            tint = CyanAccent,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Ambient Soundscapes",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    }

                    if (isSoundPlaying) {
                        Surface(
                            color = CyanAccent.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "PLAYING 🎵",
                                color = CyanAccent,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Sound Preset Chips
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SoundPreset.values().forEach { preset ->
                        val isSelected = selectedSound == preset && isSoundPlaying
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) CyanAccent.copy(alpha = 0.2f) else Color(0xFF1D2A4A),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) CyanAccent else Color.Transparent
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    if (selectedSound == preset && isSoundPlaying) {
                                        FocusSoundEngine.stop()
                                        isSoundPlaying = false
                                    } else {
                                        selectedSound = preset
                                        FocusSoundEngine.play(preset, context)
                                        isSoundPlaying = true
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = preset.emoji, fontSize = 14.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = preset.displayName,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) CyanAccent else Color(0xFF94A3B8)
                                )
                            }
                        }
                    }
                }

                // Volume slider if sound is active
                if (isSoundPlaying) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Slider(
                            value = soundVolume,
                            onValueChange = {
                                soundVolume = it
                                FocusSoundEngine.setSoundVolume(it)
                            },
                            colors = SliderDefaults.colors(
                                thumbColor = CyanAccent,
                                activeTrackColor = CyanAccent,
                                inactiveTrackColor = Color(0xFF1D2A4A)
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Normal Mode End Button / Strict Mode Emergency Developer Unlock
        if (!sessionState.isStrictMode) {
            Button(
                onClick = {
                    FocusSoundEngine.stop()
                    onEndNormalSession()
                    onBack()
                },
                colors = ButtonDefaults.buttonColors(containerColor = CrimsonStrict.copy(alpha = 0.2f)),
                border = BorderStroke(1.dp, CrimsonStrict.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("end_normal_session_button")
            ) {
                Icon(imageVector = Icons.Default.Stop, contentDescription = null, tint = CrimsonStrict)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "End Session", color = CrimsonStrict, fontWeight = FontWeight.Bold)
            }
        } else {
            Column(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = { showUnlockConfirmDialog = true },
                    enabled = canExitStrict,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CrimsonStrict,
                        disabledContainerColor = Color(0xFF334155)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("session_emergency_unlock_button")
                ) {
                    Icon(imageVector = Icons.Default.LockOpen, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isDeveloperMode) "Developer Emergency Unlock (∞)" else if (canExitStrict) "Emergency Unlock (10 Exits/Day)" else "0 Exits Left (Limit Reached)",
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (isDeveloperMode)
                        "Developer Mode: Unlimited emergency exits enabled (∞)."
                    else if (dailyExitsLeft > 0)
                        "Uses 1 of your 10 daily exits ($dailyExitsLeft/10 remaining today). Resets at midnight."
                    else
                        "You have reached today's limit of 10 exits. Exits reset at midnight.",
                    fontSize = 11.sp,
                    color = if (canExitStrict) Color(0xFF94A3B8) else CrimsonStrict,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }

    if (showUnlockConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showUnlockConfirmDialog = false },
            icon = {
                Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = CrimsonStrict)
            },
            title = {
                Text("Emergency Unlock Session?", fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    text = if (isDeveloperMode)
                        "Developer Mode is active: Unlimited exits. This will end the Strict Mode session immediately and return you to the dashboard."
                    else
                        "This will use 1 of your 10 emergency exits for today ($dailyExitsLeft/10 remaining). The active Strict Mode block will be cancelled immediately."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        FocusSoundEngine.stop()
                        onEmergencyUnlock()
                        showUnlockConfirmDialog = false
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonStrict)
                ) {
                    Text(if (isDeveloperMode) "Unlock (∞ Dev)" else "Unlock Session")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUnlockConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
