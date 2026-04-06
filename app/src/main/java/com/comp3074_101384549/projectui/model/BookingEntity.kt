package com.comp3074_101384549.projectui.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a booking/reservation in the local Room database.
 */
@Entity(tableName = "bookings")
data class BookingEntity(
    @PrimaryKey val id: String,

    @ColumnInfo(name = "listing_id") val listingId: String,
    @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "address") val address: String,
    @ColumnInfo(name = "price_per_hour") val pricePerHour: Double,
    @ColumnInfo(name = "booking_date") val bookingDate: String,
    @ColumnInfo(name = "start_time") val startTime: String,
    @ColumnInfo(name = "end_time") val endTime: String,
    @ColumnInfo(name = "total_price") val totalPrice: Double,
    @ColumnInfo(name = "status") val status: String = "confirmed",
    @ColumnInfo(name = "reference_code") val referenceCode: String = "",
    @ColumnInfo(name = "fine_amount") val fineAmount: Double = 0.0,
    @ColumnInfo(name = "fine_paid") val finePaid: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)
