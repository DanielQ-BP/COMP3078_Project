package com.comp3074_101384549.projectui.model

data class EarningsListing(
    val listingId: String,
    val address: String,
    val earnings: Double,
    val fineEarnings: Double,
    val bookingCount: Int
)

data class OwnerEarningsResponse(
    val totalEarnings: Double,
    val totalFineEarnings: Double,
    val totalBookings: Int,
    val listings: List<EarningsListing>
)
