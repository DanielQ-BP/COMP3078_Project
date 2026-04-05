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
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class PaymentFragment : Fragment() {

    companion object {
        fun newInstance(totalPrice: Double) = PaymentFragment().apply {
            arguments = android.os.Bundle().apply {
                putDouble("totalPrice", totalPrice)
            }
        }
    }

    private lateinit var paymentSheet: PaymentSheet
    private lateinit var authPreferences: AuthPreferences
    private lateinit var apiService: ApiService
    private lateinit var payButton: Button
    private lateinit var progressBar: ProgressBar
    private var totalPrice: Double = 0.0

    override fun onAttach(context: Context) {
        super.onAttach(context)
        authPreferences = AuthPreferences(context)
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(authPreferences))
            .build()
        val retrofit = Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        apiService = retrofit.create(ApiService::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_payment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        totalPrice = arguments?.getDouble("totalPrice") ?: 0.0

        payButton = view.findViewById(R.id.payButton)
        progressBar = view.findViewById(R.id.progressBar)
        view.findViewById<TextView>(R.id.textAmount).text =
            "Amount: $${String.format("%.2f", totalPrice)}"

        paymentSheet = PaymentSheet(this, ::onPaymentSheetResult)

        payButton.setOnClickListener {
            startCheckout()
        }
    }

    private fun startCheckout() {
        payButton.isEnabled = false
        progressBar.visibility = View.VISIBLE

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val amountCents = (totalPrice * 100).toInt().coerceAtLeast(50) // Stripe min is 50 cents
                val response = apiService.createPaymentIntent(
                    PaymentIntentRequest(amount = amountCents, currency = "usd")
                )
                val config = PaymentSheet.Configuration(merchantDisplayName = "ParkSpot")
                paymentSheet.presentWithPaymentIntent(response.clientSecret, config)
            } catch (e: Exception) {
                if (isAdded) {
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            } finally {
                if (isAdded) {
                    payButton.isEnabled = true
                    progressBar.visibility = View.GONE
                }
            }
        }
    }

    private fun onPaymentSheetResult(result: PaymentSheetResult) {
        if (!isAdded) return
        when (result) {
            is PaymentSheetResult.Completed ->
                Toast.makeText(requireContext(), "Payment successful!", Toast.LENGTH_LONG).show()
            is PaymentSheetResult.Canceled ->
                Toast.makeText(requireContext(), "Payment cancelled.", Toast.LENGTH_SHORT).show()
            is PaymentSheetResult.Failed ->
                Toast.makeText(requireContext(), "Failed: ${result.error.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }
}
