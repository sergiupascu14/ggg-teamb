package com.example.teamb.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyPulseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: DailyPulseEntity)

    @Query("SELECT * FROM daily_pulse WHERE userId = :userId AND date = :date LIMIT 1")
    suspend fun forDate(userId: String, date: String): DailyPulseEntity?

    @Query("SELECT * FROM daily_pulse WHERE userId = :userId ORDER BY date DESC")
    fun observeAll(userId: String): Flow<List<DailyPulseEntity>>

    @Query("SELECT date FROM daily_pulse WHERE userId = :userId ORDER BY date DESC")
    suspend fun allDates(userId: String): List<String>
}

@Dao
interface FreezerDao {
    @Insert
    suspend fun insert(item: FreezerItemEntity): Long

    @Update
    suspend fun update(item: FreezerItemEntity)

    @Query("SELECT * FROM freezer_items WHERE ownerId = :ownerId AND present = 1 ORDER BY checkInAt DESC")
    fun observePresent(ownerId: String): Flow<List<FreezerItemEntity>>

    @Query("SELECT * FROM freezer_items WHERE present = 1")
    suspend fun allPresent(): List<FreezerItemEntity>

    @Query("SELECT * FROM freezer_items WHERE id = :id LIMIT 1")
    suspend fun byId(id: Long): FreezerItemEntity?
}

@Dao
interface FeedbackDao {
    @Insert
    suspend fun insert(item: FeedbackEntity): Long

    @Query("SELECT * FROM feedback ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<FeedbackEntity>>

    @Query("SELECT * FROM feedback WHERE id = :id LIMIT 1")
    suspend fun byId(id: Long): FeedbackEntity?
}

@Dao
interface TicketDao {
    @Insert
    suspend fun insert(item: TicketEntity): Long

    @Update
    suspend fun update(item: TicketEntity)

    @Query("SELECT * FROM tickets ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<TicketEntity>>
}
