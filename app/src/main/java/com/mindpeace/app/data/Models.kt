package com.mindpeace.app.data

import kotlinx.serialization.Serializable

@Serializable
data class WatchedApp(
    val packageName: String,
    val enabled: Boolean = true,
    val dailyLimitMinutes: Int = 0,
)

@Serializable
data class DailyUsageSnapshot(
    val date: String = "",
    val usedMillis: Map<String, Long> = emptyMap(),
)

@Serializable
data class UsageHistory(
    val days: Map<String, Map<String, Long>> = emptyMap(),
)

@Serializable
data class PersistedSession(
    val packageName: String,
    val remainingMillis: Long,
    val paused: Boolean = false,
    val lastElapsedRealtime: Long = 0L,
)

data class ActiveSession(
    val packageName: String,
    val remainingMillis: Long,
    val paused: Boolean,
)

data class AppEntry(
    val packageName: String,
    val label: String,
    val isSystem: Boolean,
)

sealed class OverlayState {
    data object Hidden : OverlayState()

    data class Confirm(
        val packageName: String,
        val appLabel: String,
        val remainingDailyMillis: Long,
        val selfMessage: String = "",
    ) : OverlayState()

    data class DurationPick(
        val packageName: String,
        val appLabel: String,
        val remainingDailyMillis: Long,
    ) : OverlayState()

    data class CapWarning(
        val packageName: String,
        val appLabel: String,
        val remainingDailyMillis: Long,
        val requestedMinutes: Int,
    ) : OverlayState()

    data class CustomDuration(
        val packageName: String,
        val appLabel: String,
        val remainingDailyMillis: Long,
    ) : OverlayState()

    data class TimeUp(
        val packageName: String,
        val appLabel: String,
    ) : OverlayState()
}

const val UNLIMITED_BUDGET = Long.MAX_VALUE / 4

const val CELEBRATION_KIND_4H = "h4"
const val CELEBRATION_KIND_1D = "d1"
const val CELEBRATION_KIND_3D = "d3"
const val BILIBILI_AUTHOR_URL = "https://space.bilibili.com/3546678682454822"

enum class ColorMode {
    LIGHT, DARK, SYSTEM;
    companion object {
        fun from(raw: String?): ColorMode =
            entries.firstOrNull { it.name == raw } ?: SYSTEM
    }
}

enum class VisualStyle {
    MATERIAL_YOU, ORANGE, APPLE;
    companion object {
        fun from(raw: String?): VisualStyle = when (raw) {
            "WHITE", "ORANGE" -> ORANGE
            "APPLE" -> APPLE
            "MATERIAL_YOU" -> MATERIAL_YOU
            else -> MATERIAL_YOU
        }
    }
}

data class Appearance(
    val colorMode: ColorMode = ColorMode.SYSTEM,
    val style: VisualStyle = VisualStyle.MATERIAL_YOU,
)
