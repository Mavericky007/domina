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
import com.domina.cycle.domain.insights.PatternInsights
import com.domina.cycle.domain.prediction.CyclePredictor
import com.domina.cycle.domain.prediction.PeriodDeriver
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import java.time.LocalDate
import javax.inject.Inject

data class InsightsUiState(
    val cycleLengths: List<Int>,
    val averageCycle: Int,
    val shortest: Int,
    val longest: Int,
    val bbtSeries: List<Float>,
    val coverline: Float?,
    val weightSeries: List<Float>,
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
        fun buildState(logs: List<DayLog>, weights: List<WeightEntryEntity>): InsightsUiState {
            val periods = PeriodDeriver.derive(logs.filter { it.flow != FlowIntensity.NONE }.map { it.date })
            val history = CycleHistoryStats.compute(periods)
            val avgCycle = if (history.average > 0) history.average else CyclePredictor.DEFAULT_CYCLE_LENGTH
            val bbt = logs.filter { it.bbt != null }.sortedBy { it.date }.map { it.bbt!! }
            val cover = BbtAnalysis.detectCoverline(bbt)
            return InsightsUiState(
                cycleLengths = history.cycleLengths,
                averageCycle = history.average,
                shortest = history.shortest,
                longest = history.longest,
                bbtSeries = bbt.map { it.toFloat() },
                coverline = cover.coverline?.toFloat(),
                weightSeries = weights.sortedBy { it.dateEpochDay }.map { it.weightKg.toFloat() },
                insights = PatternInsights.generate(logs, periods, avgCycle),
            )
        }
    }
}
