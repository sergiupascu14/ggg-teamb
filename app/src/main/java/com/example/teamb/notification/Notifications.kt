package com.example.teamb.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.teamb.R

object NotificationChannels {
    const val REMINDERS = "reminders"

    fun ensure(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                REMINDERS,
                "Reminders",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply { description = "Daily Pulse and freezer reminders" }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
}

object Notifier {
    fun canPost(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        } else true

    fun show(context: Context, id: Int, title: String, text: String) {
        if (!canPost(context)) return
        NotificationChannels.ensure(context)
        val notification = androidx.core.app.NotificationCompat.Builder(context, NotificationChannels.REMINDERS)
            .setSmallIcon(R.mipmap.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(id, notification) }
    }
}
