package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class PlantType(val displayName: String, val emoji: String, val requiredMinutes: Int, val description: String) {
    SPROUT("Bonsai Sprout", "🌱", 15, "A delicate green sprout that begins your journey"),
    SUCCULENT("Jade Succulent", "🪴", 25, "Hardy succulent that thrives in steady focus"),
    CHERRY_BLOSSOM("Cherry Blossom", "🌸", 45, "Graceful blooming sakura of serene concentration"),
    OAK_TREE("Mighty Oak", "🌳", 60, "A towering oak tree built with solid discipline"),
    GOLDEN_LOTUS("Golden Lotus", "🪷", 90, "Enlightened golden bloom for deep focus masters"),
    ANCIENT_REDWOOD("Ancient Redwood", "🌲", 120, "Legendary evergreen that withstands all digital distraction")
}

enum class PlantStatus {
    GROWING,
    BLOOMED,
    WITHERED
}

@Entity(tableName = "garden_plants")
data class GardenPlant(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val plantType: PlantType,
    val sessionMinutes: Int,
    val plantedAtMillis: Long,
    val completedAtMillis: Long? = null,
    val status: PlantStatus = PlantStatus.GROWING,
    val associatedSessionTitle: String = "Focus Session",
    val slotIndex: Int = 0
)
