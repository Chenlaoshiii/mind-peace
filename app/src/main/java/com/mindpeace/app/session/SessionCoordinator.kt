package com.mindpeace.app.session

import android.app.Application
import android.content.Intent
import android.os.SystemClock
import android.util.Log
import com.mindpeace.app.data.ActiveSession
import com.mindpeace.app.data.InstalledAppsRepository
import com.mindpeace.app.data.OverlayState
import com.mindpeace.app.data.PersistedSession
import com.mindpeace.app.data.SettingsRepository
import com.mindpeace.app.data.UNLIMITED_BUDGET
import com.mindpeace.app.service.MindPeaceAccessibilityService
import com.mindpeace.app.service.SessionForegroundService
import com.mindpeace.app.ui.overlay.OverlayActivity
import com.mindpeace.app.ui.overlay.TimeUpActivity
import com.mindpeace.app.util.Notifications
import com.mindpeace.app.util.Permissions
import com.mindpeace.app.util.formatDurationMillis
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SessionCoordinator(
    private val app: Application,
    private val settings: SettingsRepository,
    private val installedApps: InstalledAppsRepository,
    private val scope: CoroutineScope,
) {
    private val _overlay = MutableStateFlow<OverlayState>(OverlayState.Hidden)
    val overlay: StateFlow<OverlayState> = _overlay.asStateFlow()

    private val _session = MutableStateFlow<ActiveSession?>(null)
    val session: StateFlow<ActiveSession?> = _session.asStateFlow()

    private val _foreground = MutableStateFlow<String?>(null)
    val foregroundPackage: StateFlow<String?> = _foreground.asStateFlow()

    val selfMessage: StateFlow<String> get() = settings.selfMessage

    @Volatile
    private var accessibility: MindPeaceAccessibilityService? = null
    private var overlayWindow: OverlayWindow? = null

    private var lastPkg: String? = null
    private var lastPkgAt: Long = 0L
    private var usageSincePersist: Long = 0L

    init {
        restoreSessionIfAny()
        scope.launch {
            overlay.collect { state ->
                withContext(Dispatchers.Main) {
                    syncOverlayWindow(state)
                }
            }
        }
    }

    fun attachAccessibility(service: MindPeaceAccessibilityService) {
        accessibility = service
        overlayWindow = OverlayWindow(service, this)
        if (_overlay.value !is OverlayState.Hidden) {
            overlayWindow?.show()
        }
    }

    fun detachAccessibility(service: MindPeaceAccessibilityService) {
        if (accessibility === service) {
            overlayWindow?.dismiss()
            overlayWindow = null
            accessibility = null
        }
    }

    fun onForegroundPackage(pkg: String) {
        if (pkg.isBlank()) return
        if (pkg == app.packageName) {
            // Never intercept ourselves. Leaving the watched app ends the session.
            endSessionForLeave(pkg)
            return
        }
        if (isIgnored(pkg)) {
            endSessionForLeave(pkg)
            return
        }

        val now = SystemClock.uptimeMillis()
        val pkgChanged = pkg != lastPkg
        if (pkgChanged) {
            lastPkg = pkg
            lastPkgAt = now
        }
        _foreground.value = pkg

        val sess = _session.value
        if (sess != null && sess.packageName == pkg) {
            if (sess.remainingMillis <= 0L) {
                onTimeUp()
                return
            }
            if (_overlay.value !is OverlayState.Hidden && _overlay.value !is OverlayState.TimeUp) {
                hideOverlay()
            }
            return
        }

        if (sess != null && sess.packageName != pkg) {
            endSessionForLeave(pkg)
        }

        val watched = settings.watchedApps.value.firstOrNull { it.packageName == pkg && it.enabled }
            ?: return

        if (!pkgChanged && _overlay.value !is OverlayState.Hidden) {
            return
        }
        if (!pkgChanged && now - lastPkgAt < 400L) {
            return
        }

        showIntercept(pkg, watched.dailyLimitMinutes)
    }

    fun onConfirm() {
        val state = _overlay.value as? OverlayState.Confirm ?: return
        if (state.remainingDailyMillis <= 0L) {
            onExit()
            return
        }
        _overlay.value = OverlayState.DurationPick(
            packageName = state.packageName,
            appLabel = state.appLabel,
            remainingDailyMillis = state.remainingDailyMillis,
        )
    }

    fun onExit() {
        hideOverlay()
        goHome()
    }

    fun onPickDuration(minutes: Int) {
        val state = when (val s = _overlay.value) {
            is OverlayState.DurationPick -> s
            is OverlayState.CustomDuration -> OverlayState.DurationPick(s.packageName, s.appLabel, s.remainingDailyMillis)
            else -> return
        }
        if (minutes <= 0) return
        val requested = minutes * 60_000L
        val cap = state.remainingDailyMillis
        if (cap != UNLIMITED_BUDGET && requested > cap) {
            _overlay.value = OverlayState.CapWarning(
                packageName = state.packageName,
                appLabel = state.appLabel,
                remainingDailyMillis = cap,
                requestedMinutes = minutes,
            )
            return
        }
        beginSession(state.packageName, requested)
    }

    fun onOpenCustom() {
        val state = _overlay.value as? OverlayState.DurationPick ?: return
        _overlay.value = OverlayState.CustomDuration(
            packageName = state.packageName,
            appLabel = state.appLabel,
            remainingDailyMillis = state.remainingDailyMillis,
        )
    }

    fun onCancelCustom() {
        val state = _overlay.value as? OverlayState.CustomDuration ?: return
        _overlay.value = OverlayState.DurationPick(
            packageName = state.packageName,
            appLabel = state.appLabel,
            remainingDailyMillis = state.remainingDailyMillis,
        )
    }

    fun onAcceptCap() {
        val state = _overlay.value as? OverlayState.CapWarning ?: return
        if (state.remainingDailyMillis <= 0L) {
            onExit()
            return
        }
        beginSession(state.packageName, state.remainingDailyMillis)
    }

    fun onCancelCap() {
        val state = _overlay.value as? OverlayState.CapWarning ?: return
        _overlay.value = OverlayState.DurationPick(
            packageName = state.packageName,
            appLabel = state.appLabel,
            remainingDailyMillis = state.remainingDailyMillis,
        )
    }

    fun onTick(deltaMillis: Long) {
        val sess = _session.value ?: return
        if (sess.paused) return
        if (_foreground.value != sess.packageName) return
        if (deltaMillis <= 0L) return

        scope.launch { settings.addUsedMillis(sess.packageName, deltaMillis) }
        usageSincePersist += deltaMillis
        val remaining = sess.remainingMillis - deltaMillis
        if (remaining <= 0L) {
            _session.value = sess.copy(remainingMillis = 0L)
            onTimeUp()
            return
        }
        _session.value = sess.copy(remainingMillis = remaining)
        if (usageSincePersist >= 5_000L) {
            usageSincePersist = 0L
            persistSession()
        }
        SessionForegroundService.refresh(app)
    }

    fun onTimeUpAck() {
        hideOverlay()
        goHome()
        Permissions.notificationManager(app).cancel(Notifications.ID_REMINDER)
    }

    fun remainingLabel(): String {
        val sess = _session.value ?: return formatDurationMillis(app, 0)
        return formatDurationMillis(app, sess.remainingMillis)
    }

    private fun showIntercept(pkg: String, dailyLimitMinutes: Int) {
        val remaining = settings.remainingDailyMillis(pkg, dailyLimitMinutes)
        val label = installedApps.labelOf(pkg)
        _overlay.value = OverlayState.Confirm(
            packageName = pkg,
            appLabel = label,
            remainingDailyMillis = remaining,
            selfMessage = settings.selfMessage.value,
        )
    }

    private fun beginSession(packageName: String, durationMillis: Long) {
        val duration = durationMillis.coerceAtLeast(1_000L)
        _session.value = ActiveSession(
            packageName = packageName,
            remainingMillis = duration,
            paused = false,
        )
        usageSincePersist = 0L
        persistSession()
        hideOverlay()
        SessionForegroundService.start(app)
    }

    private fun onTimeUp() {
        val sess = _session.value
        _session.value = null
        persistSession(clear = true)
        SessionForegroundService.stop(app)
        val pkg = sess?.packageName ?: return
        val label = installedApps.labelOf(pkg)
        goHome()
        _overlay.value = OverlayState.TimeUp(packageName = pkg, appLabel = label)
        try {
            Permissions.notificationManager(app)
                .notify(Notifications.ID_REMINDER, Notifications.timeUpNotification(app, label))
        } catch (_: Exception) {
        }
        // Full-screen UI is driven by overlay state (accessibility overlay or fallback activity).
    }

    private fun hideOverlay() {
        _overlay.value = OverlayState.Hidden
        OverlayActivity.close(app)
        TimeUpActivity.close(app)
    }

    private fun goHome() {
        val svc = accessibility
        var ok = false
        if (svc != null) {
            ok = svc.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME)
        }
        if (!ok) {
            try {
                app.startActivity(
                    Intent(Intent.ACTION_MAIN).apply {
                        addCategory(Intent.CATEGORY_HOME)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    },
                )
            } catch (t: Throwable) {
                Log.w(TAG, "home failed", t)
            }
        }
    }

    private fun endSessionForLeave(newPkg: String) {
        val sess = _session.value ?: return
        if (newPkg == sess.packageName) return
        _session.value = null
        usageSincePersist = 0L
        persistSession(clear = true)
        SessionForegroundService.stop(app)
    }

    private fun isIgnored(pkg: String): Boolean {
        if (pkg in IGNORED_PACKAGES) return true
        if (pkg.startsWith("com.android.systemui")) return true
        if (installedApps.isHomeLauncher(pkg)) return true
        return false
    }

    private fun persistSession(clear: Boolean = false) {
        scope.launch {
            if (clear) {
                settings.saveSession(null)
                return@launch
            }
            val s = _session.value
            if (s == null) {
                settings.saveSession(null)
            } else {
                settings.saveSession(
                    PersistedSession(
                        packageName = s.packageName,
                        remainingMillis = s.remainingMillis,
                        paused = s.paused,
                        lastElapsedRealtime = SystemClock.elapsedRealtime(),
                    ),
                )
            }
        }
    }

    private fun restoreSessionIfAny() {
        scope.launch {
            val p = settings.loadPersistedSession() ?: return@launch
            // Never restore leftover minutes from a leave/pause. Only a mid-tick
            // crash while still in the watched app (not paused) may continue.
            if (p.paused || p.remainingMillis <= 0L) {
                settings.saveSession(null)
                return@launch
            }
            var remaining = p.remainingMillis
            if (p.lastElapsedRealtime > 0L) {
                val elapsed = SystemClock.elapsedRealtime() - p.lastElapsedRealtime
                if (elapsed > 0L) remaining -= elapsed
            }
            if (remaining <= 0L) {
                settings.saveSession(null)
                return@launch
            }
            _session.value = ActiveSession(
                packageName = p.packageName,
                remainingMillis = remaining,
                paused = false,
            )
            SessionForegroundService.start(app)
        }
    }

    private fun syncOverlayWindow(state: OverlayState) {
        if (state is OverlayState.Hidden) {
            overlayWindow?.dismiss()
            OverlayActivity.close(app)
            return
        }
        val window = overlayWindow
        if (window != null) {
            if (!window.isShowing) window.show()
            if (window.isShowing) return
        }
        // Fallback: full-screen activity if accessibility overlay cannot attach.
        if (state is OverlayState.TimeUp) {
            launchTimeUpActivity(state.appLabel)
        } else {
            OverlayActivity.open(app)
        }
    }

    private fun launchTimeUpActivity(label: String) {
        try {
            app.startActivity(
                Intent(app, TimeUpActivity::class.java).apply {
                    putExtra(TimeUpActivity.EXTRA_LABEL, label)
                    addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_NO_ANIMATION,
                    )
                },
            )
        } catch (t: Throwable) {
            Log.w(TAG, "time-up activity failed", t)
        }
    }

    companion object {
        private const val TAG = "MindPeace"
        private val IGNORED_PACKAGES = setOf(
            "com.android.systemui",
            "android",
            "com.android.intentresolver",
            "com.android.permissioncontroller",
            "com.google.android.permissioncontroller",
            "com.google.android.packageinstaller",
            "com.android.packageinstaller",
            "com.android.phone",
            "com.android.incallui",
        )
    }
}
