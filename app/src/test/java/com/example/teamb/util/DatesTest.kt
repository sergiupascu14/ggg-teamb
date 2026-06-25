package com.example.teamb.util

import com.example.teamb.data.util.Dates
import org.junit.Assert.assertEquals
import org.junit.Test

class DatesTest {

    private val dayMs = 86_400_000L

    @Test
    fun isoDate_epoch_is_1970_01_01() {
        assertEquals("1970-01-01", Dates.isoDate(0L))
    }

    @Test
    fun isoDate_known_millis_in_utc() {
        // 2021-01-01T00:00:00Z == 1609459200000 ms
        assertEquals("2021-01-01", Dates.isoDate(1_609_459_200_000L))
    }

    @Test
    fun isoDate_just_before_midnight_stays_same_day_utc() {
        // 2021-01-01T23:59:59Z
        assertEquals("2021-01-01", Dates.isoDate(1_609_459_200_000L + dayMs - 1000L))
    }

    @Test
    fun epochDay_of_epoch_is_zero() {
        assertEquals(0L, Dates.epochDay(0L))
    }

    @Test
    fun epochDay_within_first_day_is_zero() {
        assertEquals(0L, Dates.epochDay(dayMs - 1L))
    }

    @Test
    fun epochDay_advances_one_per_day() {
        assertEquals(1L, Dates.epochDay(dayMs))
        assertEquals(10L, Dates.epochDay(10L * dayMs))
    }

    @Test
    fun epochDay_matches_known_date() {
        // 2021-01-01 == epoch day 18628
        assertEquals(18628L, Dates.epochDay(1_609_459_200_000L))
    }
}
