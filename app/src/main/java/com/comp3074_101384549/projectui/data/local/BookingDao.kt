package com.comp3074_101384549.projectui.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.comp3074_101384549.projectui.model.BookingEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for booking/reservation data.
 */
@Dao
interface BookingDao {

    @Query("SELECT * FROM bookings WHERE user_id = :userId ORDER BY created_at DESC")
    fun getAllBookings(userId: String): Flow<List<BookingEntity>>

    @Query("SELECT * FROM bookings WHERE user_id = :userId AND status = 'confirmed' ORDER BY booking_date ASC")
    fun getUpcomingBookings(userId: String): Flow<List<BookingEntity>>

    @Query("SELECT * FROM bookings WHERE user_id = :userId AND status = 'completed' ORDER BY created_at DESC")
    fun getPastBookings(userId: String): Flow<List<BookingEntity>>

    @Query("SELECT * FROM bookings WHERE id = :bookingId")
    suspend fun getBookingById(bookingId: String): BookingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(booking: BookingEntity)

    @Query("UPDATE bookings SET status = :status WHERE id = :bookingId")
    suspend fun updateStatus(bookingId: String, status: String)

    @Query("DELETE FROM bookings WHERE id = :bookingId")
    suspend fun deleteById(bookingId: String)

    @Query("DELETE FROM bookings WHERE user_id = :userId")
    suspend fun deleteAllByUserId(userId: String)
}
