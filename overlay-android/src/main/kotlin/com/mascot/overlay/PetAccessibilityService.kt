package com.mascot.overlay

import android.accessibilityservice.AccessibilityService
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.Button
import android.widget.TextView

class PetAccessibilityService : AccessibilityService() {

    private lateinit var windowManager: WindowManager
    private var petView: View? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
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
        super.onDestroy()
    }

    private fun showPet() {
        if (petView != null) return

        val inflater = LayoutInflater.from(this)
        val view = inflater.inflate(R.layout.overlay_pet, null)

        val body = view.findViewById<TextView>(R.id.overlay_body)
        val closeButton = view.findViewById<Button>(R.id.overlay_close)

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
        )

        params.gravity = Gravity.TOP or Gravity.START
        params.x = 100
        params.y = 200

        // 拖动逻辑：绑定在主体上
        body.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f
            private var isDragging = false

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        isDragging = false
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val deltaX = (event.rawX - initialTouchX).toInt()
                        val deltaY = (event.rawY - initialTouchY).toInt()
                        if (Math.abs(deltaX) > 10 || Math.abs(deltaY) > 10) {
                            isDragging = true
                        }
                        if (isDragging) {
                            params.x = initialX + deltaX
                            params.y = initialY + deltaY
                            windowManager.updateViewLayout(view, params)
                        }
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (!isDragging) {
                            // 点击主体暂时不处理，后续接对话
                        }
                        return true
                    }
                }
                return false
            }
        })

        // 关闭按钮逻辑
        closeButton.setOnClickListener {
            removePet()
            disableSelf()
        }

        windowManager.addView(view, params)
        petView = view
        layoutParams = params
    }

    private fun removePet() {
        petView?.let {
            if (it.isAttachedToWindow) {
                windowManager.removeView(it)
            }
        }
        petView = null
        layoutParams = null
    }
}
