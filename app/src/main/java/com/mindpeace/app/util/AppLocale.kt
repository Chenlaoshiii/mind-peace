package com.mindpeace.app.util

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.mindpeace.app.R
import java.util.Locale

object AppLocale {
    const val DEFAULT = "zh-CN"

    data class Option(val tag: String, val labelRes: Int)

    val options = listOf(
        Option("zh-CN", R.string.settings_language_zh_cn),
        Option("zh-TW", R.string.settings_language_zh_tw),
        Option("en", R.string.settings_language_en),
        Option("ja", R.string.settings_language_ja),
        Option("ru", R.string.settings_language_ru),
    )

    fun normalize(tag: String?): String {
        if (tag.isNullOrBlank()) return DEFAULT
        val t = tag.replace('_', '-').trim()
        val first = t.split(",", limit = 2)[0].trim()
        options.firstOrNull { it.tag.equals(first, ignoreCase = true) }?.let { return it.tag }
        return when {
            first.startsWith("zh-Hant", ignoreCase = true) ||
                first.startsWith("zh-TW", ignoreCase = true) ||
                first.startsWith("zh-HK", ignoreCase = true) ||
                first.startsWith("zh-MO", ignoreCase = true) -> "zh-TW"
            first.startsWith("zh", ignoreCase = true) -> "zh-CN"
            first.startsWith("en", ignoreCase = true) -> "en"
            first.startsWith("ja", ignoreCase = true) -> "ja"
            first.startsWith("ru", ignoreCase = true) -> "ru"
            else -> DEFAULT
        }
    }

    fun localeFor(tag: String): Locale {
        return when (normalize(tag)) {
            "zh-TW" -> Locale.Builder().setLanguage("zh").setRegion("TW").build()
            "en" -> Locale.ENGLISH
            "ja" -> Locale.JAPANESE
            "ru" -> Locale.Builder().setLanguage("ru").build()
            else -> Locale.Builder().setLanguage("zh").setRegion("CN").build()
        }
    }

    fun apply(app: Application, tag: String) {
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
        val tag = normalize(AppCompatDelegate.getApplicationLocales().toLanguageTags())
        val locale = localeFor(tag)
        val config = Configuration(context.resources.configuration)
        if (Build.VERSION.SDK_INT >= 24) {
            config.setLocales(LocaleList(locale))
        } else {
            @Suppress("DEPRECATION")
            config.locale = locale
        }
        return context.createConfigurationContext(config)
    }
}
