package com.mascot.overlay.ui

import android.content.Context
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView
import com.mascot.overlay.bridge.ServiceBridge
import com.mascot.overlay.lock.LockManager
import com.mascot.overlay.role.Role
import com.mascot.overlay.role.RoleManager
import com.mascot.overlay.util.ScreenUtils

class OverlayView(
    context: Context,
    private val windowManager: WindowManager,
    private val params: WindowManager.LayoutParams,
    private val bridge: ServiceBridge?
) : FrameLayout(context) {

    private val petView: TextView = TextView(context)
    private val menuView = OverlayMenuView(context)
    private val controlMenuView = ControlMenuView(context)
    private val edgeDockView = EdgeDockView(context)
    private val roleManager = RoleManager()
    private var currentRole: Role = roleManager.currentRole

    private var isDragging = false
    private var isScaling = false
    private var startDistance = 0f
    private var startScale = 1f
    private var startX = 0
    private var startY = 0
    private var touchDownTime = 0L

    init {
        petView.text = currentRole.avatar
        petView.textSize = 48f
        petView.gravity = Gravity.CENTER
        petView.setBackgroundColor(0x80FFFFFF.toInt())
        addView(petView, LayoutParams(dp(120), dp(120)))

        menuView.visibility = GONE
        addView(menuView, LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)

        controlMenuView.visibility = GONE
        addView(controlMenuView, LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)

        edgeDockView.visibility = GONE
        addView(edgeDockView, LayoutParams(dp(20), dp(50)))

        menuView.setRoles(roleManager.getRoles())
        menuView.setOnRoleSelectedListener { role ->
            switchRole(role.id)
            hideMenus()
        }

        controlMenuView.setOnLockToggleListener {
            LockManager.toggle()
            hideMenus()
        }
        controlMenuView.setOnSettingsListener {
            bridge?.openMainApp()
            hideMenus()
        }
        controlMenuView.setOnCloseListener {
            bridge?.removeOverlay()
        }

        setOnTouchListener(createTouchListener())
    }

    private fun createTouchListener(): OnTouchListener {
        return object : OnTouchListener {
            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        if (LockManager.isLocked()) {
                            startLongPressCheck()
                            return true
                        }
                        startX = params.x
                        startY = params.y
                        touchDownTime = System.currentTimeMillis()
                        isDragging = false
                        startLongPressCheck()
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        if (LockManager.isLocked()) return true
                        if (event.pointerCount == 1 && !isScaling) {
                            val deltaX = event.rawX - startX
                            val deltaY = event.rawY - startY
                            if (Math.abs(deltaX) > 5 || Math.abs(deltaY) > 5) {
                                isDragging = true
                            }
                            if (isDragging) {
                                params.x = startX + deltaX.toInt()
                                params.y = startY + deltaY.toInt()
                                windowManager.updateViewLayout(this@OverlayView, params)
                                checkEdgeDock(params.x, params.y)
                            }
                        } else if (event.pointerCount == 2) {
                            if (!isScaling) {
                                isScaling = true
                                startDistance = distance(event)
                                startScale = petView.scaleX
                            }
                            val newDistance = distance(event)
                            val scale = (newDistance / startDistance * startScale).coerceIn(
                                currentRole.minScale,
                                currentRole.maxScale
                            )
                            petView.scaleX = scale
                            petView.scaleY = scale
                        }
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (isScaling) {
                            isScaling = false
                        } else if (!isDragging) {
                            if (System.currentTimeMillis() - touchDownTime < 300) {
                                currentRole.reaction.execute(petView)
                                if (menuView.visibility == VISIBLE) hideMenus()
                                else showMenu()
                            }
                        } else {
                            snapToEdgeIfNeeded()
                        }
                        isDragging = false
                        removeLongPressCheck()
                        return true
                    }
                    MotionEvent.ACTION_CANCEL -> {
                        isDragging = false
                        isScaling = false
                        removeLongPressCheck()
                        return true
                    }
                }
                return false
            }
        }
    }

    private var longPressRunnable: Runnable? = null
    private fun startLongPressCheck() {
        removeLongPressCheck()
        longPressRunnable = Runnable {
            if (menuView.visibility != VISIBLE) {
                showControlMenu()
            }
        }
        postDelayed(longPressRunnable, 500)
    }

    private fun removeLongPressCheck() {
        longPressRunnable?.let { removeCallbacks(it) }
        longPressRunnable = null
    }

    private fun distance(event: MotionEvent): Float {
        val dx = event.getX(0) - event.getX(1)
        val dy = event.getY(0) - event.getY(1)
        return Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
    }

    private fun switchRole(roleId: String) {
        if (roleManager.switchRole(roleId)) {
            currentRole = roleManager.currentRole
            petView.text = currentRole.avatar
            petView.scaleX = currentRole.defaultScale
            petView.scaleY = currentRole.defaultScale
        }
    }

    private fun showMenu() {
        val lp = menuView.layoutParams as LayoutParams
        lp.gravity = Gravity.START or Gravity.TOP
        lp.leftMargin = petView.width + dp(10)
        lp.topMargin = 0
        menuView.layoutParams = lp
        menuView.show()
    }

    private fun showControlMenu() {
        val lp = controlMenuView.layoutParams as LayoutParams
        lp.gravity = Gravity.START or Gravity.TOP
        lp.leftMargin = petView.width + dp(10)
        lp.topMargin = 0
        controlMenuView.layoutParams = lp
        controlMenuView.show()
    }

    private fun hideMenus() {
        menuView.hide()
        controlMenuView.hide()
    }

    private fun checkEdgeDock(x: Int, y: Int) {
        val screenWidth = ScreenUtils.getScreenWidth(context)
        val screenHeight = ScreenUtils.getScreenHeight(context)
        val edgeThreshold = dp(20)
        val closeToLeft = x < edgeThreshold
        val closeToRight = x > screenWidth - petView.width - edgeThreshold
        val closeToTop = y < edgeThreshold
        val closeToBottom = y > screenHeight - petView.height - edgeThreshold

        if (closeToLeft || closeToRight || closeToTop || closeToBottom) {
            edgeDockView.setVisible(true)
        } else {
            edgeDockView.setVisible(false)
        }
    }

    private fun snapToEdgeIfNeeded() {
        val screenWidth = ScreenUtils.getScreenWidth(context)
        val screenHeight = ScreenUtils.getScreenHeight(context)
        val edgeThreshold = dp(20)
        val x = params.x
        val y = params.y

        var newX = x
        var newY = y
        var snapped = false

        if (x < edgeThreshold) {
            newX = 0
            snapped = true
        } else if (x > screenWidth - petView.width - edgeThreshold) {
            newX = screenWidth - petView.width
            snapped = true
        }

        if (y < edgeThreshold) {
            newY = 0
            snapped = true
        } else if (y > screenHeight - petView.height - edgeThreshold) {
            newY = screenHeight - petView.height
            snapped = true
        }

        if (snapped) {
            params.x = newX
            params.y = newY
            windowManager.updateViewLayout(this@OverlayView, params)
            edgeDockView.setVisible(true)
        } else {
            edgeDockView.setVisible(false)
        }
    }

    private fun dp(value: Int): Int {
        return (value * context.resources.displayMetrics.density).toInt()
    }
}
