package com.mascot.overlay.ui.pet

import android.view.View
import com.mascot.overlay.role.Role

interface PetView {
    fun getView(): View
    fun setRole(role: Role)
    fun setScale(scale: Float)
    fun setLocked(locked: Boolean)
}
