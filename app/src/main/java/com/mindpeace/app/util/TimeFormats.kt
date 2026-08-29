package com.mindpeace.app.util

import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.ceil

fun todayDateKey(): String = dateKey(System.currentTimeMillis())

fun yesterdayDateKey(): String = dateKeyOffset(-1)

fun dateKey(millis: Long): String {
    val cal = Calendar.getInstance().apply { timeInMillis = millis }
    val y = cal.get(Calendar.YEAR)
    val m = cal.get(Calendar.MONTH) + 1
    val d = cal.get(Calendar.DAY_OF_MONTH)
    return String.format(Locale.US, "%04d-%02d-%02d", y, m, d)
}

fun dateKeyOffset(days: Int, fromMillis: Long = System.currentTimeMillis()): String {
    val cal = Calendar.getInstance().apply { timeInMillis = fromMillis }
    cal.add(Calendar.DAY_OF_MONTH, days)
    return dateKey(cal.timeInMillis)
}

fun dateKeyPlusDays(dateKey: String, days: Int): String {
    val parts = dateKey.split("-")
    if (parts.size != 3) return dateKey
    val cal = Calendar.getInstance()
    cal.set(Calendar.YEAR, parts[0].toInt())
    cal.set(Calendar.MONTH, parts[1].toInt() - 1)
    cal.set(Calendar.DAY_OF_MONTH, parts[2].toInt())
    cal.set(Calendar.HOUR_OF_DAY, 12)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    cal.add(Calendar.DAY_OF_MONTH, days)
    return dateKey(cal.timeInMillis)
}

fun chartDateLabel(dateKey: String): String {
    val parts = dateKey.split("-")
    if (parts.size != 3) return dateKey
    return "${parts[1].toInt()}/${parts[2].toInt()}"
}

fun weekdayShortLabel(dateKey: String): String {
    val parts = dateKey.split("-")
    if (parts.size != 3) return dateKey
    val cal = Calendar.getInstance()
    cal.set(Calendar.YEAR, parts[0].toInt())
    cal.set(Calendar.MONTH, parts[1].toInt() - 1)
    cal.set(Calendar.DAY_OF_MONTH, parts[2].toInt())
    return when (cal.get(Calendar.DAY_OF_WEEK)) {
        Calendar.MONDAY -> "周一"
        Calendar.TUESDAY -> "周二"
        Calendar.WEDNESDAY -> "周三"
        Calendar.THURSDAY -> "周四"
        Calendar.FRIDAY -> "周五"
        Calendar.SATURDAY -> "周六"
        else -> "周日"
    }
}

fun calendarDaysBetween(fromMillis: Long, toMillis: Long): Int {
    val from = startOfDay(fromMillis)
    val to = startOfDay(toMillis)
    return TimeUnit.MILLISECONDS.toDays(to - from).toInt()
}

fun startOfDay(millis: Long): Long {
    val cal = Calendar.getInstance().apply { timeInMillis = millis }
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

fun isQuietHours(nowMillis: Long = System.currentTimeMillis()): Boolean {
    val hour = Calendar.getInstance().apply { timeInMillis = nowMillis }.get(Calendar.HOUR_OF_DAY)
    return hour >= 22 || hour < 8
}

fun millisUntilNextHour(hourOfDay: Int, nowMillis: Long = System.currentTimeMillis()): Long {
    val now = Calendar.getInstance().apply { timeInMillis = nowMillis }
    val target = Calendar.getInstance().apply { timeInMillis = nowMillis }
    target.set(Calendar.HOUR_OF_DAY, hourOfDay)
    target.set(Calendar.MINUTE, 0)
    target.set(Calendar.SECOND, 0)
    target.set(Calendar.MILLISECOND, 0)
    if (!target.after(now)) {
        target.add(Calendar.DAY_OF_MONTH, 1)
    }
    return (target.timeInMillis - now.timeInMillis).coerceAtLeast(0L)
}

fun formatDurationMillis(millis: Long): String {
    val totalSec = (millis / 1000L).coerceAtLeast(0)
    val h = TimeUnit.SECONDS.toHours(totalSec)
    val m = TimeUnit.SECONDS.toMinutes(totalSec) % 60
    val s = totalSec % 60
    return when {
        h > 0 -> String.format(Locale.CHINA, "%d小时%02d分", h, m)
        m > 0 && s > 0 && m < 3 -> String.format(Locale.CHINA, "%d分%02d秒", m, s)
        m > 0 -> String.format(Locale.CHINA, "%d分钟", m)
        else -> String.format(Locale.CHINA, "%d秒", s)
    }
}

fun formatMinutesShort(minutes: Int): String {
    return if (minutes <= 0) "0 分钟" else "${minutes} 分钟"
}

fun millisToWholeMinutes(millis: Long): Int {
    if (millis <= 0L) return 0
    return (millis / 60_000L).toInt()
}

fun remainingMinutesFloor(remainingMillis: Long): Int {
    if (remainingMillis <= 0L) return 0
    return (remainingMillis / 60_000L).toInt()
}

fun remainingMinutesCeil(remainingMillis: Long): Int {
    if (remainingMillis <= 0L) return 0
    return ceil(remainingMillis / 60_000.0).toInt()
}
