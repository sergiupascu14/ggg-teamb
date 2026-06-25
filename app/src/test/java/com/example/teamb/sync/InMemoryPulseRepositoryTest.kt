package com.example.teamb.sync

import com.example.teamb.data.model.PulseRecord
import com.example.teamb.data.sync.InMemoryPulseRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class InMemoryPulseRepositoryTest {

    private val week = listOf("2021-01-04", "2021-01-05", "2021-01-06")

    @Test
    fun submit_then_observe_week_returns_record() = runTest {
        val repo = InMemoryPulseRepository()
        repo.submit(PulseRecord("u1", "2021-01-04", 4, "T", 4), updatedAt = 1L)

        val records = repo.observeWeek(week).first()
        assertEquals(1, records.size)
        assertEquals(4, records.first().mood)
    }

    @Test
    fun observe_week_filters_out_other_weeks() = runTest {
        val repo = InMemoryPulseRepository(
            seed = listOf(
                PulseRecord("u1", "2021-01-04", 4, "T", 4),
                PulseRecord("u1", "2020-12-25", 2, "T", 4),
            )
        )
        val records = repo.observeWeek(week).first()
        assertEquals(1, records.size)
        assertEquals("2021-01-04", records.first().date)
    }

    @Test
    fun latest_submission_wins_per_user_per_day() = runTest {
        val repo = InMemoryPulseRepository()
        repo.submit(PulseRecord("u1", "2021-01-04", 2, "T", 4), updatedAt = 1L)
        repo.submit(PulseRecord("u1", "2021-01-04", 5, "T", 4), updatedAt = 2L)

        val records = repo.observeWeek(week).first()
        assertEquals(1, records.size)
        assertEquals(5, records.first().mood)
    }
}
