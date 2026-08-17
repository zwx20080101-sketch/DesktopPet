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
import com.mascot.overlay.util.ScreenUtils

class OverlayView(
    val ctx: Context,
    private val wm: WindowManager,
    private val params: WindowManager.LayoutParams,
    private val bridge: ServiceBridge?,
    private val onRequestDock: () -> Unit
) : FrameLayout(ctx) {

    private val petView: PetView = EmojiPetView(ctx)
    private val menuView: MenuView = RoleMenuView(ctx)
    private val controlMenuView: ControlMenuView = DefaultControlMenuView(ctx)

    private val actionExecutor: ActionExecutor
    private val gestureDetector: GestureDetector

    private var scaleValue = 1f
    private val baseWidth = 120.dp
    private val baseHeight = 120.dp

    init {
        addView(petView.getView(), LayoutParams(baseWidth, baseHeight))
        addView(menuView.getView(), LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        addView(controlMenuView.getView(), LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)

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

        actionExecutor = ActionExecutor(
            onDrag = { dx, dy ->
                params.x += dx
                params.y += dy
                wm.updateViewLayout(this, params)
            },
            onDragEnd = {
                // 如果宠物已经超出屏幕边界，触发停靠
                val sw = ScreenUtils.getScreenWidth(ctx)
                val sh = ScreenUtils.getScreenHeight(ctx)
                val outOfBounds =
                    params.x < 0 ||
                    params.x > sw - params.width ||
                    params.y < 0 ||
                    params.y > sh - params.height

                if (outOfBounds) {
                    onRequestDock()
                }
            },
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
