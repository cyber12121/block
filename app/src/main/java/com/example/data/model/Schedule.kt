package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "schedules")
data class Schedule(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val startHour: Int,
    val startMinute: Int,
    val endHour: Int,
    val endMinute: Int,
    // Comma-separated calendar day constants (e.g., "2,3,4,5,6" for Mon-Fri)
    val daysOfWeek: String,
    val isStrictMode: Boolean = false,
    val activeListNames: String = "",
    val isEnabled: Boolean = true
)
