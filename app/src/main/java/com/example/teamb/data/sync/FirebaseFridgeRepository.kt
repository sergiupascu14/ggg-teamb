package com.example.teamb.data.sync

import com.example.teamb.data.model.FridgeOccupancy
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Firebase Realtime Database implementation of the shared per-floor fridge occupancy.
 *
 * Layout: fridges/{floorKey}/{fridgeId} -> { occupancy, updatedBy, updatedAt }
 */
class FirebaseFridgeRepository(
    private val root: DatabaseReference,
) : FridgeRepository {

    private val fridgesRef get() = root.child("fridges")

    override fun observeFloor(floorKey: String): Flow<List<FridgeOccupancy>> = callbackFlow {
        val ref = fridgesRef.child(floorKey)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val items = snapshot.children.mapNotNull { it.toFridge() }
                trySend(Fridges.normalize(items))
            }

            override fun onCancelled(error: DatabaseError) { close(error.toException()) }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    override suspend fun setOccupancy(
        floorKey: String,
        fridgeId: String,
        occupancy: Int,
        userId: String?,
        updatedAt: Long,
    ) {
        val payload = mapOf(
            "occupancy" to occupancy.coerceIn(0, 100),
            "updatedBy" to userId,
            "updatedAt" to updatedAt,
        )
        fridgesRef.child(floorKey).child(fridgeId).setValue(payload).await()
    }

    private fun DataSnapshot.toFridge(): FridgeOccupancy? {
        val id = key ?: return null
        val occupancy = child("occupancy").getValue(Int::class.java) ?: return null
        return FridgeOccupancy(
            fridgeId = id,
            occupancy = occupancy.coerceIn(0, 100),
            updatedBy = child("updatedBy").getValue(String::class.java),
            updatedAt = child("updatedAt").getValue(Long::class.java) ?: 0L,
        )
    }
}
