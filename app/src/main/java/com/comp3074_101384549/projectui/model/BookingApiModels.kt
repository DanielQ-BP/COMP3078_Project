package com.comp3074_101384549.projectui.model

data class CreateBookingRequest(
    val listingId: String,
    val startTime: String,
    val endTime: String,
    val totalPrice: Double,
)

data class CreateBookingResponse(
    val id: String,
    val listingId: String,
    val userId: String,
    val startTime: String,
    val endTime: String,
    val totalPrice: Double,
    val status: String,
    val referenceCode: String,
)
