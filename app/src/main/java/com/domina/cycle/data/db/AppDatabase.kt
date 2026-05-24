package com.domina.cycle.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.domina.cycle.data.db.entity.CycleEventEntity
import com.domina.cycle.data.db.entity.DayLogEntity

@Database(
    entities = [DayLogEntity::class, CycleEventEntity::class],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dayLogDao(): DayLogDao
    abstract fun cycleEventDao(): CycleEventDao
}
