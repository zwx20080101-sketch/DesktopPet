package com.mascot.overlay.role

import com.mascot.overlay.reaction.Reaction

data class Role(
    val id: String,
    val name: String,
    val avatar: String,
    val defaultScale: Float,
    val reaction: Reaction
)
