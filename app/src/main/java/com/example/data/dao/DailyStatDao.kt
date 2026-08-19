package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.DailyStat
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyStatDao {
    @Query("SELECT * FROM daily_stats ORDER BY dateString DESC LIMIT 14")
    fun getRecentStats(): Flow<List<DailyStat>>

    @Query("SELECT * FROM daily_stats WHERE dateString = :dateString")
    suspend fun getStatForDate(dateString: String): DailyStat?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(stat: DailyStat)

    @Query("UPDATE daily_stats SET blocksPreventedCount = blocksPreventedCount + 1 WHERE dateString = :dateString")
    suspend fun incrementBlockedCount(dateString: String)
}
