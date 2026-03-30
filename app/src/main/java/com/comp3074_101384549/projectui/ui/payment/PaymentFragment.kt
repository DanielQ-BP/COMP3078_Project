package com.comp3074_101384549.projectui.ui.payment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.comp3074_101384549.projectui.R
import com.comp3074_101384549.projectui.data.remote.ApiService
import com.comp3074_101384549.projectui.model.PaymentIntentRequest
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class PaymentFragment : Fragment() {

    private lateinit var paymentSheet: PaymentSheet
    private lateinit var apiService: ApiService
    private lateinit var payButton: Button
    private lateinit var progressBar: ProgressBar

    override fun onAttach(context: Context) {
        super.onAttach(context)

        val retrofit = Retrofit.Builder()
            .baseUrl("http://10.0.2.2:3000") // 🔁 Replace with your real backend URL
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

        payButton = view.findViewById(R.id.payButton)
        progressBar = view.findViewById(R.id.progressBar)

        // Initialize PaymentSheet
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
                val response = apiService.createPaymentIntent(
                    PaymentIntentRequest(amount = 1999, currency = "usd") // $19.99
                )

                val config = PaymentSheet.Configuration(
                    merchantDisplayName = "ParkSpot"
                )

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
                Toast.makeText(requireContext(), "✅ Payment successful!", Toast.LENGTH_LONG).show()
            is PaymentSheetResult.Canceled ->
                Toast.makeText(requireContext(), "Payment cancelled.", Toast.LENGTH_SHORT).show()
            is PaymentSheetResult.Failed ->
                Toast.makeText(requireContext(), "❌ Failed: ${result.error.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }
}