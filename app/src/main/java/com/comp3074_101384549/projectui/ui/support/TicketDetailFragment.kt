package com.comp3074_101384549.projectui.ui.support

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.comp3074_101384549.projectui.BuildConfig
import com.comp3074_101384549.projectui.R
import com.comp3074_101384549.projectui.data.local.AuthPreferences
import com.comp3074_101384549.projectui.data.remote.ApiService
import com.comp3074_101384549.projectui.model.TicketRespondRequest
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class TicketDetailFragment : Fragment() {

    private lateinit var authPreferences: AuthPreferences
    private lateinit var apiService: ApiService
    private lateinit var responsesContainer: LinearLayout
    private lateinit var replyInput: TextInputEditText
    private lateinit var sendButton: Button
    private var ticketId: String = ""

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
    ): View? = inflater.inflate(R.layout.fragment_ticket_detail, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ticketId = arguments?.getString("ticketId") ?: return

        responsesContainer = view.findViewById(R.id.responsesContainer)
        replyInput = view.findViewById(R.id.editTextReply)
        sendButton = view.findViewById(R.id.buttonSendReply)

        view.findViewById<Button>(R.id.buttonBack).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        sendButton.setOnClickListener { sendReply() }
        loadTicketDetail(view)
    }

    private fun loadTicketDetail(view: View) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val ticket = apiService.getTicket(ticketId)
                if (!isAdded) return@launch

                view.findViewById<TextView>(R.id.textSubject).text = ticket.subject
                view.findViewById<TextView>(R.id.textDescription).text = ticket.description

                val statusView = view.findViewById<TextView>(R.id.textStatus)
                statusView.text = ticket.status.replace("_", " ").uppercase()
                statusView.setBackgroundColor(
                    Color.parseColor(
                        when (ticket.status) {
                            "open" -> "#2196F3"
                            "in_progress" -> "#FF9800"
                            "resolved" -> "#4CAF50"
                            "closed" -> "#9E9E9E"
                            else -> "#2196F3"
                        }
                    )
                )

                responsesContainer.removeAllViews()
                ticket.responses?.forEach { response ->
                    val responseView = LayoutInflater.from(requireContext())
                        .inflate(R.layout.fragment_item_ticket_response, responsesContainer, false)

                    responseView.findViewById<TextView>(R.id.textUsername).text =
                        if (response.isAdminResponse) "Support Team" else response.username
                    responseView.findViewById<TextView>(R.id.textMessage).text = response.message
                    responseView.findViewById<TextView>(R.id.textDate).text =
                        response.createdAt.take(10)

                    if (response.isAdminResponse) {
                        responseView.setBackgroundColor(
                            Color.parseColor("#E3F2FD")
                        )
                    }

                    responsesContainer.addView(responseView)
                }
            } catch (e: Exception) {
                if (isAdded) {
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun sendReply() {
        val message = replyInput.text.toString().trim()
        if (message.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter a message", Toast.LENGTH_SHORT).show()
            return
        }

        sendButton.isEnabled = false
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                apiService.respondToTicket(ticketId, TicketRespondRequest(message))
                if (!isAdded) return@launch
                replyInput.text?.clear()
                Toast.makeText(requireContext(), "Reply sent!", Toast.LENGTH_SHORT).show()
                loadTicketDetail(requireView())
            } catch (e: Exception) {
                if (isAdded) {
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                if (isAdded) sendButton.isEnabled = true
            }
        }
    }
}