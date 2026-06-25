package com.example.teamb.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.teamb.TeamBApp
import java.util.concurrent.TimeUnit

/** Posts a Daily Pulse reminder only if the user has not checked in today. */
class DailyPulseReminderWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val container = (applicationContext as TeamBApp).container
        if (!container.dailyPulseRepository.checkedInToday()) {
            Notifier.show(applicationContext, 1001, "Daily Pulse", "How is the office today? Tap to check in.")
        }
        return Result.success()
    }
}

/** Posts a freezer cleanup reminder when items exceed the age threshold. */
class FreezerReminderWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val container = (applicationContext as TeamBApp).container
        val stale = container.freezerRepository.staleItems(FRESHNESS_DAYS)
        if (stale.isNotEmpty()) {
            Notifier.show(
                applicationContext, 1002, "Freezer cleanup",
                "You have ${stale.size} item(s) to clear from the freezer.",
            )
        }
        return Result.success()
    }

    private companion object {
        const val FRESHNESS_DAYS = 7
    }
}

object Reminders {
    fun schedule(context: Context) {
        val wm = WorkManager.getInstance(context)
        wm.enqueueUniquePeriodicWork(
            "daily-pulse-reminder",
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<DailyPulseReminderWorker>(1, TimeUnit.DAYS).build(),
        )
        wm.enqueueUniquePeriodicWork(
            "freezer-reminder",
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<FreezerReminderWorker>(1, TimeUnit.DAYS).build(),
        )
    }
}
