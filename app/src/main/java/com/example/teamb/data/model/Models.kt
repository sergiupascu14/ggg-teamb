package com.example.teamb.data.model

/** A Garmin employee loaded from the bundled (anonymized) desk allocation dataset. */
data class Employee(
    val staffId: String,
    val name: String,
    val status: String,
    val supervisor: String,
)

/** A physical desk parsed from the desk allocation dataset. */
data class Desk(
    val deskId: String,
    val building: String,
    val floor: Int,
    val zone: String,
    val row: Int,
    val deskNum: String,
    val staffId: String?,
)

/** Parsed components of a desk id of the form `{Building}{Floor}-{Zone}{Row}-{DeskNum}` (e.g. T6-C2-01). */
data class DeskId(
    val building: String,
    val floor: Int,
    val zone: String,
    val row: Int,
    val deskNum: String,
) {
    val canonical: String get() = "$building$floor-$zone$row-$deskNum"

    companion object {
        // Grammar reflects the real desk allocation data: building T/R, zones A-H, rows and desk
        // numbers are 1-2 digits. Floor is validated against the building's actual floor range.
        private val REGEX = Regex("^([TR])(\\d{1,2})-([A-Z])(\\d{1,2})-(\\d{1,2})$")

        /** Returns the parsed desk id, or null if it does not match the grammar. */
        fun parse(raw: String): DeskId? {
            val m = REGEX.matchEntire(raw.trim().uppercase()) ?: return null
            val (b, f, z, r, n) = m.destructured
            val building = Building.fromCode(b) ?: return null
            val floor = f.toInt()
            if (floor !in building.floors) return null
            return DeskId(b, floor, z, r.toInt(), n)
        }
    }
}

/** The locally-stored user profile. Identity never leaves the device. */
data class UserProfile(
    val staffId: String,
    val name: String,
    val supervisor: String,
    val building: String,
    val floor: Int,
    val zone: String,
    val deskArea: String,
) {
    val isComplete: Boolean
        get() = staffId.isNotBlank() && name.isNotBlank() && deskArea.isNotBlank()
}

enum class Building(val code: String, val label: String, val floors: IntRange) {
    TOWER("T", "Tower", 3..6),
    RIVIERA("R", "Riviera", 3..5);

    companion object {
        fun fromCode(code: String): Building? = entries.firstOrNull { it.code == code }
    }
}

enum class FeedbackCategory(val label: String) {
    ELEVATORS("Elevators"),
    KITCHEN("Kitchen"),
    DESK_AREA("Desk Area"),
    MEETING_ROOMS("Meeting Rooms"),
    BATHROOMS("Bathrooms"),
    TEMPERATURE("Temperature / A/C"),
    PARKING("Parking"),
    OTHER("Other"),
}

/** Drives ticket suppression: positive feedback never creates a ticket. */
enum class FeedbackSentiment { POSITIVE, ISSUE }

enum class TicketRoute { EMAIL, JIRA }

enum class TicketStatus(val label: String) {
    OPEN("Open"),
    IN_PROGRESS("In progress"),
    RESOLVED("Resolved"),
}

/** A community-visible feedback item as stored/shared (linked only by userId; null = anonymous). */
data class CommunityFeedback(
    val id: String,
    val userId: String?,
    val category: FeedbackCategory,
    val sentiment: FeedbackSentiment,
    val message: String,
    val building: String?,
    val floor: Int?,
    val location: String?,
    val photoRef: String?,
    val createdAt: Long,
    val votes: Int = 0,
    val votedByMe: Boolean = false,
)

data class LeaderboardEntry(
    val userId: String,
    val displayName: String,
    val publicFeedbackCount: Int,
    val isOfficeChampion: Boolean = false,
    val isCurrentUser: Boolean = false,
)

data class Reward(
    val id: String,
    val title: String,
    val threshold: Int,
    val unlocked: Boolean,
    val progress: String,
    val hint: String,
)

// ---------------------------------------------------------------------------
// Shared (Firebase-synced) surfaces
// ---------------------------------------------------------------------------

/**
 * Live occupancy of one shared fridge on a floor. Synced across devices; carries no PII —
 * [updatedBy] is a Staff ID only. Occupancy is a 0..100 percentage.
 */
data class FridgeOccupancy(
    val fridgeId: String,
    val occupancy: Int,
    val updatedBy: String? = null,
    val updatedAt: Long = 0L,
) {
    init {
        require(occupancy in 0..100) { "occupancy must be 0..100" }
    }
}

/**
 * One person's daily pulse mood, synced so the team can see company/floor averages.
 * Linked only by [userId] + location (building/floor) + mood — never names.
 */
data class PulseRecord(
    val userId: String,
    val date: String, // ISO yyyy-MM-dd
    val mood: Int,     // 1..5
    val building: String? = null,
    val floor: Int? = null,
)

/** One day in the weekly pulse graph: the viewer's own mood + floor & company averages. */
data class WeeklyPulsePoint(
    val date: String,
    val youMood: Double? = null,
    val floorAverage: Double? = null,
    val companyAverage: Double? = null,
)

/** Aggregated weekly pulse: per-day series + week-long averages for you/floor/company. */
data class WeeklyPulse(
    val days: List<WeeklyPulsePoint> = emptyList(),
    val youAverage: Double? = null,
    val floorAverage: Double? = null,
    val companyAverage: Double? = null,
) {
    val hasData: Boolean get() = days.any { it.companyAverage != null }
}

/** App theme preference. Resolved against the current system setting. */
enum class ThemeMode {
    SYSTEM, LIGHT, DARK;

    /** Whether dark colors should be used given the current [systemDark] state. */
    fun isDark(systemDark: Boolean): Boolean = when (this) {
        SYSTEM -> systemDark
        LIGHT -> false
        DARK -> true
    }
}

enum class PhotoAnalysisFailure {
    DISABLED,
    UNAVAILABLE,
    TIMEOUT,
}

/** Result of analyzing a feedback photo into a user-reviewable issue draft. */
data class PhotoCategorizationResult(
    val detectedIssue: String? = null,
    val description: String? = null,
    val suggestedCategory: FeedbackCategory? = null,
    val confidence: Float = 0f,
    val failure: PhotoAnalysisFailure? = null,
) {
    init {
        require(confidence in 0f..1f) { "confidence must be between 0 and 1" }
    }
}
