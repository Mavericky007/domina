package com.domina.cycle.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.domina.cycle.data.model.DayLog
import com.domina.cycle.data.repository.DayLogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class TodayViewModel @Inject constructor(
    repository: DayLogRepository,
) : ViewModel() {
    val today: LocalDate = LocalDate.now()
    val log: StateFlow<DayLog?> = flow { emit(repository.getByDate(today)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
}
