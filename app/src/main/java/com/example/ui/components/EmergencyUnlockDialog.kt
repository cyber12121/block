package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CrimsonStrict
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurface
import kotlinx.coroutines.delay

@Composable
fun EmergencyUnlockDialog(
    isUltraStrictActive: Boolean,
    dailyExitsLeft: Int,
    onDismiss: () -> Unit,
    onConfirmUnlock: () -> Unit
) {
    var reflectionSeconds by remember { mutableIntStateOf(if (isUltraStrictActive) 0 else 60) }

    LaunchedEffect(Unit) {
        if (!isUltraStrictActive) {
            while (reflectionSeconds > 0) {
                delay(1000)
                reflectionSeconds--
            }
        }
    }

    val canUnlock = !isUltraStrictActive && dailyExitsLeft > 0 && reflectionSeconds <= 0

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = if (isUltraStrictActive) Icons.Default.Lock else Icons.Default.Warning,
                contentDescription = null,
                tint = CrimsonStrict,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = if (isUltraStrictActive) "Ultra Strict Lock Active 🔒" else "Emergency Unlock Safety Gate",
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 18.sp
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (isUltraStrictActive) {
                    Text(
                        text = "This session was locked in Ultra Strict Mode. Early exit is completely disabled until the focus timer completes to ensure total distraction resistance.",
                        color = Color(0xFFCBD5E1),
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                } else if (dailyExitsLeft <= 0) {
                    Text(
                        text = "You have used all emergency exits for today (0/10 remaining). The active shield cannot be unlocked until the timer finishes.",
                        color = Color(0xFFF87171),
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                } else {
                    Surface(
                        color = CrimsonStrict.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, CrimsonStrict.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.LockOpen, contentDescription = null, tint = CrimsonStrict, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "$dailyExitsLeft / 10 Emergency Exits Left Today",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "A 60-second reflection pause enforces conscious decisions before quitting a focus session. Unlocking now will consume 1 daily exit quota.",
                        color = Color(0xFFCBD5E1),
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        },
        confirmButton = {
            if (!isUltraStrictActive && dailyExitsLeft > 0) {
                Button(
                    onClick = onConfirmUnlock,
                    enabled = canUnlock,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CrimsonStrict,
                        disabledContainerColor = CrimsonStrict.copy(alpha = 0.35f)
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = if (reflectionSeconds > 0) "Wait ${reflectionSeconds}s..." else "Confirm Emergency Unlock",
                        fontWeight = FontWeight.Bold,
                        color = if (canUnlock) Color.White else Color.LightGray
                    )
                }
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = "Keep Shield Active",
                    color = Color(0xFFCBD5E1)
                )
            }
        },
        containerColor = DarkSurface
    )
}
