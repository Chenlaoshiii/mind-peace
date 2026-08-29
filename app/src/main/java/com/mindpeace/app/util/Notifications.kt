package com.mindpeace.app.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import com.mindpeace.app.MainActivity
import com.mindpeace.app.R
import com.mindpeace.app.ui.overlay.TimeUpActivity

object Notifications {
    const val CHANNEL_SESSION = "session"
    const val CHANNEL_REMINDER = "reminder"
    const val CHANNEL_CELEBRATION = "celebration"
    const val CHANNEL_SUMMARY = "daily_summary"
    const val ID_SESSION = 1001
    const val ID_REMINDER = 1002
    const val ID_CELEBRATION = 1003
    const val ID_SUMMARY = 1004

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < 26) return
        val nm = Permissions.notificationManager(context)
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_SESSION,
                context.getString(R.string.channel_session_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = context.getString(R.string.channel_session_desc)
                setShowBadge(false)
            },
        )
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_REMINDER,
                context.getString(R.string.channel_reminder_name),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = context.getString(R.string.channel_reminder_desc)
                enableVibration(true)
            },
        )
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_CELEBRATION,
                context.getString(R.string.channel_celebration_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = context.getString(R.string.channel_celebration_desc)
            },
        )
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_SUMMARY,
                context.getString(R.string.channel_summary_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = context.getString(R.string.channel_summary_desc)
            },
        )
    }

    fun sessionNotification(
        context: Context,
        appLabel: String,
        remainingText: String,
        paused: Boolean,
    ): Notification {
        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val text = if (paused) {
            context.getString(R.string.session_notif_paused, remainingText)
        } else {
            context.getString(R.string.session_notif_text, remainingText)
        }
        return NotificationCompat.Builder(context, CHANNEL_SESSION)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.session_notif_title, appLabel))
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setContentIntent(open)
            .setColor(context.getColor(R.color.notification_color))
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .build()
    }

    fun timeUpNotification(context: Context, appLabel: String): Notification {
        val full = PendingIntent.getActivity(
            context,
            1,
            Intent(context, TimeUpActivity::class.java).apply {
                putExtra(TimeUpActivity.EXTRA_LABEL, appLabel)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(context, CHANNEL_REMINDER)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.timeup_notif_title))
            .setContentText(context.getString(R.string.timeup_notif_text, appLabel))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(full)
            .setFullScreenIntent(full, true)
            .setColor(context.getColor(R.color.notification_color))
            .build()
    }

    fun celebrationNotification(context: Context, body: String): Notification {
        val open = PendingIntent.getActivity(
            context,
            2,
            Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(context, CHANNEL_CELEBRATION)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.celebration_notif_title))
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(open)
            .setColor(context.getColor(R.color.notification_color))
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .build()
    }

    fun summaryNotification(context: Context, body: String): Notification {
        val open = PendingIntent.getActivity(
            context,
            3,
            Intent(context, MainActivity::class.java).apply {
                putExtra(MainActivity.EXTRA_OPEN, MainActivity.OPEN_STATS)
                data = "mindpeace://stats".toUri()
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP,
                )
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(context, CHANNEL_SUMMARY)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.summary_notif_title))
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(open)
            .setColor(context.getColor(R.color.notification_color))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()
    }
}
