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

    fun onTouch(v: View, event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                startX = event.rawX.toInt()
                startY = event.rawY.toInt()
                lastX = startX
                lastY = startY
                downTime = System.currentTimeMillis()
                isDragging = false
                isPinching = false
                startLongPressCheck(v)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (event.pointerCount == 1 && !isPinching) {
                    val deltaX = event.rawX.toInt() - lastX
                    val deltaY = event.rawY.toInt() - lastY
                    if (abs(deltaX) > 5 || abs(deltaY) > 5) {
                        isDragging = true
                        listener.onDrag(deltaX, deltaY)
                        lastX = event.rawX.toInt()
                        lastY = event.rawY.toInt()
                    }
                } else if (event.pointerCount == 2) {
                    if (!isPinching) {
                        isPinching = true
                        startDistance = distance(event)
                        startScale = v.scaleX
                        removeLongPressCheck()
                    }
                    val newDistance = distance(event)
                    val scale = newDistance / startDistance * startScale
                    listener.onPinch(scale)
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                removeLongPressCheck()
                if (isPinching) {
                    isPinching = false
                } else if (isDragging) {
                    isDragging = false
                    // 拖动结束处理
                    listener.onDragEnd()
                } else {
                    val time = System.currentTimeMillis() - downTime
                    if (time < 200) {
                        // 单击
                        listener.onSingleTap()
                    } else {
                        // 双击检测：简单的连续两次点击
                        if (lastTapTime > 0 && System.currentTimeMillis() - lastTapTime < 300) {
                            listener.onDoubleTap()
                            lastTapTime = 0
                        } else {
                            lastTapTime = System.currentTimeMillis()
                            listener.onSingleTap()
                        }
                    }
                }
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                removeLongPressCheck()
                isDragging = false
                isPinching = false
                return true
            }
        }
        return false
    }

    private var lastTapTime = 0L

    private fun startLongPressCheck(v: View) {
        removeLongPressCheck()
        longPressRunnable = Runnable {
            if (!isDragging && !isPinching) {
                listener.onLongPress()
            }
        }
        v.postDelayed(longPressRunnable, 500)
    }

    private fun removeLongPressCheck() {
        longPressRunnable?.let { runnable ->
            runnable.let { }
        }
        longPressRunnable = null
    }

    private fun distance(event: MotionEvent): Float {
        val dx = event.getX(0) - event.getX(1)
        val dy = event.getY(0) - event.getY(1)
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }
}
