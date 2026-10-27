package com.audioeffect.djmixer.music.mixer.core.ui

import android.animation.ValueAnimator
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.animation.DecelerateInterpolator
import android.widget.ProgressBar
import androidx.appcompat.widget.AppCompatTextView
import com.audioeffect.djmixer.music.mixer.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

open class OutlineTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {

    var outlineColor: Int = Color.WHITE
    var outlineWidth: Float = 0f
    private var isDrawing = false

    init {
        if (attrs != null) {
            val a = context.obtainStyledAttributes(attrs, R.styleable.OutlineTextView)
            outlineColor = a.getColor(R.styleable.OutlineTextView_outlineColor, Color.WHITE)
            outlineWidth = a.getDimension(R.styleable.OutlineTextView_outlineWidth, 0f)
            a.recycle()
        }
    }

    override fun invalidate() {
        if (isDrawing) return
        super.invalidate()
    }

    override fun requestLayout() {
        if (isDrawing) return
        super.requestLayout()
    }

    override fun onDraw(canvas: Canvas) {
        if (outlineWidth > 0f) {
            isDrawing = true
            val originalColors = textColors
            val textPaint = paint

            // 1. Draw outline
            textPaint.style = Paint.Style.STROKE
            textPaint.strokeJoin = Paint.Join.ROUND
            textPaint.strokeCap = Paint.Cap.ROUND
            textPaint.strokeWidth = outlineWidth
            setTextColor(outlineColor)
            super.onDraw(canvas)

            // 2. Draw interior fill
            textPaint.style = Paint.Style.FILL
            setTextColor(originalColors)
            super.onDraw(canvas)

            isDrawing = false
        } else {
            super.onDraw(canvas)
        }
    }
}

open class SplashProgressBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.progressBarStyleHorizontal
) : ProgressBar(context, attrs, defStyleAttr) {

    private var progressAnimator: ValueAnimator? = null

    init {
        max = 100
        progress = 15
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        startLoadingAnimation()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        progressAnimator?.cancel()
    }

    private fun startLoadingAnimation() {
        progressAnimator?.cancel()
        progressAnimator = ValueAnimator.ofInt(15, 95).apply {
            duration = 2800
            interpolator = DecelerateInterpolator()
            addUpdateListener { animator ->
                progress = animator.animatedValue as Int
            }
            start()
        }
    }
}

open class SplashDateTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {

    private val timeTickReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, intent: Intent?) {
            updateDate()
        }
    }

    init {
        updateDate()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        updateDate()
        try {
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_TIME_TICK)
                addAction(Intent.ACTION_TIME_CHANGED)
                addAction(Intent.ACTION_TIMEZONE_CHANGED)
            }
            androidx.core.content.ContextCompat.registerReceiver(
                context,
                timeTickReceiver,
                filter,
                androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        try {
            context.unregisterReceiver(timeTickReceiver)
        } catch (e: Exception) {
            // ignore
        }
    }

    fun updateDate() {
        try {
            val now = Date()
            val format = SimpleDateFormat("EEE, MMMM d", Locale.ENGLISH)
            text = format.format(now)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
