package com.audioeffect.djmixer.music.mixer.sampler

import android.content.Context
import com.audioeffect.djmixer.music.mixer.R
import org.json.JSONObject
import java.io.InputStreamReader

object SamplerManager {

    private const val PREFS_NAME = "dj_mixer_sampler_prefs"
    private const val KEY_ACTIVE_PACK_DECK_A = "active_pack_deck_a"
    private const val KEY_ACTIVE_PACK_DECK_B = "active_pack_deck_b"
    const val ID_ESSENTIAL = "essential"
    const val ID_CUSTOM = "custom"

    // Essential 28 sounds
    val ESSENTIAL_ITEMS = listOf(
        SamplerItem("01", "Come on", "sampler/audio_1.mp3"),
        SamplerItem("02", "Horn", "sampler/audio_2.mp3"),
        SamplerItem("03", "Oh yeah", "sampler/audio_3.mp3"),
        SamplerItem("04", "Lazer", "sampler/audio_4.mp3"),
        SamplerItem("05", "Gunshot", "sampler/audio_5.mp3"),
        SamplerItem("06", "Trumpet", "sampler/audio_28.mp3"),
        SamplerItem("07", "Boing", "sampler/audio_6.mp3"),
        SamplerItem("08", "Alarm", "sampler/audio_18.mp3"),
        SamplerItem("09", "Clap", "sampler/audio_13.mp3"),
        SamplerItem("10", "Cymbal", "sampler/audio_14.mp3"),
        SamplerItem("11", "Boom", "sampler/audio_15.mp3"),
        SamplerItem("12", "Prick", "sampler/audio_16.mp3"),
        SamplerItem("13", "Rhythm", "sampler/audio_11.mp3"),
        SamplerItem("14", "Walker", "sampler/audio_12.mp3"),
        SamplerItem("15", "Scratch", "sampler/audio_21.mp3"),
        SamplerItem("16", "DJScratch", "sampler/audio_22.mp3"),
        SamplerItem("17", "Squeak", "sampler/audio_17.mp3"),
        SamplerItem("18", "Whoosh", "sampler/audio_19.mp3"),
        SamplerItem("19", "Yes", "sampler/audio_20.mp3"),
        SamplerItem("20", "Ohh oh", "sampler/audio_27.mp3"),
        SamplerItem("21", "Beat", "sampler/audio_24.mp3"),
        SamplerItem("22", "Crunchy", "sampler/audio_10.mp3"),
        SamplerItem("23", "Downlifter", "sampler/audio_8.mp3"),
        SamplerItem("24", "Uplifter", "sampler/audio_9.mp3"),
        SamplerItem("25", "Drums", "sampler/audio_7.mp3"),
        SamplerItem("26", "Drop it", "sampler/audio_23.mp3"),
        SamplerItem("27", "Vocal 1", "sampler/audio_25.mp3"),
        SamplerItem("28", "Vocal 2", "sampler/audio_26.mp3")
    )

    private var cachedPacks: List<SamplePack>? = null

    fun getActivePackDeckA(context: Context): String {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sp.getString(KEY_ACTIVE_PACK_DECK_A, ID_ESSENTIAL) ?: ID_ESSENTIAL
    }

    fun getActivePackDeckB(context: Context): String {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sp.getString(KEY_ACTIVE_PACK_DECK_B, ID_ESSENTIAL) ?: ID_ESSENTIAL
    }

    fun setActivePackDeckA(context: Context, packId: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ACTIVE_PACK_DECK_A, packId)
            .apply()
    }

    fun setActivePackDeckB(context: Context, packId: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ACTIVE_PACK_DECK_B, packId)
            .apply()
    }

    private const val KEY_UNLOCKED_PACK_PREFIX = "unlocked_pack_"

    fun isPackUnlocked(context: Context, packId: String): Boolean {
        if (packId == ID_ESSENTIAL || packId == ID_CUSTOM) return true
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sp.getBoolean(KEY_UNLOCKED_PACK_PREFIX + packId, false)
    }

    fun setPackUnlocked(context: Context, packId: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_UNLOCKED_PACK_PREFIX + packId, true)
            .apply()
    }

    fun loadAllPacks(context: Context): List<SamplePack> {
        cachedPacks?.let { return it }

        val list = mutableListOf<SamplePack>()

        // 1. Custom Sound Sample
        list.add(
            SamplePack(
                id = ID_CUSTOM,
                title = context.getString(R.string.custom_sound_sample),
                subtitle = context.getString(R.string.sound_count, 0),
                coverAssetPath = "sampler/icon_custom.webp",
                isCustom = true,
                isEssential = false,
                isDownloaded = true,
                soundCount = 0,
                previewAssetPath = "sampler/preview.mp3"
            )
        )

        // 2. Essential Pack
        list.add(
            SamplePack(
                id = ID_ESSENTIAL,
                title = context.getString(R.string.essential),
                subtitle = context.getString(R.string.instrument_sounds),
                coverAssetPath = "sampler/icon.webp",
                isCustom = false,
                isEssential = true,
                isDownloaded = true,
                soundCount = 28,
                previewAssetPath = "sampler/preview.mp3",
                items = ESSENTIAL_ITEMS
            )
        )

        // 3. Load packs from JSON (01..38)
        try {
            val jsonStream = context.assets.open("sampler/djmixer_sampler_data.json")
            val jsonStr = InputStreamReader(jsonStream).readText()
            jsonStream.close()

            val rootObj = JSONObject(jsonStr)
            val samplersArr = rootObj.optJSONArray("samplers")
            if (samplersArr != null) {
                for (i in 0 until samplersArr.length()) {
                    val sObj = samplersArr.getJSONObject(i)
                    val id = sObj.optString("id")
                    val title = sObj.optString("title")
                    val des = sObj.optString("des")
                    val itemsArr = sObj.optJSONArray("items")

                    val packItems = mutableListOf<SamplerItem>()
                    val itemCount = itemsArr?.length() ?: 16
                    if (itemsArr != null) {
                        for (j in 0 until itemsArr.length()) {
                            val itemObj = itemsArr.getJSONObject(j)
                            val itemId = itemObj.optString("id", "${j + 1}")
                            val itemTitle = itemObj.optString("title", "Sound ${j + 1}")
                            // Deterministic audio mapping from rich sampler audio assets
                            val audioNum = ((i * 3 + j * 5) % 28) + 1
                            packItems.add(
                                SamplerItem(
                                    id = itemId,
                                    title = itemTitle,
                                    soundPath = "sampler/audio_$audioNum.mp3"
                                )
                            )
                        }
                    }

                    list.add(
                        SamplePack(
                            id = id,
                            title = title,
                            subtitle = des,
                            coverAssetPath = "sampler/covers/pack_$id.png",
                            isCustom = false,
                            isEssential = false,
                            isDownloaded = true,
                            soundCount = itemCount,
                            previewAssetPath = "sampler/preview.mp3",
                            items = packItems
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        cachedPacks = list
        return list
    }

    fun getSamplerItemsForDeck(context: Context, isLeft: Boolean): List<SamplerItem> {
        val activePackId = if (isLeft) getActivePackDeckA(context) else getActivePackDeckB(context)
        val packs = loadAllPacks(context)
        val found = packs.find { it.id == activePackId }
        return if (found != null && found.items.isNotEmpty()) {
            found.items
        } else {
            ESSENTIAL_ITEMS
        }
    }
}
