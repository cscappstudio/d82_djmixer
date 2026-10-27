package com.ijoysoft.music.view.square

import android.content.Context
import android.graphics.Canvas
import android.graphics.Outline
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import com.audioeffect.djmixer.music.mixer.R

class CornerFrameLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private var radius = 0f
    private val clipPath = Path()
    private val rectF = RectF()

    init {
        if (attrs != null) {
            val a = context.obtainStyledAttributes(attrs, R.styleable.CornerFrameLayout)
            radius = a.getDimension(R.styleable.CornerFrameLayout_corner_radius, 0f)
            a.recycle()
        }
        updateOutline()
    }

    private fun updateOutline() {
        if (radius > 0f) {
            clipToOutline = true
            outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setRoundRect(0, 0, view.width, view.height, radius)
                }
            }
        } else {
            clipToOutline = false
            outlineProvider = ViewOutlineProvider.BACKGROUND
        }
    }

    fun setCornerRadius(r: Float) {
        radius = r
        updateOutline()
        invalidateOutline()
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        rectF.set(0f, 0f, w.toFloat(), h.toFloat())
        clipPath.reset()
        if (w > 0 && h > 0 && radius > 0f) {
            clipPath.addRoundRect(rectF, radius, radius, Path.Direction.CW)
        }
    }

    override fun draw(canvas: Canvas) {
        if (radius > 0f && !clipToOutline) {
            canvas.save()
            canvas.clipPath(clipPath)
            super.draw(canvas)
            canvas.restore()
        } else {
            super.draw(canvas)
        }
    }
}
