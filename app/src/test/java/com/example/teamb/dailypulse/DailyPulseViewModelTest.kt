package com.example.teamb.dailypulse

import com.example.teamb.data.model.PulseRecord
import com.example.teamb.data.repository.DailyPulseRepository
import com.example.teamb.data.sync.InMemoryPulseRepository
import com.example.teamb.data.util.Dates
import com.example.teamb.ui.dailypulse.DailyPulseViewModel
import com.example.teamb.util.FakeClock
import com.example.teamb.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DailyPulseViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val dayMs = 86_400_000L
    private val now = 18628 * dayMs
    private val today = Dates.isoDate(now)

    private fun vm(
        dao: FakeDailyPulseDao = FakeDailyPulseDao(),
        pulse: InMemoryPulseRepository = InMemoryPulseRepository(),
    ) = DailyPulseViewModel(DailyPulseRepository(dao, FakeClock(now)), pulse, FakeClock(now))

    @Test
    fun configure_reflects_not_checked_in_for_fresh_user() = runTest {
        val vm = vm()
        vm.configure(userId = "me", building = "T", floor = 4)
        advanceUntilIdle()

        val s = vm.state.value
        assertFalse(s.loading)
        assertFalse(s.checkedInToday)
        assertEquals(0, s.streak)
    }

    @Test
    fun submit_marks_checked_in_and_updates_streak() = runTest {
        val vm = vm()
        vm.configure(userId = "me", building = "T", floor = 4)
        advanceUntilIdle()

        vm.submit(mood = 4, note = "fine")
        advanceUntilIdle()

        val s = vm.state.value
        assertTrue(s.checkedInToday)
        assertFalse(s.submitting)
        assertEquals(1, s.streak)
    }

    @Test
    fun submit_is_ignored_when_already_checked_in() = runTest {
        val pulse = InMemoryPulseRepository()
        val vm = vm(pulse = pulse)
        vm.configure(userId = "me", building = "T", floor = 4)
        advanceUntilIdle()

        vm.submit(mood = 4, note = null)
        advanceUntilIdle()
        // A second attempt must not overwrite the day's check-in.
        vm.submit(mood = 1, note = "changed my mind")
        advanceUntilIdle()

        val records = pulse.observeWeek(Dates.currentWeekDates(now)).first()
        assertEquals(1, records.size)
        assertEquals(4, records.first().mood) // still the first submission
    }

    @Test
    fun submit_is_ignored_without_a_user() = runTest {
        val pulse = InMemoryPulseRepository()
        val vm = vm(pulse = pulse)
        vm.configure(userId = null, building = null, floor = null)
        advanceUntilIdle()

        vm.submit(mood = 5, note = null)
        advanceUntilIdle()

        assertFalse(vm.state.value.checkedInToday)
        assertTrue(pulse.observeWeek(Dates.currentWeekDates(now)).first().isEmpty())
    }

    @Test
    fun configure_loads_weekly_company_and_floor_averages() = runTest {
        val pulse = InMemoryPulseRepository(
            seed = listOf(
                PulseRecord("mate", today, 3, "T", 4),
                PulseRecord("other", today, 1, "R", 3),
            )
        )
        val vm = vm(pulse = pulse)
        vm.configure(userId = "me", building = "T", floor = 4)
        advanceUntilIdle()

        val weekly = vm.state.value.weekly
        assertEquals(2.0, weekly.companyAverage!!, 0.001) // (3+1)/2
        assertEquals(3.0, weekly.floorAverage!!, 0.001)   // only the T4 record
        assertTrue(weekly.hasData)
    }

    @Test
    fun submit_syncs_mood_to_shared_store_and_weekly_you_average() = runTest {
        val pulse = InMemoryPulseRepository()
        val vm = vm(pulse = pulse)
        vm.configure(userId = "me", building = "T", floor = 4)
        advanceUntilIdle()

        vm.submit(mood = 5, note = null)
        advanceUntilIdle()

        val records = pulse.observeWeek(Dates.currentWeekDates(now)).first()
        assertEquals(1, records.size)
        assertEquals("me", records.first().userId)
        assertEquals(5, records.first().mood)
        assertEquals(5.0, vm.state.value.weekly.youAverage!!, 0.001)
    }
}
