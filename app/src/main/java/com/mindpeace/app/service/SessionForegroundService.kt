package com.mindpeace.app.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import com.mindpeace.app.MindPeaceApp
import com.mindpeace.app.util.Notifications
import com.mindpeace.app.util.formatDurationMillis
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class SessionForegroundService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var ticker: Job? = null

    private val container get() = (application as MindPeaceApp).container

    override fun onCreate() {
        super.onCreate()
        Notifications.ensureChannels(this)
        startAsForeground()
        ticker = scope.launch {
            while (isActive) {
                delay(1_000)
                val session = container.session.session.value
                if (session == null) {
                    stopSelf()
                    break
                }
                container.session.onTick(1_000L)
                updateNotification()
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_REFRESH -> updateNotification()
        }
        startAsForeground()
        return START_STICKY
    }

    override fun onDestroy() {
        ticker?.cancel()
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startAsForeground() {
        val n = currentNotification()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(
                Notifications.ID_SESSION,
                n,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } else {
            startForeground(Notifications.ID_SESSION, n)
        }
    }

    private fun updateNotification() {
        try {
            startAsForeground()
        } catch (_: Exception) {
        }
    }

    private fun currentNotification() = run {
        val session = container.session.session.value
        val pkg = session?.packageName.orEmpty()
        val label = if (pkg.isBlank()) getString(com.mindpeace.app.R.string.app_name)
        else container.installedApps.labelOf(pkg)
        val remaining = formatDurationMillis(session?.remainingMillis ?: 0L)
        Notifications.sessionNotification(
            context = this,
            appLabel = label,
            remainingText = remaining,
            paused = session?.paused != false,
        )
    }

    companion object {
        const val ACTION_STOP = "com.mindpeace.app.action.STOP_SESSION"
        const val ACTION_REFRESH = "com.mindpeace.app.action.REFRESH_SESSION"

        fun start(context: Context) {
            val intent = Intent(context, SessionForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= 26) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, SessionForegroundService::class.java))
        }

        fun refresh(context: Context) {
            val intent = Intent(context, SessionForegroundService::class.java).setAction(ACTION_REFRESH)
            try {
                if (Build.VERSION.SDK_INT >= 26) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (_: Exception) {
            }
        }
    }
}
