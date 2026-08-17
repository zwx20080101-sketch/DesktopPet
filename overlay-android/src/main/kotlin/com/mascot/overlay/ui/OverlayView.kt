package com.mascot.overlay.ui

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
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
        textSize = 48f
        gravity = Gravity.CENTER
        setBackgroundColor(Color.TRANSPARENT)
    }
    private val roleMenu = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        visibility = GONE
        setBackgroundColor(Color.TRANSPARENT)
    }
    private val controlMenu = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        visibility = GONE
        setBackgroundColor(Color.argb(200, 30, 30, 30))
        setPadding(10.dp, 10.dp, 10.dp, 10.dp)
    }
    private val edgeDock = TextView(context).apply {
        text = ""
        setBackgroundColor(Color.GRAY)
        alpha = 0.8f
        visibility = GONE
    }

    private var startX = 0
    private var startY = 0
    private var lastX = 0
    private var lastY = 0
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
        RoleManager.roles.forEachIndexed { index, role ->
            val tv = TextView(context).apply {
                text = role.avatar
                textSize = 28f
                setBackgroundColor(Color.TRANSPARENT)
                setOnClickListener {
                    RoleManager.switch(role.id)
                    petText.text = RoleManager.current.avatar
                    roleMenu.visibility = GONE
                }
            }
            val lp = LinearLayout.LayoutParams(50.dp, 50.dp)
            lp.leftMargin = index * 60.dp
            roleMenu.addView(tv, lp)
        }
    }

    private fun buildControlMenu() {
        controlMenu.removeAllViews()
        val lockBtn = TextView(context).apply {
            text = "锁定/解锁"
            textSize = 14f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setOnClickListener {
                LockManager.toggle()
                updateLockIndicator()
                controlMenu.visibility = GONE
            }
        }
        val settingsBtn = TextView(context).apply {
            text = "设置"
            textSize = 14f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setOnClickListener {
                bridge?.openMainApp()
                controlMenu.visibility = GONE
            }
        }
        val closeBtn = TextView(context).apply {
            text = "关闭"
            textSize = 14f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setOnClickListener {
                PetAccessibilityService.instance?.removePet()
            }
        }
        controlMenu.addView(lockBtn, LinearLayout.LayoutParams(80.dp, 30.dp))
        controlMenu.addView(settingsBtn, LinearLayout.LayoutParams(80.dp, 30.dp))
        controlMenu.addView(closeBtn, LinearLayout.LayoutParams(80.dp, 30.dp))
    }

    private fun updateLockIndicator() {
        petText.alpha = if (LockManager.isLocked()) 0.6f else 1.0f
    }

    private fun handleTouch(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                startX = event.rawX.toInt()
                startY = event.rawY.toInt()
                lastX = startX
                lastY = startY
                downTime = System.currentTimeMillis()
                isDragging = false
                isPinching = false
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (event.pointerCount == 1 && !isPinching && !LockManager.isLocked()) {
                    val dx = event.rawX.toInt() - lastX
                    val dy = event.rawY.toInt() - lastY
                    if (Math.abs(dx) > 5 || Math.abs(dy) > 5) {
                        isDragging = true
                        params.x += dx
                        params.y += dy
                        wm.updateViewLayout(this, params)
                        lastX = event.rawX.toInt()
                        lastY = event.rawY.toInt()
                        checkEdge()
                    }
                } else if (event.pointerCount == 2 && !LockManager.isLocked()) {
                    if (!isPinching) {
                        isPinching = true
                        startDistance = distance(event)
                        startScale = scaleValue
                    }
                    val newDist = distance(event)
                    if (newDist > 0) {
                        val scale = (newDist / startDistance * startScale).coerceIn(0.3f, 5.0f)
                        scaleValue = scale
                        params.width = (120.dp * scale).toInt()
                        params.height = (120.dp * scale).toInt()
                        wm.updateViewLayout(this, params)
                    }
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                if (isDragging) {
                    isDragging = false
                    snapToEdgeIfNeeded()
                } else if (isPinching) {
                    isPinching = false
                } else {
                    val now = System.currentTimeMillis()
                    if (now - downTime < 300) {
                        tapCount++
                        if (tapCount == 1) {
                            lastTapTime = now
                            postDelayed({
                                if (tapCount == 1) {
                                    // 单击反应：跳动
                                    petText.animate().scaleX(1.2f).scaleY(1.2f).setDuration(100).withEndAction {
                                        petText.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                                    }.start()
                                }
                                tapCount = 0
                            }, 250)
                        } else if (tapCount >= 2) {
                            // 双击：切换角色菜单
                            roleMenu.visibility = if (roleMenu.visibility == VISIBLE) GONE else VISIBLE
                            if (roleMenu.visibility == VISIBLE) {
                                // 菜单位置调整到宠物右侧
                                val lp = roleMenu.layoutParams as LayoutParams
                                lp.gravity = Gravity.START or Gravity.TOP
                                lp.leftMargin = 120.dp + 10.dp
                                lp.topMargin = 0
                                roleMenu.layoutParams = lp
                            }
                            tapCount = 0
                        }
                    } else {
                        // 长按：控制菜单
                        controlMenu.visibility = if (controlMenu.visibility == VISIBLE) GONE else VISIBLE
                        if (controlMenu.visibility == VISIBLE) {
                            val lp = controlMenu.layoutParams as LayoutParams
                            lp.gravity = Gravity.START or Gravity.TOP
                            lp.leftMargin = 120.dp + 10.dp
                            lp.topMargin = 0
                            controlMenu.layoutParams = lp
                        }
                    }
                }
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                isDragging = false
                isPinching = false
                tapCount = 0
                return true
            }
        }
        return false
    }

    private fun distance(e: MotionEvent): Float {
        val dx = e.getX(0) - e.getX(1)
        val dy = e.getY(0) - e.getY(1)
        return kotlin.math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
    }

    private fun checkEdge() {
        val sw = ScreenUtils.getScreenWidth(context)
        val sh = ScreenUtils.getScreenHeight(context)
        val threshold = 40.dp
        val close = params.x < threshold ||
                params.x > sw - params.width - threshold ||
                params.y < threshold ||
                params.y > sh - params.height - threshold
        if (close) {
            edgeDock.visibility = VISIBLE
            // 根据边缘设置位置
            when {
                params.x < threshold -> {
                    edgeDock.x = 0f
                    edgeDock.y = (params.y + params.height / 2 - 30.dp).toFloat()
                }
                params.x > sw - params.width - threshold -> {
                    edgeDock.x = (sw - 20.dp).toFloat()
                    edgeDock.y = (params.y + params.height / 2 - 30.dp).toFloat()
                }
                params.y < threshold -> {
                    edgeDock.x = (params.x + params.width / 2 - 10.dp).toFloat()
                    edgeDock.y = 0f
                }
                else -> {
                    edgeDock.x = (params.x + params.width / 2 - 10.dp).toFloat()
                    edgeDock.y = (sh - 60.dp).toFloat()
                }
            }
        } else {
            edgeDock.visibility = GONE
        }
    }

    private fun snapToEdgeIfNeeded() {
        val sw = ScreenUtils.getScreenWidth(context)
        val sh = ScreenUtils.getScreenHeight(context)
        val threshold = 40.dp
        var docked = false

        if (params.x < threshold) {
            params.x = -params.width + 20.dp
            docked = true
        } else if (params.x > sw - params.width - threshold) {
            params.x = sw - 20.dp
            docked = true
        }

        if (params.y < threshold) {
            params.y = -params.height + 20.dp
            docked = true
        } else if (params.y > sh - params.height - threshold) {
            params.y = sh - 20.dp
            docked = true
        }

        if (docked) {
            wm.updateViewLayout(this, params)
            edgeDock.visibility = VISIBLE
            LockManager.setLocked(true)
            updateLockIndicator()
        } else {
            edgeDock.visibility = GONE
        }
    }

    private fun unDock() {
        val sw = ScreenUtils.getScreenWidth(context)
        val sh = ScreenUtils.getScreenHeight(context)
        if (params.x < 0) params.x = 20.dp
        else if (params.x > sw - params.width) params.x = sw - params.width - 20.dp
        if (params.y < 0) params.y = 20.dp
        else if (params.y > sh - params.height) params.y = sh - params.height - 20.dp
        wm.updateViewLayout(this, params)
        edgeDock.visibility = GONE
        LockManager.setLocked(false)
        updateLockIndicator()
    }

    private val Int.dp: Int get() = (this * resources.displayMetrics.density).toInt()
}
