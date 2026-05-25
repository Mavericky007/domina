package com.domina.cycle.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.domina.cycle.data.db.entity.CheckInEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CheckInDao {
    @Insert suspend fun insert(e: CheckInEntity): Long

    @Query("SELECT * FROM check_ins WHERE epochDay >= :startEpochDay ORDER BY epochDay, minuteOfDay")
    fun observeSince(startEpochDay: Long): Flow<List<CheckInEntity>>

    @Query("SELECT * FROM check_ins") suspend fun getAll(): List<CheckInEntity>
    @Query("DELETE FROM check_ins") suspend fun clearAll()
}
