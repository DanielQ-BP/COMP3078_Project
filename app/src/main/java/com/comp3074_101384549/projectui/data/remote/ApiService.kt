package com.comp3074_101384549.projectui.data.remote

import com.comp3074_101384549.projectui.model.AdminBookingDetailResponse
import com.comp3074_101384549.projectui.model.AdminBookingRow
import com.comp3074_101384549.projectui.model.AdminLoginRequest
import com.comp3074_101384549.projectui.model.AdminUserDetailResponse
import com.comp3074_101384549.projectui.model.AdminResolveConflictRequest
import com.comp3074_101384549.projectui.model.AdminResolveConflictResponse
import com.comp3074_101384549.projectui.model.AdminUserRow
import com.comp3074_101384549.projectui.model.CreateBookingRequest
import com.comp3074_101384549.projectui.model.CreateBookingResponse
import com.comp3074_101384549.projectui.model.CreateTicketRequest
import com.comp3074_101384549.projectui.model.Listing
import com.comp3074_101384549.projectui.model.PaymentIntentRequest
import com.comp3074_101384549.projectui.model.PaymentIntentResponse
import com.comp3074_101384549.projectui.model.Ticket
import com.comp3074_101384549.projectui.model.TicketResponse
import com.comp3074_101384549.projectui.model.TicketRespondRequest
import com.comp3074_101384549.projectui.model.User
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    @POST("auth/register")
    suspend fun register(@Body user: User): String

    @POST("auth/login")
    suspend fun login(@Body user: User): String

    @POST("auth/admin/login")
    suspend fun adminLogin(@Body body: AdminLoginRequest): String

    @GET("listings/all")
    suspend fun getRemoteListings(): List<Listing>

    @POST("listings/create")
    suspend fun createListing(@Body listing: Listing): Listing

    @POST("bookings/create")
    suspend fun createBooking(@Body body: CreateBookingRequest): CreateBookingResponse

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

    @POST("payments/create-payment-intent")
    suspend fun createPaymentIntent(@Body request: PaymentIntentRequest): PaymentIntentResponse

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
}