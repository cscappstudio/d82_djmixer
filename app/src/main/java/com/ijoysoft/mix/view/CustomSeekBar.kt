package com.ijoysoft.mix.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.audioeffect.djmixer.music.mixer.R
import kotlin.math.abs

open class CustomSeekBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    interface OnSeekBarChangeListener {
        fun onProgressChanged(seekBar: CustomSeekBar, progress: Int, fromUser: Boolean)
    }

    var isVertical: Boolean = false
    var symmetryMode: Boolean = false
    var allowClick: Boolean = true

    var maxProgress: Int = 1000
    var progress: Int = 500
        set(value) {
            val clamped = value.coerceIn(0, maxProgress)
            if (field != clamped) {
                field = clamped
                invalidate()
                listener?.onProgressChanged(this, field, false)
            }
        }

    var thumbDrawable: Drawable? = null
    var progressBgColor: Int = Color.parseColor("#1F2228")
    var progressColor: Int = Color.parseColor("#448AFF")
        set(value) {
            field = value
            invalidate()
        }
    var progressColorLeft: Int = Color.parseColor("#00F0FF")
    var progressColorRight: Int = Color.parseColor("#FFD600")

    var scaleNum: Int = 7
    var scaleLineColor: Int = Color.parseColor("#373A40")
    var scaleLineShortColor: Int = Color.parseColor("#26292E")

    var adsorptionProgress: Int = 500
    var adsorptionRadius: Int = 30
    var thumbEdgeAlign: Boolean = true
    var thumbEdgeShadowRatio: Float = 0f
    var progressThickness: Float = 6f * resources.displayMetrics.density

    var listener: OnSeekBarChangeListener? = null

    protected val trackRect = RectF()
    protected val rangeRect = RectF()
    protected val progressRect = RectF()
    protected val thumbRect = Rect()
    protected val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    protected val scalePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    protected val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 24f
        textAlign = Paint.Align.CENTER
    }

    private var isDragging = false
    protected var thumbWidth: Float = 0f
    protected var thumbHeight: Float = 0f

    init {
        val density = resources.displayMetrics.density
        progressThickness = 6f * density

        if (attrs != null) {
            val a = context.obtainStyledAttributes(attrs, R.styleable.CustomSeekBar)
            isVertical = a.getBoolean(R.styleable.CustomSeekBar_seekVerticalMode, isVertical)
            symmetryMode = a.getBoolean(R.styleable.CustomSeekBar_seekSymmetryMode, symmetryMode)
            allowClick = a.getBoolean(R.styleable.CustomSeekBar_seekAllowClick, allowClick)
            progressBgColor = a.getColor(R.styleable.CustomSeekBar_seekProgressBgColor, progressBgColor)
            progressColor = a.getColor(R.styleable.CustomSeekBar_seekProgressColor, progressColor)
            progressColorLeft = a.getColor(R.styleable.CustomSeekBar_seekProgressColorLeftBottom, progressColorLeft)
            progressColorRight = a.getColor(R.styleable.CustomSeekBar_seekProgressColorRightTop, progressColorRight)
            progressThickness = a.getDimension(R.styleable.CustomSeekBar_seekProgressSize, progressThickness)
            thumbEdgeAlign = a.getBoolean(R.styleable.CustomSeekBar_seekThumbEdgeAlign, thumbEdgeAlign)
            thumbEdgeShadowRatio = a.getFloat(R.styleable.CustomSeekBar_seekThumbEdgeShadowRatio, thumbEdgeShadowRatio)

            a.getDrawable(R.styleable.CustomSeekBar_seekProgressThumb)?.let {
                thumbDrawable = it
            }

            scaleNum = a.getInt(R.styleable.CustomSeekBar_seekScaleNum, scaleNum)
            scaleLineColor = a.getColor(R.styleable.CustomSeekBar_seekScaleLineColor, scaleLineColor)
            scaleLineShortColor = a.getColor(R.styleable.CustomSeekBar_seekScaleLineShortColor, scaleLineShortColor)
            adsorptionProgress = a.getInt(R.styleable.CustomSeekBar_seekAdsorptionProgress, adsorptionProgress)
            adsorptionRadius = a.getInt(R.styleable.CustomSeekBar_seekAdsorptionRadius, adsorptionRadius)
            a.recycle()
        }

        if (thumbDrawable == null) {
            thumbDrawable = ContextCompat.getDrawable(
                context,
                if (isVertical) R.drawable.seekbar_thumb_v_selector_left else R.drawable.seekbar_thumb_h_selector
            )
        }
    }

    fun setOnSeekBarChangeListener(listener: OnSeekBarChangeListener?) {
        this.listener = listener
    }

    fun setProgress(value: Int, fromUser: Boolean) {
        val clamped = value.coerceIn(0, maxProgress)
        if (progress != clamped) {
            progress = clamped
            invalidate()
            listener?.onProgressChanged(this, clamped, fromUser)
        }
    }

    fun getMax(): Int = maxProgress

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val padL = paddingLeft.toFloat()
        val padT = paddingTop.toFloat()
        val padR = paddingRight.toFloat()
        val padB = paddingBottom.toFloat()

        val availW = (w - padL - padR).coerceAtLeast(1f)
        val availH = (h - padT - padB).coerceAtLeast(1f)

        if (isVertical) {
            thumbWidth = availW
            val d = thumbDrawable
            val dH = d?.intrinsicHeight?.toFloat() ?: 1f
            val dW = d?.intrinsicWidth?.toFloat() ?: 1f
            thumbHeight = if (dW > 0f) (dH / dW) * thumbWidth else thumbWidth

            val trackW = progressThickness
            val trackH = availH
            val tx = padL + (availW - trackW) / 2f
            val ty = padT + (availH - trackH) / 2f
            trackRect.set(tx, ty, tx + trackW, ty + trackH)

            rangeRect.set(trackRect)
            if (thumbEdgeAlign) {
                rangeRect.inset(0f, thumbHeight / 2f)
            } else {
                trackRect.inset(0f, thumbHeight / 2f)
                rangeRect.set(trackRect)
            }
        } else {
            thumbHeight = availH
            val d = thumbDrawable
            val dH = d?.intrinsicHeight?.toFloat() ?: 1f
            val dW = d?.intrinsicWidth?.toFloat() ?: 1f
            thumbWidth = if (dH > 0f) (dW / dH) * thumbHeight else thumbHeight

            val trackW = availW
            val trackH = progressThickness
            val tx = padL + (availW - trackW) / 2f
            val ty = padT + (availH - trackH) / 2f
            trackRect.set(tx, ty, tx + trackW, ty + trackH)

            rangeRect.set(trackRect)
            if (thumbEdgeAlign) {
                rangeRect.inset(thumbWidth / 2f, 0f)
            } else {
                trackRect.inset(thumbWidth / 2f, 0f)
                rangeRect.set(trackRect)
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (trackRect.isEmpty) return

        // 1. Draw scale lines (ticks) behind track
        drawScaleLines(canvas)

        // 2. Draw background track
        trackPaint.color = progressBgColor
        val cornerRadius = if (isVertical) trackRect.width() / 2f else trackRect.height() / 2f
        canvas.drawRoundRect(trackRect, cornerRadius, cornerRadius, trackPaint)

        // 3. Draw active progress
        drawProgressTrack(canvas)

        // 4. Draw thumb
        drawThumb(canvas)
    }

    protected open fun drawScaleLines(canvas: Canvas) {
        if (scaleNum < 2) return

        val density = resources.displayMetrics.density
        val majorWidth = 1f * density
        val minorWidth = 1f * density
        val spacing = 2f * density
        val subCount = 2
        val totalTicks = scaleNum + (scaleNum - 1) * subCount
        val cornerRadius = if (isVertical) trackRect.width() / 2f else trackRect.height() / 2f

        if (isVertical) {
            val usableH = trackRect.height() - (cornerRadius * 2f) - majorWidth
            if (usableH <= 0) return
            val step = usableH / (totalTicks - 1)
            val startY = trackRect.top + cornerRadius + majorWidth / 2f

            val leftEdge = trackRect.left - spacing
            val leftMajor = paddingLeft.toFloat()
            val leftMinor = leftEdge - (leftEdge - leftMajor) * 0.5f

            val rightEdge = trackRect.right + spacing
            val rightMajor = (width - paddingRight).toFloat()
            val rightMinor = rightEdge + (rightMajor - rightEdge) * 0.5f

            for (i in 0 until totalTicks) {
                val y = startY + i * step
                val isMajor = (i % (subCount + 1) == 0)
                if (isMajor) {
                    scalePaint.color = scaleLineColor
                    scalePaint.strokeWidth = majorWidth
                    canvas.drawLine(leftEdge, y, leftMajor, y, scalePaint)
                    canvas.drawLine(rightEdge, y, rightMajor, y, scalePaint)
                } else {
                    scalePaint.color = scaleLineShortColor
                    scalePaint.strokeWidth = minorWidth
                    canvas.drawLine(leftEdge, y, leftMinor, y, scalePaint)
                    canvas.drawLine(rightEdge, y, rightMinor, y, scalePaint)
                }
            }
        } else {
            val usableW = trackRect.width() - (cornerRadius * 2f) - majorWidth
            if (usableW <= 0) return
            val step = usableW / (totalTicks - 1)
            val startX = trackRect.left + cornerRadius + majorWidth / 2f

            val topEdge = trackRect.top - spacing
            val topMajor = paddingTop.toFloat()
            val topMinor = topEdge - (topEdge - topMajor) * 0.5f

            val bottomEdge = trackRect.bottom + spacing
            val bottomMajor = (height - paddingBottom).toFloat()
            val bottomMinor = bottomEdge + (bottomMajor - bottomEdge) * 0.5f

            for (i in 0 until totalTicks) {
                val x = startX + i * step
                val isMajor = (i % (subCount + 1) == 0)
                if (isMajor) {
                    scalePaint.color = scaleLineColor
                    scalePaint.strokeWidth = majorWidth
                    canvas.drawLine(x, topEdge, x, topMajor, scalePaint)
                    canvas.drawLine(x, bottomEdge, x, bottomMajor, scalePaint)
                } else {
                    scalePaint.color = scaleLineShortColor
                    scalePaint.strokeWidth = minorWidth
                    canvas.drawLine(x, topEdge, x, topMinor, scalePaint)
                    canvas.drawLine(x, bottomEdge, x, bottomMinor, scalePaint)
                }
            }
        }
    }

    protected open fun drawProgressTrack(canvas: Canvas) {
        val ratio = if (maxProgress > 0) progress.toFloat() / maxProgress else 0f
        if (isVertical) {
            val top = rangeRect.bottom - (rangeRect.height() * ratio)
            progressRect.set(trackRect.left, top, trackRect.right, trackRect.bottom)
            trackPaint.color = progressColor
            val radius = trackRect.width() / 2f
            canvas.drawRoundRect(progressRect, radius, radius, trackPaint)
        } else {
            if (symmetryMode) {
                val cx = trackRect.centerX()
                val radius = trackRect.height() / 2f

                if (ratio < 0.5f) {
                    val factor = ratio / 0.5f
                    // Left half (CYAN) is full
                    progressRect.set(trackRect.left, trackRect.top, cx, trackRect.bottom)
                    trackPaint.color = progressColorLeft
                    canvas.drawRoundRect(progressRect, radius, radius, trackPaint)

                    // Right half (YELLOW) shrinks as slider moves left
                    val rightStart = (1.0f - factor) * (trackRect.width() / 2f) + cx
                    if (rightStart < trackRect.right) {
                        progressRect.set(rightStart, trackRect.top, trackRect.right, trackRect.bottom)
                        trackPaint.color = progressColorRight
                        canvas.drawRoundRect(progressRect, radius, radius, trackPaint)
                    }
                } else {
                    val factor = (ratio - 0.5f) / 0.5f
                    // Left half (CYAN) shrinks as slider moves right
                    val leftEnd = (1.0f - factor) * (trackRect.width() / 2f) + trackRect.left
                    if (leftEnd > trackRect.left) {
                        progressRect.set(trackRect.left, trackRect.top, leftEnd, trackRect.bottom)
                        trackPaint.color = progressColorLeft
                        canvas.drawRoundRect(progressRect, radius, radius, trackPaint)
                    }

                    // Right half (YELLOW) is full
                    progressRect.set(cx, trackRect.top, trackRect.right, trackRect.bottom)
                    trackPaint.color = progressColorRight
                    canvas.drawRoundRect(progressRect, radius, radius, trackPaint)
                }
            } else {
                val right = trackRect.left + (trackRect.width() * ratio)
                progressRect.set(trackRect.left, trackRect.top, right, trackRect.bottom)
                trackPaint.color = progressColor
                val radius = trackRect.height() / 2f
                canvas.drawRoundRect(progressRect, radius, radius, trackPaint)
            }
        }
    }

    protected open fun drawThumb(canvas: Canvas) {
        val ratio = if (maxProgress > 0) progress.toFloat() / maxProgress else 0f
        if (isVertical) {
            val ty = rangeRect.bottom - rangeRect.height() * ratio
            val tx = rangeRect.centerX()
            thumbRect.set(
                (tx - thumbWidth / 2f).toInt(),
                (ty - thumbHeight / 2f).toInt(),
                (tx + thumbWidth / 2f).toInt(),
                (ty + thumbHeight / 2f).toInt()
            )
        } else {
            val tx = rangeRect.left + rangeRect.width() * ratio
            val ty = rangeRect.centerY()
            thumbRect.set(
                (tx - thumbWidth / 2f).toInt(),
                (ty - thumbHeight / 2f).toInt(),
                (tx + thumbWidth / 2f).toInt(),
                (ty + thumbHeight / 2f).toInt()
            )
        }

        thumbDrawable?.let {
            it.bounds = thumbRect
            it.state = drawableState
            it.draw(canvas)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled || !allowClick) return false
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                isDragging = true
                updateTouchProgress(event.x, event.y)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                updateTouchProgress(event.x, event.y)
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                parent?.requestDisallowInterceptTouchEvent(false)
                isDragging = false
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun updateTouchProgress(x: Float, y: Float) {
        val ratio = if (isVertical) {
            if (rangeRect.height() <= 0f) 0f
            else (rangeRect.bottom - y) / rangeRect.height()
        } else {
            if (rangeRect.width() <= 0f) 0f
            else (x - rangeRect.left) / rangeRect.width()
        }

        var newProgress = (ratio.coerceIn(0f, 1f) * maxProgress).toInt()
        if (adsorptionRadius > 0 && abs(newProgress - adsorptionProgress) <= adsorptionRadius) {
            newProgress = adsorptionProgress
        }
        setProgress(newProgress, true)
    }
}
