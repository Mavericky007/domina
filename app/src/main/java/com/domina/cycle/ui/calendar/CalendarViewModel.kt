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
import kotlinx.coroutines.flow.*
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import javax.inject.Inject

enum class CalEventKind { PREDICTED_PERIOD, FERTILE, OVULATION, LOGGED }

data class CalendarEvent(
    val date: LocalDate?,   // null for month-level prediction summaries; set for tappable logged days
    val title: String,
    val detail: String,
    val kind: CalEventKind,
)

/** Per-month derived view, computed purely from the observed logs. */
data class CalendarUiState(
    val month: YearMonth,
    val cells: List<Int?>,
    val periodDays: Set<Int>,
    val predictedPeriodDays: Set<Int>,
    val fertileDays: Set<Int>,
    val ovulationDays: Set<Int>,
    val today: Int?,
    val events: List<CalendarEvent>,
)

@HiltViewModel
class CalendarViewModel @Inject constructor(
    repository: DayLogRepository,
) : ViewModel() {

    /** Raw history the UI pages over; one observation feeds every visible month. */
    data class CalendarData(val logs: List<DayLog>, val today: LocalDate)

    private val today = LocalDate.now()

    val data: StateFlow<CalendarData> =
        repository.observeRange(today.minusDays(400), today.plusDays(400))
            .map { CalendarData(it, today) }
            .stateIn(
                viewModelScope, SharingStarted.WhileSubscribed(5000),
                CalendarData(emptyList(), today),
            )

    companion object {
        private val dayFmt = DateTimeFormatter.ofPattern("MMM d")
        private val loggedFmt = DateTimeFormatter.ofPattern("EEE, MMM d")

        fun buildState(m: YearMonth, logs: List<DayLog>, today: LocalDate): CalendarUiState {
            val flowDates = logs.filter { it.flow != FlowIntensity.NONE }.map { it.date }
            val periods = PeriodDeriver.derive(flowDates)
            val pred = CyclePredictor.predict(periods, today)
            val marks = CycleProjection.marksFor(periods, m, pred.averageCycleLength, pred.averagePeriodLength, today)
            val loggedDays = flowDates.filter { YearMonth.from(it) == m }.map { it.dayOfMonth }.toSet()

            val events = buildList {
                if (marks.predictedPeriod.isNotEmpty()) {
                    add(CalendarEvent(null, "Predicted period",
                        rangeLabel(m, marks.predictedPeriod), CalEventKind.PREDICTED_PERIOD))
                }
                val fertileAll = marks.fertile + marks.ovulation
                if (fertileAll.isNotEmpty()) {
                    add(CalendarEvent(null, "Fertile window",
                        rangeLabel(m, fertileAll), CalEventKind.FERTILE))
                }
                marks.ovulation.minOrNull()?.let { d ->
                    add(CalendarEvent(m.atDay(d), "Ovulation", m.atDay(d).format(dayFmt), CalEventKind.OVULATION))
                }
                logs.filter { YearMonth.from(it.date) == m && !it.isEmpty() }
                    .sortedBy { it.date }
                    .forEach { add(CalendarEvent(it.date, it.date.format(loggedFmt), summarize(it), CalEventKind.LOGGED)) }
            }

            return CalendarUiState(
                month = m,
                cells = CalendarMonth.cells(m),
                periodDays = loggedDays,
                predictedPeriodDays = marks.predictedPeriod,
                fertileDays = marks.fertile,
                ovulationDays = marks.ovulation,
                today = today.takeIf { YearMonth.from(it) == m }?.dayOfMonth,
                events = events,
            )
        }

        private fun rangeLabel(m: YearMonth, days: Set<Int>): String {
            val lo = days.min(); val hi = days.max()
            return if (lo == hi) m.atDay(lo).format(dayFmt) else "${m.atDay(lo).format(dayFmt)} – ${hi}"
        }

        private fun summarize(log: DayLog): String {
            val parts = buildList {
                if (log.flow != FlowIntensity.NONE) add("${log.flow.name.lowercase()} flow")
                log.mood?.let { add(it.name.lowercase()) }
                if (log.symptoms.isNotEmpty()) add(log.symptoms.joinToString(", "))
                log.bbt?.let { add("BBT $it°") }
                if (log.note.isNotBlank()) add("note")
            }
            return parts.joinToString(" · ").ifEmpty { "logged" }
        }
    }
}
