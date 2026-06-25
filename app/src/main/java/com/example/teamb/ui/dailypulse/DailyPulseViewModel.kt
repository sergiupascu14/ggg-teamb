package com.example.teamb.ui.dailypulse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.teamb.data.model.PulseRecord
import com.example.teamb.data.model.WeeklyPulse
import com.example.teamb.data.repository.DailyPulseRepository
import com.example.teamb.data.sync.PulseAggregator
import com.example.teamb.data.sync.PulseRepository
import com.example.teamb.data.util.Clock
import com.example.teamb.data.util.Dates
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DailyPulseUiState(
    val loading: Boolean = true,
    val checkedInToday: Boolean = false,
    val streak: Int = 0,
    val submitting: Boolean = false,
    val weekly: WeeklyPulse = WeeklyPulse(),
)

private data class PulseUser(val userId: String?, val building: String?, val floor: Int?)

/**
 * Drives the Daily Pulse screen: local check-in status + streak, plus the shared weekly pulse
 * (you / floor / company) read from [PulseRepository]. Submissions are written both locally and to
 * the shared store so the team-wide averages stay in sync.
 */
class DailyPulseViewModel(
    private val repository: DailyPulseRepository,
    private val pulseSync: PulseRepository,
    private val clock: Clock,
) : ViewModel() {

    private val _state = MutableStateFlow(DailyPulseUiState())
    val state: StateFlow<DailyPulseUiState> = _state.asStateFlow()

    private var user: PulseUser? = null
    private var weekJob: Job? = null

    init {
        refresh()
    }

    /** Re-reads local check-in status + streak. */
    fun refresh() {
        viewModelScope.launch {
            val checkedIn = repository.checkedInToday()
            val streak = repository.currentStreak()
            _state.update { it.copy(loading = false, checkedInToday = checkedIn, streak = streak) }
        }
    }

    /** Supplies the signed-in user's identity/location and starts observing the weekly pulse. */
    fun configure(userId: String?, building: String?, floor: Int?) {
        val next = PulseUser(userId, building, floor)
        if (user == next && weekJob != null) return
        user = next
        observeWeek()
    }

    private fun observeWeek() {
        weekJob?.cancel()
        val weekDates = Dates.currentWeekDates(clock.nowMillis())
        weekJob = viewModelScope.launch {
            pulseSync.observeWeek(weekDates).collect { records ->
                val u = user
                val weekly = PulseAggregator.weekly(records, weekDates, u?.userId, u?.building, u?.floor)
                _state.update { it.copy(weekly = weekly) }
            }
        }
    }

    /** Submits today's mood (1..5) + optional note locally, then mirrors it to the shared store. */
    fun submit(mood: Int, note: String?) {
        if (_state.value.submitting) return
        _state.update { it.copy(submitting = true) }
        viewModelScope.launch {
            repository.submit(mood, note)
            val now = clock.nowMillis()
            val u = user
            if (u?.userId != null) {
                pulseSync.submit(
                    PulseRecord(u.userId, Dates.isoDate(now), mood.coerceIn(1, 5), u.building, u.floor),
                    now,
                )
            }
            val streak = repository.currentStreak()
            _state.update { it.copy(submitting = false, checkedInToday = true, streak = streak) }
        }
    }
}
