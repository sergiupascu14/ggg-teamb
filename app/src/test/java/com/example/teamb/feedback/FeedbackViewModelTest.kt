package com.example.teamb.feedback

import com.example.teamb.data.community.InMemoryCommunityRepository
import com.example.teamb.data.integration.MockJiraTicketRouter
import com.example.teamb.data.integration.PhotoIssueDetector
import com.example.teamb.data.model.PhotoAnalysisFailure
import com.example.teamb.data.model.PhotoCategorizationResult
import com.example.teamb.data.model.FeedbackCategory
import com.example.teamb.data.model.FeedbackSentiment
import com.example.teamb.data.repository.FeedbackRepository
import com.example.teamb.ui.feedback.FeedbackViewModel
import com.example.teamb.ui.feedback.PhotoDraftStatus
import com.example.teamb.util.FakeClock
import com.example.teamb.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FeedbackViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private data class RepoFixture(
        val repository: FeedbackRepository,
        val feedbackDao: FakeFeedbackDao,
    )

    private fun repoFixture(community: InMemoryCommunityRepository = InMemoryCommunityRepository()): RepoFixture {
        val feedbackDao = FakeFeedbackDao()
        return RepoFixture(
            repository = FeedbackRepository(
                feedbackDao,
                FakeTicketDao(),
                community,
                MockJiraTicketRouter(),
                FakeClock(1_000L),
            ),
            feedbackDao = feedbackDao,
        )
    }

    private class FixedDetector(private val result: PhotoCategorizationResult) : PhotoIssueDetector {
        override suspend fun analyze(photoUri: String): PhotoCategorizationResult = result
    }

    @Test
    fun `field setters update the form`() {
        val vm = FeedbackViewModel(
            repoFixture().repository,
            FixedDetector(PhotoCategorizationResult(failure = PhotoAnalysisFailure.DISABLED)),
        )
        vm.setSentiment(FeedbackSentiment.POSITIVE)
        vm.setCategory(FeedbackCategory.KITCHEN)
        vm.setIssueLabel("Kitchen spill")
        vm.setMessage("Great coffee")
        vm.setAnonymous(true)
        vm.setLocation("Floor 4")
        vm.setCommunityVisible(true)
        vm.setWantsTicket(true)
        val form = vm.state.value.form
        assertEquals(FeedbackSentiment.POSITIVE, form.sentiment)
        assertEquals(FeedbackCategory.KITCHEN, form.category)
        assertEquals("Kitchen spill", form.issueLabel)
        assertEquals("Great coffee", form.message)
        assertTrue(form.anonymous)
        assertEquals("Floor 4", form.location)
        assertTrue(form.communityVisible)
        assertTrue(form.wantsTicket)
    }

    @Test
    fun `blank location becomes null`() {
        val vm = FeedbackViewModel(
            repoFixture().repository,
            FixedDetector(PhotoCategorizationResult(failure = PhotoAnalysisFailure.DISABLED)),
        )
        vm.setLocation("   ")
        assertNull(vm.state.value.form.location)
    }

    @Test
    fun `prefill from profile fills building and floor`() {
        val vm = FeedbackViewModel(
            repoFixture().repository,
            FixedDetector(PhotoCategorizationResult(failure = PhotoAnalysisFailure.DISABLED)),
        )
        vm.prefillFromProfile("T", 6, "T6-C2-01")
        val form = vm.state.value.form
        assertEquals("T", form.building)
        assertEquals(6, form.floor)
        assertEquals("T6-C2-01", form.location)
    }

    @Test
    fun `submit with blank message sets error and does not submit`() {
        val vm = FeedbackViewModel(
            repoFixture().repository,
            FixedDetector(PhotoCategorizationResult(failure = PhotoAnalysisFailure.DISABLED)),
        )
        vm.setCategory(FeedbackCategory.OTHER)
        vm.submit("123")
        assertEquals("Please describe your feedback", vm.state.value.error)
        assertNull(vm.state.value.result)
    }

    @Test
    fun `submit issue with ticket produces a created ticket result and resets form`() = runTest {
        val vm = FeedbackViewModel(
            repoFixture().repository,
            FixedDetector(PhotoCategorizationResult(failure = PhotoAnalysisFailure.DISABLED)),
        )
        vm.setSentiment(FeedbackSentiment.ISSUE)
        vm.setCategory(FeedbackCategory.ELEVATORS)
        vm.setMessage("Elevator stuck")
        vm.setWantsTicket(true)
        vm.submit("123")
        advanceUntilIdle()
        val state = vm.state.value
        assertTrue(state.result!!.ticketCreated)
        assertFalse(state.submitting)
        assertNull(state.form.category)
        assertEquals("", state.form.message) // reset
    }

    @Test
    fun `submit positive with ticket is suppressed`() = runTest {
        val vm = FeedbackViewModel(
            repoFixture().repository,
            FixedDetector(PhotoCategorizationResult(failure = PhotoAnalysisFailure.DISABLED)),
        )
        vm.setSentiment(FeedbackSentiment.POSITIVE)
        vm.setCategory(FeedbackCategory.OTHER)
        vm.setMessage("Love the plants")
        vm.setWantsTicket(true)
        vm.submit("123")
        advanceUntilIdle()
        val result = vm.state.value.result!!
        assertFalse(result.ticketCreated)
        assertTrue(result.ticketSuppressed)
    }

    @Test
    fun `photo analysis prefills the draft on success`() = runTest {
        val draft = PhotoCategorizationResult(
            detectedIssue = "Kitchen maintenance issue",
            description = "Kitchen equipment or cleanliness may need attention.",
            suggestedCategory = FeedbackCategory.KITCHEN,
            confidence = 0.92f,
        )
        val vm = FeedbackViewModel(repoFixture().repository, FixedDetector(draft))
        vm.onPhotoPicked("file://kitchen.jpg")
        advanceUntilIdle()
        assertEquals(PhotoDraftStatus.READY, vm.state.value.photoDraftStatus)
        assertEquals(draft, vm.state.value.photoDraft)
        assertEquals(FeedbackCategory.KITCHEN, vm.state.value.form.category)
        assertEquals("Kitchen maintenance issue", vm.state.value.form.issueLabel)
        assertEquals("Kitchen equipment or cleanliness may need attention.", vm.state.value.form.message)
    }

    @Test
    fun `low confidence analysis leaves category unset`() = runTest {
        val draft = PhotoCategorizationResult(
            detectedIssue = "Unclear facilities issue",
            description = "A possible office issue was detected, but the category needs your review.",
            suggestedCategory = null,
            confidence = 0.35f,
        )
        val vm = FeedbackViewModel(repoFixture().repository, FixedDetector(draft))
        vm.onPhotoPicked("file://kitchen.jpg")
        advanceUntilIdle()
        assertEquals(PhotoDraftStatus.LOW_CONFIDENCE, vm.state.value.photoDraftStatus)
        assertNull(vm.state.value.form.category)
        assertEquals("Unclear facilities issue", vm.state.value.form.issueLabel)
    }

    @Test
    fun `analysis unavailable keeps manual fallback`() = runTest {
        val vm = FeedbackViewModel(
            repoFixture().repository,
            FixedDetector(PhotoCategorizationResult(failure = PhotoAnalysisFailure.UNAVAILABLE)),
        )
        vm.onPhotoPicked("file://offline.jpg")
        advanceUntilIdle()
        assertEquals(PhotoDraftStatus.UNAVAILABLE, vm.state.value.photoDraftStatus)
        assertNull(vm.state.value.form.category)
        assertEquals("file://offline.jpg", vm.state.value.form.photoUri)
    }

    @Test
    fun `removing photo clears generated draft fields`() = runTest {
        val vm = FeedbackViewModel(
            repoFixture().repository,
            FixedDetector(
                PhotoCategorizationResult(
                    detectedIssue = "Kitchen maintenance issue",
                    description = "Kitchen equipment or cleanliness may need attention.",
                    suggestedCategory = FeedbackCategory.KITCHEN,
                    confidence = 0.92f,
                )
            ),
        )
        vm.onPhotoPicked("file://kitchen.jpg")
        advanceUntilIdle()
        vm.onPhotoPicked(null)
        assertEquals(PhotoDraftStatus.IDLE, vm.state.value.photoDraftStatus)
        assertNull(vm.state.value.photoDraft)
        assertNull(vm.state.value.form.photoUri)
        assertNull(vm.state.value.form.category)
        assertNull(vm.state.value.form.issueLabel)
        assertEquals("", vm.state.value.form.message)
    }

    @Test
    fun `manual overrides are persisted on submit`() = runTest {
        val fixture = repoFixture()
        val vm = FeedbackViewModel(
            fixture.repository,
            FixedDetector(
                PhotoCategorizationResult(
                    detectedIssue = "Kitchen maintenance issue",
                    description = "Kitchen equipment or cleanliness may need attention.",
                    suggestedCategory = FeedbackCategory.KITCHEN,
                    confidence = 0.92f,
                )
            ),
        )
        vm.onPhotoPicked("file://kitchen.jpg")
        advanceUntilIdle()
        vm.setIssueLabel("Leaking sink")
        vm.setCategory(FeedbackCategory.OTHER)
        vm.setMessage("Water is pooling under the sink.")
        vm.submit("1")
        advanceUntilIdle()
        val saved = fixture.feedbackDao.all.single()
        assertEquals("Leaking sink", saved.issueLabel)
        assertEquals(FeedbackCategory.OTHER.name, saved.category)
        assertEquals("Water is pooling under the sink.", saved.message)
    }

    @Test
    fun `consume result clears submit result`() = runTest {
        val vm = FeedbackViewModel(
            repoFixture().repository,
            FixedDetector(PhotoCategorizationResult(failure = PhotoAnalysisFailure.DISABLED)),
        )
        vm.setCategory(FeedbackCategory.OTHER)
        vm.setMessage("hi")
        vm.submit("1")
        advanceUntilIdle()
        vm.consumeResult()
        assertNull(vm.state.value.result)
    }
}
