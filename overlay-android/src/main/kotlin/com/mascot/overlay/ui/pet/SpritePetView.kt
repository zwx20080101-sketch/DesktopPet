package com.mascot.overlay.ui.pet

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import com.mascot.overlay.animation.SpriteSheetAnimator
import com.mascot.overlay.role.Role

class SpritePetView(val context: Context) : PetView {
    private val container = FrameLayout(context).apply {
        setBackgroundColor(Color.TRANSPARENT)
    }
    private val canvasView = object : View(context) {
        override fun onDraw(canvas: Canvas) {
            animator.draw(canvas)
        }
    }
    private val lockIndicator = TextView(context).apply {
        text = ""
        textSize = 14f
        gravity = Gravity.TOP or Gravity.END
        setBackgroundColor(Color.TRANSPARENT)
    }
    private val animator = SpriteSheetAnimator(context, "sprite_sheet.png", rows = 4, cols = 8)

    init {
        container.addView(canvasView, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))
        container.addView(lockIndicator, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.TOP or Gravity.END
        ))
        animator.attach(canvasView)
        animator.playRow(0) // 默认待机
    }

    override fun getView(): View = container

    override fun setRole(role: Role) {
        // 后续根据角色切换不同精灵图，现在共用一张
    }

    override fun setScale(scale: Float) {
        container.scaleX = scale
        container.scaleY = scale
    }

    override fun setLocked(locked: Boolean) {
        lockIndicator.text = if (locked) "🔒" else ""
        container.alpha = if (locked) 0.6f else 1.0f
    }

    fun playState(state: String) {
        when (state) {
            "idle" -> animator.playRow(0)
            "walk" -> animator.playRow(1)
            "jump" -> animator.playRow(2, loop = false)
            "sleep" -> animator.playRow(3)
            else -> animator.playRow(0)
        }
    }
}
