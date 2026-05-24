package com.domina.cycle.data.repository

import com.domina.cycle.data.model.DayLog
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface DayLogRepository {
    suspend fun save(log: DayLog)
    suspend fun getByDate(date: LocalDate): DayLog?
    fun observeRange(start: LocalDate, end: LocalDate): Flow<List<DayLog>>
}
