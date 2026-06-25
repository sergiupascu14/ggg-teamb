package com.example.teamb.freezer

import app.cash.turbine.test
import com.example.teamb.data.repository.FreezerRepository
import com.example.teamb.ui.freezer.FreezerViewModel
import com.example.teamb.util.FakeClock
import com.example.teamb.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FreezerViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    @Test
    fun checkIn_without_owner_is_ignored() = runTest {
        val dao = FakeFreezerDao()
        val repo = FreezerRepository(dao, FakeClock(0L))
        val vm = FreezerViewModel(repo)

        vm.checkIn("Lunch")
        advanceUntilIdle()

        assertTrue(dao.allPresent().isEmpty())
    }

    @Test
    fun checkIn_after_owner_set_persists_item() = runTest {
        val dao = FakeFreezerDao()
        val repo = FreezerRepository(dao, FakeClock(0L))
        val vm = FreezerViewModel(repo)
        vm.setOwner("staff-1")

        vm.checkIn("  Lunch  ") // trimmed
        advanceUntilIdle()

        repo.observeItems("staff-1").test {
            val items = awaitItem()
            assertEquals(1, items.size)
            assertEquals("Lunch", items.first().label)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun checkOut_marks_item_not_present() = runTest {
        val dao = FakeFreezerDao()
        val repo = FreezerRepository(dao, FakeClock(0L))
        val vm = FreezerViewModel(repo)
        vm.setOwner("staff-1")

        val id = repo.checkIn("Lunch", "staff-1")
        vm.checkOut(id)
        advanceUntilIdle()

        assertFalse(dao.byId(id)!!.present)
    }

    @Test
    fun blank_label_check_in_is_ignored() = runTest {
        val dao = FakeFreezerDao()
        val repo = FreezerRepository(dao, FakeClock(0L))
        val vm = FreezerViewModel(repo)
        vm.setOwner("staff-1")

        vm.checkIn("   ")
        advanceUntilIdle()

        assertTrue(dao.allPresent().isEmpty())
    }
}
