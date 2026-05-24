package com.domina.cycle.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.domina.cycle.data.db.entity.CycleEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CycleEventDao {
    @Insert suspend fun insert(entity: CycleEventEntity): Long

    @Query("SELECT * FROM cycle_events ORDER BY date")
    fun observeAll(): Flow<List<CycleEventEntity>>
}
