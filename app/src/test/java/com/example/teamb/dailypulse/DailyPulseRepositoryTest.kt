package com.example.teamb.dailypulse

import com.example.teamb.data.repository.DailyPulseRepository
import com.example.teamb.data.util.Dates
import com.example.teamb.util.FakeClock
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyPulseRepositoryTest {

    private val dayMs = 86_400_000L

    /** Millis at start of UTC day [epochDay]. */
    private fun atDay(epochDay: Long): Long = epochDay * dayMs

    @Test
    fun submit_upserts_today_with_coerced_mood_and_blank_note_nulled() = runTest {
        val dao = FakeDailyPulseDao()
        val clock = FakeClock(atDay(18628) + 12 * 3_600_000L) // 2021-01-01 noon
        val repo = DailyPulseRepository(dao, clock)

        repo.submit(mood = 9, note = "   ")

        val today = Dates.isoDate(clock.now)
        val saved = dao.forDate(today)!!
        assertEquals("2021-01-01", saved.date)
        assertEquals(5, saved.mood) // coerced into 1..5
        assertNull(saved.note)      // blank → null
        assertEquals(clock.now, saved.createdAt)
    }

    @Test
    fun submit_keeps_nonblank_note() = runTest {
        val dao = FakeDailyPulseDao()
        val repo = DailyPulseRepository(dao, FakeClock(atDay(100)))
        repo.submit(3, "good day")
        assertEquals("good day", dao.forDate("1970-04-11")!!.note)
    }

    @Test
    fun checkedInToday_false_then_true_after_submit() = runTest {
        val dao = FakeDailyPulseDao()
        val repo = DailyPulseRepository(dao, FakeClock(atDay(18628)))
        assertFalse(repo.checkedInToday())
        repo.submit(4, null)
        assertTrue(repo.checkedInToday())
    }

    @Test
    fun checkedInToday_only_reflects_today_not_yesterday() = runTest {
        val dao = FakeDailyPulseDao()
        val clock = FakeClock(atDay(18628))
        val repo = DailyPulseRepository(dao, clock)
        repo.submit(3, null)                 // checked in on day 18628
        clock.now = atDay(18629)             // advance one day
        assertFalse(repo.checkedInToday())   // no entry for the new day
    }

    @Test
    fun currentStreak_counts_consecutive_days_ending_today() = runTest {
        val dao = FakeDailyPulseDao()
        val clock = FakeClock(atDay(18628))
        val repo = DailyPulseRepository(dao, clock)

        // Check in three consecutive days, advancing the clock each time.
        clock.now = atDay(18626); repo.submit(3, null)
        clock.now = atDay(18627); repo.submit(4, null)
        clock.now = atDay(18628); repo.submit(5, null)

        assertEquals(3, repo.currentStreak())
    }

    @Test
    fun currentStreak_resets_after_a_missed_day() = runTest {
        val dao = FakeDailyPulseDao()
        val clock = FakeClock(atDay(18628))
        val repo = DailyPulseRepository(dao, clock)

        clock.now = atDay(18625); repo.submit(3, null)
        clock.now = atDay(18626); repo.submit(3, null)
        // gap: no entry for 18627
        clock.now = atDay(18628); repo.submit(5, null)

        // only today's run (18628) is contiguous from latest
        assertEquals(1, repo.currentStreak())
    }

    @Test
    fun currentStreak_is_zero_when_latest_older_than_yesterday() = runTest {
        val dao = FakeDailyPulseDao()
        val clock = FakeClock(atDay(18626))
        val repo = DailyPulseRepository(dao, clock)

        clock.now = atDay(18624); repo.submit(3, null)
        clock.now = atDay(18626) // today, but last check-in was 2 days ago
        assertEquals(0, repo.currentStreak())
    }
}
