package com.comp3074_101384549.projectui.data.remote

import com.comp3074_101384549.projectui.model.AdminLoginRequest
import com.comp3074_101384549.projectui.model.Booking
import com.comp3074_101384549.projectui.model.CreateBookingRequest
import com.comp3074_101384549.projectui.model.CreateTicketRequest
import com.comp3074_101384549.projectui.model.Listing
import com.comp3074_101384549.projectui.model.MessageResponse
import com.comp3074_101384549.projectui.model.PaymentIntentRequest
import com.comp3074_101384549.projectui.model.PaymentIntentResponse
import com.comp3074_101384549.projectui.model.Ticket
import com.comp3074_101384549.projectui.model.TicketResponse
import com.comp3074_101384549.projectui.model.TicketRespondRequest
import com.comp3074_101384549.projectui.model.UpdateTicketStatusRequest
import com.comp3074_101384549.projectui.model.Notification
import com.comp3074_101384549.projectui.model.UpdateBookingStatusRequest
import com.comp3074_101384549.projectui.model.UpdateUserRequest
import com.comp3074_101384549.projectui.model.User
import com.comp3074_101384549.projectui.model.UserProfile
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface ApiService {

    // ── Auth ────────────────────────────────────────────────────────────────

    @POST("auth/register")
    suspend fun register(@Body user: User): String

    @POST("auth/login")
    suspend fun login(@Body user: User): String

    @POST("auth/admin/login")
    suspend fun adminLogin(@Body body: AdminLoginRequest): String

    // ── Listings ────────────────────────────────────────────────────────────

    @GET("listings/all")
    suspend fun getRemoteListings(): List<Listing>

    @GET("listings/user/{userId}")
    suspend fun getUserListings(@Path("userId") userId: String): List<Listing>

    @POST("listings/create")
    suspend fun createListing(@Body listing: Listing): Listing

    @PUT("listings/{id}")
    suspend fun updateListing(@Path("id") id: String, @Body listing: Listing): Listing

    @DELETE("listings/{id}")
    suspend fun deleteListing(@Path("id") id: String): MessageResponse

    // ── Bookings ────────────────────────────────────────────────────────────

    @GET("bookings/user/{userId}")
    suspend fun getUserBookings(@Path("userId") userId: String): List<Booking>

    @POST("bookings/create")
    suspend fun createBooking(@Body request: CreateBookingRequest): Booking

    @PUT("bookings/{id}")
    suspend fun updateBookingStatus(
        @Path("id") id: String,
        @Body request: UpdateBookingStatusRequest
    ): Booking

    @DELETE("bookings/{id}")
    suspend fun deleteBooking(@Path("id") id: String): MessageResponse

    // ── Users ───────────────────────────────────────────────────────────────

    @GET("users/{id}")
    suspend fun getUserProfile(@Path("id") userId: String): UserProfile

    @PUT("users/{id}")
    suspend fun updateUserProfile(
        @Path("id") userId: String,
        @Body request: UpdateUserRequest
    ): UserProfile

    // ── Notifications ───────────────────────────────────────────────────────

    @GET("notifications/user/{userId}")
    suspend fun getUserNotifications(@Path("userId") userId: String): List<Notification>

    @PUT("notifications/{id}/read")
    suspend fun markNotificationRead(@Path("id") id: String): Notification

    @DELETE("notifications/{id}")
    suspend fun deleteNotification(@Path("id") id: String): MessageResponse

    // ── Payments ────────────────────────────────────────────────────────────

    @POST("payments/create-payment-intent")
    suspend fun createPaymentIntent(@Body request: PaymentIntentRequest): PaymentIntentResponse

    // ── Tickets ─────────────────────────────────────────────────────────────

    @POST("tickets/create")
    suspend fun createTicket(@Body request: CreateTicketRequest): Ticket

    @GET("tickets/my")
    suspend fun getMyTickets(): List<Ticket>

    @GET("tickets/{id}")
    suspend fun getTicket(@Path("id") ticketId: String): Ticket

    @POST("tickets/{id}/respond")
    suspend fun respondToTicket(
        @Path("id") ticketId: String,
        @Body request: TicketRespondRequest
    ): TicketResponse

    @GET("tickets/all")
    suspend fun getAllTickets(): List<Ticket>

    @PUT("tickets/{id}/status")
    suspend fun updateTicketStatus(
        @Path("id") ticketId: String,
        @Body request: UpdateTicketStatusRequest
    ): Ticket
}
