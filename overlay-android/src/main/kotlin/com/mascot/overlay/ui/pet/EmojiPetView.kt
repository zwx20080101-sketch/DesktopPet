package com.mascot.overlay.ui.pet

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import com.mascot.overlay.role.Role

class EmojiPetView(val context: Context) : PetView {
    private val container = FrameLayout(context).apply {
        setBackgroundColor(Color.TRANSPARENT)
    }
    private val textView = TextView(context).apply {
        textSize = 48f
        gravity = Gravity.CENTER
        setBackgroundColor(Color.TRANSPARENT)
    }
    private val lockIndicator = TextView(context).apply {
        text = ""
        textSize = 14f
        gravity = Gravity.TOP or Gravity.END
        setBackgroundColor(Color.TRANSPARENT)
    }

    init {
        container.addView(textView, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))
        container.addView(lockIndicator, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.TOP or Gravity.END
        ))
    }

    override fun getView(): View = container
    override fun setRole(role: Role) { textView.text = role.avatar }
    override fun setScale(scale: Float) {
        container.scaleX = scale
        container.scaleY = scale
    }
    override fun setLocked(locked: Boolean) {
        lockIndicator.text = if (locked) "🔒" else ""
        textView.alpha = if (locked) 0.6f else 1f
    }
}
