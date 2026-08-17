package com.mascot.overlay.ui

import android.content.Context
import android.view.Gravity
import android.view.WindowManager
import android.widget.FrameLayout
import com.mascot.overlay.bridge.ServiceBridge
import com.mascot.overlay.interaction.ActionExecutor
import com.mascot.overlay.interaction.GestureDetector
import com.mascot.overlay.lock.LockManager
import com.mascot.overlay.role.RoleManager
import com.mascot.overlay.service.PetAccessibilityService
import com.mascot.overlay.ui.pet.SpritePetView
import com.mascot.overlay.ui.pet.PetView
import com.mascot.overlay.ui.menu.MenuView
import com.mascot.overlay.ui.menu.RoleMenuView
import com.mascot.overlay.ui.control.ControlMenuView
import com.mascot.overlay.ui.control.DefaultControlMenuView
import com.mascot.overlay.util.ScreenUtils

class OverlayView(
    val ctx: Context,
    private val wm: WindowManager,
    private val params: WindowManager.LayoutParams,
    private val bridge: ServiceBridge?,
    private val onRequestDock: () -> Unit
) : FrameLayout(ctx) {

    private val petView: PetView = SpritePetView(ctx)
    private val menuView: MenuView = RoleMenuView(ctx)
    private val controlMenuView: ControlMenuView = DefaultControlMenuView(ctx)

    private val actionExecutor: ActionExecutor
    private val gestureDetector: GestureDetector

    private var scaleValue = 1f
    private val baseWidth = 120.dp
    private val baseHeight = 120.dp
    private var isDocked = false

    init {
        addView(petView.getView(), LayoutParams(baseWidth, baseHeight))
        addView(menuView.getView(), LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        addView(controlMenuView.getView(), LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)

        petView.setRole(RoleManager.current)
        petView.setScale(scaleValue)
        (petView as SpritePetView).playState("idle")

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

        actionExecutor = ActionExecutor(
            onDrag = { dx, dy ->
                if (!isDocked && !LockManager.isLocked()) {
                    params.x += dx
                    params.y += dy
                    wm.updateViewLayout(this, params)
                }
            },
            onDragEnd = {
                if (!isDocked && !LockManager.isLocked()) {
                    val sw = ScreenUtils.getScreenWidth(ctx)
                    val sh = ScreenUtils.getScreenHeight(ctx)
                    val nearEdge =
                        params.x < 20.dp ||
                        params.x > sw - params.width - 20.dp ||
                        params.y < 20.dp ||
                        params.y > sh - params.height - 20.dp
                    if (nearEdge) {
                        dockToEdge()
                    }
                }
            },
            onPinch = { scale ->
                if (!isDocked && !LockManager.isLocked()) {
                    val newScale = scale.coerceIn(0.5f, 3.0f)
                    scaleValue = newScale
                    params.width = (baseWidth * newScale).toInt()
                    params.height = (baseHeight * newScale).toInt()
                    wm.updateViewLayout(this, params)
                    petView.setScale(1f)
                }
            },
            onSingleTap = {
                if (isDocked) {
                    // 点击边缘停靠宠物，唤醒并解锁
                    undock()
                } else if (!LockManager.isLocked()) {
                    (petView as SpritePetView).playState("jump")
                    hideMenusIfVisible()
                }
            },
            onDoubleTap = {
                if (!isDocked && !LockManager.isLocked()) {
                    if (menuView.isVisible()) menuView.hide() else showMenu()
                }
            },
            onLongPress = {
                if (!isDocked && !LockManager.isLocked()) {
                    if (controlMenuView.isVisible()) controlMenuView.hide() else showControlMenu()
                }
            }
        )

        gestureDetector = GestureDetector(object : GestureDetector.GestureListener {
            override fun onSingleTap() = actionExecutor.executeSingleTap()
            override fun onDoubleTap() = actionExecutor.executeDoubleTap()
            override fun onLongPress() = actionExecutor.executeLongPress()
            override fun onDrag(dx: Int, dy: Int) = actionExecutor.executeDrag(dx, dy)
            override fun onDragEnd() = actionExecutor.executeDragEnd()
            override fun onPinch(scale: Float) = actionExecutor.executePinch(scale)
        })

        setOnTouchListener { _, event -> gestureDetector.onTouch(this, event) }
    }

    private fun dockToEdge() {
        val sw = ScreenUtils.getScreenWidth(ctx)
        val sh = ScreenUtils.getScreenHeight(ctx)
        // 将宠物吸附到最近边缘，完全在屏幕内
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
        (petView as SpritePetView).playState("sleep")
    }

    private fun undock() {
        isDocked = false
        LockManager.setLocked(false)
        petView.setLocked(false)
        (petView as SpritePetView).playState("idle")
    }

    fun setLocked(locked: Boolean) {
        petView.setLocked(locked)
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

    private fun hideMenusIfVisible() {
        if (menuView.isVisible()) menuView.hide()
        if (controlMenuView.isVisible()) controlMenuView.hide()
    }

    private val Int.dp: Int get() = (this * resources.displayMetrics.density).toInt()
}
