package com.comp3074_101384549.projectui.model

data class PaymentIntentRequest(
    val amount: Int,
    val currency: String
)

data class PaymentIntentResponse(
    val clientSecret: String
)