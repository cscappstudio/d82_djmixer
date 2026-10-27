package com.ijoysoft.mix.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.core.content.ContextCompat
import com.audioeffect.djmixer.music.mixer.R

class CrossFadeSeekBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : CustomSeekBar(context, attrs, defStyleAttr) {

    init {
        isVertical = false
        symmetryMode = true
        maxProgress = 1000
        progress = 500
        adsorptionProgress = 500
        adsorptionRadius = 40
        progressColorLeft = ContextCompat.getColor(context, R.color.theme_color_left)
        progressColorRight = ContextCompat.getColor(context, R.color.theme_color_right)
        progressBgColor = ContextCompat.getColor(context, R.color.cross_seekbar_progress_bg_color)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        val density = resources.displayMetrics.density
        val padL = paddingLeft.toFloat()
        val padT = paddingTop.toFloat()
        val padR = paddingRight.toFloat()
        val padB = paddingBottom.toFloat()

        val availW = (w - padL - padR).coerceAtLeast(1f)
        val availH = (h - padT - padB).coerceAtLeast(1f)
        val cy = padT + availH / 2f

        // Track height is progressThickness (5dp)
        val trackH = progressThickness
        val trackW = availW
        val tx = padL
        val ty = cy - trackH / 2f
        trackRect.set(tx, ty, tx + trackW, ty + trackH)

        // 7 intervals between 8 major ticks.
        // Thumb width matches exactly 1 interval: trackW / 7f
        thumbWidth = trackW / 7f
        // Thumb aspect ratio in reference image is 85/58 = 1.4655
        thumbHeight = thumbWidth * (85f / 58f)

        // Range of thumb center movement: from (trackRect.left + thumbWidth / 2) to (trackRect.right - thumbWidth / 2)
        // When progress=0, thumb left edge = trackRect.left
        // When progress=500 (center), thumb sits exactly between major tick 3 and major tick 4
        // When progress=1000, thumb right edge = trackRect.right
        rangeRect.set(
            trackRect.left + thumbWidth / 2f,
            trackRect.top,
            trackRect.right - thumbWidth / 2f,
            trackRect.bottom
        )
    }

    override fun drawScaleLines(canvas: Canvas) {
        val density = resources.displayMetrics.density
        val tickGap = 2.5f * density
        val majorLen = 9.5f * density
        val minorLen = 5.0f * density
        val majorStroke = 1.4f * density
        val minorStroke = 1.0f * density

        val majorColor = ContextCompat.getColor(
            context,
            if (isPressed) R.color.cross_seekbar_scale_line_color_pressed else R.color.cross_seekbar_scale_line_color
        )
        val minorColor = ContextCompat.getColor(
            context,
            if (isPressed) R.color.cross_seekbar_scale_line_color_short_pressed else R.color.cross_seekbar_scale_line_color_short
        )

        scalePaint.strokeCap = Paint.Cap.ROUND
        val totalTicks = 36 // 8 major ticks (7 intervals) * 5 sub-steps + 1
        val step = trackRect.width() / 35f

        for (i in 0 until totalTicks) {
            val x = trackRect.left + i * step
            val isMajor = (i % 5 == 0)
            val tickLen = if (isMajor) majorLen else minorLen

            scalePaint.color = if (isMajor) majorColor else minorColor
            scalePaint.strokeWidth = if (isMajor) majorStroke else minorStroke

            // Top tick
            canvas.drawLine(x, trackRect.top - tickGap - tickLen, x, trackRect.top - tickGap, scalePaint)
            // Bottom tick
            canvas.drawLine(x, trackRect.bottom + tickGap, x, trackRect.bottom + tickGap + tickLen, scalePaint)
        }
    }

    override fun drawProgressTrack(canvas: Canvas) {
        val r = trackRect.height() / 2f

        // 1. Recessed dark slot background
        trackPaint.color = progressBgColor
        canvas.drawRoundRect(trackRect, r, r, trackPaint)

        // 2. Dual-color split track
        val ratio = if (maxProgress > 0) progress.toFloat() / maxProgress else 0.5f
        val thumbCenterX = rangeRect.left + rangeRect.width() * ratio

        // Left half (Deck A - Cyan)
        val save1 = canvas.save()
        canvas.clipRect(trackRect.left, trackRect.top, thumbCenterX, trackRect.bottom)
        trackPaint.color = progressColorLeft
        canvas.drawRoundRect(trackRect, r, r, trackPaint)
        canvas.restoreToCount(save1)

        // Right half (Deck B - Purple)
        val save2 = canvas.save()
        canvas.clipRect(thumbCenterX, trackRect.top, trackRect.right, trackRect.bottom)
        trackPaint.color = progressColorRight
        canvas.drawRoundRect(trackRect, r, r, trackPaint)
        canvas.restoreToCount(save2)
    }

    override fun drawThumb(canvas: Canvas) {
        val ratio = if (maxProgress > 0) progress.toFloat() / maxProgress else 0.5f
        val tx = rangeRect.left + rangeRect.width() * ratio
        val ty = trackRect.centerY()

        thumbRect.set(
            (tx - thumbWidth / 2f).toInt(),
            (ty - thumbHeight / 2f).toInt(),
            (tx + thumbWidth / 2f).toInt(),
            (ty + thumbHeight / 2f).toInt()
        )

        thumbDrawable?.let {
            it.bounds = thumbRect
            it.state = drawableState
            it.draw(canvas)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> isPressed = true
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> isPressed = false
        }
        return super.onTouchEvent(event)
    }

    /**
     * Get relative gains for Left (Deck A) and Right (Deck B).
     * At 0 (all the way left): Left=1.0, Right=0.0
     * At 500 (center): Left=1.0, Right=1.0 (or 0.707 for equal power)
     * At 1000 (all the way right): Left=0.0, Right=1.0
     */
    fun getLeftGain(): Float {
        val ratio = progress.toFloat() / maxProgress
        return if (ratio <= 0.5f) 1.0f else (1.0f - ratio) / 0.5f
    }

    fun getRightGain(): Float {
        val ratio = progress.toFloat() / maxProgress
        return if (ratio >= 0.5f) 1.0f else ratio / 0.5f
    }
}
