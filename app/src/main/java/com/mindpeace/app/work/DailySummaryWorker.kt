package com.mindpeace.app.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mindpeace.app.MindPeaceApp
import com.mindpeace.app.R
import com.mindpeace.app.util.Notifications
import com.mindpeace.app.util.Permissions
import com.mindpeace.app.util.millisToWholeMinutes
import com.mindpeace.app.util.todayDateKey
import com.mindpeace.app.util.yesterdayDateKey

class DailySummaryWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? MindPeaceApp ?: return Result.success()
        val settings = app.container.settings
        if (!settings.readOnboarded()) return Result.success()
        if (!Permissions.areNotificationsEnabled(app)) return Result.success()

        val today = todayDateKey()
        if (settings.readLastSummaryDate() == today) return Result.success()

        settings.snapshotToday()

        val watched = settings.watchedApps.value
            .ifEmpty { settings.readUsageHistory().let { _ -> settings.watchedApps.value } }
        val history = settings.readUsageHistory()
        val yesterday = yesterdayDateKey()
        val todayMap = history.days[today].orEmpty()
        val yestMap = history.days[yesterday]

        val packages = (watched.map { it.packageName } + todayMap.keys + (yestMap?.keys ?: emptySet()))
            .toSet()
        val todayTotal = packages.sumOf { todayMap[it] ?: 0L }
        val todayMin = millisToWholeMinutes(todayTotal)

        val body = if (yestMap == null) {
            if (todayMin <= 0) app.getString(R.string.summary_none)
            else app.getString(R.string.summary_no_yesterday, todayMin)
        } else {
            val yestTotal = packages.sumOf { yestMap[it] ?: 0L }
            val yestMin = millisToWholeMinutes(yestTotal)
            val delta = todayMin - yestMin
            val base = when {
                todayMin <= 0 && yestMin <= 0 -> app.getString(R.string.summary_none)
                delta < 0 -> app.getString(R.string.summary_less, todayMin, -delta)
                delta > 0 -> app.getString(R.string.summary_more, todayMin, delta)
                else -> app.getString(R.string.summary_same, todayMin)
            }
            val standout = standoutLine(app, todayMap, yestMap)
            if (standout.isNullOrBlank()) base else "$base $standout"
        }

        Notifications.ensureChannels(app)
        try {
            Permissions.notificationManager(app)
                .notify(Notifications.ID_SUMMARY, Notifications.summaryNotification(app, body))
        } catch (_: Exception) {
            return Result.retry()
        }
        settings.markSummarySent(today)
        return Result.success()
    }

    private fun standoutLine(
        app: Context,
        todayMap: Map<String, Long>,
        yestMap: Map<String, Long>,
    ): String? {
        val mind = app.applicationContext as? MindPeaceApp ?: return null
        var bestPkg: String? = null
        var bestAbs = 0
        var bestDelta = 0
        val pkgs = (todayMap.keys + yestMap.keys)
        for (pkg in pkgs) {
            val d = millisToWholeMinutes(todayMap[pkg] ?: 0L) - millisToWholeMinutes(yestMap[pkg] ?: 0L)
            if (kotlin.math.abs(d) >= 5 && kotlin.math.abs(d) > bestAbs) {
                bestAbs = kotlin.math.abs(d)
                bestDelta = d
                bestPkg = pkg
            }
        }
        val pkg = bestPkg ?: return null
        val label = mind.container.installedApps.labelOf(pkg)
        return if (bestDelta < 0) {
            app.getString(R.string.summary_standout_less, label, -bestDelta)
        } else {
            app.getString(R.string.summary_standout_more, label, bestDelta)
        }
    }
}
