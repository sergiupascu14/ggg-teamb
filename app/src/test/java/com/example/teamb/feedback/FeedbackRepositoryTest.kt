package com.example.teamb.feedback

import com.example.teamb.data.integration.MockJiraTicketRouter
import com.example.teamb.data.model.FeedbackCategory
import com.example.teamb.data.model.FeedbackSentiment
import com.example.teamb.data.model.TicketStatus
import com.example.teamb.data.repository.FeedbackForm
import com.example.teamb.data.repository.FeedbackRepository
import com.example.teamb.util.FakeClock
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FeedbackRepositoryTest {

    private val userId = "staff-42"

    private fun fixture(): Triple<FeedbackRepository, Pair<FakeFeedbackDao, FakeTicketDao>, RecordingCommunityRepository> {
        val feedbackDao = FakeFeedbackDao()
        val ticketDao = FakeTicketDao()
        val community = RecordingCommunityRepository()
        val repo = FeedbackRepository(
            feedbackDao = feedbackDao,
            ticketDao = ticketDao,
            community = community,
            ticketRouter = MockJiraTicketRouter(),
            clock = FakeClock(1_000L),
        )
        return Triple(repo, feedbackDao to ticketDao, community)
    }

    private fun form(
        sentiment: FeedbackSentiment,
        message: String = "Something to report",
        wantsTicket: Boolean = false,
        anonymous: Boolean = false,
        communityVisible: Boolean = false,
        category: FeedbackCategory = FeedbackCategory.KITCHEN,
        issueLabel: String? = null,
    ) = FeedbackForm(
        category = category,
        sentiment = sentiment,
        issueLabel = issueLabel,
        message = message,
        anonymous = anonymous,
        communityVisible = communityVisible,
        wantsTicket = wantsTicket,
    )

    @Test
    fun positive_with_wantsTicket_suppresses_ticket() = runTest {
        val (repo, daos, _) = fixture()
        val (_, ticketDao) = daos

        val result = repo.submit(form(FeedbackSentiment.POSITIVE, wantsTicket = true), userId)

        assertFalse(result.ticketCreated)
        assertTrue(result.ticketSuppressed)
        assertNull(result.ticketExternalId)
        assertTrue(ticketDao.all.isEmpty())
    }

    @Test
    fun issue_with_wantsTicket_creates_and_persists_ticket() = runTest {
        val (repo, daos, _) = fixture()
        val (_, ticketDao) = daos

        val result = repo.submit(form(FeedbackSentiment.ISSUE, wantsTicket = true), userId)

        assertTrue(result.ticketCreated)
        assertFalse(result.ticketSuppressed)
        assertNotNull(result.ticketExternalId)
        assertEquals(1, ticketDao.all.size)
        val ticket = ticketDao.all.single()
        assertEquals(result.ticketExternalId, ticket.externalId)
        assertEquals(result.feedbackId, ticket.feedbackId)
        assertEquals(TicketStatus.OPEN.name, ticket.status)
    }

    @Test
    fun issue_without_wantsTicket_creates_no_ticket() = runTest {
        val (repo, daos, _) = fixture()
        val (_, ticketDao) = daos

        val result = repo.submit(form(FeedbackSentiment.ISSUE, wantsTicket = false), userId)

        assertFalse(result.ticketCreated)
        assertFalse(result.ticketSuppressed)
        assertNull(result.ticketExternalId)
        assertTrue(ticketDao.all.isEmpty())
    }

    @Test
    fun communityVisible_non_anonymous_publishes_with_userId() = runTest {
        val (repo, _, community) = fixture()

        repo.submit(form(FeedbackSentiment.ISSUE, communityVisible = true, anonymous = false), userId)

        assertEquals(1, community.published.size)
        assertEquals(userId, community.published.single().userId)
    }

    @Test
    fun communityVisible_anonymous_publishes_with_null_userId() = runTest {
        val (repo, _, community) = fixture()

        repo.submit(form(FeedbackSentiment.ISSUE, communityVisible = true, anonymous = true), userId)

        assertEquals(1, community.published.size)
        assertNull(community.published.single().userId)
    }

    @Test
    fun not_communityVisible_publishes_nothing() = runTest {
        val (repo, _, community) = fixture()

        repo.submit(form(FeedbackSentiment.ISSUE, communityVisible = false), userId)

        assertTrue(community.published.isEmpty())
    }

    @Test
    fun validate_blank_message_is_error_and_valid_is_null() {
        val (repo, _, _) = fixture()

        assertNotNull(repo.validate(form(FeedbackSentiment.ISSUE, message = "   ")))
        assertNull(repo.validate(form(FeedbackSentiment.ISSUE, message = "Real feedback")))
    }

    @Test
    fun validate_missing_category_is_error() {
        val (repo, _, _) = fixture()

        assertEquals(
            "Please choose a category",
            repo.validate(form(FeedbackSentiment.ISSUE, category = FeedbackCategory.KITCHEN).copy(category = null)),
        )
    }

    @Test
    fun submit_persists_issue_label_and_final_message() = runTest {
        val (repo, daos, _) = fixture()
        val (feedbackDao, _) = daos

        repo.submit(
            form(
                sentiment = FeedbackSentiment.ISSUE,
                category = FeedbackCategory.KITCHEN,
                issueLabel = "Leaking sink",
                message = "Water is pooling under the sink.",
            ),
            userId,
        )

        val saved = feedbackDao.all.single()
        assertEquals("Leaking sink", saved.issueLabel)
        assertEquals("Water is pooling under the sink.", saved.message)
        assertEquals(FeedbackCategory.KITCHEN.name, saved.category)
    }
}
