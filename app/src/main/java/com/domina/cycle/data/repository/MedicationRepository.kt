package com.domina.cycle.data.repository

import com.domina.cycle.data.db.MedicationDao
import com.domina.cycle.data.db.entity.MedicationEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

interface MedicationRepository {
    fun observeAll(): Flow<List<MedicationEntity>>
    suspend fun add(name: String, timeMinutes: Int): Long
    suspend fun delete(id: Long)
}

class RoomMedicationRepository @Inject constructor(private val dao: MedicationDao) : MedicationRepository {
    override fun observeAll() = dao.observeAll()
    override suspend fun add(name: String, timeMinutes: Int) =
        dao.insert(MedicationEntity(name = name, timeMinutes = timeMinutes, enabled = true))
    override suspend fun delete(id: Long) = dao.deleteById(id)
}
