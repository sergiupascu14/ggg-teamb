package com.example.teamb.sync

import com.example.teamb.data.model.FridgeOccupancy
import com.example.teamb.data.sync.Fridges
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FridgesTest {

    @Test
    fun floor_key_combines_building_and_floor() {
        assertEquals("R4", Fridges.floorKey("R", 4))
        assertEquals("T6", Fridges.floorKey("T", 6))
    }

    @Test
    fun floor_key_null_when_location_unknown() {
        assertNull(Fridges.floorKey(null, 4))
        assertNull(Fridges.floorKey("", 4))
        assertNull(Fridges.floorKey("R", null))
        assertNull(Fridges.floorKey("R", 0))
    }

    @Test
    fun normalize_always_returns_two_fridges_defaulting_to_zero() {
        val result = Fridges.normalize(emptyList())
        assertEquals(listOf("1", "2"), result.map { it.fridgeId })
        assertEquals(listOf(0, 0), result.map { it.occupancy })
    }

    @Test
    fun normalize_preserves_provided_values_and_fills_the_rest() {
        val result = Fridges.normalize(listOf(FridgeOccupancy("2", 70)))
        assertEquals(0, result.first { it.fridgeId == "1" }.occupancy)
        assertEquals(70, result.first { it.fridgeId == "2" }.occupancy)
        assertEquals(2, result.size)
    }
}
