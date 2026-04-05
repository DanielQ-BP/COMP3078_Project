package com.comp3074_101384549.projectui.model

data class Booking(
    val id: String,
    val listingId: String,
    val userId: String,
    val startTime: String,
    val endTime: String,
    val totalPrice: Double,
    val status: String,
    val address: String = "",
    val pricePerHour: Double = 0.0,
    val referenceCode: String = ""
) {
    fun toBookingEntity(): BookingEntity {
        val date = if (startTime.length >= 10) startTime.take(10) else startTime
        return BookingEntity(
            id = id,
            listingId = listingId,
            userId = userId,
            address = address,
            pricePerHour = pricePerHour,
            bookingDate = date,
            startTime = startTime,
            endTime = endTime,
            totalPrice = totalPrice,
            status = status,
            referenceCode = referenceCode,
            createdAt = System.currentTimeMillis()
        )
    }
}
