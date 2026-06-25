package com.example.teamb.data.sync

import com.example.teamb.data.model.PulseRecord
import com.example.teamb.data.model.WeeklyPulse
import com.example.teamb.data.model.WeeklyPulsePoint
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * Shared, multi-device pulse mood store. Each user writes one mood per day; everyone can read the
 * week to compute company/floor averages. Privacy: only userId + mood + building/floor + date.
 *
 * Layout (Firebase): pulse/{date}/{userId} -> { mood, building, floor, updatedAt }
 */
interface PulseRepository {
    suspend fun submit(record: PulseRecord, updatedAt: Long)

    /** All company-wide pulse records whose date is within [weekDates]. */
    fun observeWeek(weekDates: List<String>): Flow<List<PulseRecord>>
}

/** In-memory implementation: demoable and the fake used by tests. */
class InMemoryPulseRepository(
    seed: List<PulseRecord> = emptyList(),
) : PulseRepository {

    // Keyed by "date|userId" so one entry per user per day (latest wins).
    private val state = MutableStateFlow(seed.associateBy { "${it.date}|${it.userId}" })

    override suspend fun submit(record: PulseRecord, updatedAt: Long) {
        state.value = state.value + ("${record.date}|${record.userId}" to record)
    }

    override fun observeWeek(weekDates: List<String>): Flow<List<PulseRecord>> {
        val week = weekDates.toSet()
        return state.map { all -> all.values.filter { it.date in week } }
    }
}

/** Pure aggregation of weekly pulse records into the per-day series + week averages. */
object PulseAggregator {

    /**
     * Builds the [WeeklyPulse] for the viewer. [you], [building] and [floor] scope the personal and
     * floor series; floor averages are only computed when both [building] and [floor] are known.
     */
    fun weekly(
        records: List<PulseRecord>,
        weekDates: List<String>,
        you: String?,
        building: String?,
        floor: Int?,
    ): WeeklyPulse {
        val floorKnown = !building.isNullOrBlank() && floor != null && floor > 0

        val points = weekDates.map { date ->
            val sameDay = records.filter { it.date == date }
            WeeklyPulsePoint(
                date = date,
                youMood = if (you == null) null else sameDay.filter { it.userId == you }.average0(),
                floorAverage = if (!floorKnown) null
                    else sameDay.filter { it.building == building && it.floor == floor }.average0(),
                companyAverage = sameDay.average0(),
            )
        }

        val all = records.filter { it.date in weekDates.toSet() }
        return WeeklyPulse(
            days = points,
            youAverage = if (you == null) null else all.filter { it.userId == you }.average0(),
            floorAverage = if (!floorKnown) null
                else all.filter { it.building == building && it.floor == floor }.average0(),
            companyAverage = all.average0(),
        )
    }

    /** Mean mood, or null when empty. */
    private fun List<PulseRecord>.average0(): Double? =
        if (isEmpty()) null else sumOf { it.mood }.toDouble() / size
}
