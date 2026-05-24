package com.domina.cycle.data.db

import androidx.room.*
import com.domina.cycle.data.db.entity.AppointmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppointmentDao {
    @Insert suspend fun insert(entity: AppointmentEntity): Long
    @Query("SELECT * FROM appointments ORDER BY atEpochMillis") fun observeAll(): Flow<List<AppointmentEntity>>
    @Query("DELETE FROM appointments WHERE id = :id") suspend fun deleteById(id: Long)
}
