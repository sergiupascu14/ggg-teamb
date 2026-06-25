package com.example.teamb.data.community

import com.example.teamb.data.model.CommunityFeedback
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * Shared, multi-device community surface (newsfeed + votes).
 *
 * Privacy: records are linked ONLY by [CommunityFeedback.userId] (the Staff ID) plus content —
 * never names/supervisors/emails. Anonymous items carry a null userId. Display names are
 * resolved locally from the desk allocation dataset by the UI layer.
 */
interface CommunityRepository {
    /** Stream of community-visible feedback, newest first, with vote state for [currentUserId]. */
    fun observeFeedback(currentUserId: String?): Flow<List<CommunityFeedback>>

    suspend fun publish(feedback: CommunityFeedback)

    /** Toggles the current user's vote on an item. */
    suspend fun toggleVote(itemId: String, voterId: String)

    /** One-shot snapshot (used to derive the leaderboard). */
    suspend fun snapshot(): List<CommunityFeedback>
}

private data class CommunityRecord(
    val item: CommunityFeedback,
    val voters: Set<String>,
)

/** Default in-memory implementation: fully demoable, optionally seeded. */
class InMemoryCommunityRepository(
    seed: List<CommunityFeedback> = emptyList(),
) : CommunityRepository {

    private val state = MutableStateFlow(
        seed.map { CommunityRecord(it.copy(votedByMe = false), emptySet()) }
    )

    override fun observeFeedback(currentUserId: String?): Flow<List<CommunityFeedback>> =
        state.map { records ->
            records
                .sortedByDescending { it.item.createdAt }
                .map { r ->
                    r.item.copy(
                        votes = r.voters.size,
                        votedByMe = currentUserId != null && currentUserId in r.voters,
                    )
                }
        }

    override suspend fun publish(feedback: CommunityFeedback) {
        state.update { records -> records + CommunityRecord(feedback, emptySet()) }
    }

    override suspend fun toggleVote(itemId: String, voterId: String) {
        state.update { records ->
            records.map { r ->
                if (r.item.id != itemId) r
                else {
                    val voters = if (voterId in r.voters) r.voters - voterId else r.voters + voterId
                    r.copy(voters = voters)
                }
            }
        }
    }

    override suspend fun snapshot(): List<CommunityFeedback> =
        state.value.map { it.item.copy(votes = it.voters.size) }
}
