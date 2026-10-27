package com.audioeffect.djmixer.music.mixer.sampler

data class SamplerItem(
    val id: String,
    val title: String,
    val soundPath: String = ""
)

data class SamplePack(
    val id: String,
    val title: String,
    val subtitle: String,
    val coverAssetPath: String,
    val isCustom: Boolean = false,
    val isEssential: Boolean = false,
    val isDownloaded: Boolean = true,
    val soundCount: Int = 16,
    val previewAssetPath: String = "sampler/preview.mp3",
    val items: List<SamplerItem> = emptyList()
)
