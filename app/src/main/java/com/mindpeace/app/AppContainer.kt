package com.mindpeace.app

import android.app.Application
import com.mindpeace.app.data.InstalledAppsRepository
import com.mindpeace.app.data.SettingsRepository
import com.mindpeace.app.session.SessionCoordinator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class AppContainer(app: Application) {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val settings = SettingsRepository(app, scope)
    val installedApps = InstalledAppsRepository(app)
    val session = SessionCoordinator(app, settings, installedApps, scope)
}
