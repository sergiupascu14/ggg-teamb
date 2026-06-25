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

    /**
     * Supplies the signed-in user and (re)loads their check-in status, streak and the weekly
     * pulse. Check-ins are per user, so switching users reloads everything for that user.
     */
    fun configure(userId: String?, building: String?, floor: Int?) {
        val next = PulseUser(userId, building, floor)
        if (user == next && weekJob != null) return
        user = next
        loadLocal()
        observeWeek()
    }

    /** Re-reads this user's local check-in status + streak. */
    private fun loadLocal() {
        val uid = user?.userId
        viewModelScope.launch {
            if (uid == null) {
                _state.update { it.copy(loading = false, checkedInToday = false, streak = 0) }
                return@launch
            }
            val checkedIn = repository.checkedInToday(uid)
            val streak = repository.currentStreak(uid)
            _state.update { it.copy(loading = false, checkedInToday = checkedIn, streak = streak) }
        }
    }

    private fun observeWeek() {
        weekJob?.cancel()
        // Rolling last 7 days ending today.
        val weekDates = Dates.lastSevenDays(clock.nowMillis())
        weekJob = viewModelScope.launch {
            pulseSync.observeWeek(weekDates).collect { records ->
                val u = user
                val weekly = PulseAggregator.weekly(records, weekDates, u?.userId, u?.building, u?.floor)
                _state.update { it.copy(weekly = weekly) }
            }
        }
    }

    /**
     * Submits today's mood (1..5) + optional note for the current user, then mirrors it to the
     * shared store. Ignored if already submitting, already checked in today, or no user is set —
     * a user may only check in once per day.
     */
    fun submit(mood: Int, note: String?) {
        val uid = user?.userId
        if (_state.value.submitting || _state.value.checkedInToday || uid == null) return
        _state.update { it.copy(submitting = true) }
        viewModelScope.launch {
            val now = clock.nowMillis()
            repository.submit(uid, mood, note)
            pulseSync.submit(
                PulseRecord(uid, Dates.isoDate(now), mood.coerceIn(1, 5), user?.building, user?.floor),
                now,
            )
            val streak = repository.currentStreak(uid)
            _state.update { it.copy(submitting = false, checkedInToday = true, streak = streak) }
        }
    }
}
