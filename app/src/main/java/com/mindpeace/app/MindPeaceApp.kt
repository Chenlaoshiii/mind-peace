package com.mindpeace.app

import android.app.Application
import com.mindpeace.app.util.Notifications
import com.mindpeace.app.work.WorkScheduler

class MindPeaceApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        Notifications.ensureChannels(this)
        WorkScheduler.ensureScheduled(this)
    }
}
