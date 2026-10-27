package com.ijoysoft.mix.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.appcompat.widget.AppCompatImageView

class SelectBox @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr), View.OnClickListener {

    interface OnSelectChangedListener {
        fun onSelectChanged(fromUser: Boolean, isSelected: Boolean)
    }

    private var listener: OnSelectChangedListener? = null

    init {
        setOnClickListener(this)
    }

    fun setOnSelectChangedListener(listener: OnSelectChangedListener?) {
        this.listener = listener
    }

    fun getOnSelectChangedListener(): OnSelectChangedListener? = listener

    override fun onClick(v: View?) {
        isSelected = !isSelected
        listener?.onSelectChanged(true, isSelected)
    }

    override fun setSelected(selected: Boolean) {
        val changed = selected != isSelected
        super.setSelected(selected)
        if (changed) {
            listener?.onSelectChanged(false, selected)
        }
    }
}
