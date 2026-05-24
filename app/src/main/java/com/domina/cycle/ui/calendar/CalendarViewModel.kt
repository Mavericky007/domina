package com.domina.cycle.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.domina.cycle.data.model.FlowIntensity
import com.domina.cycle.data.repository.DayLogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

data class CalendarUiState(
    val month: YearMonth,
    val cells: List<Int?>,
    val periodDays: Set<Int>,
    val today: Int?,           // day-of-month if today is in this month, else null
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val repository: DayLogRepository,
) : ViewModel() {
    private val month = MutableStateFlow(YearMonth.now())
    val visibleMonth: StateFlow<YearMonth> = month.asStateFlow()

    val state: StateFlow<CalendarUiState> = month.flatMapLatest { m ->
        repository.observeRange(m.atDay(1), m.atEndOfMonth()).map { logs ->
            val periodDays = logs.filter { it.flow != FlowIntensity.NONE }.map { it.date.dayOfMonth }.toSet()
            val today = LocalDate.now().takeIf { YearMonth.from(it) == m }?.dayOfMonth
            CalendarUiState(m, CalendarMonth.cells(m), periodDays, today)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000),
        CalendarUiState(YearMonth.now(), CalendarMonth.cells(YearMonth.now()), emptySet(), LocalDate.now().dayOfMonth))

    fun nextMonth() { month.value = month.value.plusMonths(1) }
    fun prevMonth() { month.value = month.value.minusMonths(1) }
}
