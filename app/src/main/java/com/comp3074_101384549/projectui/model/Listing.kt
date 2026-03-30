package com.comp3074_101384549.projectui.model

data class Listing(
    val id: String,
    val pricePerHour: Double,
    val availability: String,
    val description: String,
    val isActive: Boolean,
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val userId: String // Owner of the listing
){
    fun toListingEntity(): ListingEntity {
        return ListingEntity(
            id = this.id,
            address = this.address,
            pricePerHour = this.pricePerHour,
            availability = this.availability,
            description = this.description,
            isActive = this.isActive,
            latitude = this.latitude,
            longitude = this.longitude,
            userId = this.userId
            )
    }
}
