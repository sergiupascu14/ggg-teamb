package com.example.teamb.dailypulse

import com.example.teamb.data.repository.DailyPulseRepository
import com.example.teamb.ui.dailypulse.DailyPulseViewModel
import com.example.teamb.util.FakeClock
import com.example.teamb.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

    @Test
    fun initial_refresh_reflects_not_checked_in() = runTest {
        val dao = FakeDailyPulseDao()
        val repo = DailyPulseRepository(dao, FakeClock(18628 * dayMs))
        val vm = DailyPulseViewModel(repo)

        advanceUntilIdle()

        val s = vm.state.value
        assertFalse(s.loading)
        assertFalse(s.checkedInToday)
        assertEquals(0, s.streak)
    }

    @Test
    fun submit_marks_checked_in_and_updates_streak() = runTest {
        val dao = FakeDailyPulseDao()
        val repo = DailyPulseRepository(dao, FakeClock(18628 * dayMs))
        val vm = DailyPulseViewModel(repo)
        advanceUntilIdle()

        vm.submit(mood = 4, note = "fine")
        advanceUntilIdle()

        val s = vm.state.value
        assertTrue(s.checkedInToday)
        assertFalse(s.submitting)
        assertEquals(1, s.streak)
    }
}
