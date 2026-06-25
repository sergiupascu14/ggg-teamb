package com.example.teamb.sync

import com.example.teamb.data.model.PulseRecord
import com.example.teamb.data.sync.PulseAggregator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PulseAggregatorTest {

    private val week = listOf("2021-01-04", "2021-01-05", "2021-01-06", "2021-01-07", "2021-01-08", "2021-01-09", "2021-01-10")

    private fun rec(user: String, date: String, mood: Int, building: String? = "T", floor: Int? = 4) =
        PulseRecord(user, date, mood, building, floor)

    @Test
    fun company_average_is_mean_of_all_records_in_week() {
        val records = listOf(
            rec("a", "2021-01-04", 4),
            rec("b", "2021-01-04", 2),
            rec("c", "2021-01-05", 3),
        )
        val weekly = PulseAggregator.weekly(records, week, you = "a", building = "T", floor = 4)
        assertEquals(3.0, weekly.companyAverage!!, 0.001) // (4+2+3)/3
    }

    @Test
    fun per_day_series_splits_you_floor_company() {
        val records = listOf(
            rec("me", "2021-01-04", 5, "T", 4),
            rec("mate", "2021-01-04", 3, "T", 4),     // same floor
            rec("other", "2021-01-04", 1, "R", 3),    // different floor
        )
        val weekly = PulseAggregator.weekly(records, week, you = "me", building = "T", floor = 4)
        val monday = weekly.days.first { it.date == "2021-01-04" }
        assertEquals(5.0, monday.youMood!!, 0.001)
        assertEquals(4.0, monday.floorAverage!!, 0.001)   // (5+3)/2
        assertEquals(3.0, monday.companyAverage!!, 0.001) // (5+3+1)/3
    }

    @Test
    fun floor_average_is_null_when_location_unknown() {
        val records = listOf(rec("me", "2021-01-04", 5, null, null))
        val weekly = PulseAggregator.weekly(records, week, you = "me", building = null, floor = null)
        assertNull(weekly.floorAverage)
        assertNull(weekly.days.first().floorAverage)
        assertEquals(5.0, weekly.companyAverage!!, 0.001)
    }

    @Test
    fun records_outside_week_are_ignored() {
        val records = listOf(
            rec("a", "2021-01-04", 4),
            rec("a", "2020-12-31", 1), // previous week
        )
        val weekly = PulseAggregator.weekly(records, week, you = "a", building = "T", floor = 4)
        assertEquals(4.0, weekly.companyAverage!!, 0.001)
        assertEquals(4.0, weekly.youAverage!!, 0.001)
    }

    @Test
    fun empty_records_yield_no_data() {
        val weekly = PulseAggregator.weekly(emptyList(), week, you = "a", building = "T", floor = 4)
        assertNull(weekly.companyAverage)
        assertEquals(false, weekly.hasData)
        assertEquals(7, weekly.days.size)
    }
}
