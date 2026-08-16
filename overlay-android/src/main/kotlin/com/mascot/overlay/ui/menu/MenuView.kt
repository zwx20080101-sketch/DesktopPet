package com.mascot.overlay.ui.menu

import android.view.View
import com.mascot.overlay.role.Role

interface MenuView {
    fun getView(): View
    fun setRoles(roles: List<Role>)
    fun setOnRoleSelectedListener(listener: (Role) -> Unit)
    fun show()
    fun hide()
    fun isVisible(): Boolean
}
