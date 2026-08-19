package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_stats")
data class DailyStat(
    @PrimaryKey
    val dateString: String, // YYYY-MM-DD
    val totalFocusMinutes: Int = 0,
    val completedSessionsCount: Int = 0,
    val blocksPreventedCount: Int = 0
)
