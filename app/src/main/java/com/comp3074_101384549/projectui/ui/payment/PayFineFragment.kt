package com.comp3074_101384549.projectui.ui.payment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.comp3074_101384549.projectui.BuildConfig
import com.comp3074_101384549.projectui.R
import com.comp3074_101384549.projectui.data.local.AuthPreferences
import com.comp3074_101384549.projectui.data.remote.ApiService
import com.comp3074_101384549.projectui.data.remote.AuthInterceptor
import com.comp3074_101384549.projectui.model.PaymentIntentRequest
import com.comp3074_101384549.projectui.ui.reservations.ReservedListingsFragment
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class PayFineFragment : Fragment() {

    companion object {
        fun newInstance(bookingId: String, fineAmount: Double, address: String) =
            PayFineFragment().apply {
                arguments = Bundle().apply {
                    putString("bookingId", bookingId)
                    putDouble("fineAmount", fineAmount)
                    putString("address", address)
                }
            }
    }

    private lateinit var paymentSheet: PaymentSheet
    private lateinit var apiService: ApiService
    private lateinit var payButton: Button
    private lateinit var progressBar: ProgressBar
    private var bookingId = ""
    private var fineAmount = 0.0

    override fun onAttach(context: Context) {
        super.onAttach(context)
        val prefs = AuthPreferences(context)
        val client = OkHttpClient.Builder().addInterceptor(AuthInterceptor(prefs)).build()
        apiService = Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_pay_fine, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bookingId  = arguments?.getString("bookingId") ?: ""
        fineAmount = arguments?.getDouble("fineAmount") ?: 0.0
        val address = arguments?.getString("address") ?: ""

        payButton   = view.findViewById(R.id.buttonPayFineNow)
        progressBar = view.findViewById(R.id.progressBarFine)

        view.findViewById<TextView>(R.id.textFineAddress).text = address
        view.findViewById<TextView>(R.id.textFineTotal).text =
            "Fine amount: $${String.format("%.2f", fineAmount)}"

        paymentSheet = PaymentSheet(this, ::onPaymentResult)
        payButton.setOnClickListener { startFinePayment() }
    }

    private fun startFinePayment() {
        payButton.isEnabled = false
        progressBar.visibility = View.VISIBLE
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val cents = (fineAmount * 100).toInt().coerceAtLeast(50)
                val response = apiService.createPaymentIntent(
                    PaymentIntentRequest(amount = cents, currency = "usd")
                )
                paymentSheet.presentWithPaymentIntent(
                    response.clientSecret,
                    PaymentSheet.Configuration(merchantDisplayName = "ParkSpot")
                )
            } catch (e: Exception) {
                if (isAdded) {
                    payButton.isEnabled = true
                    progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            } finally {
                if (isAdded) progressBar.visibility = View.GONE
            }
        }
    }

    private fun onPaymentResult(result: PaymentSheetResult) {
        if (!isAdded) return
        when (result) {
            is PaymentSheetResult.Completed -> {
                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        apiService.payFine(bookingId)
                    } catch (_: Exception) { }
                    if (isAdded) {
                        Toast.makeText(requireContext(), "Fine paid successfully!", Toast.LENGTH_SHORT).show()
                        parentFragmentManager.beginTransaction()
                            .replace(R.id.homeFragmentContainer, ReservedListingsFragment())
                            .commit()
                    }
                }
            }
            is PaymentSheetResult.Canceled -> {
                payButton.isEnabled = true
            }
            is PaymentSheetResult.Failed -> {
                payButton.isEnabled = true
                Toast.makeText(requireContext(), "Payment failed: ${result.error.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
