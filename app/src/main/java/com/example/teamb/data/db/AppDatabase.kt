package com.example.teamb.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        DailyPulseEntity::class,
        FreezerItemEntity::class,
        FeedbackEntity::class,
        TicketEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dailyPulseDao(): DailyPulseDao
    abstract fun freezerDao(): FreezerDao
    abstract fun feedbackDao(): FeedbackDao
    abstract fun ticketDao(): TicketDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "teamb.db",
                ).fallbackToDestructiveMigration().build().also { instance = it }
            }
    }
}
