package com.morse.wearos.data

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/**
 * In-app language override, so a watch set to English can still run Morse in Spanish.
 * SharedPreferences instead of DataStore because attachBaseContext needs a synchronous read.
 */
object AppLanguage {
    /** Empty tag means "follow the system language". */
    val TAGS = listOf("", "en", "es")

    private const val PREFS = "morse_language"
    private const val KEY = "tag"
    private val systemLocale: Locale = Locale.getDefault()

    fun get(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "").orEmpty()

    fun set(context: Context, tag: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, tag).commit()
    }

    /** Applies the saved language to [base]; call from attachBaseContext. Also sets the JVM default for non-UI text. */
    fun wrap(base: Context): Context {
        val tag = get(base)
        val locale = if (tag.isEmpty()) systemLocale else Locale.forLanguageTag(tag)
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration).apply { setLocale(locale) }
        return base.createConfigurationContext(config)
    }
}
