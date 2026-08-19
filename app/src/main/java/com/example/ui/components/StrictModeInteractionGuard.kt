package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CrimsonStrict

/**
 * CompositionLocal providing the current strict mode lock state across the Compose hierarchy.
 */
val LocalStrictModeActive = compositionLocalOf { false }

/**
 * Global Compose wrapper component that detects 'is_strict_active' state and dynamically
 * disables or guards interaction buttons and sensitive controls throughout the UI graph.
 */
@Composable
fun StrictModeInteractionGuard(
    isStrictActive: Boolean,
    remainingSeconds: Long = 0,
    onEmergencyUnlockRequested: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    var showLockedDialog by remember { mutableStateOf(false) }

    CompositionLocalProvider(LocalStrictModeActive provides isStrictActive) {
        Box(modifier = Modifier.testTag("strict_mode_guard_root")) {
            content()

            // Optional Top Floating Banner when strict mode is active
            AnimatedVisibility(
                visible = isStrictActive,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Surface(
                    color = CrimsonStrict.copy(alpha = 0.95f),
                    shape = RoundedCornerShape(12.dp),
                    tonalElevation = 6.dp,
                    shadowElevation = 8.dp,
                    modifier = Modifier.clickable { showLockedDialog = true }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Strict Mode Active",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Strict Mode Active • Interventions Locked",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        val minutes = remainingSeconds / 60
                        val secs = remainingSeconds % 60
                        Text(
                            text = String.format("%02d:%02d", minutes, secs),
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }

        if (showLockedDialog) {
            AlertDialog(
                onDismissRequest = { showLockedDialog = false },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = CrimsonStrict,
                        modifier = Modifier.size(28.dp)
                    )
                },
                title = {
                    Text(text = "Strict Mode Enforced", fontWeight = FontWeight.Bold)
                },
                text = {
                    Column {
                        Text(
                            text = "Modifications to rules, block lists, and schedules are currently disabled to prevent bypasses during your active focus block.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { showLockedDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonStrict)
                    ) {
                        Text("Dismiss")
                    }
                },
                dismissButton = {
                    if (onEmergencyUnlockRequested != null) {
                        TextButton(
                            onClick = {
                                showLockedDialog = false
                                onEmergencyUnlockRequested()
                            }
                        ) {
                            Text("Emergency Unlock", color = CrimsonStrict, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            )
        }
    }
}

/**
 * Modifier extension to conveniently guard any interactive component based on strict state.
 */
@Composable
fun Modifier.strictGuard(
    onClickWhenLocked: (() -> Unit)? = null
): Modifier {
    val isStrict = LocalStrictModeActive.current
    return if (isStrict) {
        this
            .alpha(0.5f)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { onClickWhenLocked?.invoke() }
            )
    } else {
        this
    }
}
