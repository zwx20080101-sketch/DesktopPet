package com.mascot.overlay.service

import android.accessibilityservice.AccessibilityService
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import com.mascot.overlay.bridge.ServiceBridge
import com.mascot.overlay.ui.OverlayView

class PetAccessibilityService : AccessibilityService() {
    companion object {
        var instance: PetAccessibilityService? = null
    }

    private lateinit var wm: WindowManager
    private var overlay: OverlayView? = null
    var bridge: ServiceBridge? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        showPet()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}
    override fun onDestroy() {
        removePet()
        instance = null
        super.onDestroy()
    }

    fun showPet() {
        if (overlay != null) return
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
        else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_SYSTEM_ALERT

        val params = WindowManager.LayoutParams(
            120.dp, 120.dp, type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 200
        }

        val view = OverlayView(this, wm, params, bridge)
        wm.addView(view, params)
        overlay = view
    }

    fun removePet() {
        overlay?.let { if (it.isAttachedToWindow) wm.removeView(it) }
        overlay = null
    }

    private val Int.dp: Int get() = (this * resources.displayMetrics.density).toInt()
}
