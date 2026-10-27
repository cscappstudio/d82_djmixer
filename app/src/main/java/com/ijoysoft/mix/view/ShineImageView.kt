package com.ijoysoft.mix.view

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.animation.LinearInterpolator
import androidx.appcompat.widget.AppCompatImageView

class ShineImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr), ValueAnimator.AnimatorUpdateListener {

    private var shineDrawable: Drawable? = null
    private var shineImage = false
    private var shineBackground = false
    private var hideRectWhenNotShine = false
    private var animator: ValueAnimator? = null
    private var isShining = false
    private var isAttached = false
    private var currentAlpha = 255

    init {
        // read shineDrawable and flags if needed
        // custom attrs: shineRectDrawable, shineImage, hideRectWhenNotShine
    }

    private fun updateAnimation() {
        if (!isShining || !isAttached) {
            animator?.let {
                it.removeUpdateListener(this)
                it.cancel()
                animator = null
            }
            return
        }
        if (animator == null) {
            animator = ValueAnimator.ofFloat(0.3f, 1.0f, 0.3f).apply {
                duration = 2000L
                interpolator = LinearInterpolator()
                repeatCount = ValueAnimator.INFINITE
                addUpdateListener(this@ShineImageView)
                start()
            }
        }
    }

    override fun onAnimationUpdate(animation: ValueAnimator) {
        currentAlpha = ((animation.animatedValue as Float) * 255).toInt()
        invalidate()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        isAttached = true
        updateAnimation()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        isAttached = false
        updateAnimation()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        shineDrawable?.setBounds(0, 0, w, h)
    }

    override fun onDraw(canvas: Canvas) {
        if (isShining || !hideRectWhenNotShine) {
            shineDrawable?.let {
                it.alpha = if (isShining) currentAlpha else 255
                it.draw(canvas)
            }
        }
        background?.let {
            it.alpha = if (isShining && shineBackground) currentAlpha else 255
        }
        drawable?.let {
            it.alpha = if (isShining && shineImage) currentAlpha else 255
        }
        super.onDraw(canvas)
    }

    fun setShine(shine: Boolean) {
        if (isShining != shine) {
            isShining = shine
            updateAnimation()
            invalidate()
        }
    }
}
