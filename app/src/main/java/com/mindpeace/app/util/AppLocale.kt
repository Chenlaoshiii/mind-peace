package com.mindpeace.app.util

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.mindpeace.app.R
import java.util.Locale

object AppLocale {
    const val DEFAULT = "zh-CN"
    const val SYSTEM = "system"

    data class Option(val tag: String, val labelRes: Int)

    val options = listOf(
        Option(SYSTEM, R.string.settings_language_system),
        Option("zh-CN", R.string.settings_language_zh_cn),
        Option("zh-TW", R.string.settings_language_zh_tw),
        Option("en", R.string.settings_language_en),
        Option("ja", R.string.settings_language_ja),
        Option("ru", R.string.settings_language_ru),
        Option("lzh", R.string.settings_language_lzh),
        Option("es", R.string.settings_language_es),
        Option("fr", R.string.settings_language_fr),
    )

    fun isSystem(tag: String?): Boolean =
        tag.isNullOrBlank() || tag.equals(SYSTEM, ignoreCase = true)

    /** Stored preference: blank / "system" stay "system"; never map those to zh-CN. */
    fun normalize(tag: String?): String {
        if (isSystem(tag)) return SYSTEM
        return matchingSupported(tag!!) ?: DEFAULT
    }

    fun systemLocaleList(): LocaleList {
        val sys = Resources.getSystem().configuration
        val fromSystem = if (Build.VERSION.SDK_INT >= 24) {
            sys.locales
        } else {
            @Suppress("DEPRECATION")
            LocaleList(sys.locale ?: Locale.getDefault())
        }
        return if (fromSystem.isEmpty) LocaleList.getDefault() else fromSystem
    }

    fun resolve(tag: String?, systemLocales: LocaleList): String {
        if (!isSystem(tag)) return normalize(tag)
        for (i in 0 until systemLocales.size()) {
            val loc = systemLocales[i] ?: continue
            val matched = matchingSupported(loc.toLanguageTag())
            if (matched != null && matched != SYSTEM) return matched
        }
        return DEFAULT
    }

    fun resolve(tag: String?, systemLocale: Locale): String {
        if (!isSystem(tag)) return normalize(tag)
        return matchingSupported(systemLocale.toLanguageTag()) ?: DEFAULT
    }

    fun localeFor(tag: String): Locale {
        val concrete = if (isSystem(tag)) resolve(tag, systemLocaleList()) else normalize(tag)
        return when (concrete) {
            "zh-TW" -> Locale.Builder().setLanguage("zh").setRegion("TW").build()
            "en" -> Locale.ENGLISH
            "ja" -> Locale.JAPANESE
            "ru" -> Locale.Builder().setLanguage("ru").build()
            "lzh" -> Locale.Builder().setLanguage("lzh").build()
            "es" -> Locale.Builder().setLanguage("es").build()
            "fr" -> Locale.FRENCH
            else -> Locale.Builder().setLanguage("zh").setRegion("CN").build()
        }
    }

    fun apply(app: Application, tag: String) {
        if (isSystem(tag)) {
            val resolved = resolve(tag, systemLocaleList())
            val locale = localeFor(resolved)
            Locale.setDefault(locale)
            copySystemLocalesInto(app.resources.configuration)?.let { config ->
                @Suppress("DEPRECATION")
                app.resources.updateConfiguration(config, app.resources.displayMetrics)
            }
            if (!AppCompatDelegate.getApplicationLocales().isEmpty) {
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
            }
            return
        }
        val normalized = normalize(tag)
        val locale = localeFor(normalized)
        Locale.setDefault(locale)
        val config = Configuration(app.resources.configuration)
        if (Build.VERSION.SDK_INT >= 24) {
            config.setLocales(LocaleList(locale))
        } else {
            @Suppress("DEPRECATION")
            config.locale = locale
        }
        @Suppress("DEPRECATION")
        app.resources.updateConfiguration(config, app.resources.displayMetrics)
        val current = normalize(AppCompatDelegate.getApplicationLocales().toLanguageTags())
        if (current != normalized) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(normalized))
        }
    }

    fun wrap(context: Context): Context {
        val stored = AppCompatDelegate.getApplicationLocales().toLanguageTags()
        val config = Configuration(context.resources.configuration)
        if (isSystem(stored)) {
            copySystemLocalesInto(config) ?: return context
            return context.createConfigurationContext(config)
        }
        val locale = localeFor(stored)
        if (Build.VERSION.SDK_INT >= 24) {
            config.setLocales(LocaleList(locale))
        } else {
            @Suppress("DEPRECATION")
            config.locale = locale
        }
        return context.createConfigurationContext(config)
    }

    fun labelRes(tag: String): Int {
        val n = normalize(tag)
        return options.firstOrNull { it.tag == n }?.labelRes ?: R.string.settings_language_system
    }

    /**
     * Maps a language tag to a supported app locale.
     * lzh is matched before zh. "system" is returned as-is and is not zh-CN.
     * Unsupported tags return null so callers can fall back.
     */
    private fun matchingSupported(tag: String): String? {
        val t = tag.replace('_', '-').trim()
        if (t.isEmpty()) return null
        val first = t.split(",", limit = 2)[0].trim()
        if (first.equals(SYSTEM, ignoreCase = true)) return SYSTEM
        options.firstOrNull { it.tag != SYSTEM && it.tag.equals(first, ignoreCase = true) }?.let {
            return it.tag
        }
        return when {
            first.equals("lzh", ignoreCase = true) ||
                first.startsWith("lzh", ignoreCase = true) -> "lzh"
            first.startsWith("zh-Hant", ignoreCase = true) ||
                first.startsWith("zh-TW", ignoreCase = true) ||
                first.startsWith("zh-HK", ignoreCase = true) ||
                first.startsWith("zh-MO", ignoreCase = true) -> "zh-TW"
            first.startsWith("zh", ignoreCase = true) -> "zh-CN"
            first.startsWith("en", ignoreCase = true) -> "en"
            first.startsWith("ja", ignoreCase = true) -> "ja"
            first.startsWith("ru", ignoreCase = true) -> "ru"
            first.startsWith("es", ignoreCase = true) -> "es"
            first.startsWith("fr", ignoreCase = true) -> "fr"
            else -> null
        }
    }

    private fun copySystemLocalesInto(into: Configuration): Configuration? {
        val sys = Resources.getSystem().configuration
        val config = Configuration(into)
        if (Build.VERSION.SDK_INT >= 24) {
            val locales = sys.locales
            if (locales.isEmpty) return null
            config.setLocales(locales)
        } else {
            @Suppress("DEPRECATION")
            val locale = sys.locale ?: return null
            @Suppress("DEPRECATION")
            config.locale = locale
        }
        return config
    }
}
