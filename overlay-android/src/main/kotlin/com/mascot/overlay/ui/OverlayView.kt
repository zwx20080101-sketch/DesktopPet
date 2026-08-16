package com.mascot.overlay.ui

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView
import com.mascot.overlay.bridge.ServiceBridge
import com.mascot.overlay.lock.LockManager
import com.mascot.overlay.role.RoleManager
import com.mascot.overlay.service.PetAccessibilityService
import com.mascot.overlay.util.ScreenUtils

class OverlayView(
    context: Context,
    private val wm: WindowManager,
    private val params: WindowManager.LayoutParams,
    private val bridge: ServiceBridge?
) : FrameLayout(context) {

    private val petText = TextView(context).apply {
        textSize = 48f; gravity = Gravity.CENTER; setBackgroundColor(Color.TRANSPARENT)
    }
    private val roleMenu = FrameLayout(context).apply { visibility = GONE }
    private val controlMenu = FrameLayout(context).apply { visibility = GONE }
    private val edgeDock = TextView(context).apply {
        text = ""; setBackgroundColor(Color.GRAY); alpha = 0.8f; visibility = GONE
    }

    private var startX = 0; private var startY = 0
    private var lastX = 0; private var lastY = 0
    private var downTime = 0L
    private var isDragging = false
    private var isPinching = false
    private var startDistance = 0f
    private var startScale = 1f
    private var lastTapTime = 0L
    private var tapCount = 0
    private var scaleValue = 1f

    init {
        petText.text = RoleManager.current.avatar
        addView(petText, LayoutParams(120.dp, 120.dp))
        addView(roleMenu, LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        addView(controlMenu, LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        addView(edgeDock, LayoutParams(20.dp, 60.dp))

        buildRoleMenu()
        buildControlMenu()

        setOnTouchListener { _, event -> handleTouch(event) }
        edgeDock.setOnClickListener { unDock() }
    }

    private fun buildRoleMenu() {
        roleMenu.removeAllViews()
        RoleManager.roles.forEachIndexed { i, role ->
            val tv = TextView(context).apply {
                text = role.avatar; textSize = 28f; setBackgroundColor(Color.TRANSPARENT)
                setOnClickListener { RoleManager.switch(role.id); petText.text = RoleManager.current.avatar; roleMenu.visibility = GONE }
            }
            roleMenu.addView(tv, FrameLayout.LayoutParams(50.dp, 50.dp).apply { leftMargin = i * 60.dp })
        }
    }

    private fun buildControlMenu() {
        controlMenu.removeAllViews()
        val lockBtn = TextView(context).apply { text = "锁定/解锁"; setTextColor(Color.WHITE); gravity = Gravity.CENTER; setOnClickListener { LockManager.toggle(); updateLock(); controlMenu.visibility = GONE } }
        val settingsBtn = TextView(context).apply { text = "设置"; setTextColor(Color.WHITE); gravity = Gravity.CENTER; setOnClickListener { bridge?.openMainApp(); controlMenu.visibility = GONE } }
        val closeBtn = TextView(context).apply { text = "关闭"; setTextColor(Color.WHITE); gravity = Gravity.CENTER; setOnClickListener { PetAccessibilityService.instance?.removePet() } }
        val linear = android.widget.LinearLayout(context).apply { orientation = android.widget.LinearLayout.VERTICAL; setBackgroundColor(Color.argb(200,30,30,30)); setPadding(10.dp,10.dp,10.dp,10.dp) }
        linear.addView(lockBtn, android.widget.LinearLayout.LayoutParams(80.dp, 30.dp))
        linear.addView(settingsBtn, android.widget.LinearLayout.LayoutParams(80.dp, 30.dp))
        linear.addView(closeBtn, android.widget.LinearLayout.LayoutParams(80.dp, 30.dp))
        controlMenu.addView(linear, FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT)
    }

    private fun updateLock() {
        petText.alpha = if (LockManager.isLocked()) 0.6f else 1f
    }

    private fun handleTouch(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                startX = event.rawX.toInt(); startY = event.rawY.toInt()
                lastX = startX; lastY = startY
                downTime = System.currentTimeMillis()
                isDragging = false; isPinching = false
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (event.pointerCount == 1 && !isPinching && !LockManager.isLocked()) {
                    val dx = event.rawX.toInt() - lastX; val dy = event.rawY.toInt() - lastY
                    if (Math.abs(dx) > 5 || Math.abs(dy) > 5) {
                        isDragging = true
                        params.x += dx; params.y += dy
                        wm.updateViewLayout(this, params)
                        lastX = event.rawX.toInt(); lastY = event.rawY.toInt()
                        checkEdge()
                    }
                } else if (event.pointerCount == 2 && !LockManager.isLocked()) {
                    if (!isPinching) { isPinching = true; startDistance = distance(event); startScale = scaleValue }
                    val newDist = distance(event)
                    val scale = (newDist / startDistance * startScale).coerceIn(0.3f, 5f)
                    scaleValue = scale
                    params.width = (120.dp * scale).toInt(); params.height = (120.dp * scale).toInt()
                    wm.updateViewLayout(this, params)
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                if (isDragging) { isDragging = false; snapToEdgeIfNeeded() }
                else if (isPinching) { isPinching = false }
                else {
                    val now = System.currentTimeMillis()
                    if (now - downTime < 300) {
                        tapCount++
                        if (tapCount == 1) {
                            lastTapTime = now
                            postDelayed({
                                if (tapCount == 1) { petText.animate().scaleX(1.2f).scaleY(1.2f).setDuration(100).withEndAction { petText.animate().scaleX(1f).scaleY(1f).setDuration(100) } }
                                tapCount = 0
                            }, 250)
                        } else if (tapCount >= 2) {
                            roleMenu.visibility = if (roleMenu.visibility == VISIBLE) GONE else VISIBLE
                            tapCount = 0
                        }
                    } else {
                        // 长按
                        controlMenu.visibility = if (controlMenu.visibility == VISIBLE) GONE else VISIBLE
                    }
                }
                return true
            }
            MotionEvent.ACTION_CANCEL -> { isDragging = false; isPinching = false; return true }
        }
        return false
    }

    private fun distance(e: MotionEvent) = Math.sqrt(((e.getX(0)-e.getX(1)).pow(2) + (e.getY(0)-e.getY(1)).pow(2)).toDouble()).toFloat()

    private fun checkEdge() {
        val sw = ScreenUtils.getScreenWidth(context); val sh = ScreenUtils.getScreenHeight(context)
        val threshold = 40.dp
        val close = params.x < threshold || params.x > sw - params.width - threshold || params.y < threshold || params.y > sh - params.height - threshold
        edgeDock.visibility = if (close) VISIBLE else GONE
        if (close) {
            when {
                params.x < threshold -> edgeDock.x = 0f; edgeDock.y = params.y + params.height/2 - 30.dp
                params.x > sw - params.width - threshold -> edgeDock.x = (sw - 20.dp).toFloat(); edgeDock.y = params.y + params.height/2 - 30.dp
                params.y < threshold -> edgeDock.x = params.x + params.width/2 - 10.dp; edgeDock.y = 0f
                else -> edgeDock.x = params.x + params.width/2 - 10.dp; edgeDock.y = (sh - 60.dp).toFloat()
            }
        }
    }

    private fun snapToEdgeIfNeeded() {
        val sw = ScreenUtils.getScreenWidth(context); val sh = ScreenUtils.getScreenHeight(context)
        val threshold = 40.dp
        var docked = false
        if (params.x < threshold) { params.x = -params.width + 20.dp; docked = true }
        else if (params.x > sw - params.width - threshold) { params.x = sw - 20.dp; docked = true }
        if (params.y < threshold) { params.y = -params.height + 20.dp; docked = true }
        else if (params.y > sh - params.height - threshold) { params.y = sh - 20.dp; docked = true }
        if (docked) {
            wm.updateViewLayout(this, params)
            edgeDock.visibility = VISIBLE
            LockManager.setLocked(true); updateLock()
        } else edgeDock.visibility = GONE
    }

    private fun unDock() {
        val sw = ScreenUtils.getScreenWidth(context); val sh = ScreenUtils.getScreenHeight(context)
        if (params.x < 0) params.x = 20.dp else if (params.x > sw - params.width) params.x = sw - params.width - 20.dp
        if (params.y < 0) params.y = 20.dp else if (params.y > sh - params.height) params.y = sh - params.height - 20.dp
        wm.updateViewLayout(this, params)
        edgeDock.visibility = GONE
        LockManager.setLocked(false); updateLock()
    }

    private val Int.dp: Int get() = (this * resources.displayMetrics.density).toInt()
    private fun Float.pow(i: Int) = this * this
}
