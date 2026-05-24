package com.domina.cycle.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "appointments")
data class AppointmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val atEpochMillis: Long,
    val note: String = "",
    val leadHours: Int = 24,
    val enabled: Boolean = true,
)
