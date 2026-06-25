package com.example.teamb.freezer

import app.cash.turbine.test
import com.example.teamb.data.repository.FreezerRepository
import com.example.teamb.util.FakeClock
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FreezerRepositoryTest {

    private val dayMs = 86_400_000L

    @Test
    fun checkIn_creates_present_item_for_owner() = runTest {
        val dao = FakeFreezerDao()
        val clock = FakeClock(1_000_000L)
        val repo = FreezerRepository(dao, clock)

        val id = repo.checkIn("Lunch", "staff-1")

        val item = dao.byId(id)!!
        assertEquals("Lunch", item.label)
        assertEquals("staff-1", item.ownerId)
        assertEquals(1_000_000L, item.checkInAt)
        assertNull(item.checkOutAt)
        assertTrue(item.present)
    }

    @Test
    fun observeItems_emits_only_owners_present_items() = runTest {
        val dao = FakeFreezerDao()
        val repo = FreezerRepository(dao, FakeClock(0L))

        repo.checkIn("Mine", "staff-1")
        repo.checkIn("Theirs", "staff-2")

        repo.observeItems("staff-1").test {
            val list = awaitItem()
            assertEquals(1, list.size)
            assertEquals("Mine", list.first().label)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun checkOut_flips_present_false_and_sets_checkOutAt() = runTest {
        val dao = FakeFreezerDao()
        val clock = FakeClock(500L)
        val repo = FreezerRepository(dao, clock)

        val id = repo.checkIn("Snack", "staff-1")
        clock.now = 9_999L
        repo.checkOut(id)

        val item = dao.byId(id)!!
        assertFalse(item.present)
        assertEquals(9_999L, item.checkOutAt)
    }

    @Test
    fun checkOut_unknown_id_is_noop() = runTest {
        val dao = FakeFreezerDao()
        val repo = FreezerRepository(dao, FakeClock(0L))
        repo.checkOut(424242L) // must not throw
        assertTrue(dao.allPresent().isEmpty())
    }

    @Test
    fun staleItems_returns_only_items_older_than_threshold() = runTest {
        val dao = FakeFreezerDao()
        val now = 30L * dayMs
        val clock = FakeClock(now)
        val repo = FreezerRepository(dao, clock)

        // old item: checked in 10 days ago
        clock.now = now - 10 * dayMs
        repo.checkIn("Old", "staff-1")
        // fresh item: checked in 2 days ago
        clock.now = now - 2 * dayMs
        repo.checkIn("Fresh", "staff-1")
        clock.now = now

        val stale = repo.staleItems(thresholdDays = 7)
        assertEquals(listOf("Old"), stale.map { it.label })
    }

    @Test
    fun staleItems_excludes_checked_out_items() = runTest {
        val dao = FakeFreezerDao()
        val now = 30L * dayMs
        val clock = FakeClock(now)
        val repo = FreezerRepository(dao, clock)

        clock.now = now - 10 * dayMs
        val id = repo.checkIn("Old", "staff-1")
        clock.now = now
        repo.checkOut(id) // no longer present

        assertTrue(repo.staleItems(7).isEmpty())
    }
}
