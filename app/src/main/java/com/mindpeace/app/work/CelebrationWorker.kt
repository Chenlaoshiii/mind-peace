package com.mindpeace.app.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mindpeace.app.MindPeaceApp
import com.mindpeace.app.R
import com.mindpeace.app.data.CELEBRATION_KIND_1D
import com.mindpeace.app.data.CELEBRATION_KIND_3D
import com.mindpeace.app.data.CELEBRATION_KIND_4H
import com.mindpeace.app.util.Notifications
import com.mindpeace.app.util.Permissions
import com.mindpeace.app.util.calendarDaysBetween
import com.mindpeace.app.util.isQuietHours
import kotlin.random.Random

class CelebrationWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? MindPeaceApp ?: return Result.success()
        val settings = app.container.settings
        if (!settings.readOnboarded()) return Result.success()
        if (app.container.session.session.value != null) return Result.success()
        if (isQuietHours()) return Result.success()
        if (!Permissions.areNotificationsEnabled(app)) return Result.success()

        val lastUsed = settings.readLastUsedWatchedAt()
        if (lastUsed <= 0L) return Result.success()
        val now = System.currentTimeMillis()
        val idle = now - lastUsed
        val days = calendarDaysBetween(lastUsed, now)
        val already = settings.readLastCelebratedKind()

        val kind = when {
            days >= 3 && already != CELEBRATION_KIND_3D -> CELEBRATION_KIND_3D
            days >= 1 && already != CELEBRATION_KIND_1D && already != CELEBRATION_KIND_3D -> CELEBRATION_KIND_1D
            idle >= FOUR_HOURS && already.isBlank() -> CELEBRATION_KIND_4H
            else -> return Result.success()
        }

        val templates = app.resources.getStringArray(R.array.celebration_templates)
        if (templates.isEmpty()) return Result.success()
        val lastIdx = settings.readLastCelebrationTemplate()
        val idx = nextTemplateIndex(templates.size, lastIdx)
        val body = templates[idx]

        Notifications.ensureChannels(app)
        try {
            Permissions.notificationManager(app)
                .notify(Notifications.ID_CELEBRATION, Notifications.celebrationNotification(app, body))
        } catch (_: Exception) {
            return Result.retry()
        }
        settings.markCelebrated(kind, idx, now)
        return Result.success()
    }

    private fun nextTemplateIndex(size: Int, lastIdx: Int): Int {
        if (size <= 1) return 0
        if (lastIdx !in 0 until size) return Random.nextInt(size)
        var n = Random.nextInt(size - 1)
        if (n >= lastIdx) n += 1
        return n
    }

    companion object {
        private const val FOUR_HOURS = 4L * 60L * 60L * 1000L
    }
}
