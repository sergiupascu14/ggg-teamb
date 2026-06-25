package com.example.teamb.community

import app.cash.turbine.test
import com.example.teamb.data.community.InMemoryCommunityRepository
import com.example.teamb.data.model.CommunityFeedback
import com.example.teamb.data.model.FeedbackCategory
import com.example.teamb.data.model.FeedbackSentiment
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class InMemoryCommunityRepositoryTest {

    private fun feedback(
        id: String,
        userId: String?,
        createdAt: Long,
        message: String = "msg-$id",
        votes: Int = 0,
    ) = CommunityFeedback(
        id = id,
        userId = userId,
        category = FeedbackCategory.KITCHEN,
        sentiment = FeedbackSentiment.POSITIVE,
        message = message,
        building = "T",
        floor = 4,
        location = "loc",
        photoRef = null,
        createdAt = createdAt,
        votes = votes,
    )

    @Test
    fun publish_adds_item() = runTest {
        val repo = InMemoryCommunityRepository()

        repo.publish(feedback("a", "u1", createdAt = 100L))

        val items = repo.snapshot()
        assertEquals(1, items.size)
        assertEquals("a", items.first().id)
    }

    @Test
    fun observeFeedback_emits_newest_first() = runTest {
        val repo = InMemoryCommunityRepository(
            seed = listOf(
                feedback("old", "u1", createdAt = 100L),
                feedback("new", "u2", createdAt = 300L),
                feedback("mid", "u3", createdAt = 200L),
            ),
        )

        repo.observeFeedback(currentUserId = null).test {
            val items = awaitItem()
            assertEquals(listOf("new", "mid", "old"), items.map { it.id })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun toggleVote_increments_then_decrements() = runTest {
        val repo = InMemoryCommunityRepository(
            seed = listOf(feedback("a", "u1", createdAt = 100L)),
        )

        repo.observeFeedback(currentUserId = "voter").test {
            assertEquals(0, awaitItem().first().votes)

            repo.toggleVote("a", "voter")
            awaitItem().first().let {
                assertEquals(1, it.votes)
                assertTrue(it.votedByMe)
            }

            repo.toggleVote("a", "voter")
            awaitItem().first().let {
                assertEquals(0, it.votes)
                assertFalse(it.votedByMe)
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun votedByMe_is_true_only_for_the_voting_user() = runTest {
        val repo = InMemoryCommunityRepository(
            seed = listOf(feedback("a", "u1", createdAt = 100L)),
        )

        repo.toggleVote("a", "alice")

        // From alice's perspective the vote is hers.
        repo.observeFeedback(currentUserId = "alice").test {
            awaitItem().first().let {
                assertEquals(1, it.votes)
                assertTrue(it.votedByMe)
            }
            cancelAndIgnoreRemainingEvents()
        }

        // From bob's perspective the same item shows the vote count but not votedByMe.
        repo.observeFeedback(currentUserId = "bob").test {
            awaitItem().first().let {
                assertEquals(1, it.votes)
                assertFalse(it.votedByMe)
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun anonymous_seed_item_keeps_null_userId() = runTest {
        val repo = InMemoryCommunityRepository(
            seed = listOf(feedback("anon", userId = null, createdAt = 100L)),
        )

        repo.observeFeedback(currentUserId = "x").test {
            assertNull(awaitItem().first().userId)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun snapshot_returns_items_with_vote_counts() = runTest {
        val repo = InMemoryCommunityRepository(
            seed = listOf(feedback("a", "u1", createdAt = 100L)),
        )

        repo.toggleVote("a", "v1")
        repo.toggleVote("a", "v2")

        val snap = repo.snapshot()
        assertEquals(1, snap.size)
        assertEquals(2, snap.first().votes)
    }
}
