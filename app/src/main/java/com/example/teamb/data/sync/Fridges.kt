package com.example.teamb.data.sync

import com.example.teamb.data.model.FridgeOccupancy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** Pure helpers for the per-floor shared fridges. Each floor has [COUNT] fridges. */
object Fridges {
    const val COUNT = 2

    /** Stable fridge ids on every floor: "1", "2". */
    val IDS: List<String> = (1..COUNT).map { it.toString() }

    /** Firebase key for a floor, e.g. building "R" + floor 4 → "R4"; null when unknown. */
    fun floorKey(building: String?, floor: Int?): String? {
        if (building.isNullOrBlank() || floor == null || floor <= 0) return null
        return "$building$floor"
    }

    /** Always returns exactly [COUNT] fridges (ids "1".."2"), defaulting missing ones to 0%. */
    fun normalize(raw: List<FridgeOccupancy>): List<FridgeOccupancy> {
        val byId = raw.associateBy { it.fridgeId }
        return IDS.map { id -> byId[id] ?: FridgeOccupancy(fridgeId = id, occupancy = 0) }
    }
}

/**
 * Shared, multi-device fridge occupancy per floor. Privacy: stores occupancy + Staff ID of the
 * last updater only — never names.
 *
 * Layout (Firebase): fridges/{floorKey}/{fridgeId} -> { occupancy, updatedBy, updatedAt }
 */
interface FridgeRepository {
    /** Live occupancy of the [Fridges.COUNT] fridges on [floorKey] (normalized, ordered by id). */
    fun observeFloor(floorKey: String): Flow<List<FridgeOccupancy>>

    suspend fun setOccupancy(floorKey: String, fridgeId: String, occupancy: Int, userId: String?, updatedAt: Long)
}

/** In-memory implementation: demoable and the fake used by tests. */
class InMemoryFridgeRepository(
    seed: Map<String, List<FridgeOccupancy>> = emptyMap(),
) : FridgeRepository {

    private val state = MutableStateFlow(
        seed.mapValues { (_, list) -> list.associateBy { it.fridgeId } }
    )

    override fun observeFloor(floorKey: String): Flow<List<FridgeOccupancy>> =
        state.map { all -> Fridges.normalize(all[floorKey]?.values?.toList().orEmpty()) }

    override suspend fun setOccupancy(
        floorKey: String,
        fridgeId: String,
        occupancy: Int,
        userId: String?,
        updatedAt: Long,
    ) {
        val clamped = occupancy.coerceIn(0, 100)
        val current = state.value
        val floor = current[floorKey].orEmpty().toMutableMap()
        floor[fridgeId] = FridgeOccupancy(fridgeId, clamped, userId, updatedAt)
        state.value = current + (floorKey to floor)
    }
}
