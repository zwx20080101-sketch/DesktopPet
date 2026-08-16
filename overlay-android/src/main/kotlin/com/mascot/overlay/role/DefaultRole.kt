package com.mascot.overlay.role

import com.mascot.overlay.reaction.JumpReaction

object DefaultRole {
    val instance = Role(
        id = "cat_paw",
        name = "默认猫爪",
        avatar = "🐾",
        defaultScale = 1.0f,
        minScale = 0.5f,
        maxScale = 2.0f,
        reaction = JumpReaction()
    )
}
