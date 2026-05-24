package com.domina.cycle.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.domina.cycle.data.db.entity.CycleEventEntity
import com.domina.cycle.data.db.entity.DayLogEntity
import com.domina.cycle.data.db.entity.MedicationEntity
import com.domina.cycle.data.db.entity.AppointmentEntity

@Database(
    entities = [DayLogEntity::class, CycleEventEntity::class,
        MedicationEntity::class,
        AppointmentEntity::class],
    version = 2,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dayLogDao(): DayLogDao
    abstract fun cycleEventDao(): CycleEventDao
    abstract fun medicationDao(): MedicationDao
    abstract fun appointmentDao(): AppointmentDao
}
