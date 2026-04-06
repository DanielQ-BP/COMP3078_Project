package com.comp3074_101384549.projectui.model

data class Booking(
    val id: String,
    val listingId: String,
    val userId: String,
    val startTime: String,
    val endTime: String,
    val totalPrice: Double,
    val status: String,
    val referenceCode: String = "",
    val address: String = "",
    val pricePerHour: Double = 0.0,
<<<<<<< HEAD
    val fineAmount: Double = 0.0,
    val finePaid: Boolean = false
=======
    val referenceCode: String = ""
>>>>>>> 7d11e11e8bd61c43c087a6873997832dc73793a3
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
            fineAmount = fineAmount,
            finePaid = finePaid,
            createdAt = System.currentTimeMillis()
        )
    }
}
