package com.example.teamb.data.repository

import com.example.teamb.data.community.CommunityRepository
import com.example.teamb.data.db.DailyPulseDao
import com.example.teamb.data.db.DailyPulseEntity
import com.example.teamb.data.db.FeedbackDao
import com.example.teamb.data.db.FeedbackEntity
import com.example.teamb.data.db.FreezerDao
import com.example.teamb.data.db.FreezerItemEntity
import com.example.teamb.data.db.TicketDao
import com.example.teamb.data.db.TicketEntity
import com.example.teamb.data.desk.DeskAllocationRepository
import com.example.teamb.data.integration.PhotoIssueDetector
import com.example.teamb.data.integration.TicketDraft
import com.example.teamb.data.integration.TicketRouter
import com.example.teamb.data.model.CommunityFeedback
import com.example.teamb.data.model.FeedbackCategory
import com.example.teamb.data.model.FeedbackSentiment
import com.example.teamb.data.model.LeaderboardEntry
import com.example.teamb.data.model.Reward
import com.example.teamb.data.model.TicketStatus
import com.example.teamb.data.util.Clock
import com.example.teamb.data.util.Dates
import com.example.teamb.data.util.StreakCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// ---------------------------------------------------------------------------
// Daily Pulse
// ---------------------------------------------------------------------------
class DailyPulseRepository(
    private val dao: DailyPulseDao,
    private val clock: Clock,
) {
    fun observeEntries(userId: String): Flow<List<DailyPulseEntity>> = dao.observeAll(userId)

    suspend fun checkedInToday(userId: String): Boolean =
        dao.forDate(userId, Dates.isoDate(clock.nowMillis())) != null

    /** Persists today's check-in for [userId]. No-op visible state if already checked in (REPLACE). */
    suspend fun submit(userId: String, mood: Int, note: String?) {
        val now = clock.nowMillis()
        dao.upsert(DailyPulseEntity(userId, Dates.isoDate(now), mood.coerceIn(1, 5), note?.takeIf { it.isNotBlank() }, now))
    }

    suspend fun currentStreak(userId: String): Int {
        val today = Dates.epochDay(clock.nowMillis())
        val days = dao.allDates(userId).mapNotNull { runCatching { isoToEpochDay(it) }.getOrNull() }.toSet()
        return StreakCalculator.currentStreak(days, today)
    }

    private fun isoToEpochDay(iso: String): Long {
        val (y, m, d) = iso.split("-").map { it.toInt() }
        // days since epoch via civil-from-date (Howard Hinnant's algorithm)
        val yy = if (m <= 2) y - 1 else y
        val era = (if (yy >= 0) yy else yy - 399) / 400
        val yoe = yy - era * 400
        val doy = (153 * (if (m > 2) m - 3 else m + 9) + 2) / 5 + d - 1
        val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
        return era.toLong() * 146097 + doe - 719468
    }
}

// ---------------------------------------------------------------------------
// Freezer
// ---------------------------------------------------------------------------
class FreezerRepository(
    private val dao: FreezerDao,
    private val clock: Clock,
) {
    fun observeItems(ownerId: String): Flow<List<FreezerItemEntity>> = dao.observePresent(ownerId)

    suspend fun checkIn(label: String, ownerId: String): Long =
        dao.insert(FreezerItemEntity(label = label, ownerId = ownerId, checkInAt = clock.nowMillis(), checkOutAt = null, present = true))

    suspend fun checkOut(id: Long) {
        val item = dao.byId(id) ?: return
        dao.update(item.copy(present = false, checkOutAt = clock.nowMillis()))
    }

    /** Items older than [thresholdDays] that are still present — drives cleanup reminders. */
    suspend fun staleItems(thresholdDays: Int): List<FreezerItemEntity> {
        val cutoff = clock.nowMillis() - thresholdDays * 86_400_000L
        return dao.allPresent().filter { it.checkInAt < cutoff }
    }
}

// ---------------------------------------------------------------------------
// Feedback + Ticketing
// ---------------------------------------------------------------------------
data class FeedbackForm(
    val category: FeedbackCategory? = null,
    val sentiment: FeedbackSentiment,
    val issueLabel: String? = null,
    val message: String,
    val photoUri: String? = null,
    // Base64-encoded, downscaled copy of [photoUri] for the shared community feed (set at submit).
    val photoData: String? = null,
    val anonymous: Boolean = false,
    val location: String? = null,
    val building: String? = null,
    val floor: Int? = null,
    val communityVisible: Boolean = false,
    val wantsTicket: Boolean = false,
)

data class SubmitResult(
    val feedbackId: Long,
    val ticketCreated: Boolean,
    val ticketExternalId: String? = null,
    val ticketSuppressed: Boolean = false,
)

class FeedbackRepository(
    private val feedbackDao: FeedbackDao,
    private val ticketDao: TicketDao,
    private val community: CommunityRepository,
    private val ticketRouter: TicketRouter,
    private val clock: Clock,
) {
    fun observeFeedback(): Flow<List<FeedbackEntity>> = feedbackDao.observeAll()

    /** Validation: category implied by type; message must be non-blank. */
    fun validate(form: FeedbackForm): String? = when {
        form.category == null -> "Please choose a category"
        form.message.isBlank() -> "Please describe your feedback"
        else -> null
    }

    suspend fun submit(form: FeedbackForm, currentUserId: String): SubmitResult {
        val category = requireNotNull(form.category) { "category must be selected before submit" }
        val now = clock.nowMillis()
        val communityId = if (form.communityVisible) "fb-$now" else null
        val feedbackId = feedbackDao.insert(
            FeedbackEntity(
                category = category.name,
                sentiment = form.sentiment.name,
                issueLabel = form.issueLabel,
                message = form.message,
                photoUri = form.photoUri,
                anonymous = form.anonymous,
                location = form.location,
                building = form.building,
                floor = form.floor,
                communityVisible = form.communityVisible,
                communityId = communityId,
                createdAt = now,
            )
        )

        if (form.communityVisible && communityId != null) {
            community.publish(
                CommunityFeedback(
                    id = communityId,
                    userId = if (form.anonymous) null else currentUserId,
                    category = category,
                    sentiment = form.sentiment,
                    message = form.message,
                    building = form.building,
                    floor = form.floor,
                    location = form.location,
                    // Share the portable base64 copy (not the device-local content:// uri).
                    photoRef = form.photoData,
                    createdAt = now,
                )
            )
        }

        // Ticket suppression: positive feedback never creates a ticket.
        if (form.sentiment == FeedbackSentiment.POSITIVE) {
            return SubmitResult(feedbackId, ticketCreated = false, ticketSuppressed = form.wantsTicket)
        }
        if (!form.wantsTicket) {
            return SubmitResult(feedbackId, ticketCreated = false)
        }
        val routed = ticketRouter.routeTicket(
            TicketDraft(feedbackId, category, form.message, form.location, form.photoUri)
        )
        ticketDao.insert(
            TicketEntity(
                feedbackId = feedbackId,
                category = category.name,
                route = routed.route.name,
                externalId = routed.externalId,
                status = TicketStatus.OPEN.name,
                createdAt = now,
            )
        )
        return SubmitResult(feedbackId, ticketCreated = true, ticketExternalId = routed.externalId)
    }
}

class TicketRepository(private val dao: TicketDao) {
    fun observeTickets(): Flow<List<TicketEntity>> = dao.observeAll()

    suspend fun updateStatus(ticket: TicketEntity, status: TicketStatus) {
        dao.update(ticket.copy(status = status.name))
    }
}

// ---------------------------------------------------------------------------
// Gamification (leaderboard + rewards) — derived from shared community data.
// ---------------------------------------------------------------------------
class GamificationRepository(
    private val community: CommunityRepository,
    private val desk: DeskAllocationRepository,
) {
    /** Ranks users by PUBLIC (non-anonymous) feedback count; top user is Office Champion. */
    suspend fun leaderboard(currentUserId: String?): List<LeaderboardEntry> {
        val counts = community.snapshot()
            .mapNotNull { it.userId }            // anonymous items carry no userId → excluded
            .groupingBy { it }
            .eachCount()
        val ranked = counts.entries
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
        val topCount = ranked.firstOrNull()?.value
        return ranked.mapIndexed { index, (userId, count) ->
            LeaderboardEntry(
                userId = userId,
                displayName = desk.displayName(userId) ?: "User $userId",
                publicFeedbackCount = count,
                isOfficeChampion = index == 0 && count == topCount && count > 0,
                isCurrentUser = userId == currentUserId,
            )
        }
    }

    fun rewardsFor(points: Int): List<Reward> = REWARD_TIERS.map { tier ->
        val unlocked = points >= tier.threshold
        Reward(
            id = tier.id,
            title = tier.title,
            threshold = tier.threshold,
            unlocked = unlocked,
            progress = if (unlocked) "✓ Unlocked" else "$points/${tier.threshold} feedbacks",
            hint = if (unlocked) "Earned! You reached ${tier.threshold} feedbacks"
                   else "Submit ${tier.threshold} public feedbacks to unlock",
        )
    }

    private data class RewardTier(
        val id: String,
        val title: String,
        val threshold: Int,
    )

    private companion object {
        val REWARD_TIERS = listOf(
            RewardTier("first-steps", "First Steps", 10),
            RewardTier("regular", "Office Regular", 50),
            RewardTier("champion", "Office Champion", 100),
        )
    }
}
