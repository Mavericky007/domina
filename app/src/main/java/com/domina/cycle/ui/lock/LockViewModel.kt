package com.domina.cycle.ui.lock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.domina.cycle.data.prefs.SettingsRepository
import com.domina.cycle.security.PinManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class LockViewModel @Inject constructor(
    private val settings: SettingsRepository,
) : ViewModel() {
    private val _unlocked = MutableStateFlow(false)
    val unlocked: StateFlow<Boolean> = _unlocked.asStateFlow()

    private val _error = MutableStateFlow(false)
    /** True once after a wrong PIN; the screen acks it via [clearError]. */
    val error: StateFlow<Boolean> = _error.asStateFlow()

    fun onPinEntered(pin: String) {
        viewModelScope.launch {
            val stored = settings.pinHash.first()
            if (stored == null) {
                val hash = withContext(Dispatchers.Default) { PinManager.hash(pin) }
                settings.setPinHash(hash)
                _unlocked.value = true
            } else {
                val ok = withContext(Dispatchers.Default) { PinManager.verify(pin, stored) }
                if (ok) _unlocked.value = true else _error.value = true
            }
        }
    }
    fun clearError() { _error.value = false }
    fun onBiometricSuccess() { _unlocked.value = true }
}
