import com.mascot.overlay.bridge.ServiceBridge
package com.mascot.overlay

import android.accessibilityservice.AccessibilityService
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import com.mascot.overlay.bridge.ServiceBridge
import com.mascot.overlay.ui.OverlayView

class PetAccessibilityService : AccessibilityService(), ServiceBridge {

    companion object {
        var instance: PetAccessibilityService? = null
    }

    private lateinit var windowManager: WindowManager
    private var overlayView: OverlayView? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        showPet()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // 暂不处理
    }

    override fun onInterrupt() {
        // 暂不处理
    }

    override fun onDestroy() {
        removePet()
        instance = null
        super.onDestroy()
    }

    fun showPet() {
        if (overlayView != null) return

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 200
        }

        val view = OverlayView(this, windowManager, params)
        windowManager.addView(view, params)
        overlayView = view
        layoutParams = params
    }

    fun removePet() {
        overlayView?.let {
            if (it.isAttachedToWindow) {
                windowManager.removeView(it)
            }
        }
        overlayView = null
        layoutParams = null
    }

    // ServiceBridge 实现
    override fun openMainApp() {
        val intent = Intent(this, com.mascot.app.MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
    }

    override fun removeOverlay() {
        removePet()
    }
}
