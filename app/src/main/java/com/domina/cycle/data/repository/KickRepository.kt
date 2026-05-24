package com.domina.cycle.data.repository

import com.domina.cycle.data.db.KickSessionDao
import com.domina.cycle.data.db.entity.KickSessionEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

interface KickRepository {
    fun observeAll(): Flow<List<KickSessionEntity>>
    suspend fun add(startMillis: Long, endMillis: Long, count: Int): Long
    suspend fun delete(id: Long)
}

class RoomKickRepository @Inject constructor(private val dao: KickSessionDao) : KickRepository {
    override fun observeAll() = dao.observeAll()
    override suspend fun add(startMillis: Long, endMillis: Long, count: Int) =
        dao.insert(KickSessionEntity(startMillis = startMillis, endMillis = endMillis, count = count))
    override suspend fun delete(id: Long) = dao.deleteById(id)
}
