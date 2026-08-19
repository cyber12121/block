package com.example.ui.components

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
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.IndigoPrimary

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ClockTimePickerDialog(
    title: String,
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onTimeSelected: (hour: Int, minute: Int) -> Unit
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = false
    )
    var showKeyboardInput by remember { mutableStateOf(false) }

    val presets = listOf(
        Pair("08:00 AM", Pair(8, 0)),
        Pair("09:00 AM", Pair(9, 0)),
        Pair("01:00 PM", Pair(13, 0)),
        Pair("05:00 PM", Pair(17, 0)),
        Pair("10:30 PM", Pair(22, 30)),
        Pair("07:00 AM", Pair(7, 0))
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = Color(0xFF16223E),
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(28.dp))
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header with Title & Mode Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(IndigoPrimary.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = null,
                                tint = CyanAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    IconButton(
                        onClick = { showKeyboardInput = !showKeyboardInput }
                    ) {
                        Icon(
                            imageVector = if (showKeyboardInput) Icons.Default.AccessTime else Icons.Default.Keyboard,
                            contentDescription = "Toggle Input Mode",
                            tint = CyanAccent
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Interactive Material 3 Clock Dial / Time Picker
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    if (showKeyboardInput) {
                        TimeInput(
                            state = timePickerState,
                            colors = TimePickerDefaults.colors(
                                clockDialColor = Color(0xFF1D2A4A),
                                clockDialSelectedContentColor = Color.White,
                                clockDialUnselectedContentColor = Color(0xFF94A3B8),
                                selectorColor = IndigoPrimary,
                                containerColor = Color(0xFF1D2A4A),
                                periodSelectorBorderColor = IndigoPrimary,
                                periodSelectorSelectedContainerColor = IndigoPrimary,
                                periodSelectorUnselectedContainerColor = Color(0xFF1D2A4A),
                                periodSelectorSelectedContentColor = Color.White,
                                periodSelectorUnselectedContentColor = Color(0xFF94A3B8),
                                timeSelectorSelectedContainerColor = IndigoPrimary,
                                timeSelectorUnselectedContainerColor = Color(0xFF1D2A4A),
                                timeSelectorSelectedContentColor = Color.White,
                                timeSelectorUnselectedContentColor = Color(0xFF94A3B8)
                            )
                        )
                    } else {
                        TimePicker(
                            state = timePickerState,
                            colors = TimePickerDefaults.colors(
                                clockDialColor = Color(0xFF1D2A4A),
                                clockDialSelectedContentColor = Color.White,
                                clockDialUnselectedContentColor = Color(0xFFCBD5E1),
                                selectorColor = IndigoPrimary,
                                containerColor = Color(0xFF1D2A4A),
                                periodSelectorBorderColor = IndigoPrimary,
                                periodSelectorSelectedContainerColor = IndigoPrimary,
                                periodSelectorUnselectedContainerColor = Color(0xFF1D2A4A),
                                periodSelectorSelectedContentColor = Color.White,
                                periodSelectorUnselectedContentColor = Color(0xFF94A3B8),
                                timeSelectorSelectedContainerColor = IndigoPrimary,
                                timeSelectorUnselectedContainerColor = Color(0xFF1D2A4A),
                                timeSelectorSelectedContentColor = Color.White,
                                timeSelectorUnselectedContentColor = Color(0xFFCBD5E1)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Quick Presets Chips
                Text(
                    text = "Quick Presets",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF94A3B8),
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presets.forEach { (label, hourMin) ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF1D2A4A),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    onTimeSelected(hourMin.first, hourMin.second)
                                }
                        ) {
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = CyanAccent,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Dialog Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Color(0xFF94A3B8))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onTimeSelected(timePickerState.hour, timePickerState.minute)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Set Time", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
