package com.mindpeace.app.service

import android.accessibilityservice.AccessibilityService
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityWindowInfo
import com.mindpeace.app.MindPeaceApp
import com.mindpeace.app.session.SessionCoordinator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MindPeaceAccessibilityService : AccessibilityService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var pollJob: Job? = null
    private var lastEventPkg: String? = null

    private val coordinator: SessionCoordinator?
        get() = (application as? MindPeaceApp)?.container?.session

    override fun onServiceConnected() {
        super.onServiceConnected()
        coordinator?.attachAccessibility(this)
        pollJob?.cancel()
        pollJob = scope.launch {
            while (isActive) {
                delay(800)
                currentApplicationPackage()?.let { pkg ->
                    coordinator?.onForegroundPackage(pkg)
                }
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val type = event.eventType
        if (type != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            type != AccessibilityEvent.TYPE_WINDOWS_CHANGED
        ) {
            return
        }
        // Prefer the focused application over raw event.packageName when they disagree.
        // If nothing is focused, skip rather than using a stale leftover package.
        val pkg = currentApplicationPackage() ?: return
        if (pkg == lastEventPkg && type == AccessibilityEvent.TYPE_WINDOWS_CHANGED) {
            return
        }
        lastEventPkg = pkg
        coordinator?.onForegroundPackage(pkg)
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        pollJob?.cancel()
        coordinator?.detachAccessibility(this)
        scope.cancel()
        super.onDestroy()
    }

    fun windowsManagerCompat(): WindowManager {
        return getSystemService(WINDOW_SERVICE) as WindowManager
    }

    /**
     * Foreground app from focused/active TYPE_APPLICATION windows only.
     * Returns null when nothing is focused so the coordinator is skipped,
     * instead of reporting a stale unfocused leftover (e.g. Douyin after Home).
     */
    private fun currentApplicationPackage(): String? {
        val wins = try {
            windows
        } catch (_: Exception) {
            null
        } ?: return null
        val appWindows = wins.filter { w ->
            w.type == AccessibilityWindowInfo.TYPE_APPLICATION
        }
        val focused = appWindows.firstOrNull { it.isFocused }
            ?: appWindows.firstOrNull { it.isActive }
        val fromFocused = packageOf(focused)
        if (!fromFocused.isNullOrBlank()) return fromFocused
        return null
    }

    private fun packageOf(window: AccessibilityWindowInfo?): String? {
        if (window == null) return null
        return try {
            window.root?.packageName?.toString()?.takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        }
    }
}
