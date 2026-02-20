package com.comp3074_101384549.projectui.repository

import com.comp3074_101384549.projectui.data.local.BookingDao
import com.comp3074_101384549.projectui.model.BookingEntity
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class BookingRepository @Inject constructor(
    private val bookingDao: BookingDao
) {

    /**
     * Gets all bookings for a user.
     */
    suspend fun getAllBookings(userId: String): List<BookingEntity> {
        return bookingDao.getAllBookings(userId).first()
    }

    /**
     * Gets upcoming (confirmed) bookings for a user.
     */
    suspend fun getUpcomingBookings(userId: String): List<BookingEntity> {
        return bookingDao.getUpcomingBookings(userId).first()
    }

    /**
     * Gets past (completed) bookings for a user.
     */
    suspend fun getPastBookings(userId: String): List<BookingEntity> {
        return bookingDao.getPastBookings(userId).first()
    }

    /**
     * Creates a new booking.
     */
    suspend fun createBooking(booking: BookingEntity) {
        bookingDao.insert(booking)
    }

    /**
     * Updates the status of a booking.
     */
    suspend fun updateBookingStatus(bookingId: String, status: String) {
        bookingDao.updateStatus(bookingId, status)
    }

    /**
     * Cancels a booking.
     */
    suspend fun cancelBooking(bookingId: String) {
        bookingDao.updateStatus(bookingId, "cancelled")
    }

    /**
     * Deletes a booking.
     */
    suspend fun deleteBooking(bookingId: String) {
        bookingDao.deleteById(bookingId)
    }
}
