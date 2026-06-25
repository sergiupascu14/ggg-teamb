package com.example.teamb.newsfeed

import app.cash.turbine.test
import com.example.teamb.data.community.InMemoryCommunityRepository
import com.example.teamb.data.desk.DeskAllocationRepository
import com.example.teamb.data.model.CommunityFeedback
import com.example.teamb.data.model.Desk
import com.example.teamb.data.model.Employee
import com.example.teamb.data.model.FeedbackCategory
import com.example.teamb.data.model.FeedbackSentiment
import com.example.teamb.ui.newsfeed.NewsfeedViewModel
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
class NewsfeedViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val employees = listOf(
        Employee("1001", "Alice Anderson", "active", "Carol Chief"),
    )
    private val desks = listOf(
        Desk("T6-C2-01", "T", 6, "C", 2, "01", "1001"),
    )
    private val desk = DeskAllocationRepository(employees, desks)

    private fun fb(
        id: String,
        userId: String?,
        building: String?,
        floor: Int?,
        createdAt: Long,
    ) = CommunityFeedback(
        id = id,
        userId = userId,
        category = FeedbackCategory.KITCHEN,
        sentiment = FeedbackSentiment.ISSUE,
        message = "m-$id",
        building = building,
        floor = floor,
        location = null,
        photoRef = null,
        createdAt = createdAt,
    )

    private fun seededRepo() = InMemoryCommunityRepository(
        seed = listOf(
            fb("t4", "1001", "T", 4, createdAt = 400L),
            fb("t6", "1001", "T", 6, createdAt = 300L),
            fb("r3", null, "R", 3, createdAt = 200L),
        ),
    )

    @Test
    fun resolves_names_with_anonymous_fallback_and_newest_first() = runTest {
        val vm = NewsfeedViewModel(seededRepo(), desk)
        vm.setCurrentUserId("1001")

        vm.state.test {
            // Skip initial empty state, wait for populated feed.
            val populated = awaitItemMatching { it.rows.size == 3 }
            assertEquals(listOf("t4", "t6", "r3"), populated.rows.map { it.item.id })
            assertEquals("Alice Anderson", populated.rows[0].displayName)
            assertEquals("Anonymous", populated.rows[2].displayName)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun filtering_by_building_and_floor_narrows_then_clear_restores() = runTest {
        val vm = NewsfeedViewModel(seededRepo(), desk)
        vm.setCurrentUserId("1001")
        advanceUntilIdle()

        // Filter to Tower → drops the Riviera item.
        vm.selectBuilding("T")
        advanceUntilIdle()
        assertEquals(setOf("t4", "t6"), vm.state.value.rows.map { it.item.id }.toSet())

        // Add floor 4 → only the T4 item remains.
        vm.selectFloor(4)
        advanceUntilIdle()
        assertEquals(listOf("t4"), vm.state.value.rows.map { it.item.id })

        // Floor options come from the selected building (Tower 3..6).
        assertEquals(listOf(3, 4, 5, 6), vm.state.value.floorOptions)

        // Clear restores the full feed.
        vm.clearFilter()
        advanceUntilIdle()
        assertEquals(3, vm.state.value.rows.size)
    }

    @Test
    fun selecting_building_resets_floor() = runTest {
        val vm = NewsfeedViewModel(seededRepo(), desk)
        vm.setCurrentUserId("1001")
        vm.selectBuilding("T")
        vm.selectFloor(4)
        advanceUntilIdle()
        assertEquals(4, vm.state.value.filter.floor)

        vm.selectBuilding("R")
        advanceUntilIdle()
        assertEquals(null, vm.state.value.filter.floor)
    }

    @Test
    fun vote_toggles_state_for_current_user() = runTest {
        val vm = NewsfeedViewModel(seededRepo(), desk)
        vm.setCurrentUserId("voter")
        advanceUntilIdle()

        fun rowT4() = vm.state.value.rows.first { it.item.id == "t4" }.item
        assertEquals(0, rowT4().votes)
        assertFalse(rowT4().votedByMe)

        vm.toggleVote("t4")
        advanceUntilIdle()
        assertEquals(1, rowT4().votes)
        assertTrue(rowT4().votedByMe)

        vm.toggleVote("t4")
        advanceUntilIdle()
        assertEquals(0, rowT4().votes)
        assertFalse(rowT4().votedByMe)
    }

    @Test
    fun vote_is_noop_when_no_current_user() = runTest {
        val vm = NewsfeedViewModel(seededRepo(), desk)
        vm.setCurrentUserId(null)
        advanceUntilIdle()

        vm.toggleVote("t4")
        advanceUntilIdle()

        assertEquals(0, vm.state.value.rows.first { it.item.id == "t4" }.item.votes)
    }
}

/** Awaits items until [predicate] holds, returning the matching item. */
private suspend fun <T> app.cash.turbine.ReceiveTurbine<T>.awaitItemMatching(
    predicate: (T) -> Boolean,
): T {
    while (true) {
        val item = awaitItem()
        if (predicate(item)) return item
    }
}
