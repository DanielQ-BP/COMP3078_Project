package com.comp3074_101384549.projectui.model

data class CreateBookingRequest(
    val listingId: String,
    val startTime: String,
    val endTime: String,
    val totalPrice: Double
)
