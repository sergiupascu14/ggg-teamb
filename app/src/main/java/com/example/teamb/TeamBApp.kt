package com.example.teamb

import android.app.Application
import com.example.teamb.notification.NotificationChannels

class TeamBApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        NotificationChannels.ensure(this)
    }
}
