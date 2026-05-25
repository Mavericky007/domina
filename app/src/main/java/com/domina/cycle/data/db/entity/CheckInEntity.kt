package com.domina.cycle.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A single quick mood/energy/symptom check-in tapped from a notification. */
@Entity(tableName = "check_ins")
data class CheckInEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val epochDay: Long,
    val minuteOfDay: Int,
    val kind: String,
    val score: Int,
)
