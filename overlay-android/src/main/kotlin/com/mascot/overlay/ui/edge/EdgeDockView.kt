package com.mascot.overlay.ui.edge

import android.view.View

interface EdgeDockView {
    fun getView(): View
    fun setOnClickListener(listener: () -> Unit)
    fun show()
    fun hide()
    fun isVisible(): Boolean
    fun setPosition(x: Int, y: Int)
}
