package com.comp3074_101384549.projectui.model

data class Ticket(
    val id: String,
    val userId: String,
    val username: String? = null,
    val subject: String,
    val description: String,
    val status: String,
    val createdAt: String,
    val updatedAt: String? = null,
    val responses: List<TicketResponse>? = null
)

data class TicketResponse(
    val id: String,
    val ticketId: String,
    val userId: String,
    val username: String,
    val message: String,
    val isAdminResponse: Boolean,
    val createdAt: String
)

data class CreateTicketRequest(
    val subject: String,
    val description: String
)

data class TicketRespondRequest(
    val message: String
)