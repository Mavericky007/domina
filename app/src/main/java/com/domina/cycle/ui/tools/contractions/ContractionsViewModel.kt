package com.domina.cycle.ui.tools.contractions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.domina.cycle.data.db.entity.ContractionEntity
import com.domina.cycle.data.repository.ContractionRepository
import com.domina.cycle.domain.pregnancy.ContractionStats
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ContractionsViewModel @Inject constructor(
    private val repository: ContractionRepository,
) : ViewModel() {
    private val _running = MutableStateFlow(false)
    val running: StateFlow<Boolean> = _running.asStateFlow()
    private var startMillis = 0L

    val contractions: StateFlow<List<ContractionEntity>> =
        repository.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val stats: StateFlow<ContractionStats.Stats> = contractions
        .map { list -> ContractionStats.compute(list.map { ContractionStats.Contraction(it.startMillis, it.endMillis) }) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ContractionStats.Stats(0, 0, 0))

    fun toggle() {
        if (!_running.value) { startMillis = System.currentTimeMillis(); _running.value = true }
        else {
            val s = startMillis; val e = System.currentTimeMillis()
            viewModelScope.launch { repository.add(s, e) }
            _running.value = false
        }
    }
    fun clearAll() { viewModelScope.launch { repository.clear() } }
}
