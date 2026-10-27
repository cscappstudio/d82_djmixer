package com.ijoysoft.mix.view.visualizer

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.text.TextUtils
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import com.audioeffect.djmixer.music.mixer.R

abstract class VisualizerViewBase @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr), ValueAnimator.AnimatorUpdateListener {

    var defaultColor: Int = -14605786 // 0xFF212226
    var overlayColor: Int = -805306368 // 0xD0000000
    var gradientColors: IntArray = intArrayOf(-907482, -9568390, -13645063)
    val drawPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        strokeWidth = 1.0f
    }
    var baseGradient: LinearGradient? = null
    val animator: ValueAnimator = ValueAnimator().apply {
        duration = 100L
        interpolator = AccelerateDecelerateInterpolator()
        addUpdateListener(this@VisualizerViewBase)
    }
    var progressFactor: Float = 0.0f
    var isPlaying: Boolean = false
        set(value) {
            field = value
            if (animator.isRunning) animator.cancel()
            if (!value) {
                animator.setFloatValues(progressFactor, 0.0f)
                animator.duration = 800L
                animator.start()
            }
        }
    private val gradientCache = HashMap<Int, LinearGradient>()

    init {
        if (attrs != null) {
            val a = context.obtainStyledAttributes(attrs, R.styleable.VisualizerView)
            defaultColor = a.getColor(R.styleable.VisualizerView_visualizer_default_color, defaultColor)
            overlayColor = a.getColor(R.styleable.VisualizerView_visualizer_overlay_color, defaultColor)
            val colorsStr = a.getString(R.styleable.VisualizerView_visualizer_gradient_colors)
            if (!TextUtils.isEmpty(colorsStr)) {
                try {
                    val parts = colorsStr!!.split(",")
                    val parsed = IntArray(parts.size)
                    for (i in parts.indices) {
                        parsed[i] = Color.parseColor(parts[i].trim())
                    }
                    gradientColors = parsed
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            a.recycle()
        }
        drawPaint.color = defaultColor
    }

    fun getOverlayShader(): Shader {
        val fMax = 1.0f - Math.max(0.0f, Math.min(1.0f, progressFactor))
        val key = (200.0f * fMax).toInt()
        val cached = gradientCache[key]
        if (cached != null) return cached

        val usableH = (measuredHeight - paddingTop - paddingBottom).toFloat()
        val grad = LinearGradient(
            0f, 0f, 0f, usableH,
            intArrayOf(overlayColor, 0),
            floatArrayOf(fMax, fMax),
            Shader.TileMode.CLAMP
        )
        gradientCache[key] = grad
        return grad
    }

    override fun onAnimationUpdate(animation: ValueAnimator) {
        progressFactor = animation.animatedValue as Float
        if (isAttachedToWindow && windowToken != null) {
            postInvalidate()
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        gradientCache.clear()
    }

    override fun onDetachedFromWindow() {
        if (animator.isRunning) animator.cancel()
        super.onDetachedFromWindow()
        gradientCache.clear()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        gradientCache.clear()
        val usableH = (measuredHeight - paddingTop - paddingBottom).toFloat()
        baseGradient = LinearGradient(
            0f, usableH, 0f, 0f,
            gradientColors,
            null,
            Shader.TileMode.CLAMP
        )
    }

    open fun setProgress(progress: Float) {
        val clamped = progress.coerceIn(0f, 1f)
        if (clamped != progressFactor) {
            progressFactor = clamped
            invalidate()
        }
    }
}
