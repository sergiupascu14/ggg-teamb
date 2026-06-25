package com.example.teamb.dailypulse

import com.example.teamb.data.db.DailyPulseDao
import com.example.teamb.data.db.DailyPulseEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** In-memory [DailyPulseDao] keyed by (userId, ISO date), mirroring REPLACE-on-conflict semantics. */
class FakeDailyPulseDao : DailyPulseDao {
    private val byKey = MutableStateFlow<Map<String, DailyPulseEntity>>(emptyMap())

    private fun key(userId: String, date: String) = "$userId|$date"

    override suspend fun upsert(entry: DailyPulseEntity) {
        byKey.value = byKey.value + (key(entry.userId, entry.date) to entry)
    }

    override suspend fun forDate(userId: String, date: String): DailyPulseEntity? =
        byKey.value[key(userId, date)]

    override fun observeAll(userId: String): Flow<List<DailyPulseEntity>> =
        byKey.map { m -> m.values.filter { it.userId == userId }.sortedByDescending { it.date } }

    override suspend fun allDates(userId: String): List<String> =
        byKey.value.values.filter { it.userId == userId }.map { it.date }.sortedDescending()
}
