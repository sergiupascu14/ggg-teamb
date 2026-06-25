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
}
