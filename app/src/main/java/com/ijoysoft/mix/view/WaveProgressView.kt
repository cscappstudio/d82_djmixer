package com.ijoysoft.mix.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.audioeffect.djmixer.music.mixer.R

class WaveProgressView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    interface OnSeekListener {
        fun onSeek(progressRatio: Float)
    }

    var seekListener: OnSeekListener? = null
    var hasAudio: Boolean = false
        set(value) {
            field = value
            invalidate()
        }

    var progress: Float = 0f
        set(value) {
            field = value.coerceIn(0f, 1f)
            if (hasAudio) invalidate()
        }

    var cuePosition: Float = -1f // 0f to 1f, or -1f if none
        set(value) {
            field = value
            if (hasAudio) invalidate()
        }

    var hotCuePositions: FloatArray? = null
        set(value) {
            field = value
            if (hasAudio) invalidate()
        }

    private var waveColor: Int = Color.parseColor("#00d7ff")
    private val unplayedColor: Int = Color.parseColor("#37474F")
    private val cueColor: Int = Color.parseColor("#FF5252")

    private val wavePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = 1.5f
    }
    private val cuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = cueColor
        strokeWidth = 2f
        style = Paint.Style.FILL_AND_STROKE
    }
    private val playheadPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        strokeWidth = 2f
        style = Paint.Style.STROKE
    }
    private val hotCueTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    private val hotCueBadgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val hotCueLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = 1.5f
    }
    private val cuePath = Path()
    private val boundsRect = RectF()

    private var sampleData: FloatArray? = null
    private var linePoints: FloatArray? = null

    init {
        if (attrs != null) {
            val a = context.obtainStyledAttributes(attrs, R.styleable.WaveProgressView)
            waveColor = a.getColor(R.styleable.WaveProgressView_waveWaveLineColor, waveColor)
            a.recycle()
        }
    }

    fun setWaveColor(color: Int) {
        waveColor = color
        invalidate()
    }

    fun setSampleData(samples: FloatArray) {
        sampleData = samples
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        boundsRect.set(paddingLeft.toFloat(), paddingTop.toFloat(), (w - paddingRight).toFloat(), (h - paddingBottom).toFloat())
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!hasAudio || boundsRect.isEmpty) return

        val width = boundsRect.width()
        val height = boundsRect.height()
        val centerY = boundsRect.centerY()
        val stepCount = width.toInt()
        if (stepCount <= 0) return

        val playheadX = boundsRect.left + (width * progress)

        val samples = sampleData
        val sampleLen = samples?.size ?: 0

        // Draw waveform spikes across the full width
        for (i in 0 until stepCount) {
            val x = boundsRect.left + i
            val amp = if (samples != null && sampleLen > 0) {
                val idx = (i * sampleLen / stepCount).coerceIn(0, sampleLen - 1)
                samples[idx].coerceIn(0.05f, 1.0f)
            } else {
                // High-fidelity musical fallback profile with rhythmic variation
                val phase = i * 0.15f
                val base = (Math.sin(phase * 0.4) * 0.35 + Math.cos(phase * 1.1) * 0.25 + 0.5).toFloat()
                val pulse = if (i % 24 < 3) 0.3f else 0f
                (base + pulse).coerceIn(0.1f, 0.95f)
            }

            val barHalfHeight = (height * 0.48f) * amp
            val top = centerY - barHalfHeight
            val bottom = centerY + barHalfHeight

            // Played area is brightly colored, unplayed is darker
            wavePaint.color = if (x <= playheadX) waveColor else unplayedColor
            canvas.drawLine(x, top, x, bottom, wavePaint)
        }

        val dp = resources.displayMetrics.density

        // 1. Draw Hot Cue Markers (Letter badges A..F)
        val cues = hotCuePositions
        if (cues != null) {
            val cueLabels = arrayOf("A", "B", "C", "D", "E", "F")
            val cueColors = intArrayOf(
                Color.parseColor("#00E5FF"), // Cyan for A
                Color.parseColor("#B388FF"), // Purple for B
                Color.parseColor("#00E676"), // Green for C
                Color.parseColor("#FFD600"), // Yellow for D
                Color.parseColor("#FF9100"), // Orange for E
                Color.parseColor("#FF1744")  // Red for F
            )
            val badgeW = 9f * dp
            val badgeH = 11f * dp
            hotCueTextPaint.textSize = 7.5f * dp
            hotCueLinePaint.strokeWidth = 1.5f * dp

            for (idx in cues.indices) {
                val pos = cues[idx]
                if (pos in 0f..1f) {
                    val cx = boundsRect.left + (width * pos)
                    val col = cueColors[idx % cueColors.size]
                    hotCueBadgePaint.color = col
                    hotCueLinePaint.color = col

                    // Draw thin vertical line across waveform
                    canvas.drawLine(cx, boundsRect.top, cx, boundsRect.bottom, hotCueLinePaint)

                    // Draw small marker badge at top
                    val rect = RectF(cx - badgeW / 2, boundsRect.top, cx + badgeW / 2, boundsRect.top + badgeH)
                    canvas.drawRoundRect(rect, 2f * dp, 2f * dp, hotCueBadgePaint)

                    // Draw cue letter
                    val textY = boundsRect.top + badgeH * 0.78f
                    canvas.drawText(cueLabels[idx % cueLabels.size], cx, textY, hotCueTextPaint)
                }
            }
        }

        // 2. Draw Temp Cue Marker (SET cue: red triangle flag + line)
        if (cuePosition in 0f..1f) {
            val cueX = boundsRect.left + (width * cuePosition)
            val triWidth = 9f * dp
            val triHeight = 8f * dp
            cuePaint.color = cueColor
            cuePaint.strokeWidth = 2f * dp

            cuePath.reset()
            cuePath.moveTo(cueX - triWidth / 2, boundsRect.top)
            cuePath.lineTo(cueX + triWidth / 2, boundsRect.top)
            cuePath.lineTo(cueX, boundsRect.top + triHeight)
            cuePath.close()
            canvas.drawPath(cuePath, cuePaint)

            canvas.drawLine(cueX, boundsRect.top + triHeight, cueX, boundsRect.bottom, cuePaint)
        }

        // 3. Draw Playhead Marker (Crisp white vertical cursor line)
        playheadPaint.strokeWidth = 2f * dp
        canvas.drawLine(playheadX, boundsRect.top, playheadX, boundsRect.bottom, playheadPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!hasAudio) return false
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                val ratio = ((event.x - boundsRect.left) / boundsRect.width()).coerceIn(0f, 1f)
                progress = ratio
                seekListener?.onSeek(ratio)
                return true
            }
        }
        return super.onTouchEvent(event)
    }
}
