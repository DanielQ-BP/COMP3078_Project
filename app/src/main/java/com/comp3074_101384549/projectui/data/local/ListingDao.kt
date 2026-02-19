package com.comp3074_101384549.projectui.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.comp3074_101384549.projectui.model.ListingEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for interacting with the local listing data.
 */
@Dao
interface ListingDao {

    @Query("SELECT * FROM listings WHERE user_id = :userId ORDER BY price_hour ASC")
    fun getAllListings(userId: String): Flow<List<ListingEntity>>

    @Query("SELECT * FROM listings WHERE user_id = :userId AND (address LIKE :query OR description LIKE :query OR availability LIKE :query) ORDER BY price_hour ASC")
    fun searchListings(userId: String, query: String): Flow<List<ListingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(listings: List<ListingEntity>)

    /**
     * Inserts a single ListingEntity into the database.
     * This is required by the ListingRepository.saveNewListing() function.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE) // <-- NEW FUNCTION ADDED
    suspend fun insert(listing: ListingEntity)

    @Query("DELETE FROM listings WHERE user_id = :userId")
    suspend fun deleteAllByUserId(userId: String)

    @Query("DELETE FROM listings")
    suspend fun deleteAll()
}