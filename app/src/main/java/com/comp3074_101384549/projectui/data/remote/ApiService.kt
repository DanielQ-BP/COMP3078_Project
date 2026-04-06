package com.comp3074_101384549.projectui.data.remote

import com.comp3074_101384549.projectui.model.AdminBookingDetailResponse
import com.comp3074_101384549.projectui.model.AdminBookingRow
import com.comp3074_101384549.projectui.model.AdminLoginRequest
import com.comp3074_101384549.projectui.model.AdminResolveConflictRequest
import com.comp3074_101384549.projectui.model.AdminResolveConflictResponse
import com.comp3074_101384549.projectui.model.AdminUserDetailResponse
import com.comp3074_101384549.projectui.model.AdminUserRow
import com.comp3074_101384549.projectui.model.Booking
import com.comp3074_101384549.projectui.model.CreateBookingRequest
import com.comp3074_101384549.projectui.model.CreateBookingResponse
import com.comp3074_101384549.projectui.model.CreateTicketRequest
import com.comp3074_101384549.projectui.model.Listing
import com.comp3074_101384549.projectui.model.MessageResponse
import com.comp3074_101384549.projectui.model.CreatePaymentRequest
import com.comp3074_101384549.projectui.model.CreatePaymentResponse
import com.comp3074_101384549.projectui.model.PaymentIntentRequest
import com.comp3074_101384549.projectui.model.PaymentIntentResponse
import com.comp3074_101384549.projectui.model.Ticket
import com.comp3074_101384549.projectui.model.TicketResponse
import com.comp3074_101384549.projectui.model.TicketRespondRequest
import com.comp3074_101384549.projectui.model.UpdateTicketStatusRequest
import com.comp3074_101384549.projectui.model.FcmTokenRequest
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
import retrofit2.http.Query

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

    @GET("listings/search")
    suspend fun searchListings(
        @Query("address") address: String? = null,
        @Query("minPrice") minPrice: Double? = null,
        @Query("maxPrice") maxPrice: Double? = null,
        @Query("date") date: String? = null,
        @Query("sortBy") sortBy: String? = null
    ): List<Listing>

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
    suspend fun createBooking(@Body request: CreateBookingRequest): CreateBookingResponse

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

    @PUT("users/{id}/fcm-token")
    suspend fun registerFcmToken(
        @Path("id") userId: String,
        @Body request: FcmTokenRequest
    ): MessageResponse

    // ── Notifications ───────────────────────────────────────────────────────

    @GET("notifications/user/{userId}")
    suspend fun getUserNotifications(@Path("userId") userId: String): List<Notification>

    @PUT("notifications/{id}/read")
    suspend fun markNotificationRead(@Path("id") id: String): Notification

    @DELETE("notifications/{id}")
    suspend fun deleteNotification(@Path("id") id: String): MessageResponse

    @PUT("notifications/read-all")
    suspend fun markAllNotificationsRead(): MessageResponse

    // ── Admin ────────────────────────────────────────────────────────────────

    @GET("admin/bookings")
    suspend fun adminGetBookings(): List<AdminBookingRow>

    @GET("admin/bookings/search")
    suspend fun adminSearchBookings(@Query("q") q: String): List<AdminBookingRow>

    @GET("admin/bookings/{id}/detail")
    suspend fun adminGetBookingDetail(@Path("id") bookingId: String): AdminBookingDetailResponse

    @POST("admin/conflicts/{ticketId}/resolve")
    suspend fun adminResolveConflict(
        @Path("ticketId") ticketId: String,
        @Body body: AdminResolveConflictRequest,
    ): AdminResolveConflictResponse

    @GET("admin/users")
    suspend fun adminGetUsers(): List<AdminUserRow>

    @GET("admin/users/{id}/detail")
    suspend fun adminGetUserDetail(@Path("id") userId: String): AdminUserDetailResponse

    // ── Payments ────────────────────────────────────────────────────────────

    @POST("payments/create-payment-intent")
    suspend fun createPaymentIntent(@Body request: PaymentIntentRequest): PaymentIntentResponse

    @POST("payments/create")
    suspend fun createPayment(@Body request: CreatePaymentRequest): CreatePaymentResponse

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
