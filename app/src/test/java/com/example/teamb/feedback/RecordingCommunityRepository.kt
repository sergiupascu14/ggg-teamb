package com.example.teamb.feedback

import com.example.teamb.data.community.CommunityRepository
import com.example.teamb.data.model.CommunityFeedback
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** [CommunityRepository] that records every publish so tests can assert privacy behavior. */
class RecordingCommunityRepository : CommunityRepository {
    val published = mutableListOf<CommunityFeedback>()

    override fun observeFeedback(currentUserId: String?): Flow<List<CommunityFeedback>> =
        flowOf(published.toList())

    override suspend fun publish(feedback: CommunityFeedback) {
        published += feedback
    }

    override suspend fun toggleVote(itemId: String, voterId: String) {}

    override suspend fun snapshot(): List<CommunityFeedback> = published.toList()
}
