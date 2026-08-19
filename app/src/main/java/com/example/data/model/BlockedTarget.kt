package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class TargetType {
    APP,
    WEBSITE,
    KEYWORD
}

@Entity(
    tableName = "blocked_targets",
    foreignKeys = [
        ForeignKey(
            entity = BlockList::class,
            parentColumns = ["id"],
            childColumns = ["listId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["listId"])]
)
data class BlockedTarget(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val listId: Long,
    val targetType: TargetType,
    val identifier: String, // package name (e.g. com.instagram.android) or domain (e.g. instagram.com) or keyword (e.g. reddit)
    val displayName: String,
    val category: String = "General",
    val isEnabled: Boolean = true
)
