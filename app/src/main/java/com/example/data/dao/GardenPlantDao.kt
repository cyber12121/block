package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.GardenPlant
import com.example.data.model.PlantStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface GardenPlantDao {
    @Query("SELECT * FROM garden_plants ORDER BY plantedAtMillis DESC")
    fun getAllPlants(): Flow<List<GardenPlant>>

    @Query("SELECT * FROM garden_plants WHERE status = 'BLOOMED' ORDER BY completedAtMillis DESC")
    fun getBloomedPlants(): Flow<List<GardenPlant>>

    @Query("SELECT * FROM garden_plants WHERE status = 'GROWING' LIMIT 1")
    suspend fun getCurrentGrowingPlant(): GardenPlant?

    @Query("SELECT COUNT(*) FROM garden_plants WHERE status = 'BLOOMED'")
    fun getBloomedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM garden_plants WHERE status = 'WITHERED'")
    fun getWitheredCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlant(plant: GardenPlant): Long

    @Update
    suspend fun updatePlant(plant: GardenPlant)

    @Query("DELETE FROM garden_plants")
    suspend fun clearGarden()
}
