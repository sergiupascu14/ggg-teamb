package com.example.teamb.util

import com.example.teamb.data.util.Dates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class CurrentWeekDatesTest {

    private val dayMs = 86_400_000L

    @Test
    fun returns_seven_consecutive_days_monday_to_sunday() {
        // 18628 epoch days = Fri 2021-01-01.
        val week = Dates.currentWeekDates(18628 * dayMs)

        assertEquals(7, week.size)
        assertEquals(week.sorted(), week)
        assertEquals(DayOfWeek.MONDAY, LocalDate.parse(week.first()).dayOfWeek)
        assertEquals(DayOfWeek.SUNDAY, LocalDate.parse(week.last()).dayOfWeek)

        week.zipWithNext().forEach { (a, b) ->
            assertEquals(LocalDate.parse(a).plusDays(1), LocalDate.parse(b))
        }
    }

    @Test
    fun week_contains_the_queried_day() {
        val millis = 18626 * dayMs // Wed 2020-12-30
        val week = Dates.currentWeekDates(millis)
        assertTrue(Dates.isoDate(millis) in week)
        assertEquals(DayOfWeek.MONDAY, LocalDate.parse(week.first()).dayOfWeek)
    }

    @Test
    fun sunday_belongs_to_the_week_that_started_the_prior_monday() {
        // 18630 epoch days = Sun 2021-01-03 → week of Mon 2020-12-28 .. Sun 2021-01-03.
        val week = Dates.currentWeekDates(18630 * dayMs)
        assertEquals("2020-12-28", week.first())
        assertEquals("2021-01-03", week.last())
    }

    @Test
    fun week_to_date_runs_monday_through_today_only() {
        // 18628 epoch days = Fri 2021-01-01 → Mon 2020-12-28 .. Fri 2021-01-01 (5 days).
        val toDate = Dates.currentWeekToDate(18628 * dayMs)
        assertEquals(5, toDate.size)
        assertEquals("2020-12-28", toDate.first())
        assertEquals("2021-01-01", toDate.last())
    }

    @Test
    fun week_to_date_is_just_monday_on_monday() {
        // 18631 epoch days = Mon 2021-01-04.
        val toDate = Dates.currentWeekToDate(18631 * dayMs)
        assertEquals(listOf("2021-01-04"), toDate)
    }

    @Test
    fun last_seven_days_ends_today_and_spans_a_week() {
        // 18628 epoch days = Fri 2021-01-01 → 2020-12-26 .. 2021-01-01.
        val days = Dates.lastSevenDays(18628 * dayMs)
        assertEquals(7, days.size)
        assertEquals("2020-12-26", days.first())
        assertEquals("2021-01-01", days.last())
        assertEquals(days.sorted(), days)
    }

    @Test
    fun weekday_initial_maps_each_day() {
        assertEquals("F", Dates.weekdayInitial("2021-01-01")) // Friday
        assertEquals("S", Dates.weekdayInitial("2021-01-02")) // Saturday
        assertEquals("S", Dates.weekdayInitial("2021-01-03")) // Sunday
        assertEquals("M", Dates.weekdayInitial("2021-01-04")) // Monday
        assertEquals("?", Dates.weekdayInitial("not-a-date"))
    }
}
