package com.ijoysoft.mix.view.visualizer

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import com.audioeffect.djmixer.music.mixer.R

class VisualizerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : VisualizerViewBase(context, attrs, defStyleAttr) {

    var lineWidth: Float = 1.5f * resources.displayMetrics.density
    var strokeColor: Int = Color.BLACK
    private val cellRect = RectF()

    var rowCount: Int = 11
    var colCount: Int = 1
    private var cellWidth: Float = 0f
    private var cellHeight: Float = 0f
    private var verticalSpacing: Float = 0f
    private var horizontalSpacing: Float = 0f
    var spaceFactorV: Float = 0.25f
    var spaceFactorH: Float = 0.2f

    init {
        if (attrs != null) {
            val a = context.obtainStyledAttributes(attrs, R.styleable.VisualizerView)
            rowCount = a.getInt(R.styleable.VisualizerView_visualizer_row, 11)
            colCount = a.getInt(R.styleable.VisualizerView_visualizer_column, 1)
            spaceFactorV = a.getFloat(R.styleable.VisualizerView_visualizer_space_factor_v, 0.25f)
            spaceFactorH = a.getFloat(R.styleable.VisualizerView_visualizer_space_factor_h, 0.2f)
            lineWidth = a.getDimension(R.styleable.VisualizerView_visualizer_line_width, lineWidth)
            strokeColor = a.getColor(R.styleable.VisualizerView_visualizer_linear_color, Color.BLACK)
            a.recycle()
        }
    }

    private fun drawCells(canvas: Canvas, isFill: Boolean) {
        val stroke = lineWidth
        val paint = drawPaint
        if (isFill) {
            paint.color = defaultColor
            paint.strokeWidth = 1.0f
            paint.style = Paint.Style.FILL
        } else {
            paint.color = strokeColor
            paint.strokeWidth = stroke
            paint.style = Paint.Style.STROKE
        }

        val cornerRadius = 2f * resources.displayMetrics.density
        for (c in 0 until colCount) {
            val left = (horizontalSpacing + cellWidth) * c
            val right = left + cellWidth
            for (r in 0 until rowCount) {
                val top = (verticalSpacing + cellHeight) * r
                val bottom = top + cellHeight
                cellRect.set(left, top, right, bottom)
                if (isFill) {
                    canvas.drawRoundRect(cellRect, cornerRadius, cornerRadius, paint)
                } else if (stroke > 0f && strokeColor != Color.TRANSPARENT) {
                    val inset = stroke / 2.0f
                    cellRect.inset(inset, inset)
                    canvas.drawRoundRect(cellRect, cornerRadius, cornerRadius, paint)
                }
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        if (rowCount <= 0 || colCount <= 0 || cellWidth <= 0f || cellHeight <= 0f) return

        canvas.save()
        canvas.translate(paddingLeft.toFloat(), paddingTop.toFloat())

        // 1. Base vibrant gradient
        drawPaint.shader = baseGradient
        drawCells(canvas, isFill = true)

        // 2. Dark overlay mask for inactive LEDs
        drawPaint.shader = getOverlayShader()
        drawCells(canvas, isFill = true)

        // 3. Cell borders (if enabled)
        if (lineWidth > 0f && strokeColor != Color.TRANSPARENT) {
            drawPaint.shader = null
            drawCells(canvas, isFill = false)
        }

        canvas.restore()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val usableW = (measuredWidth - paddingLeft - paddingRight).toFloat()
        val usableH = (measuredHeight - paddingTop - paddingBottom).toFloat()
        cellWidth = 0f
        cellHeight = 0f
        verticalSpacing = 0f
        horizontalSpacing = 0f

        if (rowCount <= 0 || colCount <= 0 || usableW <= 0f || usableH <= 0f) return

        val totalColUnit = usableW / (colCount - spaceFactorH)
        horizontalSpacing = spaceFactorH * totalColUnit
        cellWidth = totalColUnit - horizontalSpacing

        val totalRowUnit = usableH / (rowCount - spaceFactorV)
        verticalSpacing = spaceFactorV * totalRowUnit
        cellHeight = totalRowUnit - verticalSpacing
    }
}
