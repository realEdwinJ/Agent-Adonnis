package com.adonnis.app.data.local.dao

import androidx.room.*
import com.adonnis.app.data.local.entity.MemoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryDao {

    /** All memories, newest first */
    @Query("SELECT * FROM memory_items ORDER BY created_at DESC")
    fun getAllMemories(): Flow<List<MemoryEntity>>

    /** All memories once (for prompt building) */
    @Query("SELECT * FROM memory_items ORDER BY created_at DESC")
    suspend fun getAllMemoriesOnce(): List<MemoryEntity>

    /** Most recent N memories */
    @Query("SELECT * FROM memory_items ORDER BY created_at DESC LIMIT :limit")
    suspend fun getRecentMemories(limit: Int): List<MemoryEntity>

    /** Insert a memory. Returns generated ID. */
    @Insert
    suspend fun insert(memory: MemoryEntity): Long

    /** Delete a memory */
    @Delete
    suspend fun delete(memory: MemoryEntity)

    /** Delete all memories */
    @Query("DELETE FROM memory_items")
    suspend fun deleteAll()

    /** Count memories */
    @Query("SELECT COUNT(*) FROM memory_items")
    suspend fun count(): Int
}
