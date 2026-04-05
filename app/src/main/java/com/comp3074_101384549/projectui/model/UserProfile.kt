package com.comp3074_101384549.projectui.model

data class UserProfile(
    val id: String,
    val username: String,
    val email: String? = null,
    val createdAt: String? = null
)
