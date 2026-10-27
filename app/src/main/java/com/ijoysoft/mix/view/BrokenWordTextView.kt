package com.ijoysoft.mix.view

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView

class BrokenWordTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {

    private var rawText: CharSequence? = null
    private var updating = false

    init {
        setTextIsSelectable(false)
        isLongClickable = false
    }

    override fun setText(text: CharSequence?, type: BufferType?) {
        if (!updating) {
            rawText = text
        }
        super.setText(text, type)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        formatText()
    }

    private fun formatText() {
        val availableWidth = width - paddingLeft - paddingRight
        val text = rawText
        if (text.isNullOrEmpty() || availableWidth <= 0) return

        val count = paint.breakText(text, 0, text.length, true, availableWidth.toFloat(), null)
        if (count >= text.length || count <= 0) {
            setSuperText(text)
            return
        }

        var sub = text.subSequence(0, count)
        if (text[count] != ' ') {
            val lastSpace = sub.lastIndexOf(' ')
            if (lastSpace > 0) {
                sub = sub.subSequence(0, lastSpace)
            }
        }
        setSuperText(sub)
    }

    private fun setSuperText(text: CharSequence) {
        updating = true
        super.setText(text, BufferType.NORMAL)
        updating = false
    }
}
