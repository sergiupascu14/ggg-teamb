package com.example.teamb.dailypulse

import com.example.teamb.data.db.DailyPulseDao
import com.example.teamb.data.db.DailyPulseEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** In-memory [DailyPulseDao] keyed by ISO date, mirroring REPLACE-on-conflict semantics. */
class FakeDailyPulseDao : DailyPulseDao {
    private val byDate = MutableStateFlow<Map<String, DailyPulseEntity>>(emptyMap())

    override suspend fun upsert(entry: DailyPulseEntity) {
        byDate.value = byDate.value + (entry.date to entry)
    }

    override suspend fun forDate(date: String): DailyPulseEntity? = byDate.value[date]

    override fun observeAll(): Flow<List<DailyPulseEntity>> =
        byDate.map { m -> m.values.sortedByDescending { it.date } }

    override suspend fun allDates(): List<String> =
        byDate.value.keys.sortedDescending()
}
