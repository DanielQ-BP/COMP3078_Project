package com.comp3074_101384549.projectui.model

/**
 * Data model representing a user, used for sending login/registration data
 * to the API, and potentially for fetching user profile data.
 */
data class User(
    val username: String,
    val email: String? = null, // Optional for login, but good for registration
    val password: String
)

