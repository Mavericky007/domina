package com.domina.cycle.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.domina.cycle.data.db.entity.CycleEventEntity
import com.domina.cycle.data.db.entity.DayLogEntity
import com.domina.cycle.data.db.entity.MedicationEntity
import com.domina.cycle.data.db.entity.AppointmentEntity
import com.domina.cycle.data.db.entity.KickSessionEntity
import com.domina.cycle.data.db.entity.ContractionEntity
import com.domina.cycle.data.db.entity.WeightEntryEntity
import com.domina.cycle.data.db.entity.ChecklistItemEntity

@Database(
    entities = [DayLogEntity::class, CycleEventEntity::class,
        MedicationEntity::class,
        AppointmentEntity::class,
        KickSessionEntity::class,
        ContractionEntity::class,
        WeightEntryEntity::class,
        ChecklistItemEntity::class],
    version = 4,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dayLogDao(): DayLogDao
    abstract fun cycleEventDao(): CycleEventDao
    abstract fun medicationDao(): MedicationDao
    abstract fun appointmentDao(): AppointmentDao
    abstract fun kickSessionDao(): KickSessionDao
    abstract fun contractionDao(): ContractionDao
    abstract fun weightEntryDao(): WeightEntryDao
    abstract fun checklistItemDao(): ChecklistItemDao
}
