package com.mascot.overlay.lock

object LockManager {
    var state: LockState = LockState.UNLOCKED
        private set

    fun toggle() {
        state = if (state == LockState.LOCKED) LockState.UNLOCKED else LockState.LOCKED
    }

    fun isLocked(): Boolean = state == LockState.LOCKED
}
