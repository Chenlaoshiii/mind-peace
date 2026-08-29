package com.mindpeace.app.work

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.mindpeace.app.util.millisUntilNextHour
import java.util.concurrent.TimeUnit

object WorkScheduler {
    const val CELEBRATION_WORK = "mindpeace_celebration"
    const val SUMMARY_WORK = "mindpeace_daily_summary"

    fun ensureScheduled(context: Context) {
        val wm = WorkManager.getInstance(context.applicationContext)

        val celebration = PeriodicWorkRequestBuilder<CelebrationWorker>(
            20, TimeUnit.MINUTES,
            10, TimeUnit.MINUTES,
        ).build()
        wm.enqueueUniquePeriodicWork(
            CELEBRATION_WORK,
            ExistingPeriodicWorkPolicy.KEEP,
            celebration,
        )

        val summary = PeriodicWorkRequestBuilder<DailySummaryWorker>(
            1, TimeUnit.DAYS,
            2, TimeUnit.HOURS,
        ).setInitialDelay(millisUntilNextHour(21), TimeUnit.MILLISECONDS)
            .build()
        wm.enqueueUniquePeriodicWork(
            SUMMARY_WORK,
            ExistingPeriodicWorkPolicy.KEEP,
            summary,
        )
    }
}
