package com.domina.cycle.ui.tools.weight

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.domina.cycle.data.db.entity.WeightEntryEntity
import com.domina.cycle.data.repository.WeightRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class WeightViewModel @Inject constructor(
    private val repository: WeightRepository,
) : ViewModel() {
    val entries: StateFlow<List<WeightEntryEntity>> =
        repository.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun add(kg: Double, date: LocalDate = LocalDate.now()) {
        viewModelScope.launch { repository.add(date.toEpochDay(), kg) }
    }
    fun delete(id: Long) { viewModelScope.launch { repository.delete(id) } }
}
