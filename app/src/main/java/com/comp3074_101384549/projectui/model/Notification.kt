package com.comp3074_101384549.projectui.model

data class Notification(
    val id: String,
    val userId: String,
    val title: String,
    val message: String,
    val isRead: Boolean,
    val createdAt: String? = null
)
