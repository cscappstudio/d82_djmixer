package com.ijoysoft.mix.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.audioeffect.djmixer.music.mixer.R

class WaveView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    interface OnWaveDragListener {
        fun onScrub(deltaX: Float)
    }

    var dragListener: OnWaveDragListener? = null
    var hasAudio: Boolean = false
        set(value) {
            field = value
            invalidate()
        }

    var wavTopDirection: Boolean = true // true = Deck A (peaks UP), false = Deck B (peaks DOWN)
    var playOffset: Float = 0f
        set(value) {
            field = value
            if (hasAudio) invalidate()
        }

    private var waveColor: Int = Color.parseColor("#00d7ff")
    private val wavePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = 1.5f
    }
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = 1.5f
    }
    private val centerMarkerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#ffff0000")
        strokeWidth = 2f
    }

    private var lastTouchX = 0f
    private val boundsRect = RectF()
    private var sampleData: FloatArray? = null

    init {
        if (attrs != null) {
            val a = context.obtainStyledAttributes(attrs, R.styleable.WaveView)
            wavTopDirection = a.getBoolean(R.styleable.WaveView_wavTopDirection, true)
            waveColor = a.getColor(R.styleable.WaveView_wavWaveLineColor, if (wavTopDirection) Color.parseColor("#00d7ff") else Color.parseColor("#ffd200"))
            wavePaint.color = waveColor
            a.recycle()
        }
    }

    fun setWaveColor(color: Int) {
        waveColor = color
        wavePaint.color = color
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
        val cx = boundsRect.centerX()
        val baseline = if (wavTopDirection) boundsRect.bottom - 1f else boundsRect.top + 1f

        val spacing = 2f // Dense vertical lines for continuous waveform
        val stepCount = (width / spacing).toInt()
        val samples = sampleData
        val sampleLen = samples?.size ?: 0

        // 1. Draw Waveform Peaks
        for (i in -stepCount / 2..stepCount / 2) {
            val x = cx + i * spacing - (playOffset % spacing)
            if (x < boundsRect.left || x > boundsRect.right) continue

            val globalIndex = (((x - boundsRect.left) + playOffset) / 2.5f).toInt()
            val amp = if (samples != null && sampleLen > 0) {
                val idx = Math.abs(globalIndex % sampleLen)
                samples[idx].coerceIn(0.08f, 1.0f)
            } else {
                // High-fidelity audio envelope with kick/snare pulse and musical dynamics
                val p = globalIndex * 0.12f
                val env = (Math.sin(p * 0.5) * 0.35 + Math.cos(p * 1.3) * 0.25 + 0.45).toFloat()
                val beatPulse = if (globalIndex % 32 < 4) 0.35f else 0.05f
                (env + beatPulse).coerceIn(0.1f, 0.95f)
            }

            val barHeight = height * 0.85f * amp
            val peakY = if (wavTopDirection) baseline - barHeight else baseline + barHeight
            canvas.drawLine(x, baseline, x, peakY, wavePaint)
        }

        // 2. Draw Beat Grid Marks (White vertical lines)
        val beatInterval = 64f * resources.displayMetrics.density // ~48-64dp per beat
        val firstBeatOffset = -(playOffset % beatInterval)
        var beatX = cx + firstBeatOffset - beatInterval * 10

        var beatIndex = ((cx + firstBeatOffset - playOffset) / beatInterval).toInt()
        while (beatX <= boundsRect.right + beatInterval) {
            if (beatX in boundsRect.left..boundsRect.right) {
                val isMeasureStart = (beatIndex % 4 == 0)
                val gridHeight = if (isMeasureStart) height * 0.75f else height * 0.45f
                gridPaint.color = if (isMeasureStart) Color.parseColor("#E6FFFFFF") else Color.parseColor("#80FFFFFF")
                val gridY = if (wavTopDirection) baseline - gridHeight else baseline + gridHeight
                canvas.drawLine(beatX, baseline, beatX, gridY, gridPaint)
            }
            beatX += beatInterval
            beatIndex++
        }

        // 3. Draw Center Red Marker Line (dead center vertical line)
        canvas.drawLine(cx, boundsRect.top, cx, boundsRect.bottom, centerMarkerPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!hasAudio) return false
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                lastTouchX = event.x
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - lastTouchX
                lastTouchX = event.x
                playOffset -= dx
                dragListener?.onScrub(dx)
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }
}
