package com.comp3074_101384549.projectui.model

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

fun CreateBookingResponse.toBookingEntity(): BookingEntity {
    val date = if (startTime.length >= 10) startTime.take(10) else startTime
    return BookingEntity(
        id = id,
        listingId = listingId,
        userId = userId,
        address = "",
        pricePerHour = 0.0,
        bookingDate = date,
        startTime = startTime,
        endTime = endTime,
        totalPrice = totalPrice,
        status = status,
        referenceCode = referenceCode
    )
}
