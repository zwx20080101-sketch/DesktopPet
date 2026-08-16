package com.mascot.overlay.role

data class Role(val id: String, val avatar: String, val name: String)

object RoleManager {
    val roles = listOf(
        Role("cat", "🐾", "猫爪"),
        Role("blue", "🔵", "蓝球")
    )
    var current: Role = roles[0]
        private set

    fun switch(id: String) {
        current = roles.find { it.id == id } ?: current
    }
}
