package com.example.teamb.ui.dailypulse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.teamb.data.repository.DailyPulseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DailyPulseUiState(
    val loading: Boolean = true,
    val checkedInToday: Boolean = false,
    val streak: Int = 0,
    val submitting: Boolean = false,
)

/**
 * Drives the Daily Pulse screen. Holds the check-in status and streak so the composable
 * stays thin. All time/persistence logic lives in [DailyPulseRepository].
 */
class DailyPulseViewModel(
    private val repository: DailyPulseRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(DailyPulseUiState())
    val state: StateFlow<DailyPulseUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    /** Re-reads check-in status + streak from the repository. */
    fun refresh() {
        viewModelScope.launch {
            val checkedIn = repository.checkedInToday()
            val streak = repository.currentStreak()
            _state.value = _state.value.copy(
                loading = false,
                checkedInToday = checkedIn,
                streak = streak,
            )
        }
    }

    /** Submits today's mood (1..5) + optional note, then refreshes derived state. */
    fun submit(mood: Int, note: String?) {
        if (_state.value.submitting) return
        _state.value = _state.value.copy(submitting = true)
        viewModelScope.launch {
            repository.submit(mood, note)
            val streak = repository.currentStreak()
            _state.value = _state.value.copy(
                submitting = false,
                checkedInToday = true,
                streak = streak,
            )
        }
    }
}
