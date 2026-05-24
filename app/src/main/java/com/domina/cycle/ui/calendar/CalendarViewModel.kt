package com.domina.cycle.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.domina.cycle.data.model.DayLog
import com.domina.cycle.data.repository.DayLogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.ExperimentalCoroutinesApi

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val repository: DayLogRepository,
) : ViewModel() {
    private val month = MutableStateFlow(YearMonth.now())
    val visibleMonth: StateFlow<YearMonth> = month.asStateFlow()

    val logsThisMonth: StateFlow<List<DayLog>> = month.flatMapLatest { m ->
        repository.observeRange(m.atDay(1), m.atEndOfMonth())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun nextMonth() { month.value = month.value.plusMonths(1) }
    fun prevMonth() { month.value = month.value.minusMonths(1) }
}
