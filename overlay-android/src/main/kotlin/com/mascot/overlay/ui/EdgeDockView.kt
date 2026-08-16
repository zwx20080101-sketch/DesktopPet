package com.mascot.overlay.ui

import android.content.Context
import android.graphics.Color
import android.widget.FrameLayout
import android.widget.TextView

class EdgeDockView(context: Context) : FrameLayout(context) {
    private val dockBar = TextView(context)

    init {
        dockBar.text = ""
        dockBar.setBackgroundColor(Color.GRAY)
        dockBar.alpha = 0.8f
        addView(dockBar, LayoutParams(dp(20), dp(50)))
        setOnClickListener {
            // 点击事件由 OverlayView 处理
            performClick()
        }
    }

    fun setVisible(visible: Boolean) {
        visibility = if (visible) VISIBLE else GONE
    }

    private fun dp(value: Int): Int {
        return (value * context.resources.displayMetrics.density).toInt()
    }
}
