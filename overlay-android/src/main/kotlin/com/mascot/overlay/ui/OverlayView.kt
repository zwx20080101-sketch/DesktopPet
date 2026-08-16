package com.mascot.overlay.ui

import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView
import com.mascot.overlay.bridge.ServiceBridge
import com.mascot.overlay.interaction.ActionExecutor
import com.mascot.overlay.interaction.GestureDetector
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

    private val actionExecutor: ActionExecutor
    private val gestureDetector: GestureDetector

    private var scaleValue = 1f

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
            updateLockIndicator()
            hideMenus()
        }
        controlMenuView.setOnSettingsListener {
            bridge?.openMainApp()
            hideMenus()
        }
        controlMenuView.setOnCloseListener {
            bridge?.removeOverlay()
        }

        edgeDockView.setOnClickListener {
            unDockFromEdge()
        }

        // 先初始化 actionExecutor
        actionExecutor = ActionExecutor(
            onDrag = { dx, dy ->
                params.x += dx
                params.y += dy
                windowManager.updateViewLayout(this@OverlayView, params)
                checkEdgeDock(params.x, params.y)
            },
            onDragEnd = {
                snapToEdgeIfNeeded()
            },
            onPinch = { scale ->
                scaleValue = scale.coerceIn(0.2f, 10f)
                petView.scaleX = scaleValue
                petView.scaleY = scaleValue
            },
            onSingleTap = {
                currentRole.reaction.execute(petView)
                if (menuView.visibility == VISIBLE) hideMenus()
                else if (controlMenuView.visibility == VISIBLE) hideMenus()
            },
            onDoubleTap = {
                if (menuView.visibility == VISIBLE) hideMenus()
                else showMenu()
            },
            onLongPress = {
                if (controlMenuView.visibility == VISIBLE) hideMenus()
                else showControlMenu()
            }
        )

        // 然后初始化 gestureDetector
        gestureDetector = GestureDetector(object : GestureDetector.GestureListener {
            override fun onSingleTap() {
                actionExecutor.executeSingleTap()
            }

            override fun onDoubleTap() {
                actionExecutor.executeDoubleTap()
            }

            override fun onLongPress() {
                actionExecutor.executeLongPress()
            }

            override fun onDrag(dx: Int, dy: Int) {
                actionExecutor.executeDrag(dx, dy)
            }

            override fun onDragEnd() {
                actionExecutor.executeDragEnd()
            }

            override fun onPinch(scale: Float) {
                actionExecutor.executePinch(scale)
            }
        })

        setOnTouchListener { v, event ->
            gestureDetector.onTouch(v, event)
        }
    }

    private fun switchRole(roleId: String) {
        if (roleManager.switchRole(roleId)) {
            currentRole = roleManager.currentRole
            petView.text = currentRole.avatar
            petView.scaleX = currentRole.defaultScale
            petView.scaleY = currentRole.defaultScale
            scaleValue = currentRole.defaultScale
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

    private fun updateLockIndicator() {
        if (LockManager.isLocked()) {
            petView.alpha = 0.6f
        } else {
            petView.alpha = 1.0f
        }
    }

    private fun checkEdgeDock(x: Int, y: Int) {
        val screenWidth = ScreenUtils.getScreenWidth(context)
        val screenHeight = ScreenUtils.getScreenHeight(context)
        val edgeThreshold = dp(30)

        val closeToLeft = x < edgeThreshold
        val closeToRight = x > screenWidth - petView.width - edgeThreshold
        val closeToTop = y < edgeThreshold
        val closeToBottom = y > screenHeight - petView.height - edgeThreshold

        if (closeToLeft || closeToRight || closeToTop || closeToBottom) {
            edgeDockView.setVisible(true)
            edgeDockView.setTag("dock")
        } else {
            edgeDockView.setVisible(false)
        }
    }

    private fun snapToEdgeIfNeeded() {
        val screenWidth = ScreenUtils.getScreenWidth(context)
        val screenHeight = ScreenUtils.getScreenHeight(context)
        val edgeThreshold = dp(30)
        val x = params.x
        val y = params.y

        var newX = x
        var newY = y
        var docked = false

        if (x < edgeThreshold) {
            newX = 0
            docked = true
        } else if (x > screenWidth - petView.width - edgeThreshold) {
            newX = screenWidth - petView.width
            docked = true
        }

        if (y < edgeThreshold) {
            newY = 0
            docked = true
        } else if (y > screenHeight - petView.height - edgeThreshold) {
            newY = screenHeight - petView.height
            docked = true
        }

        if (docked) {
            params.x = newX
            params.y = newY
            windowManager.updateViewLayout(this@OverlayView, params)
            edgeDockView.setVisible(true)
            LockManager.setLocked(true)
            updateLockIndicator()
        } else {
            edgeDockView.setVisible(false)
        }
    }

    private fun unDockFromEdge() {
        val screenWidth = ScreenUtils.getScreenWidth(context)
        val screenHeight = ScreenUtils.getScreenHeight(context)
        val petWidth = petView.width
        val petHeight = petView.height

        if (params.x == 0) {
            params.x = dp(20)
        } else if (params.x == screenWidth - petWidth) {
            params.x = screenWidth - petWidth - dp(20)
        }

        if (params.y == 0) {
            params.y = dp(20)
        } else if (params.y == screenHeight - petHeight) {
            params.y = screenHeight - petHeight - dp(20)
        }

        windowManager.updateViewLayout(this@OverlayView, params)
        edgeDockView.setVisible(false)
        LockManager.setLocked(false)
        updateLockIndicator()
    }

    private fun dp(value: Int): Int {
        return (value * context.resources.displayMetrics.density).toInt()
    }
}
