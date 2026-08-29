package com.mindpeace.app.util

import android.Manifest
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import com.mindpeace.app.service.MindPeaceAccessibilityService

object Permissions {

    fun isAccessibilityEnabled(context: Context): Boolean {
        val expected = ComponentName(context, MindPeaceAccessibilityService::class.java)
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabled = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC)
        if (enabled.any { info ->
                info.resolveInfo?.serviceInfo?.let { si ->
                    si.packageName == expected.packageName && si.name == expected.className
                } == true
            }
        ) {
            return true
        }
        val raw = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ) ?: return false
        return raw.split(':').any { token ->
            val cn = ComponentName.unflattenFromString(token.trim())
            cn == expected || token.contains(expected.flattenToString()) ||
                (token.contains(context.packageName) && token.contains("MindPeaceAccessibilityService"))
        }
    }

    fun openAccessibilitySettings(context: Context) {
        val component = ComponentName(context, MindPeaceAccessibilityService::class.java)
        val highlight = component.flattenToString()
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(":settings:fragment_args_key", highlight)
            putExtra(
                ":settings:show_fragment_args",
                Bundle().apply { putString(":settings:fragment_args_key", highlight) },
            )
        }
        context.startActivity(intent)
    }

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun openBatteryOptimization(context: Context) {
        try {
            val request = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = "package:${context.packageName}".toUri()
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(request)
        } catch (_: Exception) {
            val fallback = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(fallback)
        }
    }

    fun areNotificationsEnabled(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= 33) {
            return context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED &&
                NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun openNotificationSettings(context: Context) {
        val intent = if (Build.VERSION.SDK_INT >= 26) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            }
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
            }
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun canDrawOverlays(context: Context): Boolean = Settings.canDrawOverlays(context)

    fun openOverlaySettings(context: Context) {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            "package:${context.packageName}".toUri(),
        ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        context.startActivity(intent)
    }

    fun notificationManager(context: Context): NotificationManager {
        return context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    const val GET_INSTALLED_APPS = "android.permission.GET_INSTALLED_APPS"
    const val GET_INSTALLED_APPS_OEM = "com.android.permission.GET_INSTALLED_APPS"

    fun canListInstalledApps(context: Context): Boolean {
        val pm = context.packageManager
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val launchCount = try {
            if (Build.VERSION.SDK_INT >= 33) {
                pm.queryIntentActivities(
                    launcher,
                    PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong()),
                ).size
            } else {
                @Suppress("DEPRECATION")
                pm.queryIntentActivities(launcher, PackageManager.MATCH_ALL).size
            }
        } catch (_: Exception) {
            0
        }
        val installedCount = try {
            if (Build.VERSION.SDK_INT >= 33) {
                pm.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0)).size
            } else {
                @Suppress("DEPRECATION")
                pm.getInstalledApplications(0).size
            }
        } catch (_: Exception) {
            0
        }
        return maxOf(launchCount, installedCount) > 8
    }

    fun pendingGetInstalledAppsPermission(context: Context): String? {
        for (perm in listOf(GET_INSTALLED_APPS, GET_INSTALLED_APPS_OEM)) {
            if (!isPermissionDeclared(context, perm)) continue
            if (!isPermissionKnown(context, perm)) continue
            if (context.checkSelfPermission(perm) != PackageManager.PERMISSION_GRANTED) {
                return perm
            }
        }
        return null
    }

    fun openAppDetailsSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    private fun isPermissionDeclared(context: Context, perm: String): Boolean {
        return try {
            val info = context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.GET_PERMISSIONS,
            )
            info.requestedPermissions?.contains(perm) == true
        } catch (_: Exception) {
            false
        }
    }

    private fun isPermissionKnown(context: Context, perm: String): Boolean {
        return try {
            context.packageManager.getPermissionInfo(perm, 0)
            true
        } catch (_: Exception) {
            false
        }
    }
}
