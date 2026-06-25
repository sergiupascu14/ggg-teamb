package com.example.teamb.data.sync

import com.example.teamb.data.model.PulseRecord
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Firebase Realtime Database implementation of the shared pulse store.
 *
 * Layout: pulse/{date}/{userId} -> { mood, building, floor, updatedAt }
 */
class FirebasePulseRepository(
    private val root: DatabaseReference,
) : PulseRepository {

    private val pulseRef get() = root.child("pulse")

    override suspend fun submit(record: PulseRecord, updatedAt: Long) {
        val payload = mapOf(
            "mood" to record.mood,
            "building" to record.building,
            "floor" to record.floor,
            "updatedAt" to updatedAt,
        )
        pulseRef.child(record.date).child(record.userId).setValue(payload).await()
    }

    override fun observeWeek(weekDates: List<String>): Flow<List<PulseRecord>> = callbackFlow {
        val week = weekDates.toSet()
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val records = snapshot.children
                    .filter { it.key in week }
                    .flatMap { dateNode ->
                        val date = dateNode.key ?: return@flatMap emptyList()
                        dateNode.children.mapNotNull { it.toRecord(date) }
                    }
                trySend(records)
            }

            override fun onCancelled(error: DatabaseError) { close(error.toException()) }
        }
        pulseRef.addValueEventListener(listener)
        awaitClose { pulseRef.removeEventListener(listener) }
    }

    private fun DataSnapshot.toRecord(date: String): PulseRecord? {
        val userId = key ?: return null
        val mood = child("mood").getValue(Int::class.java) ?: return null
        return PulseRecord(
            userId = userId,
            date = date,
            mood = mood,
            building = child("building").getValue(String::class.java),
            floor = child("floor").getValue(Int::class.java),
        )
    }
}
