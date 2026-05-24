package com.domina.cycle.data.db

import androidx.room.*
import com.domina.cycle.data.db.entity.WeightEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WeightEntryDao {
    @Insert suspend fun insert(e: WeightEntryEntity): Long
    @Query("SELECT * FROM weight_entries ORDER BY dateEpochDay") fun observeAll(): Flow<List<WeightEntryEntity>>
    @Query("DELETE FROM weight_entries WHERE id = :id") suspend fun deleteById(id: Long)

    @Query("SELECT * FROM weight_entries")
    suspend fun getAll(): List<WeightEntryEntity>

    @Query("DELETE FROM weight_entries")
    suspend fun clearAll()
}
