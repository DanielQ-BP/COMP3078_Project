package com.comp3074_101384549.projectui.repository

import com.comp3074_101384549.projectui.data.local.ListingDao
import com.comp3074_101384549.projectui.model.Listing
import com.comp3074_101384549.projectui.model.ListingEntity
import com.comp3074_101384549.projectui.data.remote.ApiService
import kotlinx.coroutines.flow.first

class ListingRepository(
    private val apiService: ApiService,
    private val listingDao: ListingDao
) {

    private fun List<ListingEntity>.toListingList(): List<Listing> {
        return this.map { entity ->
            Listing(
                id = entity.id,
                address = entity.address,
                pricePerHour = entity.pricePerHour,
                availability = entity.availability,
                description = entity.description,
                isActive = entity.isActive,
                latitude = entity.latitude,
                longitude = entity.longitude,
                userId = entity.userId
            )
        }
    }

    /**
     * Fetches all listings for a specific user (owner view).
     * API-first: syncs from backend to Room, falls back to cache on error.
     */
    suspend fun getAllListings(userId: String): List<Listing> {
        return try {
            val remoteListings = apiService.getUserListings(userId)
            listingDao.deleteAllByUserId(userId)
            listingDao.insertAll(remoteListings.map { it.toListingEntity() })
            remoteListings
        } catch (e: Exception) {
            listingDao.getAllListings(userId).first().toListingList()
        }
    }

    /**
     * Fetches all active listings regardless of owner (driver browse view).
     * API-first: syncs from backend to Room, falls back to cache on error.
     */
    suspend fun getAllActiveListings(): List<Listing> {
        return try {
            val remoteListings = apiService.getRemoteListings()
            listingDao.deleteAll()
            listingDao.insertAll(remoteListings.map { it.toListingEntity() })
            remoteListings
        } catch (e: Exception) {
            listingDao.getAllActiveListings().first().toListingList()
        }
    }

    /**
     * Searches listings for a specific user (owner view).
     * Searches the local Room cache.
     */
    suspend fun searchListings(userId: String, address: String = "", maxPrice: Double? = null): List<Listing> {
        val addressQuery = if (address.isBlank()) "%" else "%$address%"
        var results = listingDao.searchListings(userId, addressQuery).first().toListingList()
        if (maxPrice != null && maxPrice > 0) {
            results = results.filter { it.pricePerHour <= maxPrice }
        }
        return results
    }

    /**
     * Searches listings via backend API with full filters.
     * Falls back to local searchAllListings on error.
     */
    suspend fun searchListingsRemote(
        address: String? = null,
        minPrice: Double? = null,
        maxPrice: Double? = null,
        date: String? = null,
        sortBy: String? = null
    ): List<Listing> {
        return try {
            apiService.searchListings(
                address = if (address.isNullOrBlank()) null else address,
                minPrice = if (minPrice != null && minPrice > 0) minPrice else null,
                maxPrice = if (maxPrice != null && maxPrice > 0) maxPrice else null,
                date = if (date.isNullOrBlank()) null else date,
                sortBy = sortBy
            )
        } catch (e: Exception) {
            searchAllListings(address ?: "", maxPrice)
        }
    }

    /**
     * Searches all listings regardless of owner (driver browse view).
     * Searches the local Room cache.
     */
    suspend fun searchAllListings(address: String = "", maxPrice: Double? = null): List<Listing> {
        val addressQuery = if (address.isBlank()) "%" else "%$address%"
        var results = listingDao.searchAllListings(addressQuery).first().toListingList()
        if (maxPrice != null && maxPrice > 0) {
            results = results.filter { it.pricePerHour <= maxPrice }
        }
        return results
    }

    /**
     * Creates a new listing via API and saves to local cache.
     * Falls back to local-only if API is unavailable.
     */
    suspend fun saveNewListing(listing: Listing) {
        try {
            val savedListing = apiService.createListing(listing)
            listingDao.insert(savedListing.toListingEntity())
        } catch (e: Exception) {
            listingDao.insert(listing.toListingEntity())
        }
    }

    /**
     * Updates a listing via API and in local cache.
     */
    suspend fun updateListing(listing: Listing) {
        try {
            apiService.updateListing(listing.id, listing)
        } catch (e: Exception) {
            // Fall through to local update
        }
        listingDao.update(listing.toListingEntity())
    }

    /**
     * Deletes a listing via API and from local cache.
     */
    suspend fun deleteListing(listingId: String) {
        try {
            apiService.deleteListing(listingId)
        } catch (e: Exception) {
            // Fall through to local delete
        }
        listingDao.deleteById(listingId)
    }

    /**
     * Deletes all listings for a specific user from local cache and API.
     */
    suspend fun deleteAllListings(userId: String) {
        listingDao.deleteAllByUserId(userId)
    }

    /**
     * Gets a single listing by ID from local cache.
     */
    suspend fun getListingById(listingId: String): Listing? {
        val entity = listingDao.getListingById(listingId) ?: return null
        return Listing(
            id = entity.id,
            address = entity.address,
            pricePerHour = entity.pricePerHour,
            availability = entity.availability,
            description = entity.description,
            isActive = entity.isActive,
            latitude = entity.latitude,
            longitude = entity.longitude,
            userId = entity.userId
        )
    }
}
