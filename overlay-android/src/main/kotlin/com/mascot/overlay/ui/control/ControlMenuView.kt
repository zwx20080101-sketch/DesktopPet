package com.mascot.overlay.ui.control

import android.view.View

interface ControlMenuView {
    fun getView(): View
    fun setOnLockToggleListener(listener: () -> Unit)
    fun setOnSettingsListener(listener: () -> Unit)
    fun setOnCloseListener(listener: () -> Unit)
    fun show()
    fun hide()
    fun isVisible(): Boolean
}
