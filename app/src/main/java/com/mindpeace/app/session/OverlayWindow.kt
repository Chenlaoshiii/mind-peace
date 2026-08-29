package com.mindpeace.app.session

import android.graphics.PixelFormat
import android.os.Build
import android.view.WindowManager
import android.view.WindowManager.LayoutParams.FLAG_BLUR_BEHIND
import android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND
import android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
import android.view.WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.mindpeace.app.service.MindPeaceAccessibilityService
import com.mindpeace.app.ui.overlay.InterceptHost
import com.mindpeace.app.ui.theme.MindPeaceThemed
import com.mindpeace.app.util.BlurSupport

class OverlayWindow(
    private val service: MindPeaceAccessibilityService,
    private val coordinator: SessionCoordinator,
) {
    private var view: ComposeView? = null
    private var owner: OverlayOwner? = null

    val isShowing: Boolean get() = view != null

    fun show() {
        if (view != null) return
        val owner = OverlayOwner().also {
            this.owner = it
            it.onCreate()
            it.onStart()
        }
        val compose = ComposeView(service).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setViewTreeLifecycleOwner(owner)
            setViewTreeViewModelStoreOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)
            setContent {
                MindPeaceThemed(showAtmosphere = false) {
                    InterceptHost(coordinator = coordinator)
                }
            }
        }
        val blur = BlurSupport.shouldBlurBehind(service)
        var flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            FLAG_DIM_BEHIND
        if (blur && Build.VERSION.SDK_INT >= 31) {
            flags = flags or FLAG_BLUR_BEHIND
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            TYPE_ACCESSIBILITY_OVERLAY,
            flags,
            PixelFormat.TRANSLUCENT,
        )
        params.dimAmount = 0.38f
        if (Build.VERSION.SDK_INT >= 28) {
            params.layoutInDisplayCutoutMode = LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        if (blur && Build.VERSION.SDK_INT >= 31) {
            try {
                params.setBlurBehindRadius(BlurSupport.RADIUS_PX)
            } catch (_: Exception) {
            }
        }
        params.title = "MindPeaceIntercept"
        try {
            service.windowsManagerCompat().addView(compose, params)
            view = compose
        } catch (_: Exception) {
            owner.onDestroy()
            this.owner = null
            view = null
        }
    }

    fun dismiss() {
        val v = view ?: return
        try {
            service.windowsManagerCompat().removeViewImmediate(v)
        } catch (_: Exception) {
            try {
                service.windowsManagerCompat().removeView(v)
            } catch (_: Exception) {
            }
        }
        view = null
        owner?.onDestroy()
        owner = null
    }
}

private class OverlayOwner : LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateController.savedStateRegistry

    fun onCreate() {
        savedStateController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.INITIALIZED
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    fun onStart() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    fun onDestroy() {
        if (lifecycleRegistry.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        }
        if (lifecycleRegistry.currentState != Lifecycle.State.INITIALIZED) {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        }
        store.clear()
    }
}
