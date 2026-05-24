package com.domina.cycle.data.db

import androidx.room.*
import com.domina.cycle.data.db.entity.KickSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface KickSessionDao {
    @Insert suspend fun insert(e: KickSessionEntity): Long
    @Query("SELECT * FROM kick_sessions ORDER BY startMillis DESC") fun observeAll(): Flow<List<KickSessionEntity>>
    @Query("DELETE FROM kick_sessions WHERE id = :id") suspend fun deleteById(id: Long)

    @Query("SELECT * FROM kick_sessions")
    suspend fun getAll(): List<KickSessionEntity>

    @Query("DELETE FROM kick_sessions")
    suspend fun clearAll()
}
