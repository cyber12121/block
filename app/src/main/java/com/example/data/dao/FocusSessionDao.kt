package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.FocusSession
import kotlinx.coroutines.flow.Flow

@Dao
interface FocusSessionDao {
    @Query("SELECT * FROM focus_sessions ORDER BY startTimeMillis DESC")
    fun getAllSessions(): Flow<List<FocusSession>>

    @Query("SELECT * FROM focus_sessions WHERE isCompleted = 0 ORDER BY startTimeMillis DESC LIMIT 1")
    suspend fun getActiveSession(): FocusSession?

    @Query("SELECT * FROM focus_sessions WHERE isCompleted = 0 ORDER BY startTimeMillis DESC LIMIT 1")
    fun getActiveSessionFlow(): Flow<FocusSession?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: FocusSession): Long

    @Update
    suspend fun updateSession(session: FocusSession)

    @Query("SELECT SUM(durationMinutes) FROM focus_sessions WHERE isCompleted = 1")
    fun getTotalCompletedMinutes(): Flow<Int?>

    @Query("SELECT COUNT(*) FROM focus_sessions WHERE isCompleted = 1")
    fun getCompletedSessionsCount(): Flow<Int>

    @Query("SELECT SUM(blockedAttemptsCount) FROM focus_sessions")
    fun getTotalBlockedAttempts(): Flow<Int?>

    @Query("DELETE FROM focus_sessions")
    suspend fun clearAll()
}
