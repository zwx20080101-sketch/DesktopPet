package com.mascot.overlay.ui

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

class ControlMenuView(context: Context) : FrameLayout(context) {
    private var onLockToggle: (() -> Unit)? = null
    private var onSettings: (() -> Unit)? = null
    private var onClose: (() -> Unit)? = null
    private val menuContainer = LinearLayout(context)

    init {
        menuContainer.orientation = LinearLayout.VERTICAL
        menuContainer.setBackgroundColor(Color.argb(200, 30, 30, 30))
        menuContainer.setPadding(dp(10), dp(10), dp(10), dp(10))

        val lockText = TextView(context).apply {
            text = "锁定/解锁"
            textSize = 14f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setOnClickListener { onLockToggle?.invoke() }
        }
        val settingsText = TextView(context).apply {
            text = "设置"
            textSize = 14f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setOnClickListener { onSettings?.invoke() }
        }
        val closeText = TextView(context).apply {
            text = "关闭"
            textSize = 14f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setOnClickListener { onClose?.invoke() }
        }

        menuContainer.addView(lockText, LinearLayout.LayoutParams(dp(80), dp(30)))
        menuContainer.addView(settingsText, LinearLayout.LayoutParams(dp(80), dp(30)))
        menuContainer.addView(closeText, LinearLayout.LayoutParams(dp(80), dp(30)))

        addView(menuContainer, LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
    }

    fun setOnLockToggleListener(listener: () -> Unit) { onLockToggle = listener }
    fun setOnSettingsListener(listener: () -> Unit) { onSettings = listener }
    fun setOnCloseListener(listener: () -> Unit) { onClose = listener }

    fun show() {
        visibility = VISIBLE
        alpha = 0f
        animate().alpha(1f).setDuration(150).start()
    }

    fun hide() {
        animate().alpha(0f).setDuration(150).withEndAction {
            visibility = GONE
        }.start()
    }

    private fun dp(value: Int): Int {
        return (value * context.resources.displayMetrics.density).toInt()
    }
}
