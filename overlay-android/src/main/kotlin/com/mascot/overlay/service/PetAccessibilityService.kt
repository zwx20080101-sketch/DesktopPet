package com.mascot.overlay.service

import android.accessibilityservice.AccessibilityService
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import com.mascot.overlay.bridge.ServiceBridge
import com.mascot.overlay.lock.LockManager
import com.mascot.overlay.ui.OverlayView
import com.mascot.overlay.ui.edge.DefaultEdgeDockView
import com.mascot.overlay.ui.edge.EdgeDockView
import com.mascot.overlay.util.ScreenUtils

class PetAccessibilityService : AccessibilityService() {

    companion object {
        var instance: PetAccessibilityService? = null
    }

    private lateinit var wm: WindowManager
    private var overlay: OverlayView? = null
    private var overlayParams: WindowManager.LayoutParams? = null
    private var edgeDockView: EdgeDockView? = null

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

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
        }

        val params = WindowManager.LayoutParams(
            120.dp, 120.dp, type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 200
        }

        val view = OverlayView(
            ctx = this,
            wm = wm,
            params = params,
            bridge = bridge,
            onRequestDock = { dockOverlay(params, view) }
        )

        wm.addView(view, params)
        overlay = view
        overlayParams = params
    }

    fun removePet() {
        overlay?.let {
            if (it.isAttachedToWindow) {
                wm.removeView(it)
            }
        }
        overlay = null
        overlayParams = null
        removeEdgeDock()
    }

    private fun dockOverlay(params: WindowManager.LayoutParams, view: OverlayView) {
        val sw = ScreenUtils.getScreenWidth(this)
        val sh = ScreenUtils.getScreenHeight(this)
        val threshold = 40.dp

        var docked = false
        // 左边缘
        if (params.x < threshold) {
            params.x = -params.width + 20.dp
            docked = true
        }
        // 右边缘
        else if (params.x > sw - params.width - threshold) {
            params.x = sw - 20.dp
            docked = true
        }
        // 上边缘
        if (params.y < threshold) {
            params.y = -params.height + 20.dp
            docked = true
        }
        // 下边缘
        else if (params.y > sh - params.height - threshold) {
            params.y = sh - 20.dp
            docked = true
        }

        if (docked) {
            wm.updateViewLayout(view, params)
            showEdgeDock(params)
            LockManager.setLocked(true)
        overlay?.setLocked(true)
        }
    }

    private fun showEdgeDock(params: WindowManager.LayoutParams) {
        // 如果已有边缘条，先移除
        removeEdgeDock()

        val dock = DefaultEdgeDockView(this)
        val dockParams = WindowManager.LayoutParams(
            20.dp, 60.dp,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = calculateEdgeDockX(params)
            y = calculateEdgeDockY(params)
        }

        dock.setOnClickListener {
            unDockOverlay()
        }

        wm.addView(dock.getView(), dockParams)
        edgeDockView = dock
    }

    private fun calculateEdgeDockX(params: WindowManager.LayoutParams): Int {
        val sw = ScreenUtils.getScreenWidth(this)
        return when {
            params.x < 0 -> 0
            params.x > sw - params.width -> sw - 20.dp
            else -> params.x + params.width / 2 - 10.dp
        }
    }

    private fun calculateEdgeDockY(params: WindowManager.LayoutParams): Int {
        val sh = ScreenUtils.getScreenHeight(this)
        return when {
            params.y < 0 -> params.y + params.height / 2 - 30.dp
            params.y > sh - params.height -> sh - 60.dp
            else -> params.y + params.height / 2 - 30.dp
        }
    }

    private fun unDockOverlay() {
        val params = overlayParams ?: return
        val sw = ScreenUtils.getScreenWidth(this)
        val sh = ScreenUtils.getScreenHeight(this)

        if (params.x < 0) params.x = 20.dp
        else if (params.x > sw - params.width) params.x = sw - params.width - 20.dp
        if (params.y < 0) params.y = 20.dp
        else if (params.y > sh - params.height) params.y = sh - params.height - 20.dp

        overlay?.let { wm.updateViewLayout(it, params) }
        removeEdgeDock()
        LockManager.setLocked(false)
        overlay?.setLocked(false)
        // 更新宠物锁定状态
        overlay?.let {
            // 通过 OverlayView 暴露方法更新锁定，这里简单触发
            // 但我们无法直接访问 petView，稍后 OverlayView 提供方法
        }
    }

    private fun removeEdgeDock() {
        edgeDockView?.let { dock ->
            val view = dock.getView()
            if (view.isAttachedToWindow) {
                wm.removeView(view)
            }
        }
        edgeDockView = null
    }

    private val Int.dp: Int get() = (this * resources.displayMetrics.density).toInt()
}
