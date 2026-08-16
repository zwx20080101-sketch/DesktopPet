package com.mascot.overlay.ui

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.TextView
import com.mascot.overlay.role.Role

class OverlayMenuView(context: Context) : FrameLayout(context) {
    private var onRoleSelected: ((Role) -> Unit)? = null
    private val menuContainer = FrameLayout(context)

    init {
        menuContainer.setBackgroundColor(Color.TRANSPARENT)
        addView(menuContainer, LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
    }

    fun setRoles(roles: List<Role>) {
        menuContainer.removeAllViews()
        for ((index, role) in roles.withIndex()) {
            val avatarView = TextView(context).apply {
                text = role.avatar
                textSize = 28f
                gravity = Gravity.CENTER
                setBackgroundColor(Color.argb(150, 255, 255, 255))
                setOnClickListener {
                    onRoleSelected?.invoke(role)
                }
            }
            val lp = LayoutParams(dp(50), dp(50))
            lp.leftMargin = index * dp(60)
            lp.topMargin = 0
            menuContainer.addView(avatarView, lp)
        }
    }

    fun setOnRoleSelectedListener(listener: (Role) -> Unit) {
        onRoleSelected = listener
    }

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
