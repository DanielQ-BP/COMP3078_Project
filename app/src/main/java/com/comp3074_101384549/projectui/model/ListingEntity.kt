package com.comp3074_101384549.projectui.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents the structure of the 'listings' table in the local Room database.
 */
@Entity(tableName = "listings")
data class ListingEntity(
    @PrimaryKey val id: String,

    @ColumnInfo(name = "address") val address: String,
    @ColumnInfo(name = "price_hour") val pricePerHour: Double,
    @ColumnInfo(name = "availability") val availability: String,
    @ColumnInfo(name = "description") val description: String,
    @ColumnInfo(name = "is_active") val isActive: Boolean,
    @ColumnInfo(name = "latitude") val latitude: Double,
    @ColumnInfo(name = "longitude") val longitude: Double,
    @ColumnInfo(name = "user_id") val userId: String // Owner of the listing

)