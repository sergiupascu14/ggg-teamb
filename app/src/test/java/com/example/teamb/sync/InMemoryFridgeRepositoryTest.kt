package com.example.teamb.sync

import com.example.teamb.data.sync.InMemoryFridgeRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class InMemoryFridgeRepositoryTest {

    @Test
    fun observe_floor_returns_two_normalized_fridges_by_default() = runTest {
        val repo = InMemoryFridgeRepository()
        val fridges = repo.observeFloor("R4").first()
        assertEquals(listOf("1", "2"), fridges.map { it.fridgeId })
        assertEquals(listOf(0, 0), fridges.map { it.occupancy })
    }

    @Test
    fun set_occupancy_is_reflected_and_clamped() = runTest {
        val repo = InMemoryFridgeRepository()
        repo.setOccupancy("R4", "1", occupancy = 130, userId = "u1", updatedAt = 5L)

        val fridge1 = repo.observeFloor("R4").first().first { it.fridgeId == "1" }
        assertEquals(100, fridge1.occupancy) // clamped to 100
        assertEquals("u1", fridge1.updatedBy)
    }

    @Test
    fun floors_are_isolated_from_each_other() = runTest {
        val repo = InMemoryFridgeRepository()
        repo.setOccupancy("R4", "1", 40, "u1", 1L)
        repo.setOccupancy("T6", "1", 90, "u2", 2L)

        assertEquals(40, repo.observeFloor("R4").first().first { it.fridgeId == "1" }.occupancy)
        assertEquals(90, repo.observeFloor("T6").first().first { it.fridgeId == "1" }.occupancy)
    }
}
