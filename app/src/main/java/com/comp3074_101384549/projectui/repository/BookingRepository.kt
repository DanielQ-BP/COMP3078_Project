package com.comp3074_101384549.projectui.repository

import com.comp3074_101384549.projectui.data.local.BookingDao
import com.comp3074_101384549.projectui.data.remote.ApiService
import com.comp3074_101384549.projectui.model.BookingEntity
import com.comp3074_101384549.projectui.model.CreateBookingRequest
import com.comp3074_101384549.projectui.model.UpdateBookingStatusRequest
import kotlinx.coroutines.flow.first
import retrofit2.HttpException

class BookingRepository(
    private val apiService: ApiService,
    private val bookingDao: BookingDao
) {

    /**
     * Gets all bookings for a user.
     * API-first: syncs from backend to Room, falls back to cache on error.
     */
    suspend fun getAllBookings(userId: String): List<BookingEntity> {
        return try {
            val remoteBookings = apiService.getUserBookings(userId)
            bookingDao.deleteAllByUserId(userId)
            val entities = remoteBookings.map { it.toBookingEntity() }
            entities.forEach { bookingDao.insert(it) }
            entities
        } catch (e: Exception) {
            bookingDao.getAllBookings(userId).first()
        }
    }

    /**
     * Gets upcoming (confirmed/pending) bookings for a user.
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
     * Creates a new booking via API and saves to local cache.
     * Combines bookingDate + startTime/endTime into ISO timestamps if needed.
     */
    suspend fun createBooking(booking: BookingEntity): BookingEntity {
        return try {
            // Convert "HH:mm" + date to full ISO timestamp if not already ISO
            val isoStart = if (!booking.startTime.contains("T") && booking.bookingDate.isNotEmpty()) {
                "${booking.bookingDate}T${booking.startTime}:00"
            } else booking.startTime
            val isoEnd = if (!booking.endTime.contains("T") && booking.bookingDate.isNotEmpty()) {
                "${booking.bookingDate}T${booking.endTime}:00"
            } else booking.endTime
            val request = CreateBookingRequest(
                listingId = booking.listingId,
                startTime = isoStart,
                endTime = isoEnd,
                totalPrice = booking.totalPrice
            )
            val remote = apiService.createBooking(request)
            val entity = remote.toBookingEntity().copy(
                address = booking.address,
                pricePerHour = booking.pricePerHour
            )
            bookingDao.insert(entity)
            entity
        } catch (e: HttpException) {
            // Re-throw HTTP errors (e.g. 409 conflict) so the UI can show the right message
            throw e
        } catch (e: Exception) {
            // Network unavailable — save locally as fallback
            bookingDao.insert(booking)
            booking
        }
    }

    /**
     * Cancels a booking via API and updates local cache.
     */
    suspend fun cancelBooking(bookingId: String) {
        try {
            apiService.updateBookingStatus(bookingId, UpdateBookingStatusRequest("cancelled"))
        } catch (e: Exception) {
            // Fall through to local update
        }
        bookingDao.updateStatus(bookingId, "cancelled")
    }

    /**
     * Updates the status of a booking.
     */
    suspend fun updateBookingStatus(bookingId: String, status: String) {
        try {
            apiService.updateBookingStatus(bookingId, UpdateBookingStatusRequest(status))
        } catch (e: Exception) {
            // Fall through to local update
        }
        bookingDao.updateStatus(bookingId, status)
    }

    /**
     * Deletes a booking from API and local cache.
     */
    suspend fun deleteBooking(bookingId: String) {
        try {
            apiService.deleteBooking(bookingId)
        } catch (e: Exception) {
            // Fall through to local delete
        }
        bookingDao.deleteById(bookingId)
    }
}
