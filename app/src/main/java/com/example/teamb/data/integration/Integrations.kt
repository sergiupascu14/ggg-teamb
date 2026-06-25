package com.example.teamb.data.integration

import com.example.teamb.data.desk.DeskAllocationRepository
import com.example.teamb.data.model.Employee
import com.example.teamb.data.model.FeedbackCategory
import com.example.teamb.data.model.PhotoAnalysisFailure
import com.example.teamb.data.model.PhotoCategorizationResult
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

class PhotoIssueCategoryMapper(
    private val categoryConfidenceThreshold: Float = 0.6f,
) {
    fun map(issueLabel: String, confidence: Float): FeedbackCategory? {
        if (confidence < categoryConfidenceThreshold) return null
        val lower = issueLabel.lowercase()
        return when {
            "elevator" in lower || "lift" in lower || "escalator" in lower -> FeedbackCategory.ELEVATORS
            "kitchen" in lower || "sink" in lower || "coffee" in lower
                || "food" in lower || "microwave" in lower || "refrigerator" in lower
                || "countertop" in lower || "tableware" in lower -> FeedbackCategory.KITCHEN
            "desk" in lower || "chair" in lower || "workspace" in lower
                || "computer" in lower || "monitor" in lower || "keyboard" in lower
                || "laptop" in lower || "office" in lower -> FeedbackCategory.DESK_AREA
            "meeting" in lower || "room" in lower || "conference" in lower
                || "whiteboard" in lower || "projector" in lower -> FeedbackCategory.MEETING_ROOMS
            "bathroom" in lower || "restroom" in lower || "dryer" in lower
                || "toilet" in lower || "faucet" in lower || "plumbing" in lower -> FeedbackCategory.BATHROOMS
            "temperature" in lower || Regex("\\bac\\b").containsMatchIn(lower) || "air" in lower
                || "hvac" in lower || "vent" in lower || "heater" in lower -> FeedbackCategory.TEMPERATURE
            "parking" in lower || "car" in lower || "vehicle" in lower
                || "automobile" in lower -> FeedbackCategory.PARKING
            else -> FeedbackCategory.OTHER
        }
    }
}

/**
 * Encodes a picked photo (a `content://`/`file://` uri) into a compact, portable form so it can be
 * shared on the community newsfeed. Returns a base64 JPEG string (no `data:` prefix) downscaled for
 * the feed, or null when no photo / encoding fails. The Android impl lives in [AndroidPhotoEncoder];
 * [NoopPhotoEncoder] is the test/in-memory stand-in.
 */
interface PhotoEncoder {
    suspend fun encode(photoUri: String): String?
}

/** Encodes nothing — used in unit tests and demos without a real device [android.content.Context]. */
class NoopPhotoEncoder : PhotoEncoder {
    override suspend fun encode(photoUri: String): String? = null
}

/** Detects issues from a feedback photo. Phase-3 capability; always optional and non-blocking. */
interface PhotoIssueDetector {
    /** Returns a reviewable issue draft or a failure state; callers must not block submission on it. */
    suspend fun analyze(photoUri: String): PhotoCategorizationResult
}

/** Heuristic stand-in: keys off the file name so the demo is deterministic. */
class MockPhotoIssueDetector(
    private val mapper: PhotoIssueCategoryMapper = PhotoIssueCategoryMapper(),
) : PhotoIssueDetector {
    override suspend fun analyze(photoUri: String): PhotoCategorizationResult {
        val lower = photoUri.lowercase()
        when {
            "disabled" in lower -> return PhotoCategorizationResult(failure = PhotoAnalysisFailure.DISABLED)
            "offline" in lower || "unavailable" in lower -> {
                return PhotoCategorizationResult(failure = PhotoAnalysisFailure.UNAVAILABLE)
            }
            "timeout" in lower -> return PhotoCategorizationResult(failure = PhotoAnalysisFailure.TIMEOUT)
        }
        val (issueLabel, description, confidence) = when {
            "elevator" in lower || "lift" in lower -> Triple(
                "Elevator access issue",
                "Elevator equipment appears to be malfunctioning or obstructed.",
                0.95f,
            )
            "kitchen" in lower || "sink" in lower || "coffee" in lower -> Triple(
                "Kitchen maintenance issue",
                "Kitchen equipment or cleanliness may need attention.",
                0.92f,
            )
            "desk" in lower || "chair" in lower -> Triple(
                "Desk area issue",
                "A desk area problem is visible in the uploaded photo.",
                0.9f,
            )
            "meeting" in lower || "room" in lower -> Triple(
                "Meeting room issue",
                "A meeting room problem is visible in the uploaded photo.",
                0.88f,
            )
            "bathroom" in lower || "dryer" in lower -> Triple(
                "Bathroom maintenance issue",
                "A bathroom maintenance problem is visible in the uploaded photo.",
                0.9f,
            )
            "parking" in lower || "car" in lower -> Triple(
                "Parking issue",
                "The uploaded photo suggests a parking-area problem.",
                0.87f,
            )
            else -> Triple(
                "Unclear facilities issue",
                "A possible office issue was detected, but the category needs your review.",
                0.35f,
            )
        }
        return PhotoCategorizationResult(
            detectedIssue = issueLabel,
            description = description,
            suggestedCategory = mapper.map(issueLabel, confidence),
            confidence = confidence,
        )
    }
}
