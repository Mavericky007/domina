package com.domina.cycle.data.repository

import com.domina.cycle.data.db.ContractionDao
import com.domina.cycle.data.db.entity.ContractionEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

interface ContractionRepository {
    fun observeAll(): Flow<List<ContractionEntity>>
    suspend fun add(startMillis: Long, endMillis: Long): Long
    suspend fun clear()
}

class RoomContractionRepository @Inject constructor(private val dao: ContractionDao) : ContractionRepository {
    override fun observeAll() = dao.observeAll()
    override suspend fun add(startMillis: Long, endMillis: Long) =
        dao.insert(ContractionEntity(startMillis = startMillis, endMillis = endMillis))
    override suspend fun clear() = dao.clear()
}
