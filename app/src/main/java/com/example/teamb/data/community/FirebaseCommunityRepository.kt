package com.example.teamb.data.community

import com.example.teamb.data.model.CommunityFeedback
import com.example.teamb.data.model.FeedbackCategory
import com.example.teamb.data.model.FeedbackSentiment
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Firebase Realtime Database implementation of the shared community surface.
 *
 * Wire-up requires a `google-services.json` / configured [DatabaseReference]; until then the app
 * uses [InMemoryCommunityRepository]. The schema stores ONLY userId + content, never PII.
 *
 * Layout:
 *   feedback/{id} -> { userId, category, sentiment, message, building, floor, location, photoRef, createdAt }
 *   votes/{id}/{voterId} -> true
 */
class FirebaseCommunityRepository(
    private val root: DatabaseReference,
) : CommunityRepository {

    private val feedbackRef get() = root.child("feedback")
    private val votesRef get() = root.child("votes")

    override fun observeFeedback(currentUserId: String?): Flow<List<CommunityFeedback>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val feedback = snapshot.child("feedback")
                val votes = snapshot.child("votes")
                val items = feedback.children.mapNotNull { it.toCommunityFeedback() }
                    .map { item ->
                        val voters = votes.child(item.id).children.mapNotNull { it.key }
                        item.copy(
                            votes = voters.size,
                            votedByMe = currentUserId != null && currentUserId in voters,
                        )
                    }
                    .sortedByDescending { it.createdAt }
                trySend(items)
            }

            override fun onCancelled(error: DatabaseError) { close(error.toException()) }
        }
        root.addValueEventListener(listener)
        awaitClose { root.removeEventListener(listener) }
    }

    override suspend fun publish(feedback: CommunityFeedback) {
        val payload = mapOf(
            "userId" to feedback.userId,
            "category" to feedback.category.name,
            "sentiment" to feedback.sentiment.name,
            "message" to feedback.message,
            "building" to feedback.building,
            "floor" to feedback.floor,
            "location" to feedback.location,
            "photoRef" to feedback.photoRef,
            "createdAt" to feedback.createdAt,
        )
        feedbackRef.child(feedback.id).setValue(payload).await()
    }

    override suspend fun toggleVote(itemId: String, voterId: String) {
        val ref = votesRef.child(itemId).child(voterId)
        val existing = ref.get().await().getValue(Boolean::class.java)
        if (existing == true) ref.removeValue().await() else ref.setValue(true).await()
    }

    override suspend fun snapshot(): List<CommunityFeedback> {
        val snap = feedbackRef.get().await()
        return snap.children.mapNotNull { it.toCommunityFeedback() }
    }

    private fun DataSnapshot.toCommunityFeedback(): CommunityFeedback? {
        val category = child("category").getValue(String::class.java) ?: return null
        val sentiment = child("sentiment").getValue(String::class.java) ?: return null
        return CommunityFeedback(
            id = key ?: return null,
            userId = child("userId").getValue(String::class.java),
            category = runCatching { FeedbackCategory.valueOf(category) }.getOrNull() ?: return null,
            sentiment = runCatching { FeedbackSentiment.valueOf(sentiment) }.getOrNull() ?: return null,
            message = child("message").getValue(String::class.java) ?: "",
            building = child("building").getValue(String::class.java),
            floor = child("floor").getValue(Int::class.java),
            location = child("location").getValue(String::class.java),
            photoRef = child("photoRef").getValue(String::class.java),
            createdAt = child("createdAt").getValue(Long::class.java) ?: 0L,
        )
    }
}
