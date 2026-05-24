package com.domina.cycle.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "contractions")
data class ContractionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startMillis: Long,
    val endMillis: Long,
)
