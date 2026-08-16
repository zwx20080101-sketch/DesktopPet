package com.mascot.overlay.role

class RoleManager {
    private val roles = listOf(DefaultRole.instance, BlueBallRole.instance)
    var currentRole: Role = DefaultRole.instance
        private set

    fun getRoles(): List<Role> = roles

    fun switchRole(roleId: String): Boolean {
        val role = roles.find { it.id == roleId } ?: return false
        currentRole = role
        return true
    }
}
