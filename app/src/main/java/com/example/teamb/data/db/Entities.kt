package com.example.teamb.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_pulse")
data class DailyPulseEntity(
    @PrimaryKey val date: String, // ISO yyyy-MM-dd, one per day
    val mood: Int,                // 1..5
    val note: String?,
    val createdAt: Long,
)

@Entity(tableName = "freezer_items")
data class FreezerItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,
    val ownerId: String,
    val checkInAt: Long,
    val checkOutAt: Long?,
    val present: Boolean,
)

@Entity(tableName = "feedback")
data class FeedbackEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val category: String,
    val sentiment: String,
    val message: String,
    val photoUri: String?,
    val anonymous: Boolean,
    val location: String?,
    val building: String?,
    val floor: Int?,
    val communityVisible: Boolean,
    val communityId: String?, // id in the shared community store, if published
    val createdAt: Long,
)

@Entity(tableName = "tickets")
data class TicketEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val feedbackId: Long,
    val category: String,
    val route: String,
    val externalId: String,
    val status: String,
    val createdAt: Long,
)
