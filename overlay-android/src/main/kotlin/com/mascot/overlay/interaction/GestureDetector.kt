package com.mascot.overlay.interaction

import android.view.MotionEvent
import android.view.View
import kotlin.math.abs

class GestureDetector(private val listener: GestureListener) {
    interface GestureListener {
        fun onSingleTap()
        fun onDoubleTap()
        fun onLongPress()
        fun onDrag(dx: Int, dy: Int)
        fun onDragEnd()
        fun onPinch(scale: Float)
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
    private var longPressRunnable: Runnable? = null
    private var lastTapTime = 0L
    private var tapCount = 0

    fun onTouch(v: View, event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                startX = event.rawX.toInt(); startY = event.rawY.toInt()
                lastX = startX; lastY = startY
                downTime = System.currentTimeMillis()
                isDragging = false; isPinching = false
                startLongPressCheck(v)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (event.pointerCount == 1 && !isPinching) {
                    val dx = event.rawX.toInt() - lastX
                    val dy = event.rawY.toInt() - lastY
                    if (abs(dx) > 5 || abs(dy) > 5) {
                        isDragging = true
                        removeLongPressCheck()
                        listener.onDrag(dx, dy)
                        lastX = event.rawX.toInt(); lastY = event.rawY.toInt()
                    }
                } else if (event.pointerCount == 2) {
                    if (!isPinching) {
                        isPinching = true
                        startDistance = distance(event)
                        startScale = v.scaleX
                        removeLongPressCheck()
                    }
                    val newDist = distance(event)
                    if (newDist > 0) listener.onPinch(newDist / startDistance * startScale)
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                removeLongPressCheck()
                if (isPinching) { isPinching = false; return true }
                if (isDragging) { isDragging = false; listener.onDragEnd(); return true }

                val now = System.currentTimeMillis()
                if (now - downTime < 300) {
                    tapCount++
                    if (tapCount == 1) {
                        lastTapTime = now
                        v.postDelayed({
                            if (tapCount == 1) listener.onSingleTap()
                            tapCount = 0
                        }, 250)
                    } else if (tapCount >= 2) {
                        listener.onDoubleTap()
                        tapCount = 0
                    }
                } else {
                    listener.onLongPress()
                }
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                removeLongPressCheck()
                isDragging = false; isPinching = false; tapCount = 0
                return true
            }
        }
        return false
    }

    private fun startLongPressCheck(v: View) {
        removeLongPressCheck()
        longPressRunnable = Runnable { if (!isDragging && !isPinching) listener.onLongPress() }
        v.postDelayed(longPressRunnable, 500)
    }
    private fun removeLongPressCheck() { longPressRunnable?.let { }; longPressRunnable = null }
    private fun distance(e: MotionEvent): Float {
        val dx = e.getX(0) - e.getX(1)
        val dy = e.getY(0) - e.getY(1)
        return kotlin.math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
    }
}
