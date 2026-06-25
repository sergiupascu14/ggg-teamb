package com.example.teamb.feedback

import com.example.teamb.data.community.InMemoryCommunityRepository
import com.example.teamb.data.integration.MockJiraTicketRouter
import com.example.teamb.data.integration.PhotoIssueDetector
import com.example.teamb.data.model.FeedbackCategory
import com.example.teamb.data.model.FeedbackSentiment
import com.example.teamb.data.model.PhotoSuggestion
import com.example.teamb.data.repository.FeedbackRepository
import com.example.teamb.ui.feedback.FeedbackViewModel
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

    private fun repo(community: InMemoryCommunityRepository = InMemoryCommunityRepository()) =
        FeedbackRepository(FakeFeedbackDao(), FakeTicketDao(), community, MockJiraTicketRouter(), FakeClock(1_000L))

    private class FixedDetector(private val suggestion: PhotoSuggestion?) : PhotoIssueDetector {
        override suspend fun analyze(photoUri: String): PhotoSuggestion? = suggestion
    }

    @Test
    fun `field setters update the form`() {
        val vm = FeedbackViewModel(repo(), FixedDetector(null))
        vm.setSentiment(FeedbackSentiment.POSITIVE)
        vm.setCategory(FeedbackCategory.KITCHEN)
        vm.setMessage("Great coffee")
        vm.setAnonymous(true)
        vm.setLocation("Floor 4")
        vm.setCommunityVisible(true)
        vm.setWantsTicket(true)
        val form = vm.state.value.form
        assertEquals(FeedbackSentiment.POSITIVE, form.sentiment)
        assertEquals(FeedbackCategory.KITCHEN, form.category)
        assertEquals("Great coffee", form.message)
        assertTrue(form.anonymous)
        assertEquals("Floor 4", form.location)
        assertTrue(form.communityVisible)
        assertTrue(form.wantsTicket)
    }

    @Test
    fun `blank location becomes null`() {
        val vm = FeedbackViewModel(repo(), FixedDetector(null))
        vm.setLocation("   ")
        assertNull(vm.state.value.form.location)
    }

    @Test
    fun `prefill from profile fills building and floor`() {
        val vm = FeedbackViewModel(repo(), FixedDetector(null))
        vm.prefillFromProfile("T", 6, "T6-C2-01")
        val form = vm.state.value.form
        assertEquals("T", form.building)
        assertEquals(6, form.floor)
        assertEquals("T6-C2-01", form.location)
    }

    @Test
    fun `submit with blank message sets error and does not submit`() {
        val vm = FeedbackViewModel(repo(), FixedDetector(null))
        vm.submit("123")
        assertEquals("Please describe your feedback", vm.state.value.error)
        assertNull(vm.state.value.result)
    }

    @Test
    fun `submit issue with ticket produces a created ticket result and resets form`() = runTest {
        val vm = FeedbackViewModel(repo(), FixedDetector(null))
        vm.setSentiment(FeedbackSentiment.ISSUE)
        vm.setCategory(FeedbackCategory.ELEVATORS)
        vm.setMessage("Elevator stuck")
        vm.setWantsTicket(true)
        vm.submit("123")
        advanceUntilIdle()
        val state = vm.state.value
        assertTrue(state.result!!.ticketCreated)
        assertFalse(state.submitting)
        assertEquals("", state.form.message) // reset
    }

    @Test
    fun `submit positive with ticket is suppressed`() = runTest {
        val vm = FeedbackViewModel(repo(), FixedDetector(null))
        vm.setSentiment(FeedbackSentiment.POSITIVE)
        vm.setMessage("Love the plants")
        vm.setWantsTicket(true)
        vm.submit("123")
        advanceUntilIdle()
        val result = vm.state.value.result!!
        assertFalse(result.ticketCreated)
        assertTrue(result.ticketSuppressed)
    }

    @Test
    fun `photo analysis surfaces and applies a suggestion`() = runTest {
        val suggestion = PhotoSuggestion(FeedbackCategory.KITCHEN, "kitchen issue")
        val vm = FeedbackViewModel(repo(), FixedDetector(suggestion))
        vm.onPhotoPicked("file://kitchen.jpg")
        advanceUntilIdle()
        assertEquals(suggestion, vm.state.value.suggestion)
        vm.applySuggestion()
        assertEquals(FeedbackCategory.KITCHEN, vm.state.value.form.category)
        assertNull(vm.state.value.suggestion)
    }

    @Test
    fun `removing photo clears suggestion`() = runTest {
        val vm = FeedbackViewModel(repo(), FixedDetector(PhotoSuggestion(FeedbackCategory.KITCHEN, "x")))
        vm.onPhotoPicked("file://kitchen.jpg")
        advanceUntilIdle()
        vm.onPhotoPicked(null)
        assertNull(vm.state.value.suggestion)
        assertNull(vm.state.value.form.photoUri)
    }

    @Test
    fun `dismiss suggestion and consume result clear state`() = runTest {
        val vm = FeedbackViewModel(repo(), FixedDetector(PhotoSuggestion(FeedbackCategory.KITCHEN, "x")))
        vm.onPhotoPicked("file://kitchen.jpg")
        advanceUntilIdle()
        vm.dismissSuggestion()
        assertNull(vm.state.value.suggestion)
        vm.setMessage("hi")
        vm.submit("1")
        advanceUntilIdle()
        vm.consumeResult()
        assertNull(vm.state.value.result)
    }
}
