package com.domina.cycle.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.domina.cycle.data.db.entity.DayLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DayLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: DayLogEntity)

    @Query("SELECT * FROM day_logs WHERE date = :date")
    suspend fun getByDate(date: String): DayLogEntity?

    @Query("SELECT * FROM day_logs WHERE date BETWEEN :start AND :end ORDER BY date")
    fun observeRange(start: String, end: String): Flow<List<DayLogEntity>>

    @Query("SELECT * FROM day_logs")
    suspend fun getAll(): List<DayLogEntity>

    @Query("DELETE FROM day_logs")
    suspend fun clearAll()
}
