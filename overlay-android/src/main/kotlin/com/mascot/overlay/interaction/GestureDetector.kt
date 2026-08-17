package com.mascot.overlay.interaction

import android.os.Handler
import android.os.Looper
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

    private val handler = Handler(Looper.getMainLooper())
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
    private var singleTapRunnable: Runnable? = null
    private var tapCount = 0
    private var longPressTriggered = false

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
                longPressTriggered = false
                startLongPressCheck()
                return true
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                if (event.pointerCount == 2) {
                    isPinching = true
                    startDistance = distance(event)
                    startScale = v.scaleX
                    removeLongPressCheck()
                    removeSingleTapCheck()
                }
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (isPinching && event.pointerCount == 2) {
                    val newDist = distance(event)
                    if (newDist > 0) {
                        listener.onPinch(newDist / startDistance * startScale)
                    }
                } else if (!isDragging && !isPinching && event.pointerCount == 1) {
                    val dx = event.rawX.toInt() - lastX
                    val dy = event.rawY.toInt() - lastY
                    if (abs(dx) > 10 || abs(dy) > 10) {
                        isDragging = true
                        removeLongPressCheck()
                        removeSingleTapCheck()
                        listener.onDrag(dx, dy)
                        lastX = event.rawX.toInt()
                        lastY = event.rawY.toInt()
                    }
                } else if (isDragging && event.pointerCount == 1) {
                    val dx = event.rawX.toInt() - lastX
                    val dy = event.rawY.toInt() - lastY
                    listener.onDrag(dx, dy)
                    lastX = event.rawX.toInt()
                    lastY = event.rawY.toInt()
                }
                return true
            }
            MotionEvent.ACTION_POINTER_UP -> {
                if (isPinching) {
                    isPinching = false
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                removeLongPressCheck()
                if (isDragging) {
                    isDragging = false
                    listener.onDragEnd()
                    return true
                }
                if (isPinching) {
                    isPinching = false
                    return true
                }
                if (longPressTriggered) {
                    return true
                }

                val now = System.currentTimeMillis()
                if (now - downTime < 300) {
                    tapCount++
                    if (tapCount == 1) {
                        singleTapRunnable = Runnable {
                            if (tapCount == 1) {
                                listener.onSingleTap()
                            }
                            tapCount = 0
                        }
                        handler.postDelayed(singleTapRunnable!!, 350)
                    } else if (tapCount >= 2) {
                        removeSingleTapCheck()
                        tapCount = 0
                        listener.onDoubleTap()
                    }
                } else {
                    listener.onLongPress()
                }
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                removeLongPressCheck()
                removeSingleTapCheck()
                isDragging = false
                isPinching = false
                tapCount = 0
                longPressTriggered = false
                return true
            }
        }
        return false
    }

    private fun startLongPressCheck() {
        removeLongPressCheck()
        longPressRunnable = Runnable {
            if (!isDragging && !isPinching) {
                longPressTriggered = true
                listener.onLongPress()
            }
        }
        handler.postDelayed(longPressRunnable!!, 500)
    }

    private fun removeLongPressCheck() {
        longPressRunnable?.let { handler.removeCallbacks(it) }
        longPressRunnable = null
    }

    private fun removeSingleTapCheck() {
        singleTapRunnable?.let { handler.removeCallbacks(it) }
        singleTapRunnable = null
    }

    private fun distance(e: MotionEvent): Float {
        val dx = e.getX(0) - e.getX(1)
        val dy = e.getY(0) - e.getY(1)
        return kotlin.math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
    }
}
