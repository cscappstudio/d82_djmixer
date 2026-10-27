package com.ijoysoft.mix.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.util.AttributeSet
import androidx.core.content.ContextCompat
import com.audioeffect.djmixer.music.mixer.R

class BPMSeekBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : CustomSeekBar(context, attrs, defStyleAttr) {

    private val pitchRange = 0.08f // ±8% pitch adjustment

    init {
        isVertical = false
        symmetryMode = false
        maxProgress = 1000
        progress = 500 // 0% pitch delta
        adsorptionProgress = 500
        adsorptionRadius = 25
        progressColor = Color.TRANSPARENT
        progressBgColor = ContextCompat.getColor(context, R.color.pitch_seekbar_progress_bg_color)
    }

    /**
     * Pitch factor: 1.0 is normal speed (0%).
     * Returns from (1.0 - pitchRange) to (1.0 + pitchRange).
     */
    fun getPitchFactor(): Float {
        val delta = (progress - 500) / 500.0f
        return 1.0f + (delta * pitchRange)
    }

    fun getPitchPercentString(): String {
        val pct = ((progress - 500) / 500.0f) * (pitchRange * 100)
        return if (pct > 0) String.format("+%.1f%%", pct) else String.format("%.1f%%", pct)
    }

    override fun drawProgressTrack(canvas: Canvas) {
        // Pitch seekbar has no colored progress fill bar to match Figma UI
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // No overlay percentage text painted over seekbar to match Figma UI
    }
}
