package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.BlockedTarget
import com.example.data.model.TargetType
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockedTargetDao {
    @Query("SELECT * FROM blocked_targets WHERE listId = :listId ORDER BY displayName ASC")
    fun getTargetsForList(listId: Long): Flow<List<BlockedTarget>>

    @Query("SELECT * FROM blocked_targets WHERE isEnabled = 1")
    suspend fun getAllEnabledTargets(): List<BlockedTarget>

    @Query("SELECT * FROM blocked_targets")
    fun getAllTargetsFlow(): Flow<List<BlockedTarget>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTarget(target: BlockedTarget): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTargets(targets: List<BlockedTarget>)

    @Update
    suspend fun updateTarget(target: BlockedTarget)

    @Delete
    suspend fun deleteTarget(target: BlockedTarget)

    @Query("DELETE FROM blocked_targets WHERE listId = :listId")
    suspend fun deleteAllTargetsForList(listId: Long)

    @Query("SELECT COUNT(*) FROM blocked_targets WHERE targetType = :targetType")
    fun countByType(targetType: TargetType): Flow<Int>

    @Query("DELETE FROM blocked_targets")
    suspend fun clearAll()
}
