package com.example.teamb.gamification

import com.example.teamb.data.community.InMemoryCommunityRepository
import com.example.teamb.data.desk.DeskAllocationRepository
import com.example.teamb.data.model.CommunityFeedback
import com.example.teamb.data.model.Desk
import com.example.teamb.data.model.Employee
import com.example.teamb.data.model.FeedbackCategory
import com.example.teamb.data.model.FeedbackSentiment
import com.example.teamb.data.repository.GamificationRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GamificationRepositoryTest {

    private val employees = listOf(
        Employee("1001", "Alice Anderson", "active", "Carol Chief"),
        Employee("1002", "Bob Brown", "active", "Carol Chief"),
    )
    private val desks = listOf(
        Desk("T6-C2-01", "T", 6, "C", 2, "01", "1001"),
        Desk("R3-A1-05", "R", 3, "A", 1, "05", "1002"),
    )
    private val desk = DeskAllocationRepository(employees, desks)

    private fun fb(id: String, userId: String?) = CommunityFeedback(
        id = id,
        userId = userId,
        category = FeedbackCategory.KITCHEN,
        sentiment = FeedbackSentiment.POSITIVE,
        message = "m-$id",
        building = "T",
        floor = 4,
        location = null,
        photoRef = null,
        createdAt = id.hashCode().toLong(),
    )

    private fun repo(vararg items: CommunityFeedback) =
        GamificationRepository(InMemoryCommunityRepository(items.toList()), desk)

    @Test
    fun ranks_by_public_feedback_count_descending() = runTest {
        val gam = repo(
            fb("a", "1001"),
            fb("b", "1002"), fb("c", "1002"), fb("d", "1002"),
            fb("e", "1001"),
        )

        val board = gam.leaderboard(currentUserId = null)

        assertEquals(listOf("1002", "1001"), board.map { it.userId })
        assertEquals(listOf(3, 2), board.map { it.publicFeedbackCount })
    }

    @Test
    fun anonymous_items_are_excluded() = runTest {
        val gam = repo(
            fb("a", "1001"),
            fb("anon1", null), fb("anon2", null),
        )

        val board = gam.leaderboard(currentUserId = null)

        assertEquals(1, board.size)
        assertEquals("1001", board.first().userId)
        assertEquals(1, board.first().publicFeedbackCount)
    }

    @Test
    fun top_entry_is_office_champion_only_when_count_positive() = runTest {
        val gam = repo(fb("a", "1001"), fb("b", "1002"))

        val board = gam.leaderboard(currentUserId = null)

        assertTrue(board.first().isOfficeChampion)
        assertFalse(board.drop(1).any { it.isOfficeChampion })
    }

    @Test
    fun empty_board_has_no_champion() = runTest {
        val gam = repo() // no items

        assertTrue(gam.leaderboard(currentUserId = null).isEmpty())
    }

    @Test
    fun ties_broken_deterministically_by_userId() = runTest {
        // Both have 1 post → tie broken by ascending userId, so 1001 ranks above 1002.
        val gam = repo(fb("a", "1002"), fb("b", "1001"))

        val board = gam.leaderboard(currentUserId = null)

        assertEquals(listOf("1001", "1002"), board.map { it.userId })
        assertTrue(board.first().isOfficeChampion)
        assertFalse(board[1].isOfficeChampion)
    }

    @Test
    fun isCurrentUser_flag_set_for_passed_id() = runTest {
        val gam = repo(fb("a", "1001"), fb("b", "1002"))

        val board = gam.leaderboard(currentUserId = "1002")

        assertTrue(board.first { it.userId == "1002" }.isCurrentUser)
        assertFalse(board.first { it.userId == "1001" }.isCurrentUser)
    }

    @Test
    fun displayName_resolved_from_desk_with_fallback() = runTest {
        val gam = repo(fb("a", "1001"), fb("b", "9999")) // 9999 not in directory

        val board = gam.leaderboard(currentUserId = null)

        assertEquals("Alice Anderson", board.first { it.userId == "1001" }.displayName)
        assertEquals("User 9999", board.first { it.userId == "9999" }.displayName)
    }

    @Test
    fun rewardsFor_unlocks_tiers_at_thresholds() {
        val gam = repo()

        // Below the first threshold (10): nothing unlocked.
        gam.rewardsFor(9).let { rewards ->
            assertTrue(rewards.none { it.unlocked })
        }

        // At threshold 10: first tier unlocked, higher locked.
        gam.rewardsFor(10).let { rewards ->
            assertEquals(setOf("first-steps"), rewards.filter { it.unlocked }.map { it.id }.toSet())
        }

        // At 50: first two unlocked.
        gam.rewardsFor(50).let { rewards ->
            assertEquals(
                setOf("first-steps", "regular"),
                rewards.filter { it.unlocked }.map { it.id }.toSet(),
            )
        }

        // Above the top threshold (100): all unlocked.
        gam.rewardsFor(150).let { rewards ->
            assertTrue(rewards.all { it.unlocked })
            assertEquals(3, rewards.size)
        }
    }
}
