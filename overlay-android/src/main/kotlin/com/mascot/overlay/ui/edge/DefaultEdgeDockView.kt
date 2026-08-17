package com.mascot.overlay.ui.edge

import android.content.Context
import android.graphics.Color
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView

class DefaultEdgeDockView(context: Context) : EdgeDockView {
    private val container = FrameLayout(context).apply { visibility = View.GONE }
    private val bar = TextView(context).apply {
        text = ""
        setBackgroundColor(Color.GRAY)
        alpha = 0.8f
    }

    init {
        container.addView(bar, FrameLayout.LayoutParams(20.dp, 60.dp))
    }

    override fun getView(): View = container
    override fun setOnClickListener(listener: () -> Unit) { bar.setOnClickListener { listener() } }
    override fun show() { container.visibility = View.VISIBLE }
    override fun hide() { container.visibility = View.GONE }
    override fun isVisible(): Boolean = container.visibility == View.VISIBLE
    override fun setPosition(x: Int, y: Int) {
        val lp = container.layoutParams as FrameLayout.LayoutParams
        lp.leftMargin = x
        lp.topMargin = y
        container.layoutParams = lp
    }

    private val Int.dp: Int get() = (this * container.resources.displayMetrics.density).toInt()
}
