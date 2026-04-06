package com.comp3074_101384549.projectui.model

data class PaymentIntentRequest(
    val amount: Int,
    val currency: String
)

data class PaymentIntentResponse(
    val clientSecret: String
)

data class CreatePaymentRequest(val bookingId: String, val amount: Double, val paymentMethod: String)
data class CreatePaymentResponse(val id: String, val bookingId: String, val userId: String, val amount: Double, val paymentMethod: String, val status: String, val createdAt: String)