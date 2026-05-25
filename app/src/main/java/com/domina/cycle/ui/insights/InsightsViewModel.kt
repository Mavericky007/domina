package com.domina.cycle.ui.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.domina.cycle.data.db.entity.WeightEntryEntity
import com.domina.cycle.data.model.DayLog
import com.domina.cycle.data.model.FlowIntensity
import com.domina.cycle.data.repository.DayLogRepository
import com.domina.cycle.data.repository.WeightRepository
import com.domina.cycle.domain.insights.BbtAnalysis
import com.domina.cycle.domain.insights.CycleHistoryStats
import com.domina.cycle.domain.insights.MetricForecast
import com.domina.cycle.domain.insights.PatternInsights
import com.domina.cycle.domain.prediction.CyclePredictor
import com.domina.cycle.domain.prediction.PeriodDeriver
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class InsightsUiState(
    val cycleLengths: List<Int>,
    val averageCycle: Int,
    val shortest: Int,
    val longest: Int,
    val cycleLabels: List<String>,
    val bbtSeries: List<Float>,
    val bbtProjected: List<Float>,
    val bbtLabels: List<String>,
    val coverline: Float?,
    val weightSeries: List<Float>,
    val weightProjected: List<Float>,
    val weightLabels: List<String>,
    val insights: List<String>,
)

@HiltViewModel
class InsightsViewModel @Inject constructor(
    dayLogRepository: DayLogRepository,
    weightRepository: WeightRepository,
) : ViewModel() {
    private val today: LocalDate = LocalDate.now()

    val state: StateFlow<InsightsUiState> =
        combine(
            dayLogRepository.observeRange(today.minusDays(400), today),
            weightRepository.observeAll(),
        ) { logs, weights -> buildState(logs, weights) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), buildState(emptyList(), emptyList()))

    companion object {
        private val monthFmt = DateTimeFormatter.ofPattern("MMM")
        private val dayFmt = DateTimeFormatter.ofPattern("M/d")

        fun buildState(logs: List<DayLog>, weights: List<WeightEntryEntity>): InsightsUiState {
            val periods = PeriodDeriver.derive(logs.filter { it.flow != FlowIntensity.NONE }.map { it.date })
            val history = CycleHistoryStats.compute(periods)
            val avgCycle = if (history.average > 0) history.average else CyclePredictor.DEFAULT_CYCLE_LENGTH
            val starts = periods.sortedBy { it.start }.map { it.start }

            // ── BBT: observed + phase-based projection ─────────────────
            val bbtLogs = logs.filter { it.bbt != null }.sortedBy { it.date }
            val bbtSamples = bbtLogs.map { MetricForecast.Sample(it.date, it.bbt!!.toFloat()) }
            val bbtForecast = MetricForecast.forecast(bbtSamples, starts, avgCycle, avgCycle)

            // ── Weight: observed + phase-based projection ──────────────
            val sortedWeights = weights.sortedBy { it.dateEpochDay }
            val weightSamples = sortedWeights.map {
                MetricForecast.Sample(LocalDate.ofEpochDay(it.dateEpochDay), it.weightKg.toFloat())
            }
            val weightForecast = MetricForecast.forecast(weightSamples, starts, avgCycle, avgCycle)

            val cover = BbtAnalysis.detectCoverline(bbtLogs.map { it.bbt!! })

            return InsightsUiState(
                cycleLengths = history.cycleLengths,
                averageCycle = history.average,
                shortest = history.shortest,
                longest = history.longest,
                cycleLabels = starts.dropLast(1).map { it.format(monthFmt) },
                bbtSeries = bbtSamples.map { it.value },
                bbtProjected = bbtForecast.map { it.value },
                bbtLabels = (bbtSamples.map { it.date } + bbtForecast.map { it.date }).map { it.format(dayFmt) },
                coverline = cover.coverline?.toFloat(),
                weightSeries = weightSamples.map { it.value },
                weightProjected = weightForecast.map { it.value },
                weightLabels = (weightSamples.map { it.date } + weightForecast.map { it.date }).map { it.format(dayFmt) },
                insights = PatternInsights.generate(logs, periods, avgCycle),
            )
        }
    }
}
