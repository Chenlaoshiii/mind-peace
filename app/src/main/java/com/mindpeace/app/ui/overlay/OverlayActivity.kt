package com.mindpeace.app.ui.overlay

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mindpeace.app.MindPeaceApp
import com.mindpeace.app.data.OverlayState
import com.mindpeace.app.ui.theme.MindPeaceThemed
import com.mindpeace.app.util.BlurSupport
import java.lang.ref.WeakReference

class OverlayActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        instance = WeakReference(this)
        window.setBackgroundDrawableResource(android.R.color.transparent)
        if (Build.VERSION.SDK_INT >= 31 && BlurSupport.shouldBlurBehind(this)) {
            try {
                window.setBackgroundBlurRadius(BlurSupport.RADIUS_PX)
            } catch (_: Exception) {
            }
        }
        enableEdgeToEdge()
        val coordinator = (application as MindPeaceApp).container.session
        setContent {
            val state = coordinator.overlay.collectAsStateWithLifecycle().value
            LaunchedEffect(state) {
                if (state is OverlayState.Hidden) finish()
            }
            MindPeaceThemed(showAtmosphere = false) {
                InterceptHost(coordinator)
            }
        }
    }

    override fun onDestroy() {
        if (instance?.get() === this) instance = null
        super.onDestroy()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Stay on the intercept until 确定 or 退出.
    }

    companion object {
        private var instance: WeakReference<OverlayActivity>? = null

        fun open(context: Context) {
            if (instance?.get() != null) return
            context.startActivity(
                Intent(context, OverlayActivity::class.java).apply {
                    addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_NO_ANIMATION,
                    )
                },
            )
        }

        fun close(@Suppress("UNUSED_PARAMETER") context: Context) {
            instance?.get()?.finish()
        }
    }
}
