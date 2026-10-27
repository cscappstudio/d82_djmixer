package com.audioeffect.djmixer.music.mixer

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.audioeffect.djmixer.music.mixer.core.utils.EnumSelectLanguage
import com.audioeffect.djmixer.music.mixer.core.utils.SystemUtil
import com.audioeffect.djmixer.music.mixer.databinding.ActivityLanguageSettingBinding
import com.cscmobi.libraryads.commons.sharepreference.CSCSPFHolder

class ActivityLanguageSetting : AppCompatActivity() {

    private lateinit var binding: ActivityLanguageSettingBinding
    private var initialLanguageCode: String = "en"
    private var selectedLanguageCode: String = "en"
    private lateinit var languageAdapter: LanguageSettingAdapter

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(SystemUtil.wrapContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLanguageSettingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        hideSystemUI()

        val savedCode = SystemUtil.getPreLanguage(this).ifBlank { "en" }
        val matchedLang = EnumSelectLanguage.entries.find {
            it.code.equals(savedCode, ignoreCase = true)
        }
        initialLanguageCode = matchedLang?.code ?: "en"
        selectedLanguageCode = initialLanguageCode

        setupRecyclerView()
        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        hideSystemUI()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemUI()
        }
    }

    private fun hideSystemUI() {
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
            )
    }

    private fun setupRecyclerView() {
        val languages = EnumSelectLanguage.entries.toList()
        languageAdapter = LanguageSettingAdapter(
            items = languages,
            getSelectedCode = { selectedLanguageCode },
            onSelect = { lang ->
                selectedLanguageCode = lang.code
                languageAdapter.notifyDataSetChanged()
            }
        )
        binding.rcvLanguage.apply {
            layoutManager = GridLayoutManager(this@ActivityLanguageSetting, 2)
            adapter = languageAdapter
            setHasFixedSize(true)
        }
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnDone.setOnClickListener {
            applySelectedLanguage()
        }
    }

    private fun applySelectedLanguage() {
        try {
            CSCSPFHolder.get().language_code_selected = selectedLanguageCode
        } catch (_: Exception) {
        }
        SystemUtil.setPreLanguage(this, selectedLanguageCode)
        SystemUtil.changeLang(selectedLanguageCode, this)

        if (!initialLanguageCode.equals(selectedLanguageCode, ignoreCase = true)) {
            val mainIntent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(mainIntent)
        }
        finish()
    }

    private class LanguageSettingAdapter(
        private val items: List<EnumSelectLanguage>,
        private val getSelectedCode: () -> String,
        private val onSelect: (EnumSelectLanguage) -> Unit
    ) : RecyclerView.Adapter<LanguageSettingAdapter.LangViewHolder>() {

        companion object {
            private const val TYPE_DEFAULT = 0
            private const val TYPE_SELECTED = 1
        }

        override fun getItemViewType(position: Int): Int {
            val item = items[position]
            return if (item.code.equals(getSelectedCode(), ignoreCase = true)) {
                TYPE_SELECTED
            } else {
                TYPE_DEFAULT
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LangViewHolder {
            val layoutRes = if (viewType == TYPE_SELECTED) {
                R.layout.item_select_language_selected
            } else {
                R.layout.item_select_language_default
            }
            val view = LayoutInflater.from(parent.context).inflate(layoutRes, parent, false)
            return LangViewHolder(view)
        }

        override fun onBindViewHolder(holder: LangViewHolder, position: Int) {
            val item = items[position]
            holder.txtLanguage.setText(item.nameLanguage)
            holder.itemView.setOnClickListener {
                onSelect(item)
            }
        }

        override fun getItemCount(): Int = items.size

        class LangViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val txtLanguage: TextView = itemView.findViewById(R.id.txtLanguage)
        }
    }
}
