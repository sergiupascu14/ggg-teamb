package com.example.teamb

import android.content.Context
import com.example.teamb.data.community.CommunityRepository
import com.example.teamb.data.community.InMemoryCommunityRepository
import com.example.teamb.data.datastore.CredentialStore
import com.example.teamb.data.datastore.DataStoreProfileStore
import com.example.teamb.data.datastore.EncryptedCredentialStore
import com.example.teamb.data.datastore.ProfileStore
import com.example.teamb.data.db.AppDatabase
import com.example.teamb.data.desk.DeskAllocationRepository
import com.example.teamb.data.integration.MockGarminAdDirectoryService
import com.example.teamb.data.integration.MockJiraTicketRouter
import com.example.teamb.data.integration.MlKitPhotoIssueDetector
import com.example.teamb.data.integration.PhotoIssueDetector
import com.example.teamb.data.integration.TicketRouter
import com.example.teamb.data.model.CommunityFeedback
import com.example.teamb.data.model.FeedbackCategory
import com.example.teamb.data.model.FeedbackSentiment
import com.example.teamb.data.repository.DailyPulseRepository
import com.example.teamb.data.repository.FeedbackRepository
import com.example.teamb.data.repository.FreezerRepository
import com.example.teamb.data.repository.GamificationRepository
import com.example.teamb.data.repository.TicketRepository
import com.example.teamb.data.util.Clock
import com.example.teamb.data.util.SystemClock

/** Manual dependency container. Held by [TeamBApp] and reachable from ViewModels. */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    val clock: Clock = SystemClock()

    val desk: DeskAllocationRepository by lazy { DeskAllocationRepository.fromAsset(appContext) }

    private val db by lazy { AppDatabase.get(appContext) }

    val profileStore: ProfileStore by lazy { DataStoreProfileStore(appContext) }
    val credentialStore: CredentialStore by lazy { EncryptedCredentialStore(appContext) }

    val directory by lazy { MockGarminAdDirectoryService(desk) }
    val ticketRouter: TicketRouter by lazy { MockJiraTicketRouter() }
    val photoDetector: PhotoIssueDetector by lazy { MlKitPhotoIssueDetector(appContext) }

    val community: CommunityRepository by lazy { InMemoryCommunityRepository(seedCommunity()) }

    val dailyPulseRepository by lazy { DailyPulseRepository(db.dailyPulseDao(), clock) }
    val freezerRepository by lazy { FreezerRepository(db.freezerDao(), clock) }
    val feedbackRepository by lazy {
        FeedbackRepository(db.feedbackDao(), db.ticketDao(), community, ticketRouter, clock)
    }
    val ticketRepository by lazy { TicketRepository(db.ticketDao()) }
    val gamificationRepository by lazy { GamificationRepository(community, desk) }

    /** Seed the shared newsfeed with believable sample entries so voting/filtering demo before
     *  multiple real devices connect. Linked only by userId (real ids resolve to names locally). */
    private fun seedCommunity(): List<CommunityFeedback> {
        val sampleIds = desk.desks.mapNotNull { it.staffId }.distinct().take(4)
        val now = clock.nowMillis()
        val day = 86_400_000L
        return listOf(
            CommunityFeedback("seed-1", sampleIds.getOrNull(0), FeedbackCategory.KITCHEN, FeedbackSentiment.POSITIVE,
                "The new coffee machine in the kitchen is fantastic!", "T", 4, "Tower floor 4 kitchen", null, now - day, 7),
            CommunityFeedback("seed-2", sampleIds.getOrNull(1), FeedbackCategory.ELEVATORS, FeedbackSentiment.ISSUE,
                "Elevator B has been slow all week.", "T", 5, "Tower elevators", null, now - 2 * day, 12),
            CommunityFeedback("seed-3", null, FeedbackCategory.BATHROOMS, FeedbackSentiment.ISSUE,
                "Hand dryer on floor 3 is broken.", "R", 3, "Riviera floor 3", null, now - 3 * day, 4),
            CommunityFeedback("seed-4", sampleIds.getOrNull(2), FeedbackCategory.DESK_AREA, FeedbackSentiment.POSITIVE,
                "Love the new plants in the desk area!", "T", 6, "Tower floor 6", null, now - 4 * day, 9),
        )
    }
}
