package com.domina.cycle.data.db

import androidx.room.*
import com.domina.cycle.data.db.entity.ChecklistItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChecklistItemDao {
    @Insert suspend fun insert(e: ChecklistItemEntity): Long
    @Insert suspend fun insertAll(items: List<ChecklistItemEntity>)
    @Query("SELECT * FROM checklist_items WHERE category = :category ORDER BY sortOrder, id")
    fun observeByCategory(category: String): Flow<List<ChecklistItemEntity>>
    @Query("UPDATE checklist_items SET checked = :checked WHERE id = :id") suspend fun setChecked(id: Long, checked: Boolean)
    @Query("DELETE FROM checklist_items WHERE id = :id") suspend fun deleteById(id: Long)
    @Query("SELECT COUNT(*) FROM checklist_items WHERE category = :category") suspend fun countByCategory(category: String): Int

    @Query("SELECT * FROM checklist_items")
    suspend fun getAll(): List<ChecklistItemEntity>

    @Query("DELETE FROM checklist_items")
    suspend fun clearAll()
}
