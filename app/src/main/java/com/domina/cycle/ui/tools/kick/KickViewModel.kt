package com.domina.cycle.ui.tools.kick

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.domina.cycle.data.db.entity.KickSessionEntity
import com.domina.cycle.data.repository.KickRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class KickViewModel @Inject constructor(
    private val repository: KickRepository,
) : ViewModel() {
    private val _count = MutableStateFlow(0)
    val count: StateFlow<Int> = _count.asStateFlow()
    private var startMillis: Long = 0L

    val sessions: StateFlow<List<KickSessionEntity>> =
        repository.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun kick() {
        if (_count.value == 0) startMillis = System.currentTimeMillis()
        _count.value += 1
    }
    fun save() {
        if (_count.value == 0) return
        val s = startMillis; val c = _count.value
        viewModelScope.launch { repository.add(s, System.currentTimeMillis(), c) }
        _count.value = 0
    }
    fun reset() { _count.value = 0 }
}
