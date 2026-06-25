package com.example.teamb.util

import com.example.teamb.data.util.StreakCalculator
import org.junit.Assert.assertEquals
import org.junit.Test

class StreakCalculatorTest {

    @Test
    fun empty_set_is_zero() {
        assertEquals(0, StreakCalculator.currentStreak(emptySet(), todayEpochDay = 100))
    }

    @Test
    fun single_check_in_today_is_one() {
        assertEquals(1, StreakCalculator.currentStreak(setOf(100L), todayEpochDay = 100))
    }

    @Test
    fun single_check_in_yesterday_keeps_streak_alive() {
        assertEquals(1, StreakCalculator.currentStreak(setOf(99L), todayEpochDay = 100))
    }

    @Test
    fun consecutive_run_ending_today_counts_all_days() {
        val days = setOf(96L, 97L, 98L, 99L, 100L)
        assertEquals(5, StreakCalculator.currentStreak(days, todayEpochDay = 100))
    }

    @Test
    fun consecutive_run_ending_yesterday_still_counts() {
        val days = setOf(97L, 98L, 99L)
        assertEquals(3, StreakCalculator.currentStreak(days, todayEpochDay = 100))
    }

    @Test
    fun missed_day_resets_to_run_after_the_gap() {
        // gap at 98; latest contiguous run from latest (100) back is 99,100
        val days = setOf(95L, 96L, 97L, 99L, 100L)
        assertEquals(2, StreakCalculator.currentStreak(days, todayEpochDay = 100))
    }

    @Test
    fun broken_when_latest_older_than_yesterday() {
        val days = setOf(95L, 96L, 97L)
        assertEquals(0, StreakCalculator.currentStreak(days, todayEpochDay = 100))
    }

    @Test
    fun future_check_in_does_not_break_current_run() {
        // latest is in the future (>today); still alive and counted contiguously
        val days = setOf(100L, 101L)
        assertEquals(2, StreakCalculator.currentStreak(days, todayEpochDay = 100))
    }
}
