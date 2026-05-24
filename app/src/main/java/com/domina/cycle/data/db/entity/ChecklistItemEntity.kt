package com.domina.cycle.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "checklist_items")
data class ChecklistItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val category: String,   // "BAG" or "BIRTH_PLAN"
    val text: String,
    val checked: Boolean,
    val sortOrder: Int,
)
