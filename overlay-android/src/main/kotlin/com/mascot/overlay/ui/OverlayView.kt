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
import com.mascot.overlay.ui.pet.EmojiPetView
import com.mascot.overlay.ui.pet.PetView
import com.mascot.overlay.ui.menu.MenuView
import com.mascot.overlay.ui.menu.RoleMenuView
import com.mascot.overlay.ui.control.ControlMenuView
import com.mascot.overlay.ui.control.DefaultControlMenuView
import com.mascot.overlay.ui.edge.EdgeDockView
import com.mascot.overlay.ui.edge.DefaultEdgeDockView
import com.mascot.overlay.util.ScreenUtils

class OverlayView(
    val context: Context,
    private val wm: WindowManager,
    private val params: WindowManager.LayoutParams,
    private val bridge: ServiceBridge?
) : FrameLayout(context) {

    private val petView: PetView = EmojiPetView(context)
    private val menuView: MenuView = RoleMenuView(context)
    private val controlMenuView: ControlMenuView = DefaultControlMenuView(context)
    private val edgeDockView: EdgeDockView = DefaultEdgeDockView(context)

    private val actionExecutor: ActionExecutor
    private val gestureDetector: GestureDetector

    private var scaleValue = 1f
    private val baseWidth = 120.dp
    private val baseHeight = 120.dp

    init {
        addView(petView.getView(), LayoutParams(baseWidth, baseHeight))
        addView(menuView.getView(), LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        addView(controlMenuView.getView(), LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        addView(edgeDockView.getView(), LayoutParams(20.dp, 60.dp))

        petView.setRole(RoleManager.current)
        petView.setScale(scaleValue)

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

        edgeDockView.setOnClickListener { unDockFromEdge() }

        actionExecutor = ActionExecutor(
            onDrag = { dx, dy ->
                params.x += dx
                params.y += dy
                wm.updateViewLayout(this, params)
                checkEdgeDock()
            },
            onDragEnd = { snapToEdgeIfNeeded() },
            onPinch = { scale ->
                val newScale = scale.coerceIn(0.3f, 5.0f)
                scaleValue = newScale
                params.width = (baseWidth * newScale).toInt()
                params.height = (baseHeight * newScale).toInt()
                wm.updateViewLayout(this, params)
                petView.setScale(1f)
            },
            onSingleTap = {
                petView.getView().animate()
                    .scaleX(1.2f).scaleY(1.2f)
                    .setDuration(100)
                    .withEndAction {
                        petView.getView().animate()
                            .scaleX(1f).scaleY(1f)
                            .setDuration(100).start()
                    }
                    .start()
                hideMenusIfVisible()
            },
            onDoubleTap = {
                if (menuView.isVisible()) menuView.hide() else showMenu()
            },
            onLongPress = {
                if (controlMenuView.isVisible()) controlMenuView.hide() else showControlMenu()
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

    private fun checkEdgeDock() {
        val sw = ScreenUtils.getScreenWidth(context)
        val sh = ScreenUtils.getScreenHeight(context)
        val threshold = 40.dp
        val close = params.x < threshold || params.x > sw - params.width - threshold ||
                params.y < threshold || params.y > sh - params.height - threshold

        if (close) {
            edgeDockView.show()
            when {
                params.x < threshold -> edgeDockView.setPosition(0, params.y + params.height / 2 - 30.dp)
                params.x > sw - params.width - threshold -> edgeDockView.setPosition(sw - 20.dp, params.y + params.height / 2 - 30.dp)
                params.y < threshold -> edgeDockView.setPosition(params.x + params.width / 2 - 10.dp, 0)
                else -> edgeDockView.setPosition(params.x + params.width / 2 - 10.dp, sh - 60.dp)
            }
        } else {
            edgeDockView.hide()
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
            edgeDockView.show()
            LockManager.setLocked(true)
            petView.setLocked(true)
        } else {
            edgeDockView.hide()
        }
    }

    private fun unDockFromEdge() {
        val sw = ScreenUtils.getScreenWidth(context)
        val sh = ScreenUtils.getScreenHeight(context)
        if (params.x < 0) params.x = 20.dp
        else if (params.x > sw - params.width) params.x = sw - params.width - 20.dp
        if (params.y < 0) params.y = 20.dp
        else if (params.y > sh - params.height) params.y = sh - params.height - 20.dp

        wm.updateViewLayout(this, params)
        edgeDockView.hide()
        LockManager.setLocked(false)
        petView.setLocked(false)
    }

    private val Int.dp: Int get() = (this * resources.displayMetrics.density).toInt()
}
