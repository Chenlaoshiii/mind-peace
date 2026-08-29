package com.mindpeace.app.data

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mindpeace.app.util.AppLocale
import com.mindpeace.app.util.dateKeyOffset
import com.mindpeace.app.util.todayDateKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.math.max

private val Context.mindPeaceStore: DataStore<Preferences> by preferencesDataStore(name = "mind_peace")

class SettingsRepository(
    context: Context,
    private val scope: CoroutineScope,
) {
    private val appContext = context.applicationContext
    private val store = appContext.mindPeaceStore
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    val onboarded: StateFlow<Boolean> = store.data
        .map { it[KEY_ONBOARDED] == true }
        .stateIn(scope, SharingStarted.Eagerly, false)

    val recentsLockedConfirmed: StateFlow<Boolean> = store.data
        .map { it[KEY_RECENTS] == true }
        .stateIn(scope, SharingStarted.Eagerly, false)

    val watchedApps: StateFlow<List<WatchedApp>> = store.data
        .map { prefs -> decodeList(prefs[KEY_WATCHED]) }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    val selfMessage: StateFlow<String> = store.data
        .map { it[KEY_SELF_MESSAGE].orEmpty() }
        .stateIn(scope, SharingStarted.Eagerly, "")

    val usageHistory: StateFlow<UsageHistory> = store.data
        .map { prefs -> mergedHistory(prefs) }
        .stateIn(scope, SharingStarted.Eagerly, UsageHistory())

    val dailyUsage: StateFlow<DailyUsageSnapshot> = store.data
        .map { prefs ->
            val today = todayDateKey()
            val history = mergedHistory(prefs)
            DailyUsageSnapshot(today, history.days[today].orEmpty())
        }
        .stateIn(scope, SharingStarted.Eagerly, DailyUsageSnapshot(todayDateKey()))

    val persistedSession: StateFlow<PersistedSession?> = store.data
        .map { prefs -> decodeSession(prefs[KEY_SESSION]) }
        .stateIn(scope, SharingStarted.Eagerly, null)

    val appearance: StateFlow<Appearance> = store.data
        .map { prefs ->
            Appearance(
                colorMode = ColorMode.from(prefs[KEY_COLOR_MODE]),
                style = VisualStyle.from(prefs[KEY_VISUAL_STYLE]),
            )
        }
        .stateIn(scope, SharingStarted.Eagerly, Appearance())

    val globalDailyLimitMinutes: StateFlow<Int> = store.data
        .map { it[KEY_GLOBAL_DAILY] ?: 0 }
        .stateIn(scope, SharingStarted.Eagerly, 0)

    val appLocale: StateFlow<String> = store.data
        .map { AppLocale.normalize(it[KEY_LOCALE]) }
        .stateIn(scope, SharingStarted.Eagerly, AppLocale.DEFAULT)

    init {
        scope.launch {
            store.edit { prefs ->
                val raw = prefs[KEY_VISUAL_STYLE]
                if (raw == "ORANGE" || raw == "APPLE" || raw == "WHITE") {
                    prefs[KEY_VISUAL_STYLE] = VisualStyle.MATERIAL_YOU.name
                }
                if (prefs[KEY_LOCALE].isNullOrBlank()) {
                    prefs[KEY_LOCALE] = AppLocale.DEFAULT
                }
            }
            store.data.first()
            snapshotToday()
        }
    }

    suspend fun setOnboarded(value: Boolean) {
        store.edit { prefs ->
            prefs[KEY_ONBOARDED] = value
            if (value && (prefs[KEY_LAST_USED_WATCHED] ?: 0L) == 0L) {
                prefs[KEY_LAST_USED_WATCHED] = System.currentTimeMillis()
            }
        }
    }

    suspend fun setRecentsLockedConfirmed(value: Boolean) {
        store.edit { it[KEY_RECENTS] = value }
    }

    suspend fun setSelfMessage(value: String) {
        store.edit { it[KEY_SELF_MESSAGE] = value.trim() }
    }

    suspend fun setColorMode(mode: ColorMode) {
        store.edit { it[KEY_COLOR_MODE] = mode.name }
    }

    suspend fun setVisualStyle(style: VisualStyle) {
        store.edit { it[KEY_VISUAL_STYLE] = style.name }
    }

    suspend fun setAppLocale(tag: String) {
        val normalized = AppLocale.normalize(tag)
        store.edit { it[KEY_LOCALE] = normalized }
        (appContext as? Application)?.let { AppLocale.apply(it, normalized) }
    }

    fun peekAppLocaleBlocking(): String = runBlocking {
        AppLocale.normalize(store.data.first()[KEY_LOCALE])
    }

    suspend fun setGlobalDailyLimitMinutes(minutes: Int) {
        val next = minutes.coerceIn(0, 24 * 60)
        store.edit { prefs ->
            prefs[KEY_GLOBAL_DAILY] = next
            if (next > 0) {
                val apps = decodeList(prefs[KEY_WATCHED]).toMutableList()
                val sum = apps.sumOf { it.dailyLimitMinutes }
                if (sum > next) {
                    var left = next
                    val scaled = apps.mapIndexed { i, app ->
                        if (i == apps.lastIndex) {
                            app.copy(dailyLimitMinutes = left.coerceAtLeast(0))
                        } else {
                            val v = if (sum <= 0) 0 else (app.dailyLimitMinutes * next) / sum
                            left -= v
                            app.copy(dailyLimitMinutes = v)
                        }
                    }
                    prefs[KEY_WATCHED] = json.encodeToString(scaled)
                }
            }
        }
    }

    suspend fun setAppAllocation(packageName: String, minutes: Int) {
        store.edit { prefs ->
            val apps = decodeList(prefs[KEY_WATCHED]).toMutableList()
            val global = prefs[KEY_GLOBAL_DAILY] ?: 0
            val idx = apps.indexOfFirst { it.packageName == packageName }
            if (idx < 0) return@edit
            val others = apps.filterIndexed { i, _ -> i != idx }.sumOf { it.dailyLimitMinutes }
            val max = if (global <= 0) 24 * 60 else (global - others).coerceAtLeast(0)
            val clamped = minutes.coerceIn(0, max)
            apps[idx] = apps[idx].copy(dailyLimitMinutes = clamped)
            prefs[KEY_WATCHED] = json.encodeToString(apps)
        }
    }

    fun peekAppearance(): Appearance = appearance.value

    suspend fun setWatchedApps(apps: List<WatchedApp>) {
        store.edit { it[KEY_WATCHED] = json.encodeToString(apps) }
    }

    suspend fun upsertWatched(app: WatchedApp) {
        val next = watchedApps.value.toMutableList()
        val idx = next.indexOfFirst { it.packageName == app.packageName }
        if (idx >= 0) next[idx] = app else next.add(app)
        setWatchedApps(next)
    }

    suspend fun removeWatched(packageName: String) {
        setWatchedApps(watchedApps.value.filterNot { it.packageName == packageName })
    }

    fun usedMillisToday(packageName: String): Long {
        val snap = currentUsage()
        return snap.usedMillis[packageName] ?: 0L
    }

    fun usedMillisOn(date: String, packageName: String): Long {
        return usageHistory.value.days[date]?.get(packageName) ?: 0L
    }

    fun usedMillisTodayTotal(): Long {
        return currentUsage().usedMillis.values.sum()
    }

    fun remainingGlobalMillis(): Long {
        val global = globalDailyLimitMinutes.value
        if (global <= 0) return UNLIMITED_BUDGET
        return (global * 60_000L - usedMillisTodayTotal()).coerceAtLeast(0L)
    }

    fun remainingDailyMillis(packageName: String, dailyLimitMinutes: Int = -1): Long {
        val perApp = if (dailyLimitMinutes >= 0) dailyLimitMinutes
        else watchedApps.value.firstOrNull { it.packageName == packageName }?.dailyLimitMinutes ?: 0
        val perAppRemaining = if (perApp <= 0) UNLIMITED_BUDGET
        else (perApp * 60_000L - usedMillisToday(packageName)).coerceAtLeast(0L)
        val globalRemaining = remainingGlobalMillis()
        return minOf(perAppRemaining, globalRemaining)
    }

    suspend fun addUsedMillis(packageName: String, delta: Long) {
        if (delta <= 0L) return
        store.edit { prefs ->
            val today = todayDateKey()
            val days = mergedHistory(prefs).days.toMutableMap()
            val todayMap = days[today].orEmpty().toMutableMap()
            todayMap[packageName] = (todayMap[packageName] ?: 0L) + delta
            days[today] = todayMap
            fillContinuousDays(days, today)
            writeHistory(prefs, UsageHistory(prune(days, today)))
            prefs[KEY_LAST_USED_WATCHED] = System.currentTimeMillis()
            prefs[KEY_LAST_CELEBRATED_KIND] = ""
        }
    }

    suspend fun saveSession(session: PersistedSession?) {
        store.edit { prefs ->
            if (session == null) prefs.remove(KEY_SESSION)
            else prefs[KEY_SESSION] = json.encodeToString(session)
        }
    }

    suspend fun loadPersistedSession(): PersistedSession? {
        return decodeSession(store.data.first()[KEY_SESSION])
    }

    suspend fun readOnboarded(): Boolean {
        return store.data.first()[KEY_ONBOARDED] == true
    }

    suspend fun readSelfMessage(): String {
        return store.data.first()[KEY_SELF_MESSAGE].orEmpty()
    }

    suspend fun readLastUsedWatchedAt(): Long {
        return store.data.first()[KEY_LAST_USED_WATCHED] ?: 0L
    }

    suspend fun readLastCelebratedKind(): String {
        return store.data.first()[KEY_LAST_CELEBRATED_KIND].orEmpty()
    }

    suspend fun readLastCelebratedAt(): Long {
        return store.data.first()[KEY_LAST_CELEBRATED_AT] ?: 0L
    }

    suspend fun readLastCelebrationTemplate(): Int {
        return store.data.first()[KEY_LAST_CELEBRATION_TEMPLATE] ?: -1
    }

    suspend fun markCelebrated(kind: String, templateIndex: Int, at: Long = System.currentTimeMillis()) {
        store.edit { prefs ->
            prefs[KEY_LAST_CELEBRATED_KIND] = kind
            prefs[KEY_LAST_CELEBRATED_AT] = at
            prefs[KEY_LAST_CELEBRATION_TEMPLATE] = templateIndex
        }
    }

    suspend fun readLastSummaryDate(): String {
        return store.data.first()[KEY_LAST_SUMMARY_DATE].orEmpty()
    }

    suspend fun markSummarySent(date: String) {
        store.edit { it[KEY_LAST_SUMMARY_DATE] = date }
    }

    suspend fun readUsageHistory(): UsageHistory {
        return mergedHistory(store.data.first())
    }

    fun peekOnboardedBlocking(): Boolean = runBlocking {
        store.data.first()[KEY_ONBOARDED] == true
    }

    private fun currentUsage(): DailyUsageSnapshot {
        val snap = dailyUsage.value
        val today = todayDateKey()
        return if (snap.date != today) DailyUsageSnapshot(today) else snap
    }

    suspend fun snapshotToday() {
        store.edit { prefs ->
            val today = todayDateKey()
            val days = mergedHistory(prefs).days.toMutableMap()
            fillContinuousDays(days, today)
            writeHistory(prefs, UsageHistory(prune(days, today)))
        }
    }

    private fun mergedHistory(prefs: Preferences): UsageHistory {
        val days = decodeHistory(prefs[KEY_USAGE_HISTORY]).days.toMutableMap()
        val old = decodeUsage(prefs[KEY_USAGE])
        if (old.date.isNotBlank() && old.usedMillis.isNotEmpty()) {
            val existing = days[old.date].orEmpty().toMutableMap()
            old.usedMillis.forEach { (pkg, millis) ->
                existing[pkg] = max(existing[pkg] ?: 0L, millis)
            }
            days[old.date] = existing
        }
        return UsageHistory(days)
    }

    private fun writeHistory(prefs: MutablePreferences, history: UsageHistory) {
        val today = todayDateKey()
        prefs[KEY_USAGE_HISTORY] = json.encodeToString(history)
        prefs[KEY_USAGE] = json.encodeToString(
            DailyUsageSnapshot(today, history.days[today].orEmpty()),
        )
    }

    private fun prune(days: Map<String, Map<String, Long>>, today: String): Map<String, Map<String, Long>> {
        val cutoff = dateKeyOffset(-14)
        val next = days.filterKeys { it >= cutoff && it.isNotBlank() }.toMutableMap()
        fillContinuousDays(next, today)
        return next
    }

    private fun fillContinuousDays(days: MutableMap<String, Map<String, Long>>, today: String) {
        if (today.isBlank()) return
        if (today !in days) days[today] = emptyMap()
        val existing = days.keys.filter { it.isNotBlank() }
        if (existing.isEmpty()) return
        var cursor = existing.minOrNull() ?: today
        var guard = 0
        while (cursor < today && guard < 40) {
            cursor = com.mindpeace.app.util.dateKeyPlusDays(cursor, 1)
            if (cursor.isBlank() || cursor == today && today in days && guard > 40) break
            if (cursor !in days) days[cursor] = emptyMap()
            guard++
        }
        if (today !in days) days[today] = emptyMap()
    }

    private fun decodeList(raw: String?): List<WatchedApp> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching { json.decodeFromString<List<WatchedApp>>(raw) }.getOrDefault(emptyList())
    }

    private fun decodeUsage(raw: String?): DailyUsageSnapshot {
        if (raw.isNullOrBlank()) return DailyUsageSnapshot()
        return runCatching { json.decodeFromString<DailyUsageSnapshot>(raw) }
            .getOrDefault(DailyUsageSnapshot())
    }

    private fun decodeHistory(raw: String?): UsageHistory {
        if (raw.isNullOrBlank()) return UsageHistory()
        return runCatching { json.decodeFromString<UsageHistory>(raw) }.getOrDefault(UsageHistory())
    }

    private fun decodeSession(raw: String?): PersistedSession? {
        if (raw.isNullOrBlank()) return null
        return runCatching { json.decodeFromString<PersistedSession>(raw) }.getOrNull()
    }

    companion object {
        private val KEY_ONBOARDED = booleanPreferencesKey("onboarded")
        private val KEY_RECENTS = booleanPreferencesKey("recents_locked")
        private val KEY_WATCHED = stringPreferencesKey("watched_apps")
        private val KEY_USAGE = stringPreferencesKey("daily_usage")
        private val KEY_USAGE_HISTORY = stringPreferencesKey("usage_history")
        private val KEY_SESSION = stringPreferencesKey("active_session")
        private val KEY_SELF_MESSAGE = stringPreferencesKey("self_message")
        private val KEY_LAST_USED_WATCHED = longPreferencesKey("last_used_watched_at")
        private val KEY_LAST_CELEBRATED_AT = longPreferencesKey("last_celebrated_at")
        private val KEY_LAST_CELEBRATED_KIND = stringPreferencesKey("last_celebrated_kind")
        private val KEY_LAST_CELEBRATION_TEMPLATE = intPreferencesKey("last_celebration_template")
        private val KEY_LAST_SUMMARY_DATE = stringPreferencesKey("last_summary_date")
        private val KEY_COLOR_MODE = stringPreferencesKey("color_mode")
        private val KEY_VISUAL_STYLE = stringPreferencesKey("visual_style")
        private val KEY_GLOBAL_DAILY = intPreferencesKey("global_daily_limit_minutes")
        private val KEY_LOCALE = stringPreferencesKey("app_locale")
    }
}
