package com.audioeffect.djmixer.music.mixer

import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.webkit.WebView
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.audioeffect.djmixer.music.mixer.core.ads.AdsManager
import com.audioeffect.djmixer.music.mixer.core.ads.RemoteConfigs
import com.audioeffect.djmixer.music.mixer.core.utils.EnumSelectLanguage
import com.audioeffect.djmixer.music.mixer.core.utils.SystemUtil
import com.audioeffect.djmixer.music.mixer.databinding.ActivitySettingBinding

class ActivitySetting : AppCompatActivity() {

    private lateinit var binding: ActivitySettingBinding
    private var currentLanguageCode: String = ""

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(SystemUtil.wrapContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentLanguageCode = SystemUtil.getPreLanguage(this) ?: "en"
        binding = ActivitySettingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        hideSystemUI()
        setupVersionInfo()
        updateLanguageDisplay()
        setupClickListeners()
        loadAds()
    }

    override fun onResume() {
        super.onResume()
        hideSystemUI()
        val latestLang = SystemUtil.getPreLanguage(this) ?: "en"
        if (latestLang != currentLanguageCode) {
            currentLanguageCode = latestLang
            recreate()
        } else {
            updateLanguageDisplay()
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemUI()
        }
    }

    private fun hideSystemUI() {
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_FULLSCREEN
        )
    }

    /**
     * Fetch dynamic version from package info based on versionName
     */
    private fun setupVersionInfo() {
        val versionName = try {
            val pInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, 0)
            }
            pInfo.versionName ?: BuildConfig.VERSION_NAME
        } catch (e: Exception) {
            BuildConfig.VERSION_NAME
        }
        binding.txtVersionValue.text = "v$versionName"
    }

    private fun updateLanguageDisplay() {
        val langCode = SystemUtil.getPreLanguage(this) ?: "en"
        val langEnum = EnumSelectLanguage.entries.find { it.code.equals(langCode, ignoreCase = true) }
        binding.txtLanguageValue.text = if (langEnum != null) {
            getString(langEnum.nameLanguage)
        } else {
            langCode.uppercase()
        }
    }

    private fun setupClickListeners() {
        // Back
        binding.btnBack.setOnClickListener {
            finish()
        }

        // 1. Language Change
        binding.btnLanguage.setOnClickListener {
            startActivity(Intent(this, ActivityLanguageSetting::class.java))
        }

        // 2. Privacy Policy
        binding.btnPolicy.setOnClickListener {
            openPrivacyPolicy()
        }

        // 3. Rate Us
        binding.btnRate.setOnClickListener {
            rateApp()
        }

        // 4. Version Card
        binding.cardVersion.setOnClickListener {
            Toast.makeText(this, R.string.setting_latest_version, Toast.LENGTH_SHORT).show()
        }

        // 5. Share App
        binding.btnShare.setOnClickListener {
            shareApp()
        }

        // 6. DJ Audio Settings
        binding.btnDjAudioSettings.setOnClickListener {
            showDjAudioSettingsDialog()
        }
    }

    private fun openPrivacyPolicy() {
        try {
            val browserIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://sites.google.com/view/game-empire-studio")
            )
            startActivity(browserIntent)
        } catch (e: Exception) {
            Toast.makeText(this, R.string.privacy_policy_load_failed, Toast.LENGTH_SHORT).show()
        }
    }

    private fun rateApp() {
        val pkg = packageName
        try {
            val uri = Uri.parse("market://details?id=$pkg")
            val goToMarket = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NO_HISTORY or
                    Intent.FLAG_ACTIVITY_NEW_DOCUMENT or
                    Intent.FLAG_ACTIVITY_MULTIPLE_TASK
                )
            }
            startActivity(goToMarket)
        } catch (e: ActivityNotFoundException) {
            val uri = Uri.parse("https://play.google.com/store/apps/details?id=$pkg")
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        }
    }

    private fun shareApp() {
        val pkg = packageName
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, getString(R.string.setting_title))
            putExtra(
                Intent.EXTRA_TEXT,
                getString(R.string.share_app_text, "https://play.google.com/store/apps/details?id=$pkg")
            )
        }
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_app_chooser_title)))
    }

    private fun showDjAudioSettingsDialog() {
        try {
            val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_dj_settings, null)
            val btnClose = dialogView.findViewById<ImageView>(R.id.btn_close_settings)
            val btnChangeLanguage = dialogView.findViewById<View>(R.id.btn_change_language)
            val txtCurrentLanguage = dialogView.findViewById<TextView>(R.id.txt_current_language)

            val currentLangCode = SystemUtil.getPreLanguage(this)
            val currentLangNameRes = EnumSelectLanguage.entries.find { it.code.equals(currentLangCode, ignoreCase = true) }?.nameLanguage
            txtCurrentLanguage?.text = if (currentLangNameRes != null) getString(currentLangNameRes) else currentLangCode.uppercase()

            val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
                .setView(dialogView)
                .create()
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

            btnClose?.setOnClickListener {
                dialog.dismiss()
                hideSystemUI()
            }

            btnChangeLanguage?.setOnClickListener {
                dialog.dismiss()
                startActivity(Intent(this, ActivityLanguageSetting::class.java))
            }

            dialog.show()
        } catch (e: Exception) {
            // graceful fallback
        }
    }

    private fun loadAds() {
        try {
            AdsManager.loadAndShowNative(
                activity = this,
                container = binding.layoutAds,
                placementKey = "native_setting",
                adUnitId = getString(R.string.native_banner),
                layoutRes = R.layout.layout_native_banner,
                isEnabled = RemoteConfigs.native_banner
            )
        } catch (_: Exception) {}
    }
}
