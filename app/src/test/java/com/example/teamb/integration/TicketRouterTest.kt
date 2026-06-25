package com.example.teamb.integration

import com.example.teamb.data.integration.EmailTicketRouter
import com.example.teamb.data.integration.MockJiraTicketRouter
import com.example.teamb.data.integration.TicketDraft
import com.example.teamb.data.model.FeedbackCategory
import com.example.teamb.data.model.TicketRoute
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TicketRouterTest {

    private fun draft(
        feedbackId: Long = 42L,
        category: FeedbackCategory = FeedbackCategory.ELEVATORS,
        message: String = "Lift B is stuck",
        location: String? = "Tower floor 5",
    ) = TicketDraft(feedbackId, category, message, location, photoUri = null)

    @Test
    fun mockJira_produces_jira_route_and_id_format() {
        val routed = MockJiraTicketRouter().routeTicket(draft())

        assertEquals(TicketRoute.JIRA, routed.route)
        assertTrue(
            "expected JIRA-#### but was ${routed.externalId}",
            Regex("^JIRA-\\d{4}$").matches(routed.externalId),
        )
    }

    @Test
    fun emailRouter_route_is_email() {
        val router = EmailTicketRouter()
        assertEquals(TicketRoute.EMAIL, router.route)
        assertEquals(TicketRoute.EMAIL, router.routeTicket(draft()).route)
    }

    @Test
    fun emailRouter_mailToUri_targets_facilities_inbox_with_encoded_subject_and_body() {
        val router = EmailTicketRouter()
        val uri = router.mailToUri(draft(message = "Lift B is stuck", location = "Tower floor 5"))

        assertTrue("must start with mailto: $uri", uri.startsWith("mailto:"))
        assertTrue("must target inbox: $uri", uri.contains(EmailTicketRouter.FACILITIES_INBOX))
        assertTrue("must carry a subject: $uri", uri.contains("subject="))
        assertTrue("must carry a body: $uri", uri.contains("body="))
        // spaces are percent-encoded, never left raw
        assertFalse("raw spaces must be encoded: $uri", uri.contains(" "))
        // newline between message and location is encoded
        assertTrue("location appended in body: $uri", uri.contains("Location:"))
        assertTrue("newline encoded as %0A: $uri", uri.contains("%0A"))
    }
}
