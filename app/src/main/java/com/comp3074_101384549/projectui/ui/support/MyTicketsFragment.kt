package com.comp3074_101384549.projectui.ui.support

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.comp3074_101384549.projectui.BuildConfig
import com.comp3074_101384549.projectui.R
import com.comp3074_101384549.projectui.data.local.AuthPreferences
import com.comp3074_101384549.projectui.data.remote.ApiService
import com.comp3074_101384549.projectui.model.Ticket
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class MyTicketsFragment : Fragment() {

    private lateinit var authPreferences: AuthPreferences
    private lateinit var apiService: ApiService
    private lateinit var ticketsContainer: LinearLayout
    private lateinit var emptyState: TextView

    override fun onAttach(context: Context) {
        super.onAttach(context)
        authPreferences = AuthPreferences(context)

        val token = runBlocking { authPreferences.authToken.first() } ?: ""

        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                chain.proceed(
                    chain.request().newBuilder()
                        .addHeader("Authorization", "Bearer $token")
                        .build()
                )
            }
            .build()

        apiService = Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_my_tickets, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        ticketsContainer = view.findViewById(R.id.ticketsContainer)
        emptyState = view.findViewById(R.id.emptyState)
        loadTickets()
    }

    private fun loadTickets() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val tickets = apiService.getMyTickets()
                if (!isAdded) return@launch

                if (tickets.isEmpty()) {
                    emptyState.visibility = View.VISIBLE
                    ticketsContainer.visibility = View.GONE
                } else {
                    emptyState.visibility = View.GONE
                    ticketsContainer.visibility = View.VISIBLE
                    ticketsContainer.removeAllViews()
                    tickets.forEach { addTicketCard(it) }
                }
            } catch (e: Exception) {
                if (isAdded) {
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun addTicketCard(ticket: Ticket) {
        val card = LayoutInflater.from(requireContext())
            .inflate(R.layout.fragment_item_ticket, ticketsContainer, false)

        card.findViewById<TextView>(R.id.textSubject).text = ticket.subject
        card.findViewById<TextView>(R.id.textDate).text = ticket.createdAt.take(10)

        val statusView = card.findViewById<TextView>(R.id.textStatus)
        statusView.text = ticket.status.replace("_", " ").uppercase()
        statusView.setBackgroundColor(
            android.graphics.Color.parseColor(
                when (ticket.status) {
                    "open" -> "#2196F3"
                    "in_progress" -> "#FF9800"
                    "resolved" -> "#4CAF50"
                    "closed" -> "#9E9E9E"
                    else -> "#2196F3"
                }
            )
        )

        card.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.homeFragmentContainer, TicketDetailFragment().apply {
                    arguments = bundleOf("ticketId" to ticket.id)
                })
                .addToBackStack(null)
                .commit()
        }

        ticketsContainer.addView(card)
    }
}