package com.mascot.overlay.ui.control

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

class DefaultControlMenuView(val context: Context) : ControlMenuView {
    private val container = FrameLayout(context).apply { visibility = View.GONE }
    private val linear = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setBackgroundColor(Color.argb(200, 30, 30, 30))
        setPadding(10.dp, 10.dp, 10.dp, 10.dp)
    }
    private var lockToggle: (() -> Unit)? = null
    private var settings: (() -> Unit)? = null
    private var close: (() -> Unit)? = null

    init {
        linear.addView(makeButton("锁定/解锁") { lockToggle?.invoke() })
        linear.addView(makeButton("设置") { settings?.invoke() })
        linear.addView(makeButton("关闭") { close?.invoke() })
        container.addView(linear, FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT)
    }

    private fun makeButton(text: String, onClick: () -> Unit) = TextView(context).apply {
        this.text = text
        textSize = 14f
        setTextColor(Color.WHITE)
        gravity = Gravity.CENTER
        setOnClickListener { onClick() }
        layoutParams = LinearLayout.LayoutParams(80.dp, 30.dp)
    }

    override fun getView(): View = container
    override fun setOnLockToggleListener(listener: () -> Unit) { lockToggle = listener }
    override fun setOnSettingsListener(listener: () -> Unit) { settings = listener }
    override fun setOnCloseListener(listener: () -> Unit) { close = listener }

    override fun show() {
        container.visibility = View.VISIBLE
        container.alpha = 0f
        container.animate().alpha(1f).setDuration(150).start()
    }
    override fun hide() {
        container.animate().alpha(0f).setDuration(150).withEndAction { container.visibility = View.GONE }.start()
    }
    override fun isVisible(): Boolean = container.visibility == View.VISIBLE

    private val Int.dp: Int get() = (this * container.resources.displayMetrics.density).toInt()
}
