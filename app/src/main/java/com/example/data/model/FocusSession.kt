package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "focus_sessions")
data class FocusSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String = "Focus Session",
    val startTimeMillis: Long,
    val scheduledEndTimeMillis: Long,
    val actualEndTimeMillis: Long? = null,
    val durationMinutes: Int,
    val isStrictMode: Boolean,
    val isCompleted: Boolean = false,
    val wasEarlyUnlocked: Boolean = false,
    val blockedAttemptsCount: Int = 0,
    val activeListNames: String = "" // Comma-separated list names
)
