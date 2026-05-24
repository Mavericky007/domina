package com.domina.cycle.data.repository

import com.domina.cycle.data.db.AppointmentDao
import com.domina.cycle.data.db.entity.AppointmentEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

interface AppointmentRepository {
    fun observeAll(): Flow<List<AppointmentEntity>>
    suspend fun add(title: String, atEpochMillis: Long, leadHours: Int): Long
    suspend fun delete(id: Long)
}

class RoomAppointmentRepository @Inject constructor(private val dao: AppointmentDao) : AppointmentRepository {
    override fun observeAll() = dao.observeAll()
    override suspend fun add(title: String, atEpochMillis: Long, leadHours: Int) =
        dao.insert(AppointmentEntity(title = title, atEpochMillis = atEpochMillis, leadHours = leadHours, enabled = true))
    override suspend fun delete(id: Long) = dao.deleteById(id)
}
