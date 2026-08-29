package com.mindpeace.app

import android.content.Context
import com.mindpeace.app.util.AppLocale
import com.mindpeace.app.util.Notifications
import com.mindpeace.app.work.WorkScheduler

class MindPeaceApp : android.app.Application() {
    lateinit var container: AppContainer
        private set

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(AppLocale.wrap(base))
    }

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        AppLocale.apply(this, container.settings.peekAppLocaleBlocking())
        Notifications.ensureChannels(this)
        WorkScheduler.ensureScheduled(this)
    }
}
