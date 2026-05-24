package com.domina.cycle.ui.log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.domina.cycle.data.model.*
import com.domina.cycle.data.repository.DayLogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class LogViewModel @Inject constructor(
    private val repository: DayLogRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(DayLog(date = LocalDate.now()))
    val state: StateFlow<DayLog> = _state.asStateFlow()

    fun load(date: LocalDate) {
        viewModelScope.launch {
            _state.value = repository.getByDate(date) ?: DayLog(date = date)
        }
    }
    fun setMood(mood: Mood) { _state.update { it.copy(mood = mood) } }
    fun setFlow(flow: FlowIntensity) { _state.update { it.copy(flow = flow) } }
    fun setNote(note: String) { _state.update { it.copy(note = note) } }
    fun toggleSymptom(s: String) = _state.update {
        it.copy(symptoms = if (s in it.symptoms) it.symptoms - s else it.symptoms + s)
    }
    fun save() { viewModelScope.launch { repository.save(_state.value) } }
}
