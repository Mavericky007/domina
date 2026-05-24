package com.domina.cycle.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cycle_events")
data class CycleEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,     // ISO yyyy-MM-dd
    val type: String,     // PERIOD_START / PERIOD_END
)
