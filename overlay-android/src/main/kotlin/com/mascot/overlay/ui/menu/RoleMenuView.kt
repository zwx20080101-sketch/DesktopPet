package com.mascot.overlay.ui.menu

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import com.mascot.overlay.role.Role

class RoleMenuView(context: Context) : MenuView {
    private val container = FrameLayout(context).apply {
        setBackgroundColor(Color.TRANSPARENT)
        visibility = View.GONE
    }
    private var onRoleSelected: ((Role) -> Unit)? = null

    override fun getView(): View = container

    override fun setRoles(roles: List<Role>) {
        container.removeAllViews()
        for ((index, role) in roles.withIndex()) {
            val avatarView = TextView(context).apply {
                text = role.avatar
                textSize = 28f
                gravity = Gravity.CENTER
                setBackgroundColor(Color.TRANSPARENT)
                setOnClickListener {
                    onRoleSelected?.invoke(role)
                }
            }
            val lp = FrameLayout.LayoutParams(dp(50), dp(50)).apply {
                leftMargin = index * dp(60)
                topMargin = 0
            }
            container.addView(avatarView, lp)
        }
    }

    override fun setOnRoleSelectedListener(listener: (Role) -> Unit) {
        onRoleSelected = listener
    }

    override fun show() {
        container.visibility = View.VISIBLE
        container.alpha = 0f
        container.animate().alpha(1f).setDuration(150).start()
    }

    override fun hide() {
        container.animate().alpha(0f).setDuration(150).withEndAction {
            container.visibility = View.GONE
        }.start()
    }

    override fun isVisible(): Boolean = container.visibility == View.VISIBLE

    private fun dp(value: Int): Int {
        return (value * context.resources.displayMetrics.density).toInt()
    }
}
