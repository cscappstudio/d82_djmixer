package com.audioeffect.djmixer.music.mixer.core.utils

import com.audioeffect.djmixer.music.mixer.R
import com.cscmobi.libraryads.data.LanguageModel
import java.util.Locale

enum class EnumSelectLanguage(val id: Int, val nameLanguage: Int, val flag: Int, val code: String) {
    VIETNAMESE(1, R.string.language_vietnamese, R.drawable.flag_vn, "vi"),
    SPANISH(2, R.string.language_spanish, R.drawable.flag_es, "es"),
    PORTUGUESE(3, R.string.language_portuguese, R.drawable.flag_pt, "pt"),
    HINDI(4, R.string.language_hindi, R.drawable.flag_in, "hi"),
    FRENCH(5, R.string.language_french, R.drawable.flag_fr, "fr"),
    GERMAN(6, R.string.language_german, R.drawable.flag_de, "de"),
    UNITED_STATES(7, R.string.language_english, R.drawable.flag_us, "en"),
    INDONESIAN(8, R.string.language_indonesian, R.drawable.flag_id, "id"),
    ARABIC(9, R.string.language_arabic, R.drawable.flag_arabic, "ar"),
    BENGALI(10, R.string.language_bengali, R.drawable.flag_bn, "bn"),
    RUSSIAN(11, R.string.language_russian, R.drawable.flag_ru, "ru"),
    JAPANESE(12, R.string.language_japanese, R.drawable.flag_jp, "ja"),
    KOREAN(13, R.string.language_korean, R.drawable.flag_kr, "ko"),
    CHINESE(14, R.string.language_chinese, R.drawable.flag_cn, "zh"),
    PHILIPPINES(15, R.string.language_filipino, R.drawable.flag_ph, "fil"),
    THAI(16, R.string.language_thai, R.drawable.flag_thailand, "th"),
    TURKISH(17, R.string.language_turkish, R.drawable.flag_turkish, "tr"),
    ITALIAN(18, R.string.language_italian, R.drawable.flag_it, "it");

    companion object {
        fun toLanguageModelList(): ArrayList<LanguageModel> {
            val systemLang = Locale.getDefault().language
            val hasSystemMatch = entries.any { it.code.equals(systemLang, ignoreCase = true) }
            val defaultSelectedCode = if (hasSystemMatch) systemLang else "en"

            val list = arrayListOf<LanguageModel>()
            for (lang in entries) {
                list.add(
                    LanguageModel(
                        txtLanguage = lang.nameLanguage,
                        icFlag = lang.flag,
                        code = lang.code,
                        isSelect = lang.code.equals(defaultSelectedCode, ignoreCase = true)
                    )
                )
            }
            return list
        }
    }
}
