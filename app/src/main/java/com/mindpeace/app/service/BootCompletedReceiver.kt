package com.mindpeace.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mindpeace.app.MindPeaceApp
import com.mindpeace.app.util.Notifications
import com.mindpeace.app.work.WorkScheduler

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_LOCKED_BOOT_COMPLETED &&
            action != "android.intent.action.QUICKBOOT_POWERON" &&
            action != "com.htc.intent.action.QUICKBOOT_POWERON"
        ) {
            return
        }
        val pending = goAsync()
        Thread {
            try {
                Notifications.ensureChannels(context)
                WorkScheduler.ensureScheduled(context)
                val app = context.applicationContext as? MindPeaceApp ?: return@Thread
                if (!app.container.settings.peekOnboardedBlocking()) return@Thread
                val session = app.container.session.session.value
                if (session != null && session.remainingMillis > 0L) {
                    SessionForegroundService.start(app)
                }
            } finally {
                pending.finish()
            }
        }.start()
    }
}
