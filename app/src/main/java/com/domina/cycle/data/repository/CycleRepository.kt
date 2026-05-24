package com.domina.cycle.data.repository

import com.domina.cycle.data.model.CycleEvent
import kotlinx.coroutines.flow.Flow

interface CycleRepository {
    suspend fun add(event: CycleEvent)
    fun observeAll(): Flow<List<CycleEvent>>
}
