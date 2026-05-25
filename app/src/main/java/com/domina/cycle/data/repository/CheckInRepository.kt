package com.domina.cycle.data.repository

import com.domina.cycle.data.db.CheckInDao
import com.domina.cycle.data.db.entity.CheckInEntity
import com.domina.cycle.domain.checkin.CheckIn
import com.domina.cycle.domain.checkin.CheckInKind
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

interface CheckInRepository {
    fun observeSince(startEpochDay: Long): Flow<List<CheckIn>>
    suspend fun add(epochDay: Long, minuteOfDay: Int, kind: CheckInKind, score: Int)
}

class RoomCheckInRepository @Inject constructor(private val dao: CheckInDao) : CheckInRepository {
    override fun observeSince(startEpochDay: Long): Flow<List<CheckIn>> =
        dao.observeSince(startEpochDay).map { list ->
            list.map { CheckIn(it.epochDay, CheckInKind.valueOf(it.kind), it.score) }
        }

    override suspend fun add(epochDay: Long, minuteOfDay: Int, kind: CheckInKind, score: Int) {
        dao.insert(CheckInEntity(epochDay = epochDay, minuteOfDay = minuteOfDay, kind = kind.name, score = score))
    }
}
