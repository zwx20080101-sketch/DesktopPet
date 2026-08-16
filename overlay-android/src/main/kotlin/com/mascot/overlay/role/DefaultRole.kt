package com.mascot.overlay.role

import com.mascot.overlay.reaction.JumpReaction

object DefaultRole {
    val instance = Role(
        id = "cat_paw",
        name = "默认猫爪",
        avatar = "🐾",
        defaultScale = 1.0f,
        reaction = JumpReaction()
    )
}
