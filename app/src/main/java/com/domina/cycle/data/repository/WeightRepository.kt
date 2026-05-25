package com.domina.cycle.data.repository

import com.domina.cycle.data.db.WeightEntryDao
import com.domina.cycle.data.db.entity.WeightEntryEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

interface WeightRepository {
    fun observeAll(): Flow<List<WeightEntryEntity>>
    suspend fun add(dateEpochDay: Long, kg: Double): Long
    suspend fun delete(id: Long)
    /** The weight logged for a day, if any. */
    suspend fun weightFor(dateEpochDay: Long): Double?
    /** One entry per day: replaces any existing entry for that date (or clears it when [kg] is null). */
    suspend fun setForDate(dateEpochDay: Long, kg: Double?)
}

class RoomWeightRepository @Inject constructor(private val dao: WeightEntryDao) : WeightRepository {
    override fun observeAll() = dao.observeAll()
    override suspend fun add(dateEpochDay: Long, kg: Double) =
        dao.insert(WeightEntryEntity(dateEpochDay = dateEpochDay, weightKg = kg))
    override suspend fun delete(id: Long) = dao.deleteById(id)
    override suspend fun weightFor(dateEpochDay: Long) = dao.weightForDate(dateEpochDay)
    override suspend fun setForDate(dateEpochDay: Long, kg: Double?) {
        dao.deleteByDate(dateEpochDay)
        if (kg != null) dao.insert(WeightEntryEntity(dateEpochDay = dateEpochDay, weightKg = kg))
    }
}
