package com.mascot.overlay.interaction

import android.view.View
import com.mascot.overlay.lock.LockManager
import com.mascot.overlay.role.Role

class ActionExecutor(
    private val onDrag: (dx: Int, dy: Int) -> Unit,
    private val onDragEnd: () -> Unit,
    private val onPinch: (scale: Float) -> Unit,
    private val onSingleTap: () -> Unit,
    private val onDoubleTap: () -> Unit,
    private val onLongPress: () -> Unit
) {
    fun executeDrag(dx: Int, dy: Int) {
        if (!LockManager.isLocked()) {
            onDrag(dx, dy)
        }
    }

    fun executeDragEnd() {
        if (!LockManager.isLocked()) {
            onDragEnd()
        }
    }

    fun executePinch(scale: Float) {
        if (!LockManager.isLocked()) {
            onPinch(scale)
        }
    }

    fun executeSingleTap() {
        if (LockManager.isLocked()) return
        onSingleTap()
    }

    fun executeDoubleTap() {
        if (LockManager.isLocked()) return
        onDoubleTap()
    }

    fun executeLongPress() {
        onLongPress()
    }
}
