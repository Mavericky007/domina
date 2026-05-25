package com.domina.cycle.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.domina.cycle.data.model.DayLog
import com.domina.cycle.data.model.FlowIntensity
import com.domina.cycle.data.repository.DayLogRepository
import com.domina.cycle.domain.prediction.CyclePredictor
import com.domina.cycle.domain.prediction.CycleProjection
import com.domina.cycle.domain.prediction.PeriodDeriver
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

data class CalendarUiState(
    val month: YearMonth,
    val cells: List<Int?>,
    val periodDays: Set<Int>,           // logged flow days
    val predictedPeriodDays: Set<Int>,  // projected upcoming period
    val fertileDays: Set<Int>,
    val ovulationDays: Set<Int>,
    val today: Int?,                    // day-of-month if today is in this month, else null
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val repository: DayLogRepository,
) : ViewModel() {
    private val month = MutableStateFlow(YearMonth.now())
    val visibleMonth: StateFlow<YearMonth> = month.asStateFlow()

    val state: StateFlow<CalendarUiState> = month.flatMapLatest { m ->
        val today = LocalDate.now()
        val rangeEnd = maxOf(m.atEndOfMonth(), today)
        repository.observeRange(today.minusDays(400), rangeEnd).map { logs -> buildState(m, logs, today) }
    }.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000),
        buildState(YearMonth.now(), emptyList(), LocalDate.now()),
    )

    fun nextMonth() { month.value = month.value.plusMonths(1) }
    fun prevMonth() { month.value = month.value.minusMonths(1) }

    companion object {
        fun buildState(m: YearMonth, logs: List<DayLog>, today: LocalDate): CalendarUiState {
            val flowDates = logs.filter { it.flow != FlowIntensity.NONE }.map { it.date }
            val periods = PeriodDeriver.derive(flowDates)
            val pred = CyclePredictor.predict(periods, today)
            val marks = CycleProjection.marksFor(periods, m, pred.averageCycleLength, pred.averagePeriodLength, today)
            val loggedDays = flowDates.filter { YearMonth.from(it) == m }.map { it.dayOfMonth }.toSet()
            return CalendarUiState(
                month = m,
                cells = CalendarMonth.cells(m),
                periodDays = loggedDays,
                predictedPeriodDays = marks.predictedPeriod,
                fertileDays = marks.fertile,
                ovulationDays = marks.ovulation,
                today = today.takeIf { YearMonth.from(it) == m }?.dayOfMonth,
            )
        }
    }
}
