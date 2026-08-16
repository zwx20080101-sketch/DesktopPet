package com.mascot.overlay.role

import com.mascot.overlay.reaction.JumpReaction

object BlueBallRole {
    val instance = Role(
        id = "blue_ball",
        name = "蓝球",
        avatar = "🔵",
        defaultScale = 1.0f,
        minScale = 0.5f,
        maxScale = 2.0f,
        reaction = JumpReaction()
    )
}
