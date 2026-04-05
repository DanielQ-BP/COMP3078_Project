package com.comp3074_101384549.projectui.ui.support

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.RadioGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.comp3074_101384549.projectui.BuildConfig
import com.comp3074_101384549.projectui.R
import com.comp3074_101384549.projectui.data.local.AuthPreferences
import com.comp3074_101384549.projectui.data.remote.ApiService
import com.comp3074_101384549.projectui.model.CreateTicketRequest
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class SubmitTicketFragment : Fragment() {

    private lateinit var authPreferences: AuthPreferences
    private lateinit var apiService: ApiService

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
    ): View? = inflater.inflate(R.layout.fragment_submit_ticket, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val subjectInput = view.findViewById<TextInputEditText>(R.id.editTextSubject)
        val descriptionInput = view.findViewById<TextInputEditText>(R.id.editTextDescription)
        val reservationCodeInput = view.findViewById<TextInputEditText>(R.id.editReservationCode)
        val layoutReservationCode = view.findViewById<TextInputLayout>(R.id.layoutReservationCode)
        val radioGroup = view.findViewById<RadioGroup>(R.id.radioGroupTicketType)
        val submitButton = view.findViewById<Button>(R.id.buttonSubmitTicket)
        val progressBar = view.findViewById<ProgressBar>(R.id.progressBar)

        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            val dispute = checkedId == R.id.radioDispute
            layoutReservationCode.visibility = if (dispute) android.view.View.VISIBLE else android.view.View.GONE
            if (!dispute) reservationCodeInput.text?.clear()
        }

        view.findViewById<Button>(R.id.buttonViewTickets).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.homeFragmentContainer, MyTicketsFragment())
                .addToBackStack(null)
                .commit()
        }

        submitButton.setOnClickListener {
            val subject = subjectInput.text.toString().trim()
            val description = descriptionInput.text.toString().trim()
            val isDispute = radioGroup.checkedRadioButtonId == R.id.radioDispute
            val reservationCode = reservationCodeInput.text.toString().trim()

            if (subject.isEmpty() || description.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill out all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (isDispute && reservationCode.isEmpty()) {
                Toast.makeText(
                    requireContext(),
                    "Enter your reservation code to report a dispute (from your booking confirmation).",
                    Toast.LENGTH_LONG,
                ).show()
                return@setOnClickListener
            }

            submitButton.isEnabled = false
            progressBar.visibility = View.VISIBLE

            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val request = if (isDispute) {
                        CreateTicketRequest(
                            subject = subject,
                            description = description,
                            category = "conflict",
                            bookingReferenceCode = reservationCode.uppercase(),
                        )
                    } else {
                        CreateTicketRequest(subject = subject, description = description)
                    }
                    apiService.createTicket(request)
                    if (isAdded) {
                        Toast.makeText(requireContext(), "✅ Ticket submitted!", Toast.LENGTH_LONG).show()
                        subjectInput.text?.clear()
                        descriptionInput.text?.clear()
                        reservationCodeInput.text?.clear()
                    }
                } catch (e: Exception) {
                    if (isAdded) {
                        Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                } finally {
                    if (isAdded) {
                        submitButton.isEnabled = true
                        progressBar.visibility = View.GONE
                    }
                }
            }
        }
    }
}