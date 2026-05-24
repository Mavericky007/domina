package com.domina.cycle.data.db

import androidx.room.*
import com.domina.cycle.data.db.entity.ContractionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ContractionDao {
    @Insert suspend fun insert(e: ContractionEntity): Long
    @Query("SELECT * FROM contractions ORDER BY startMillis DESC") fun observeAll(): Flow<List<ContractionEntity>>
    @Query("DELETE FROM contractions") suspend fun clear()

    @Query("SELECT * FROM contractions")
    suspend fun getAll(): List<ContractionEntity>

    @Query("DELETE FROM contractions")
    suspend fun clearAll()
}
