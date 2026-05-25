package com.domina.cycle.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "day_logs")
data class DayLogEntity(
    @PrimaryKey val date: String,          // ISO yyyy-MM-dd
    val mood: String? = null,
    val energy: String? = null,
    val flow: String = "NONE",
    val symptoms: List<String> = emptyList(),
    val bbt: Double? = null,
    val cervicalMucus: String? = null,
    val lhResult: String = "NOT_TESTED",
    val libido: Int? = null,
    val sleepHours: Double? = null,
    val weight: Double? = null,
    val intimacy: String = "NONE",
    val emergencyContraception: Boolean = false,
    val pregnancyTest: String = "NOT_TESTED",
    val note: String = "",
)
