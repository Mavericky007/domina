package com.domina.cycle.data.db

import androidx.room.*
import com.domina.cycle.data.db.entity.MedicationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicationDao {
    @Insert suspend fun insert(entity: MedicationEntity): Long
    @Query("SELECT * FROM medications ORDER BY timeMinutes") fun observeAll(): Flow<List<MedicationEntity>>
    @Query("DELETE FROM medications WHERE id = :id") suspend fun deleteById(id: Long)
}
