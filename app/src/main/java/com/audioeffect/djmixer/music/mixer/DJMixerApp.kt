package com.audioeffect.djmixer.music.mixer

import android.app.Activity
import android.app.Application
import android.content.Intent
import com.cscmobi.libraryads.CSCApplication
import com.cscmobi.libraryads.commons.sharepreference.CSCSPFHolder
import com.cscmobi.libraryads.data.AdsLanguageConfig
import com.cscmobi.libraryads.data.AdsOBConfig
import com.cscmobi.libraryads.data.AdsSplashConfig
import com.cscmobi.libraryads.data.LanguageConfig
import com.cscmobi.libraryads.data.LanguageSetting
import com.cscmobi.libraryads.data.OBConfig
import com.cscmobi.libraryads.data.OnActivityCallBack
import com.cscmobi.libraryads.data.SplashConfig
import com.cscmobi.libraryads.data.UiLanguageConfig
import com.cscmobi.libraryads.data.UiOBConfig
import com.cscmobi.libraryads.data.UiSplashConfig
import com.audioeffect.djmixer.music.mixer.core.ads.AdsManager
import com.audioeffect.djmixer.music.mixer.core.ads.RemoteConfigs
import com.audioeffect.djmixer.music.mixer.core.utils.EnumSelectLanguage
import com.audioeffect.djmixer.music.mixer.core.utils.SystemUtil

class DJMixerApp : Application() {

    override fun attachBaseContext(base: android.content.Context) {
        super.attachBaseContext(SystemUtil.wrapContext(base))
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        SystemUtil.setLocale(this)
    }

    override fun onCreate() {
        super.onCreate()
        CSCSPFHolder.init(this)
        SystemUtil.setLocale(this)

        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: android.os.Bundle?) {
                SystemUtil.setLocale(activity)
            }
            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityResumed(activity: Activity) {
                SystemUtil.setLocale(activity)
            }
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: android.os.Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })

        try {
            com.google.firebase.FirebaseApp.initializeApp(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        initFO()
    }

    private fun initFO() {
        val cscLibrary = CSCApplication(this, RemoteConfigs, getString(R.string.app_id), BuildConfig.DEBUG)

        try {
            val remoteConfig = com.google.firebase.remoteconfig.FirebaseRemoteConfig.getInstance()
            val configSettings = com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(if (BuildConfig.DEBUG) 60L else 3600L)
                .build()
            remoteConfig.setConfigSettingsAsync(configSettings)
            remoteConfig.addOnConfigUpdateListener(object : com.google.firebase.remoteconfig.ConfigUpdateListener {
                override fun onUpdate(configUpdate: com.google.firebase.remoteconfig.ConfigUpdate) {
                    remoteConfig.activate()
                }

                override fun onError(error: com.google.firebase.remoteconfig.FirebaseRemoteConfigException) {
                }
            })
        } catch (e: Exception) {
            e.printStackTrace()
        }

        cscLibrary.initSdk(
            adjustAppToken = "htdhy4jh8sn4",
            gsmAppId = "6acdea0bcaa0183a0e6b5a35",
            splashConfig = SplashConfig(
                uiSplashConfig = UiSplashConfig(
                    resLayout = R.layout.activity_splash,
                    showFOForever = false,
                    homeActivity = MainActivity::class.java,
                    timeout = 45_000
                ),
                adsSplashConfig = AdsSplashConfig(
                    bannerId = getString(R.string.banner_splash),
                    interHighId = getString(R.string.inter_splash_high),
                    interAllId = getString(R.string.inter_splash),
                    nativeFullLayout = R.layout.layout_native_full,
                    admobAOAId = getString(R.string.resume_open_app),
                    isCheckOrganicUser = true
                )
            ),
            languageConfig = LanguageConfig(
                uiLanguageConfig = UiLanguageConfig(
                    resLayout = R.layout.activity_language_app,
                    itemLangDefault = R.layout.item_select_language_default,
                    itemLangSelected = R.layout.item_select_language_selected,
                    listLanguage = EnumSelectLanguage.toLanguageModelList(),
                    languageSetting = object : LanguageSetting {
                        override fun onDone(activity: Activity) {
                            val code = try {
                                CSCSPFHolder.get().language_code_selected
                            } catch (_: Exception) {
                                null
                            }
                            val previousCode = SystemUtil.getPreLanguage(activity)
                            val finalCode = if (!code.isNullOrBlank()) code else previousCode
                            SystemUtil.setPreLanguage(activity, finalCode)
                            SystemUtil.changeLang(finalCode, activity)
                            if (!previousCode.equals(finalCode, ignoreCase = true)) {
                                val mainIntent = Intent(activity, MainActivity::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                }
                                activity.startActivity(mainIntent)
                            }
                            activity.finish()
                        }
                    }
                ),
                adsLanguageConfig = AdsLanguageConfig(
                    nativeLangHighId = getString(R.string.native_language_high),
                    nativeLangId = getString(R.string.native_language),
                    nativeLangClickHighId = getString(R.string.native_language_high_click),
                    nativeLangClickId = getString(R.string.native_language_click),
                    layoutNative = R.layout.layout_native_media,
                    layoutNativeClick = R.layout.layout_native_media_click
                )
            ),
            obConfig = OBConfig(
                uiOBConfig = UiOBConfig(
                    resFragmentOB1 = R.layout.fragment_intro1,
                    resFragmentOB2 = R.layout.fragment_intro2,
                    resFragmentOB3 = R.layout.fragment_intro3,
                    resFragmentOB4 = R.layout.fragment_intro4,
                    activityCallback = object : OnActivityCallBack {
                        override fun onStartActivity(activity: Activity, inSession2: Boolean) {
                            val savedLang = try {
                                CSCSPFHolder.get().language_code_selected
                            } catch (_: Exception) {
                                null
                            }
                            val finalLang = if (!savedLang.isNullOrBlank()) savedLang else SystemUtil.getPreLanguage(activity)
                            SystemUtil.changeLang(finalLang, activity)
                        }

                        override fun onNextActivity(activity: Activity, inSession2: Boolean) {
                            val savedLang = try {
                                CSCSPFHolder.get().language_code_selected
                            } catch (_: Exception) {
                                null
                            }
                            val finalLang = if (!savedLang.isNullOrBlank()) savedLang else SystemUtil.getPreLanguage(activity)
                            SystemUtil.changeLang(finalLang, activity)
                            val intent = Intent(activity, MainActivity::class.java).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                            }
                            activity.startActivity(intent)
                        }
                    },
                    nextOBActivity = MainActivity::class.java
                ),
                adsOBConfig = AdsOBConfig(
                    nativeOB1Id = getString(R.string.native_onboarding_1),
                    nativeOB4Id = getString(R.string.native_onboarding_4),
                    nativeOBFull12Id = getString(R.string.native_onboarding_full_1),
                    nativeOBFull34Id = getString(R.string.native_onboarding_full_2),
                    layoutNativeOB1 = R.layout.layout_native_media,
                    layoutNativeOB4 = R.layout.layout_native_media,
                    layoutNativeFullOB = R.layout.admob_layout_native_full,
                    isLoadNativeOBInLanguage = true
                )
            )
        )

        try {
            val field = CSCApplication::class.java.getDeclaredField("cscOpenAds").apply { isAccessible = true }
            AdsManager.registerOpenAds(field.get(cscLibrary) as? com.cscmobi.libraryads.ads.open_ads.CSCOpenAds)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
