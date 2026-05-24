package com.domina.cycle.data.repository

import com.domina.cycle.data.db.WeightEntryDao
import com.domina.cycle.data.db.entity.WeightEntryEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

interface WeightRepository {
    fun observeAll(): Flow<List<WeightEntryEntity>>
    suspend fun add(dateEpochDay: Long, kg: Double): Long
    suspend fun delete(id: Long)
}

class RoomWeightRepository @Inject constructor(private val dao: WeightEntryDao) : WeightRepository {
    override fun observeAll() = dao.observeAll()
    override suspend fun add(dateEpochDay: Long, kg: Double) =
        dao.insert(WeightEntryEntity(dateEpochDay = dateEpochDay, weightKg = kg))
    override suspend fun delete(id: Long) = dao.deleteById(id)
}
