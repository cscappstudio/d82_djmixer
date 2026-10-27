package com.ijoysoft.mix.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.audioeffect.djmixer.music.mixer.R

class BpmSettingPreviewView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val linePaints = Array(4) { Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL } }
    private val lineRects = Array(4) { RectF() }

    private var lineSpacing = 10f
    private var normalColor = Color.parseColor("#1AEDF8FF")
    private var selectedColor = Color.parseColor("#00E5FF")
    private var errorColor = Color.RED
    private var cornerRadius = 0f

    var tapCount: Int = 0
        set(value) {
            field = value.coerceIn(0, 4)
            updateColors()
            invalidate()
        }

    init {
        if (attrs != null) {
            val a = context.obtainStyledAttributes(attrs, R.styleable.BpmSettingPreviewView)
            normalColor = a.getColor(R.styleable.BpmSettingPreviewView_bpmLineNormalColor, normalColor)
            selectedColor = a.getColor(R.styleable.BpmSettingPreviewView_bpmLineSelectedColor, selectedColor)
            errorColor = a.getColor(R.styleable.BpmSettingPreviewView_bpmLineErrorColor, errorColor)
            lineSpacing = a.getDimension(R.styleable.BpmSettingPreviewView_bpmLineSpacing, lineSpacing)
            a.recycle()
        }
        updateColors()
    }

    fun setLineSelectedColor(color: Int) {
        selectedColor = color
        updateColors()
        invalidate()
    }

    fun setLineNormalColor(color: Int) {
        normalColor = color
        updateColors()
        invalidate()
    }

    fun reset() {
        tapCount = 0
    }

    private fun updateColors() {
        for (i in 0 until 4) {
            linePaints[i].color = if (i < tapCount) selectedColor else normalColor
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w <= 0 || h <= 0) return

        val padL = paddingLeft.toFloat()
        val padT = paddingTop.toFloat()
        val padR = (w - paddingRight).toFloat()
        val padB = (h - paddingBottom).toFloat()

        val barWidth = ((padR - padL) - (3f * lineSpacing)) / 4f
        for (i in 0 until 4) {
            val left = padL + i * (barWidth + lineSpacing)
            lineRects[i].set(left, padT, left + barWidth, padB)
        }
        cornerRadius = (padB - padT) / 2f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        for (i in 0 until 4) {
            canvas.drawRoundRect(lineRects[i], cornerRadius, cornerRadius, linePaints[i])
        }
    }
}
