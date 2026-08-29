package com.mindpeace.app.data

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import android.os.Build
import android.view.inputmethod.InputMethodManager

class InstalledAppsRepository(private val context: Context) {

    private val pm: PackageManager = context.packageManager

    fun loadLaunchable(): List<AppEntry> {
        val seen = LinkedHashMap<String, AppEntry>()
        val home = homePackages()
        val ime = imePackages()

        for (ri in queryLauncherActivities()) {
            val pkg = ri.activityInfo?.packageName ?: continue
            if (pkg in seen) continue
            if (shouldHide(pkg, home, ime)) continue
            val label = ri.loadLabel(pm)?.toString()?.trim().orEmpty().ifBlank { pkg }
            val appInfo = ri.activityInfo.applicationInfo
            seen[pkg] = AppEntry(
                packageName = pkg,
                label = label,
                isSystem = isSystemApp(appInfo),
            )
        }

        for (ai in installedApplications()) {
            val pkg = ai.packageName ?: continue
            if (pkg in seen) continue
            if (pm.getLaunchIntentForPackage(pkg) == null) continue
            if (shouldHide(pkg, home, ime)) continue
            val label = pm.getApplicationLabel(ai)?.toString()?.trim().orEmpty().ifBlank { pkg }
            seen[pkg] = AppEntry(
                packageName = pkg,
                label = label,
                isSystem = isSystemApp(ai),
            )
        }

        for (pi in installedPackages()) {
            val pkg = pi.packageName ?: continue
            if (pkg in seen) continue
            if (pm.getLaunchIntentForPackage(pkg) == null) continue
            if (shouldHide(pkg, home, ime)) continue
            val ai = pi.applicationInfo
            val label = if (ai != null) {
                pm.getApplicationLabel(ai)?.toString()?.trim().orEmpty().ifBlank { pkg }
            } else {
                pkg
            }
            seen[pkg] = AppEntry(
                packageName = pkg,
                label = label,
                isSystem = isSystemApp(ai),
            )
        }

        return seen.values.sortedBy { it.label.lowercase() }
    }

    fun labelOf(packageName: String): String {
        return try {
            val info = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(info).toString()
        } catch (_: Exception) {
            packageName.substringAfterLast('.')
        }
    }

    fun iconOf(packageName: String): Drawable? {
        return try {
            pm.getApplicationIcon(packageName)
        } catch (_: Exception) {
            null
        }
    }

    fun isHomeLauncher(packageName: String): Boolean {
        return packageName in homePackages()
    }

    private fun queryLauncherActivities(): List<ResolveInfo> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return try {
            if (Build.VERSION.SDK_INT >= 33) {
                pm.queryIntentActivities(
                    intent,
                    PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong()),
                )
            } else {
                @Suppress("DEPRECATION")
                pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            }
        } catch (_: Exception) {
            try {
                @Suppress("DEPRECATION")
                pm.queryIntentActivities(intent, 0)
            } catch (_: Exception) {
                emptyList()
            }
        }
    }

    private fun installedApplications(): List<ApplicationInfo> {
        return try {
            if (Build.VERSION.SDK_INT >= 33) {
                pm.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getInstalledApplications(0)
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun installedPackages(): List<android.content.pm.PackageInfo> {
        return try {
            if (Build.VERSION.SDK_INT >= 33) {
                pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getInstalledPackages(0)
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun homePackages(): Set<String> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val list = try {
            if (Build.VERSION.SDK_INT >= 33) {
                pm.queryIntentActivities(
                    intent,
                    PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong()),
                )
            } else {
                @Suppress("DEPRECATION")
                pm.queryIntentActivities(intent, 0)
            }
        } catch (_: Exception) {
            emptyList()
        }
        val out = HashSet<String>()
        for (ri in list) {
            ri.activityInfo?.packageName?.let { out += it }
        }
        try {
            val def = if (Build.VERSION.SDK_INT >= 33) {
                pm.resolveActivity(
                    intent,
                    PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong()),
                )
            } else {
                @Suppress("DEPRECATION")
                pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
            }
            def?.activityInfo?.packageName?.let { out += it }
        } catch (_: Exception) {
        }
        return out
    }

    private fun imePackages(): Set<String> {
        return try {
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.inputMethodList.map { it.packageName }.toSet()
        } catch (_: Exception) {
            emptySet()
        }
    }

    private fun shouldHide(pkg: String, home: Set<String>, ime: Set<String>): Boolean {
        if (pkg == context.packageName) return true
        if (pkg in home) return true
        if (pkg in ime) return true
        if (pkg in HIDDEN_EXACT) return true
        if (HIDDEN_PREFIXES.any { pkg.startsWith(it) }) return true
        return false
    }

    private fun isSystemApp(info: ApplicationInfo?): Boolean {
        if (info == null) return false
        return (info.flags and ApplicationInfo.FLAG_SYSTEM) != 0 ||
            (info.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
    }

    companion object {
        private val HIDDEN_EXACT = setOf(
            "android",
            "com.android.settings",
            "com.android.phone",
            "com.android.systemui",
            "com.android.dialer",
            "com.android.incallui",
            "com.android.packageinstaller",
            "com.google.android.packageinstaller",
            "com.google.android.permissioncontroller",
            "com.android.permissioncontroller",
            "com.google.android.gms",
            "com.google.android.gsf",
            "com.google.android.googlequicksearchbox",
            "com.google.android.apps.nexuslauncher",
            "com.google.android.dialer",
            "com.miui.home",
            "com.huawei.android.launcher",
            "com.oppo.launcher",
            "com.bbk.launcher2",
            "com.vivo.launcher",
        )
        private val HIDDEN_PREFIXES = listOf(
            "com.android.launcher",
            "com.android.inputmethod",
            "com.google.android.inputmethod",
            "com.android.systemui",
            "com.android.settings",
            "com.samsung.android.app.launcher",
            "com.sec.android.app.launcher",
            "com.google.android.permission",
            "com.google.android.packageinstaller",
        )
    }
}
