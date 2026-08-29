package com.mindpeace.app.util

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.view.WindowManager

object BlurSupport {
    const val RADIUS_PX = 20

    fun shouldBlurBehind(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < 31) return false
        return try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            if (am?.isLowRamDevice == true) return false
            val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            wm.isCrossWindowBlurEnabled
        } catch (_: Exception) {
            false
        }
    }
}
