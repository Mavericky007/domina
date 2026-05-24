package com.domina.cycle.data.repository

import com.domina.cycle.data.db.CycleEventDao
import com.domina.cycle.data.db.entity.CycleEventEntity
import com.domina.cycle.data.model.CycleEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

class RoomCycleRepository @Inject constructor(
    private val dao: CycleEventDao,
) : CycleRepository {
    override suspend fun add(event: CycleEvent) {
        dao.insert(CycleEventEntity(date = event.date.toString(), type = event.type.name))
    }
    override fun observeAll(): Flow<List<CycleEvent>> =
        dao.observeAll().map { list ->
            list.map { CycleEvent(it.id, LocalDate.parse(it.date), CycleEvent.Type.valueOf(it.type)) }
        }
}
