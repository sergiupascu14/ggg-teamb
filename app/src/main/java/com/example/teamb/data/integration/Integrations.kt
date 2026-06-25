package com.example.teamb.data.integration

import com.example.teamb.data.desk.DeskAllocationRepository
import com.example.teamb.data.model.Employee
import com.example.teamb.data.model.FeedbackCategory
import com.example.teamb.data.model.PhotoSuggestion
import com.example.teamb.data.model.TicketRoute

/** Stand-in for GarminAD. The mock is backed by the bundled desk allocation dataset. */
interface DirectoryService {
    /** The employee the directory believes is using this device, if it can guess one. */
    fun suggestedProfile(): Employee?
    fun allEmployees(): List<Employee>
}

class MockGarminAdDirectoryService(
    private val desk: DeskAllocationRepository,
    private val suggestedStaffId: String? = null,
) : DirectoryService {
    override fun suggestedProfile(): Employee? =
        suggestedStaffId?.let { desk.employeeById(it) }

    override fun allEmployees(): List<Employee> = desk.employees
}

/** Lightweight description of a feedback item used when routing a ticket. */
data class TicketDraft(
    val feedbackId: Long,
    val category: FeedbackCategory,
    val message: String,
    val location: String?,
    val photoUri: String?,
)

data class RoutedTicket(val route: TicketRoute, val externalId: String)

/** Routes actionable feedback to facilities. */
interface TicketRouter {
    val route: TicketRoute
    fun routeTicket(draft: TicketDraft): RoutedTicket
}

/** Composes a mailto intent to #CLU-Facilities. The intent itself is fired by the UI layer. */
class EmailTicketRouter : TicketRouter {
    override val route = TicketRoute.EMAIL

    override fun routeTicket(draft: TicketDraft): RoutedTicket =
        RoutedTicket(route, "MAIL-${draft.feedbackId}")

    fun mailToUri(draft: TicketDraft): String {
        val subject = "[${draft.category.label}] Facilities issue #${draft.feedbackId}"
        val body = buildString {
            append(draft.message)
            draft.location?.let { append("\n\nLocation: $it") }
        }
        return "mailto:$FACILITIES_INBOX?subject=${enc(subject)}&body=${enc(body)}"
    }

    private fun enc(s: String) = s.replace(" ", "%20").replace("\n", "%0A")

    companion object {
        const val FACILITIES_INBOX = "sergiupascu312@gmail.com"
    }
}

/** Generates a deterministic mock Jira ticket id. */
class MockJiraTicketRouter : TicketRouter {
    override val route = TicketRoute.JIRA

    override fun routeTicket(draft: TicketDraft): RoutedTicket {
        val num = 1000 + (draft.feedbackId % 9000)
        return RoutedTicket(route, "JIRA-$num")
    }
}

/** Detects issues from a feedback photo. Phase-3 capability; always optional and non-blocking. */
interface PhotoIssueDetector {
    /** Returns a suggestion, or null when nothing confident / unavailable. */
    suspend fun analyze(photoUri: String): PhotoSuggestion?
}

/** Heuristic stand-in: keys off the file name so the demo is deterministic. */
class MockPhotoIssueDetector : PhotoIssueDetector {
    override suspend fun analyze(photoUri: String): PhotoSuggestion? {
        val lower = photoUri.lowercase()
        val category = when {
            "elevator" in lower || "lift" in lower -> FeedbackCategory.ELEVATORS
            "kitchen" in lower || "sink" in lower -> FeedbackCategory.KITCHEN
            "desk" in lower -> FeedbackCategory.DESK_AREA
            "room" in lower -> FeedbackCategory.MEETING_ROOMS
            else -> return null
        }
        return PhotoSuggestion(category, "Possible ${category.label.lowercase()} issue detected in photo")
    }
}
