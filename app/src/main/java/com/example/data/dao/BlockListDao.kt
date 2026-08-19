package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.BlockList
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockListDao {
    @Query("SELECT * FROM block_lists ORDER BY isDefault DESC, name ASC")
    fun getAllLists(): Flow<List<BlockList>>

    @Query("SELECT * FROM block_lists WHERE id = :id")
    suspend fun getListById(id: Long): BlockList?

    @Query("SELECT * FROM block_lists WHERE isEnabled = 1")
    suspend fun getActiveLists(): List<BlockList>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertList(blockList: BlockList): Long

    @Update
    suspend fun updateList(blockList: BlockList)

    @Delete
    suspend fun deleteList(blockList: BlockList)
}
