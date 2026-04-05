package com.comp3074_101384549.projectui.model

data class AdminBookingRow(
    val id: String,
    val referenceCode: String? = null,
    val startTime: String,
    val endTime: String,
    val totalPrice: Double,
    val status: String,
    val listingAddress: String,
    val driverUsername: String,
    val driverId: String,
    val ownerUsername: String,
    val ownerId: String,
    val hasActiveConflict: Boolean = false,
)

data class AdminBookingDetailResponse(
    val booking: AdminBookingCore,
    val driver: AdminPartyDetail,
    val owner: AdminPartyDetail,
    val activeConflictTickets: List<AdminActiveTicket> = emptyList(),
)

data class AdminBookingCore(
    val id: String,
    val referenceCode: String? = null,
    val startTime: String,
    val endTime: String,
    val totalPrice: Double,
    val status: String,
    val listingAddress: String,
    val listingId: String,
    val disputeResolutionOutcome: String? = null,
    val disputeResolvedAt: String? = null,
)

data class AdminPartyDetail(
    val id: String,
    val username: String,
    val email: String? = null,
    val previousConflictCount: Int = 0,
    val conflicts: List<AdminConflictHistoryItem> = emptyList(),
)

data class AdminConflictHistoryItem(
    val id: String,
    val subject: String,
    val cause: String,
    val bookingReferenceCode: String? = null,
    val status: String,
    val createdAt: String,
)

data class AdminActiveTicket(
    val id: String,
    val subject: String,
    val description: String,
    val status: String,
    val createdAt: String,
)

data class AdminUserRow(
    val id: String,
    val username: String,
    val email: String? = null,
    val role: String,
    val conflictCount: Int = 0,
)

data class AdminUserDetailResponse(
    val user: AdminUserCore,
    val previousConflictCount: Int = 0,
    val conflicts: List<AdminConflictHistoryItem> = emptyList(),
)

data class AdminUserCore(
    val id: String,
    val username: String,
    val email: String? = null,
    val role: String,
    val createdAt: String? = null,
)
