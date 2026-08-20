package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
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

    /**
     * Atomic increment of blocksPreventedCount.
     * INSERT OR IGNORE ensures a row exists first, then UPDATE increments it.
     * This replaces the old check-then-act pattern that caused duplicate rows
     * when two accessibility events fired simultaneously.
     */
    @Query("INSERT OR IGNORE INTO daily_stats (dateString, totalFocusMinutes, completedSessionsCount, blocksPreventedCount) VALUES (:dateString, 0, 0, 0)")
    suspend fun insertRowIfMissing(dateString: String)

    @Query("UPDATE daily_stats SET blocksPreventedCount = blocksPreventedCount + 1 WHERE dateString = :dateString")
    suspend fun incrementBlockedCount(dateString: String)

    @Transaction
    suspend fun atomicIncrementBlockedCount(dateString: String) {
        insertRowIfMissing(dateString)
        incrementBlockedCount(dateString)
    }

    @Query("UPDATE daily_stats SET totalFocusMinutes = totalFocusMinutes + :minutes, completedSessionsCount = completedSessionsCount + 1 WHERE dateString = :dateString")
    suspend fun incrementSessionStats(dateString: String, minutes: Int)

    @Transaction
    suspend fun atomicRecordCompletedSession(dateString: String, minutes: Int) {
        insertRowIfMissing(dateString)
        incrementSessionStats(dateString, minutes)
    }
}
