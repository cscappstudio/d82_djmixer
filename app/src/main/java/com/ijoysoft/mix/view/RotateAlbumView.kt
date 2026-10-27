package com.ijoysoft.mix.view

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.LinearInterpolator
import androidx.annotation.Keep
import androidx.core.content.ContextCompat
import com.audioeffect.djmixer.music.mixer.R
import kotlin.math.atan2
import kotlin.math.hypot

class RotateAlbumView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr), ValueAnimator.AnimatorUpdateListener {

    interface OnAlbumRotateListener {
        fun onScratchStart()
        fun onScratch(deltaDegrees: Float, velocity: Float)
        fun onScratchEnd()
    }

    var rotateListener: OnAlbumRotateListener? = null

    // Tonearm & disc drawables
    private var idleAlbumDrawable: Drawable? = null
    private var idleLogoDrawable: Drawable? = null
    private var defaultAlbumDrawable: Drawable? = null
    private var albumTextureDrawable: Drawable? = null
    private var albumDrawable: Drawable? = null
    private var logoDrawable: Drawable? = null
    private var indicatorDrawable: Drawable? = null

    // Turntable state
    var isRotating: Boolean = false
        set(value) {
            field = value
            updateRotationAnimation()
        }

    var isScratching: Boolean = false
        private set

    var rotateDegrees: Float = 0f
        @Keep set(value) {
            field = value % 360f
            invalidate()
        }

    var progress: Float = 0f
        set(value) {
            field = value.coerceIn(0f, 1f)
            if (!isScratching) {
                invalidate()
            }
        }

    var rotateDuration: Int = 5000
        set(value) {
            field = if (value <= 0) 5000 else value
            updateRotationAnimation()
        }

    var isDragEnable: Boolean = false
        set(value) {
            field = value
            updateRotationAnimation()
            invalidate()
        }

    var indicatorHeightFactor: Float = 0.78f
        set(value) {
            field = value
            requestLayout()
            invalidate()
        }

    var logoSizeFactor: Float = 0.60f
        set(value) {
            field = value
            requestLayout()
            invalidate()
        }

    var albumStrokeSize: Int = context.resources.getDimensionPixelSize(R.dimen.play_rotate_cd_outside_padding)

    val pivotRatio = PointF(0.5f, 62.4f / 591f) // center of circular base pivot knob (126x591)
    val needleRatio = PointF(69f / 126f, 588f / 591f)  // needle tip at the bottom
    val armPivot = PointF()                          // Pivot point in view coordinates
    val needleTip = PointF()                         // Needle tip in view coordinates

    var angleRest: Float = -10.0f       // Rest angle (off the record, parked on arm rest)
    var angleTrackStart: Float = -7.0f  // 0% progress: outer lead-in groove
    var angleTrackEnd: Float = 24.0f    // 100% progress: inner lead-out groove

    var indicatorState: Int = 0
        private set

    private val discRect = Rect()
    private val logoRect = Rect()
    private val albumRect = Rect()
    private val armRect = Rect()

    private var rotationAnimator: ObjectAnimator? = null
    private val needleAnimator: ValueAnimator = ValueAnimator().apply {
        duration = 350L
        interpolator = AccelerateDecelerateInterpolator()
        addUpdateListener(this@RotateAlbumView)
    }
    private var animatedArmAngle: Float = angleRest

    private var lastTouchAngle: Double = 0.0
    private var lastTouchTime: Long = 0L
    private var smoothedVelocity: Float = 0f

    init {
        defaultAlbumDrawable = ContextCompat.getDrawable(context, R.drawable.rotate_cd_default)
        albumTextureDrawable = ContextCompat.getDrawable(context, R.drawable.rotate_cd_texture)
        idleAlbumDrawable = ContextCompat.getDrawable(context, R.drawable.rotate_cd_idle)
        idleLogoDrawable = ContextCompat.getDrawable(context, R.drawable.rotate_cd_logo_idle)
    }

    fun setIdleAlbumResource(resId: Int) {
        idleAlbumDrawable = ContextCompat.getDrawable(context, resId)
        invalidate()
    }

    fun setIdleLogoResource(resId: Int) {
        idleLogoDrawable = ContextCompat.getDrawable(context, resId)
        invalidate()
    }

    fun setDefaultAlbumResource(resId: Int) {
        defaultAlbumDrawable = ContextCompat.getDrawable(context, resId)
        invalidate()
    }

    fun setAlbumDrawable(drawable: Drawable?) {
        albumDrawable = drawable
        invalidate()
    }

    fun setAlbumTextureResource(resId: Int) {
        albumTextureDrawable = ContextCompat.getDrawable(context, resId)
        invalidate()
    }

    fun setLogoResource(resId: Int) {
        logoDrawable = ContextCompat.getDrawable(context, resId)
        invalidate()
    }

    fun setIndicatorResource(resId: Int) {
        indicatorDrawable = ContextCompat.getDrawable(context, resId)
        requestLayout()
        invalidate()
    }

    fun setOnAlbumRotateListener(listener: OnAlbumRotateListener?) {
        this.rotateListener = listener
    }

    fun setIndicatorState(state: Int) {
        if (indicatorState == state && !needleAnimator.isRunning) return
        indicatorState = state
        if (needleAnimator.isRunning) needleAnimator.cancel()
        val currentAngle = animatedArmAngle
        val targetAngle = if (state == 1) {
            angleTrackStart + (angleTrackEnd - angleTrackStart) * progress
        } else {
            angleRest
        }
        needleAnimator.setFloatValues(currentAngle, targetAngle)
        needleAnimator.start()
    }

    fun setInterruptDrag(interrupt: Boolean) {
        // ignore drag when requested
    }

    override fun onAnimationUpdate(animation: ValueAnimator) {
        animatedArmAngle = animation.animatedValue as Float
        invalidate()
    }

    private fun updateRotationAnimation() {
        if (isRotating && !isScratching && isDragEnable) {
            if (rotationAnimator == null) {
                rotationAnimator = ObjectAnimator.ofFloat(this, "rotateDegrees", rotateDegrees, rotateDegrees + 360f).apply {
                    duration = rotateDuration.toLong()
                    interpolator = LinearInterpolator()
                    repeatCount = ValueAnimator.INFINITE
                    start()
                }
            }
        } else {
            rotationAnimator?.cancel()
            rotationAnimator = null
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val padH = (w - paddingLeft - paddingRight)
        val padV = (h - paddingTop - paddingBottom)
        val size = minOf(padH, padV)
        if (size <= 0) return

        // Vinyl disc radius is 42% of size (diameter 84%), centered with slight left offset (0.48 * size)
        // to leave comfortable margin for tonearm pivot base at top-right
        val cx = paddingLeft + (padH - size) / 2f + size * 0.50f
        val cy = paddingTop + (padV - size) / 2f + size * 0.50f
        val r = size * 0.42f

        discRect.set((cx - r).toInt(), (cy - r).toInt(), (cx + r).toInt(), (cy + r).toInt())
        logoRect.set(discRect)
        val inset = (((1.0f - logoSizeFactor) * logoRect.width()) / 2f).toInt()
        if (inset > 0) {
            logoRect.inset(inset, inset)
        }

        albumRect.set(discRect)
        albumRect.inset(albumStrokeSize, albumStrokeSize)

        indicatorDrawable?.let { arm ->
            val armH = (discRect.height() * indicatorHeightFactor).toInt()
            val armW = if (arm.intrinsicHeight > 0) {
                (armH.toFloat() / arm.intrinsicHeight * arm.intrinsicWidth).toInt()
            } else (armH * (126f / 591f)).toInt()

            val px = cx + 0.673f * r
            val py = cy - 0.916f * r

            armPivot.set(px, py)
            val left = (px - armW * pivotRatio.x).toInt()
            val top = (py - armH * pivotRatio.y).toInt()
            armRect.set(left, top, left + armW, top + armH)

            needleTip.set(armRect.left + armRect.width() * needleRatio.x, armRect.top + armRect.height() * needleRatio.y)
        }
    }

    var deckThemeColor: Int = 0
        set(value) {
            field = value
            invalidate()
        }

    private val neonRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val neonGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (discRect.isEmpty) return

        val cx = discRect.centerX().toFloat()
        val cy = discRect.centerY().toFloat()

        // 1. Draw Rotating Vinyl / Platter
        canvas.save()
        canvas.rotate(rotateDegrees, cx, cy)

        if (isDragEnable) {
            // Track loaded: realistic vinyl platter
            idleAlbumDrawable?.let {
                it.bounds = discRect
                it.draw(canvas)
            }
            logoDrawable?.let {
                it.bounds = logoRect
                it.draw(canvas)
            }
        } else {
            // Idle state: no track loaded
            idleAlbumDrawable?.let {
                it.bounds = discRect
                it.draw(canvas)
            }
            idleLogoDrawable?.let {
                it.bounds = logoRect
                it.draw(canvas)
            }
        }
        canvas.restore()

        // 1.5. Outer Neon Glow Ring when track is loaded
        if (isDragEnable && deckThemeColor != 0) {
            val r = discRect.width() / 2f
            val dp = resources.displayMetrics.density

            // Soft outer glow
            neonGlowPaint.style = Paint.Style.STROKE
            neonGlowPaint.color = androidx.core.graphics.ColorUtils.setAlphaComponent(deckThemeColor, 75)
            neonGlowPaint.strokeWidth = 10f * dp
            canvas.drawCircle(cx, cy, r - 1f, neonGlowPaint)

            // Mid glow
            neonGlowPaint.color = androidx.core.graphics.ColorUtils.setAlphaComponent(deckThemeColor, 140)
            neonGlowPaint.strokeWidth = 5f * dp
            canvas.drawCircle(cx, cy, r - 1f, neonGlowPaint)

            // Core neon ring
            neonRingPaint.style = Paint.Style.STROKE
            neonRingPaint.color = deckThemeColor
            neonRingPaint.strokeWidth = 2.5f * dp
            canvas.drawCircle(cx, cy, r - 1f, neonRingPaint)
        }

        // 2. Draw Tonearm (needle)
        indicatorDrawable?.let { arm ->
            canvas.save()
            val armAngle = if (needleAnimator.isRunning) {
                animatedArmAngle
            } else if (indicatorState == 1) {
                angleTrackStart + (angleTrackEnd - angleTrackStart) * progress
            } else {
                angleRest
            }
            animatedArmAngle = armAngle

            canvas.rotate(armAngle, armPivot.x, armPivot.y)
            arm.bounds = armRect
            arm.draw(canvas)
            canvas.restore()
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val cx = discRect.centerX().toFloat()
        val cy = discRect.centerY().toFloat()
        val radius = discRect.width() / 2f
        val distToCenter = hypot(event.x - cx, event.y - cy)

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (distToCenter <= radius * 1.15f) {
                    parent?.requestDisallowInterceptTouchEvent(true)
                    isScratching = true
                    updateRotationAnimation()
                    lastTouchAngle = Math.toDegrees(atan2((event.y - cy).toDouble(), (event.x - cx).toDouble()))
                    lastTouchTime = event.eventTime
                    smoothedVelocity = 0f
                    rotateListener?.onScratchStart()
                    return true
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (isScratching && distToCenter > 15f) {
                    val currentAngle = Math.toDegrees(atan2((event.y - cy).toDouble(), (event.x - cx).toDouble()))
                    var delta = (currentAngle - lastTouchAngle).toFloat()
                    if (delta > 180f) delta -= 360f
                    if (delta < -180f) delta += 360f

                    val now = event.eventTime
                    val dt = (now - lastTouchTime).coerceAtLeast(1)
                    val instantVelocity = (delta / dt) * 1000f
                    smoothedVelocity = if (smoothedVelocity == 0f) instantVelocity else (smoothedVelocity * 0.5f + instantVelocity * 0.5f)

                    lastTouchAngle = currentAngle
                    lastTouchTime = now

                    rotateDegrees += delta
                    invalidate()
                    rotateListener?.onScratch(delta, smoothedVelocity)
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isScratching) {
                    parent?.requestDisallowInterceptTouchEvent(false)
                    isScratching = false
                    smoothedVelocity = 0f
                    updateRotationAnimation()
                    rotateListener?.onScratchEnd()
                    return true
                }
            }
        }
        return super.onTouchEvent(event)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        updateRotationAnimation()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        rotationAnimator?.cancel()
        rotationAnimator = null
        needleAnimator.cancel()
    }
}
