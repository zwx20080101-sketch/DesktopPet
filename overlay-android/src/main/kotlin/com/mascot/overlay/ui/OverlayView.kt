package com.mascot.overlay.ui

import android.content.Context
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.WindowManager
import android.widget.FrameLayout
import com.mascot.overlay.bridge.ServiceBridge
import com.mascot.overlay.lock.LockManager
import com.mascot.overlay.role.RoleManager
import com.mascot.overlay.service.PetAccessibilityService
import com.mascot.overlay.ui.control.DefaultControlMenuView
import com.mascot.overlay.ui.menu.RoleMenuView
import com.mascot.overlay.ui.pet.SpritePetView
import com.mascot.overlay.util.ScreenUtils

class OverlayView(
    val ctx: Context,
    private val wm: WindowManager,
    private val params: WindowManager.LayoutParams,
    private val bridge: ServiceBridge?,
    private val onRequestDock: () -> Unit
) : FrameLayout(ctx) {

    private val petView = SpritePetView(ctx)
    private val menuView = RoleMenuView(ctx)
    private val controlMenuView = DefaultControlMenuView(ctx)

    private val baseWidth = 120.dp
    private val baseHeight = 120.dp
    private var scaleValue = 1f
    private var isDocked = false

    // 拖动相关
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var isDragging = false
    private var startParamsX = 0
    private var startParamsY = 0

    // 系统手势识别
    private lateinit var gestureDetector: GestureDetector
    private lateinit var scaleGestureDetector: ScaleGestureDetector

    init {
        addView(petView.getView(), LayoutParams(baseWidth, baseHeight))
        addView(menuView.getView(), LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        addView(controlMenuView.getView(), LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)

        petView.setRole(RoleManager.current)
        petView.setScale(scaleValue)
        petView.playState("idle")

        menuView.setRoles(RoleManager.roles)
        menuView.setOnRoleSelectedListener { role ->
            RoleManager.switch(role.id)
            petView.setRole(RoleManager.current)
            menuView.hide()
        }

        controlMenuView.setOnLockToggleListener {
            LockManager.toggle()
            petView.setLocked(LockManager.isLocked())
            controlMenuView.hide()
        }
        controlMenuView.setOnSettingsListener {
            bridge?.openMainApp()
            controlMenuView.hide()
        }
        controlMenuView.setOnCloseListener {
            PetAccessibilityService.instance?.removePet()
        }

        // 单击、双击、长按
        gestureDetector = GestureDetector(ctx, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                if (isDocked) {
                    undock()
                } else {
                    petView.playState("jump")
                    hideMenus()
                }
                return true
            }

            override fun onDoubleTap(e: MotionEvent): Boolean {
                if (menuView.isVisible()) menuView.hide() else showMenu()
                return true
            }

            override fun onLongPress(e: MotionEvent) {
                if (controlMenuView.isVisible()) controlMenuView.hide() else showControlMenu()
            }
        })

        // 缩放手势
        scaleGestureDetector = ScaleGestureDetector(ctx, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val factor = detector.scaleFactor
                scaleValue = (scaleValue * factor).coerceIn(0.5f, 3.0f)
                params.width = (baseWidth * scaleValue).toInt()
                params.height = (baseHeight * scaleValue).toInt()
                wm.updateViewLayout(this@OverlayView, params)
                petView.setScale(1f)
                return true
            }
        })

        setOnTouchListener { _, event ->
            scaleGestureDetector.onTouchEvent(event)
            if (!scaleGestureDetector.isInProgress) {
                gestureDetector.onTouchEvent(event)
            }

            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    if (!LockManager.isLocked() && !isDocked) {
                        isDragging = false
                        lastTouchX = event.rawX
                        lastTouchY = event.rawY
                        startParamsX = params.x
                        startParamsY = params.y
                    }
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (!LockManager.isLocked() && !isDocked && event.pointerCount == 1) {
                        val dx = event.rawX - lastTouchX
                        val dy = event.rawY - lastTouchY
                        if (Math.abs(dx) > 5 || Math.abs(dy) > 5) {
                            isDragging = true
                            params.x = startParamsX + dx.toInt()
                            params.y = startParamsY + dy.toInt()
                            wm.updateViewLayout(this@OverlayView, params)
                        }
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (isDragging) {
                        isDragging = false
                        checkDock()
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun checkDock() {
        val sw = ScreenUtils.getScreenWidth(ctx)
        val sh = ScreenUtils.getScreenHeight(ctx)
        val edgeThreshold = 20.dp
        val nearLeft = params.x < edgeThreshold
        val nearRight = params.x > sw - params.width - edgeThreshold
        val nearTop = params.y < edgeThreshold
        val nearBottom = params.y > sh - params.height - edgeThreshold

        if (nearLeft || nearRight || nearTop || nearBottom) {
            dock()
        }
    }

    private fun dock() {
        val sw = ScreenUtils.getScreenWidth(ctx)
        val sh = ScreenUtils.getScreenHeight(ctx)
        when {
            params.x < 20.dp -> params.x = 0
            params.x > sw - params.width - 20.dp -> params.x = sw - params.width
            params.y < 20.dp -> params.y = 0
            params.y > sh - params.height - 20.dp -> params.y = sh - params.height
        }
        wm.updateViewLayout(this, params)
        isDocked = true
        LockManager.setLocked(true)
        petView.setLocked(true)
        petView.playState("sleep")
    }

    private fun undock() {
        isDocked = false
        LockManager.setLocked(false)
        petView.setLocked(false)
        petView.playState("idle")
    }

    private fun showMenu() {
        val lp = menuView.getView().layoutParams as LayoutParams
        lp.gravity = Gravity.START or Gravity.TOP
        lp.leftMargin = baseWidth + 10.dp
        lp.topMargin = 0
        menuView.getView().layoutParams = lp
        menuView.show()
    }

    private fun showControlMenu() {
        val lp = controlMenuView.getView().layoutParams as LayoutParams
        lp.gravity = Gravity.START or Gravity.TOP
        lp.leftMargin = baseWidth + 10.dp
        lp.topMargin = 0
        controlMenuView.getView().layoutParams = lp
        controlMenuView.show()
    }

    private fun hideMenus() {
        menuView.hide()
        controlMenuView.hide()
    }

    private val Int.dp: Int get() = (this * resources.displayMetrics.density).toInt()
}
