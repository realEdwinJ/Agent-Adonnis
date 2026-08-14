package com.adonnis.app.data.repository

import com.adonnis.app.data.local.dao.MemoryDao
import com.adonnis.app.data.local.entity.MemoryEntity
import kotlinx.coroutines.flow.Flow

/**
 * Repository for the AI's long-term memory of the user.
 * Facts learned from diary entries and chat are stored here and
 * injected into AI prompts so the assistant plans better over time.
 */
class MemoryRepository(
    private val memoryDao: MemoryDao
) {

    /** Flow of all memories, newest first */
    fun getAllMemories(): Flow<List<MemoryEntity>> = memoryDao.getAllMemories()

    /** All memories once */
    suspend fun getAllMemoriesOnce(): List<MemoryEntity> = memoryDao.getAllMemoriesOnce()

    /** Most recent N memories (for prompt injection) */
    suspend fun getRecentMemories(limit: Int = 20): List<MemoryEntity> =
        memoryDao.getRecentMemories(limit)

    /**
     * Remember a fact. Deduplicates: if an identical fact was already
     * learned from the same source, it is not stored again.
     */
    suspend fun remember(category: String, content: String, source: String) {
        if (content.isBlank()) return
        val existing = memoryDao.getAllMemoriesOnce()
        if (existing.any {
                it.category == category &&
                it.content.equals(content, ignoreCase = true)
            }
        ) return
        memoryDao.insert(MemoryEntity(category = category, content = content, source = source))
    }

    /** Delete a memory */
    suspend fun delete(memory: MemoryEntity) = memoryDao.delete(memory)

    /** Delete all memories */
    suspend fun deleteAll() = memoryDao.deleteAll()

    /** Count memories */
    suspend fun count(): Int = memoryDao.count()
}
