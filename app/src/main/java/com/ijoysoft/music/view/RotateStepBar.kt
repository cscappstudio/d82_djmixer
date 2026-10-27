package com.ijoysoft.music.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.audioeffect.djmixer.music.mixer.R
import kotlin.math.acos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

class RotateStepBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    interface OnRotateListener {
        fun onProgressChanged(progress: Int, fromUser: Boolean)
    }

    private var onRotateListener: OnRotateListener? = null

    private var maxVal: Int = 100
    private var step: Int = 5
    private var progressVal: Int = 0
    private var adsorptionProgress: Int = -1
    private var adsorptionRadius: Int = 0
    private var degreesRange: Int = 240
    private var graduationStyle: Int = 1
    private var graduationCount: Int = 9
    private var extraGraduationSize: Int = 0
    private var plugPadding: Int = 4
    private var progressWidth: Int = 6

    private var backgroundColorVal: Int = -0x1000000
    private var progressColorVal: Int = -0xffff01
    private var disableColorVal: Int = -0x666667

    private var indicatorDrawable: Drawable? = null
    private var indicatorOverlayDrawable: Drawable? = null
    private var plugDrawable: Drawable? = null
    private var mainGraduationDrawable: Drawable? = null
    private var extraGraduationDrawable: Drawable? = null

    private val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val arcRect = RectF()
    private val knobRect = Rect()
    private val graduationRect = Rect()

    private var lastX: Float = 0f
    private var lastY: Float = 0f
    private var isKnobPressed: Boolean = false

    private val pointF1 = PointF()
    private val pointF2 = PointF()

    var progress: Int
        get() = progressVal
        set(value) {
            val rounded = (value.toFloat() / step).roundToInt() * step
            if (rounded != progressVal) {
                progressVal = rounded.coerceIn(0, maxVal)
                postInvalidate()
            }
        }

    var maxProgress: Int
        get() = maxVal
        set(value) {
            if (value > 0) {
                maxVal = value
                postInvalidate()
            }
        }

    var max: Int
        get() = maxVal
        set(value) {
            if (value > 0) {
                maxVal = value
                postInvalidate()
            }
        }

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.RotateStepBar, defStyleAttr, 0)
        indicatorDrawable = a.getDrawable(R.styleable.RotateStepBar_rotateStepIndicatorDrawable)
        indicatorOverlayDrawable = a.getDrawable(R.styleable.RotateStepBar_rotateStepIndicatorOverlayDrawable)
        plugDrawable = a.getDrawable(R.styleable.RotateStepBar_rotateStepPlugDrawable)
        graduationStyle = a.getInt(R.styleable.RotateStepBar_rotateStepGraduationStyle, 1)
        mainGraduationDrawable = a.getDrawable(R.styleable.RotateStepBar_rotateStepMainGraduationDrawable)
        extraGraduationDrawable = a.getDrawable(R.styleable.RotateStepBar_rotateStepExtraGraduationDrawable)
        graduationCount = a.getInt(R.styleable.RotateStepBar_rotateStepGraduationCount, 9)
        extraGraduationSize = a.getDimensionPixelSize(R.styleable.RotateStepBar_rotateStepExtraGraduationSize, 0)
        disableColorVal = a.getColor(R.styleable.RotateStepBar_rotateStepDisableColor, -0x666667)
        backgroundColorVal = a.getColor(R.styleable.RotateStepBar_rotateStepBackgroundColor, -0x1000000)
        progressColorVal = a.getColor(R.styleable.RotateStepBar_rotateStepProgressColor, -0xffff01)
        progressWidth = a.getDimensionPixelSize(R.styleable.RotateStepBar_rotateStepProgressWidth, 6)
        progressVal = a.getInt(R.styleable.RotateStepBar_rotateStepProgress, 0)
        adsorptionProgress = a.getInt(R.styleable.RotateStepBar_rotateStepAdsorptionProgress, -1)
        adsorptionRadius = a.getInt(R.styleable.RotateStepBar_rotateStepAdsorptionRadius, 0)
        maxVal = a.getInt(R.styleable.RotateStepBar_rotateStepMax, 1000)
        step = a.getInt(R.styleable.RotateStepBar_rotateStep, 1)
        degreesRange = a.getInt(R.styleable.RotateStepBar_rotateStepDegreesRange, 240)
        plugPadding = a.getDimensionPixelSize(R.styleable.RotateStepBar_rotateStepPlugPadding, 4)
        a.recycle()

        arcPaint.strokeWidth = progressWidth.toFloat()
    }

    fun setOnRotateListener(listener: OnRotateListener?) {
        this.onRotateListener = listener
    }

    fun setCircleBackgroundColor(color: Int) {
        backgroundColorVal = color
        postInvalidate()
    }

    fun setCircleProgressColor(color: Int) {
        progressColorVal = color
        postInvalidate()
    }

    fun setCircleDisableColor(color: Int) {
        disableColorVal = color
        postInvalidate()
    }

    fun setIndicatorDrawable(drawable: Drawable?) {
        indicatorDrawable = drawable
        postInvalidate()
    }

    fun setIndicatorOverlayDrawable(drawable: Drawable?) {
        indicatorOverlayDrawable = drawable
        postInvalidate()
    }

    fun setPlugDrawable(drawable: Drawable?) {
        plugDrawable = drawable
        postInvalidate()
    }

    fun setMainGraduationDrawable(drawable: Drawable?) {
        mainGraduationDrawable = drawable
        postInvalidate()
    }

    fun setExtraGraduationDrawable(drawable: Drawable?) {
        extraGraduationDrawable = drawable
        postInvalidate()
    }

    fun getProgressF(): Float = progressVal.toFloat() / maxVal

    private fun getGraduationSize(): Int {
        if (extraGraduationSize != 0) return extraGraduationSize
        return (mainGraduationDrawable ?: extraGraduationDrawable)?.intrinsicWidth ?: 0
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var wSpec = widthMeasureSpec
        var hSpec = heightMeasureSpec
        val wMode = MeasureSpec.getMode(widthMeasureSpec)
        if (wMode == MeasureSpec.AT_MOST) {
            val plugW = plugDrawable?.intrinsicWidth ?: indicatorDrawable?.intrinsicWidth ?: indicatorOverlayDrawable?.intrinsicWidth ?: 0
            var pW = paddingLeft + paddingRight + (getGraduationSize() + plugPadding) * 2 + plugW
            val sizeW = MeasureSpec.getSize(widthMeasureSpec)
            if (sizeW > 0 && pW > sizeW) pW = sizeW
            wSpec = MeasureSpec.makeMeasureSpec(pW, MeasureSpec.EXACTLY)
        }
        val hMode = MeasureSpec.getMode(heightMeasureSpec)
        if (hMode == MeasureSpec.AT_MOST) {
            val plugH = plugDrawable?.intrinsicHeight ?: indicatorDrawable?.intrinsicHeight ?: indicatorOverlayDrawable?.intrinsicHeight ?: 0
            var pH = paddingTop + paddingBottom + getGraduationSize() + plugPadding + plugH
            val sizeH = MeasureSpec.getSize(heightMeasureSpec)
            if (sizeH > 0 && pH > sizeH) pH = sizeH
            hSpec = MeasureSpec.makeMeasureSpec(pH, MeasureSpec.EXACTLY)
        }
        super.onMeasure(wSpec, hSpec)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (graduationStyle != 0) {
            val paddingH = (w - paddingLeft) - paddingRight
            val innerSize = min(paddingH - ((progressWidth + plugPadding) * 2), ((h - paddingTop) - paddingBottom) - (progressWidth + plugPadding))
            knobRect.set(0, 0, innerSize, innerSize)
            knobRect.offsetTo((w / 2) - (innerSize / 2), (((((h - paddingTop) - paddingBottom) - (progressWidth + plugPadding)) - innerSize) / 2) + paddingTop + progressWidth + plugPadding)
            arcRect.set(knobRect)
            val inset = -((progressWidth / 2f) + plugPadding)
            arcRect.inset(inset, inset)
            return
        }
        val gradSize = if (extraGraduationSize > 0) extraGraduationSize else getGraduationSize()
        graduationRect.set(0, 0, gradSize, gradSize)
        val innerSize2 = min(((w - paddingLeft) - paddingRight) - ((graduationRect.width() + plugPadding) * 2), ((h - paddingTop) - paddingBottom) - (graduationRect.width() + plugPadding))
        knobRect.set(0, 0, innerSize2, innerSize2)
        knobRect.offsetTo((w / 2) - (innerSize2 / 2), graduationRect.width() + (((((h - paddingTop) - paddingBottom) - (graduationRect.width() + plugPadding)) - innerSize2) / 2) + paddingTop + plugPadding)
        graduationRect.offsetTo((knobRect.left - plugPadding) - graduationRect.width(), knobRect.centerY() - (graduationRect.height() / 2))
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        plugDrawable?.let {
            it.bounds = knobRect
            it.draw(canvas)
        }

        val deg = degreesRange.toFloat()
        if (graduationStyle == 0) {
            val cx = knobRect.centerX().toFloat()
            val cy = knobRect.centerY().toFloat()
            val startOff = (180f - deg) / 2f
            val count = graduationCount
            val segCount = count - 1
            val stepDeg = deg / segCount
            val curDeg = ((progressVal * deg) / maxVal) + startOff
            for (i in 0 until count) {
                val d = if (i == 0 || i == segCount || (count % 2 == 1 && i == count / 2)) {
                    mainGraduationDrawable ?: extraGraduationDrawable
                } else {
                    extraGraduationDrawable ?: mainGraduationDrawable
                }
                d?.let {
                    val angle = (i * stepDeg) + startOff
                    canvas.save()
                    canvas.rotate(angle, cx, cy)
                    if (progressVal == 0 || angle > curDeg) {
                        it.state = intArrayOf(android.R.attr.state_enabled)
                    } else if (isEnabled) {
                        it.state = intArrayOf(android.R.attr.state_selected, android.R.attr.state_enabled)
                    } else {
                        it.state = intArrayOf(-android.R.attr.state_enabled)
                    }
                    it.bounds = graduationRect
                    it.current.draw(canvas)
                    canvas.restore()
                }
            }
        } else {
            arcPaint.strokeWidth = progressWidth.toFloat()
            if (graduationStyle == 2) {
                val ratio = progressVal.toFloat() / maxVal
                arcPaint.color = backgroundColorVal
                canvas.drawArc(arcRect, -((deg / 2f) + 90f), deg, false, arcPaint)
                arcPaint.color = if (isEnabled) progressColorVal else disableColorVal
                var start = -90f
                val sweep: Float
                if (ratio < 0.5f) {
                    sweep = (0.5f - ratio) * deg
                    start = -90f - sweep
                } else {
                    sweep = (ratio - 0.5f) * deg
                }
                canvas.drawArc(arcRect, start, sweep, false, arcPaint)
            } else if (graduationStyle == 1) {
                val progressDeg = (progressVal * deg) / maxVal
                val startAngle = -((deg / 2f) + 90f)
                arcPaint.color = backgroundColorVal
                canvas.drawArc(arcRect, startAngle, deg, false, arcPaint)
                arcPaint.color = if (isEnabled) progressColorVal else disableColorVal
                canvas.drawArc(arcRect, startAngle, progressDeg, false, arcPaint)
            }
        }

        // Draw rotated knob
        val progressDeg = (progressVal * deg) / maxVal
        val cx = knobRect.centerX().toFloat()
        val cy = knobRect.centerY().toFloat()
        canvas.save()
        canvas.rotate(progressDeg - (deg / 2f), cx, cy)

        val stateArr = if (isEnabled) intArrayOf(android.R.attr.state_pressed, android.R.attr.state_enabled) else intArrayOf(-android.R.attr.state_enabled)
        indicatorDrawable?.let {
            it.state = stateArr
            it.bounds = knobRect
            it.current.draw(canvas)
        }
        indicatorOverlayDrawable?.let {
            it.state = stateArr
            it.bounds = knobRect
            it.current.draw(canvas)
        }
        canvas.restore()
    }

    private fun trackTouch(x: Float, y: Float): Boolean {
        val cx = knobRect.centerX().toFloat()
        val cy = knobRect.centerY().toFloat()
        val dx1 = lastX - cx
        val dx2 = x - cx
        val dy1 = lastY - cy
        val dy2 = y - cy
        val dist1 = sqrt((dy1 * dy1 + dx1 * dx1).toDouble()).toFloat()
        val dist2 = sqrt((dy2 * dy2 + dx2 * dx2).toDouble()).toFloat()
        val denom = dist1 * dist2
        val deltaDeg: Float = if (denom == 0f) {
            -180f
        } else {
            val cosVal = ((dy1 * dy2 + dx1 * dx2) / denom).coerceIn(-1.0f, 1.0f)
            val deg = Math.toDegrees(acos(cosVal.toDouble())).toFloat()
            pointF1.set(dx1, dy1)
            pointF2.set(dx2, dy2)
            if ((pointF1.x * pointF2.y) - (pointF1.y * pointF2.x) < 0f) -deg else deg
        }
        val targetProgress = (progressVal + (deltaDeg / degreesRange) * maxVal).coerceIn(0f, maxVal.toFloat())
        var newProgress = (targetProgress / step).roundToInt() * step
        if (adsorptionProgress >= 0) {
            val diff = newProgress - adsorptionProgress
            if (diff >= -adsorptionRadius && diff <= adsorptionRadius) {
                newProgress = adsorptionProgress
            }
        }
        if (newProgress == progressVal) {
            return false
        }
        progressVal = newProgress
        return true
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled) return false
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                lastX = event.x
                lastY = event.y
                isKnobPressed = true
                postInvalidate()
                onRotateListener?.onProgressChanged(progressVal, true)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (trackTouch(event.x, event.y)) {
                    lastX = event.x
                    lastY = event.y
                    postInvalidate()
                    onRotateListener?.onProgressChanged(progressVal, true)
                }
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                trackTouch(event.x, event.y)
                isKnobPressed = false
                parent?.requestDisallowInterceptTouchEvent(false)
                postInvalidate()
                return true
            }
        }
        return true
    }
}
