package com.mindpeace.app.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mindpeace.app.MindPeaceApp
import com.mindpeace.app.data.UNLIMITED_BUDGET
import com.mindpeace.app.data.WatchedApp
import com.mindpeace.app.util.formatDurationMillis
import com.mindpeace.app.util.formatMinutesShort
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class HomeItem(
    val packageName: String,
    val label: String,
    val enabled: Boolean,
    val usedLine: String,
    val remainingLine: String,
    val remainingMillis: Long,
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val container = (application as MindPeaceApp).container

    val globalDailyLimitMinutes: StateFlow<Int> = container.settings.globalDailyLimitMinutes

    val items: StateFlow<List<HomeItem>> = combine(
        container.settings.watchedApps,
        container.settings.dailyUsage,
        container.settings.globalDailyLimitMinutes,
    ) { watched, _, _ ->
        watched.map { app -> toItem(app) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private fun toItem(app: WatchedApp): HomeItem {
        val label = container.installedApps.labelOf(app.packageName)
        val used = container.settings.usedMillisToday(app.packageName)
        val remaining = container.settings.remainingDailyMillis(app.packageName)
        val global = container.settings.globalDailyLimitMinutes.value
        val usedLine = when {
            app.dailyLimitMinutes > 0 -> getApplication<Application>().getString(
                com.mindpeace.app.R.string.home_used_limit,
                formatDurationMillis(getApplication(), used),
                formatMinutesShort(getApplication(), app.dailyLimitMinutes),
            )
            global > 0 -> getApplication<Application>().getString(
                com.mindpeace.app.R.string.home_used_shared,
                formatDurationMillis(getApplication(), used),
            )
            else -> getApplication<Application>().getString(
                com.mindpeace.app.R.string.home_used_unlimited,
                formatDurationMillis(getApplication(), used),
            )
        }
        val remainingLine = when {
            !app.enabled -> getApplication<Application>().getString(com.mindpeace.app.R.string.home_off)
            remaining <= 0L -> getApplication<Application>().getString(com.mindpeace.app.R.string.home_remaining_none)
            remaining >= UNLIMITED_BUDGET / 2 -> ""
            else -> getApplication<Application>().getString(
                com.mindpeace.app.R.string.home_remaining,
                formatDurationMillis(getApplication(), remaining),
            )
        }
        return HomeItem(
            packageName = app.packageName,
            label = label,
            enabled = app.enabled,
            usedLine = usedLine,
            remainingLine = remainingLine,
            remainingMillis = remaining,
        )
    }
}
