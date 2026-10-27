package com.ijoysoft.mix.view

import android.content.Context
import android.util.AttributeSet

class VolumeSeekBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : CustomSeekBar(context, attrs, defStyleAttr) {

    init {
        isVertical = true
        symmetryMode = false
        maxProgress = 1000
        progress = 1000
        adsorptionRadius = 0
    }

    fun getVolume(): Float {
        return progress.toFloat() / maxProgress
    }
}
