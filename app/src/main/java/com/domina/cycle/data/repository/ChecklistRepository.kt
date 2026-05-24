package com.domina.cycle.data.repository

import com.domina.cycle.data.db.ChecklistItemDao
import com.domina.cycle.data.db.entity.ChecklistItemEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

interface ChecklistRepository {
    fun observeByCategory(category: String): Flow<List<ChecklistItemEntity>>
    suspend fun setChecked(id: Long, checked: Boolean)
    suspend fun add(category: String, text: String, sortOrder: Int): Long
    suspend fun delete(id: Long)
    suspend fun countByCategory(category: String): Int
}

class RoomChecklistRepository @Inject constructor(private val dao: ChecklistItemDao) : ChecklistRepository {
    override fun observeByCategory(category: String) = dao.observeByCategory(category)
    override suspend fun setChecked(id: Long, checked: Boolean) = dao.setChecked(id, checked)
    override suspend fun add(category: String, text: String, sortOrder: Int) =
        dao.insert(ChecklistItemEntity(category = category, text = text, checked = false, sortOrder = sortOrder))
    override suspend fun delete(id: Long) = dao.deleteById(id)
    override suspend fun countByCategory(category: String) = dao.countByCategory(category)
}
