package com.example.teamb.data.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Injectable clock so time-dependent logic (streaks, timestamps) is unit-testable. */
interface Clock {
    fun nowMillis(): Long
}

class SystemClock : Clock {
    override fun nowMillis(): Long = System.currentTimeMillis()
}

object Dates {
    private const val DAY_MS = 86_400_000L

    fun epochDay(millis: Long): Long = millis / DAY_MS

    fun isoDate(millis: Long): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        fmt.timeZone = TimeZone.getTimeZone("UTC")
        return fmt.format(Date(millis))
    }

    /**
     * The seven ISO dates (Monday → Sunday, UTC) of the calendar week containing [millis].
     * Used to scope the weekly pulse graph to the current week.
     */
    fun currentWeekDates(millis: Long): List<String> {
        val cal = java.util.Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            firstDayOfWeek = java.util.Calendar.MONDAY
            timeInMillis = millis
        }
        // DAY_OF_WEEK: SUN=1..SAT=7 → days since Monday.
        val daysFromMonday = (cal.get(java.util.Calendar.DAY_OF_WEEK) + 5) % 7
        cal.add(java.util.Calendar.DAY_OF_MONTH, -daysFromMonday)
        return (0 until 7).map {
            val iso = isoDate(cal.timeInMillis)
            cal.add(java.util.Calendar.DAY_OF_MONTH, 1)
            iso
        }
    }

    /**
     * ISO dates from Monday of the current week through *today* (inclusive), UTC. Used so the
     * weekly pulse graph only spans elapsed days rather than padding out future days.
     */
    fun currentWeekToDate(millis: Long): List<String> {
        val full = currentWeekDates(millis)
        val today = isoDate(millis)
        val idx = full.indexOf(today)
        return if (idx >= 0) full.subList(0, idx + 1) else full
    }

    /** The last seven ISO dates ending today (today-6 … today), oldest first, UTC. */
    fun lastSevenDays(millis: Long): List<String> {
        val cal = java.util.Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = millis
            add(java.util.Calendar.DAY_OF_MONTH, -6)
        }
        return (0 until 7).map {
            val iso = isoDate(cal.timeInMillis)
            cal.add(java.util.Calendar.DAY_OF_MONTH, 1)
            iso
        }
    }

    private val WEEKDAY_INITIALS = listOf("M", "T", "W", "T", "F", "S", "S")

    /** Single-letter weekday initial (Mon→"M" … Sun→"S") for an ISO date; "?" if unparseable. */
    fun weekdayInitial(iso: String): String {
        val millis = runCatching {
            SimpleDateFormat("yyyy-MM-dd", Locale.US)
                .apply { timeZone = TimeZone.getTimeZone("UTC") }
                .parse(iso)?.time
        }.getOrNull() ?: return "?"
        val mondayIndex = (((epochDay(millis) + 3) % 7 + 7) % 7).toInt() // epoch day 0 = Thursday
        return WEEKDAY_INITIALS[mondayIndex]
    }
}

/** Pure consecutive-day streak logic. */
object StreakCalculator {
    /**
     * Current streak: the run of consecutive days ending at the most recent check-in,
     * provided that check-in is today or yesterday (otherwise the streak is broken → 0).
     */
    fun currentStreak(checkedInDays: Set<Long>, todayEpochDay: Long): Int {
        if (checkedInDays.isEmpty()) return 0
        val latest = checkedInDays.max()
        if (latest < todayEpochDay - 1) return 0
        var streak = 0
        var day = latest
        while (day in checkedInDays) {
            streak++
            day--
        }
        return streak
    }
}
