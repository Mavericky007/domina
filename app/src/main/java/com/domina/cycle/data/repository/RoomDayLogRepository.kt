package com.domina.cycle.data.repository

import com.domina.cycle.data.db.DayLogDao
import com.domina.cycle.data.db.entity.DayLogEntity
import com.domina.cycle.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

class RoomDayLogRepository @Inject constructor(
    private val dao: DayLogDao,
) : DayLogRepository {

    override suspend fun save(log: DayLog) = dao.upsert(log.toEntity())

    override suspend fun getByDate(date: LocalDate): DayLog? =
        dao.getByDate(date.toString())?.toModel()

    override fun observeRange(start: LocalDate, end: LocalDate): Flow<List<DayLog>> =
        dao.observeRange(start.toString(), end.toString()).map { list -> list.map { it.toModel() } }
}

private fun DayLog.toEntity() = DayLogEntity(
    date = date.toString(),
    mood = mood?.name,
    energy = energy?.name,
    flow = flow.name,
    symptoms = symptoms,
    bbt = bbt,
    cervicalMucus = cervicalMucus?.name,
    lhResult = lhResult.name,
    libido = libido,
    sleepHours = sleepHours,
    weight = weight,
    intimacy = intimacy.name,
    emergencyContraception = emergencyContraception,
    note = note,
)

private fun DayLogEntity.toModel() = DayLog(
    date = LocalDate.parse(date),
    mood = mood?.let { Mood.valueOf(it) },
    energy = energy?.let { Energy.valueOf(it) },
    flow = FlowIntensity.valueOf(flow),
    symptoms = symptoms,
    bbt = bbt,
    cervicalMucus = cervicalMucus?.let { CervicalMucus.valueOf(it) },
    lhResult = LhResult.valueOf(lhResult),
    libido = libido,
    sleepHours = sleepHours,
    weight = weight,
    intimacy = Intimacy.valueOf(intimacy),
    emergencyContraception = emergencyContraception,
    note = note,
)
