package com.audioeffect.djmixer.music.mixer.core.utils

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.cscmobi.libraryads.commons.sharepreference.CSCSPF
import com.cscmobi.libraryads.commons.sharepreference.CSCSPFHolder
import java.util.Locale

object SystemUtil {
    private const val TAG = "LanguageDebug"
    private var myLocale: Locale? = null

    val SUPPORTED_LANGUAGES = setOf(
        "en", "vi", "es", "pt", "hi", "fr", "de", "id", "ar",
        "bn", "ru", "ja", "ko", "zh", "fil", "th", "tr", "it"
    )

    fun getLocaleFromCode(lang: String): Locale {
        val clean = lang.trim().lowercase()
        return when {
            clean == "zh" || clean.startsWith("zh") -> Locale.SIMPLIFIED_CHINESE
            clean == "fil" -> Locale("fil")
            clean == "id" -> Locale("id")
            clean == "pt" -> Locale("pt")
            clean == "ar" -> Locale("ar")
            clean == "ja" -> Locale.JAPANESE
            clean == "ko" -> Locale.KOREAN
            clean == "fr" -> Locale.FRENCH
            clean == "de" -> Locale.GERMAN
            clean == "it" -> Locale.ITALIAN
            else -> Locale.forLanguageTag(clean)
        }
    }

    fun saveLocale(context: Context, lang: String?) {
        if (lang.isNullOrBlank()) return
        Log.d(TAG, "SystemUtil.saveLocale called with lang=$lang")
        setPreLanguage(context, lang)
    }

    fun setLocale(context: Context) {
        var language = getPreLanguage(context)
        Log.d(TAG, "SystemUtil.setLocale called, resolved preLanguage=$language")
        if (language.isNullOrBlank()) {
            language = "en"
        }
        val newLocale = getLocaleFromCode(language)
        Locale.setDefault(newLocale)
        myLocale = newLocale

        try {
            val currentLocale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                context.resources.configuration.locales.get(0)
            } else {
                @Suppress("DEPRECATION")
                context.resources.configuration.locale
            }
            if (currentLocale != null && currentLocale.language.equals(newLocale.language, ignoreCase = true)) {
                return
            }

            val config = Configuration(context.resources.configuration)
            config.setLocale(newLocale)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val localeList = LocaleList(newLocale)
                LocaleList.setDefault(localeList)
                config.setLocales(localeList)
            }
            @Suppress("DEPRECATION")
            context.resources.updateConfiguration(config, context.resources.displayMetrics)

            try {
                val appCtx = context.applicationContext
                if (appCtx != null && appCtx !== context) {
                    @Suppress("DEPRECATION")
                    appCtx.resources.updateConfiguration(config, appCtx.resources.displayMetrics)
                }
            } catch (_: Exception) {}
        } catch (e: Exception) {
            Log.e(TAG, "SystemUtil.setLocale updateConfiguration error: ${e.message}")
        }
    }

    fun changeLang(lang: String?, context: Context) {
        if (lang.isNullOrBlank()) return
        Log.d(TAG, "SystemUtil.changeLang called with lang=$lang")

        saveLocale(context, lang)
        setLocale(context)
        try {
            val appCtx = context.applicationContext
            if (appCtx != null && appCtx !== context) {
                saveLocale(appCtx, lang)
                setLocale(appCtx)
            }
        } catch (_: Exception) {}

        try {
            val currentLocales = AppCompatDelegate.getApplicationLocales()
            val currentTag = currentLocales.toLanguageTags()
            if (!lang.equals(currentTag, ignoreCase = true)) {
                AppCompatDelegate.setApplicationLocales(
                    LocaleListCompat.forLanguageTags(lang)
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "SystemUtil.changeLang setApplicationLocales error: ${e.message}")
        }
    }

    fun wrapContext(context: Context): Context {
        var language = getPreLanguage(context)
        if (language.isNullOrBlank()) {
            language = "en"
        }
        val newLocale = getLocaleFromCode(language)
        Locale.setDefault(newLocale)

        val res = context.resources
        val config = Configuration(res.configuration)
        config.setLocale(newLocale)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val localeList = LocaleList(newLocale)
            LocaleList.setDefault(localeList)
            config.setLocales(localeList)
        }

        @Suppress("DEPRECATION")
        res.updateConfiguration(config, res.displayMetrics)

        try {
            val appCtx = context.applicationContext
            if (appCtx != null && appCtx !== context) {
                @Suppress("DEPRECATION")
                appCtx.resources.updateConfiguration(config, appCtx.resources.displayMetrics)
            }
        } catch (_: Exception) {}

        return context.createConfigurationContext(config)
    }

    fun getPreLanguage(mContext: Context): String {
        var cscLang: String? = null
        try {
            val lang = CSCSPFHolder.get().language_code_selected
            if (!lang.isNullOrBlank()) {
                cscLang = lang
            }
        } catch (e: Exception) {
            try {
                val lang = CSCSPF.getInstance(mContext).language_code_selected
                if (!lang.isNullOrBlank()) {
                    cscLang = lang
                }
            } catch (e2: Exception) {
                Log.e(TAG, "SystemUtil.getPreLanguage: CSCSPF read error=${e2.message}")
            }
        }
        val preferences: SharedPreferences = mContext.getSharedPreferences("data", Context.MODE_PRIVATE)
        val dataLang = preferences.getString("KEY_LANGUAGE", null)
        val appPrefLang = mContext.getSharedPreferences("app_prefs_djmixer", Context.MODE_PRIVATE)
            .getString("user_language_code", null)

        Log.d(TAG, "SystemUtil.getPreLanguage: cscLang=$cscLang, dataLang=$dataLang, appPrefLang=$appPrefLang")
        if (!cscLang.isNullOrBlank()) {
            return cscLang
        }
        if (!dataLang.isNullOrBlank()) {
            return dataLang
        }
        if (!appPrefLang.isNullOrBlank()) {
            return appPrefLang
        }
        val systemLang = Locale.getDefault().language
        return if (SUPPORTED_LANGUAGES.contains(systemLang)) systemLang else "en"
    }

    fun setPreLanguage(context: Context, language: String?) {
        Log.d(TAG, "SystemUtil.setPreLanguage called with language=$language")
        if (language.isNullOrEmpty()) {
            return
        }
        try {
            CSCSPFHolder.get().language_code_selected = language
            Log.d(TAG, "SystemUtil.setPreLanguage: saved to CSCSPFHolder=$language")
        } catch (e: Exception) {
            try {
                CSCSPF.getInstance(context).language_code_selected = language
                Log.d(TAG, "SystemUtil.setPreLanguage: saved to CSCSPF.getInstance=$language")
            } catch (e2: Exception) {
                Log.e(TAG, "SystemUtil.setPreLanguage: CSCSPF write error=${e2.message}")
            }
        }
        val preferences: SharedPreferences = context.getSharedPreferences("data", Context.MODE_PRIVATE)
        preferences.edit().putString("KEY_LANGUAGE", language).apply()
        val appPrefs = context.getSharedPreferences("app_prefs_djmixer", Context.MODE_PRIVATE)
        appPrefs.edit().putString("user_language_code", language).apply()
        Log.d(TAG, "SystemUtil.setPreLanguage: saved to SharedPreferences(data and app_prefs_djmixer)=$language")
    }
}
