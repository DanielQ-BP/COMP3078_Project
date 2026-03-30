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
import com.comp3074_101384549.projectui.data.local.AuthPreferences
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class PaymentFragment : Fragment() {

    private lateinit var paymentSheet: PaymentSheet
    private lateinit var authPreferences: AuthPreferences
    private lateinit var payButton: Button
    private lateinit var progressBar: ProgressBar

    override fun onAttach(context: Context) {
        super.onAttach(context)
        authPreferences = AuthPreferences(context)
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
                val token = authPreferences.authToken.first() ?: ""

                val clientSecret = withContext(Dispatchers.IO) {
                    val client = OkHttpClient.Builder()
                        .connectTimeout(30, TimeUnit.SECONDS)
                        .readTimeout(30, TimeUnit.SECONDS)
                        .build()

                    val request = Request.Builder()
                        .url("http://10.0.2.2:3000/payments/create-payment-intent")
                        .addHeader("Authorization", "Bearer $token")
                        .post(
                            "{\"amount\":1999,\"currency\":\"usd\"}"
                                .toRequestBody("application/json".toMediaType())
                        )
                        .build()

                    val response = client.newCall(request).execute()
                    val body = response.body?.string().orEmpty()
                    JSONObject(body).getString("clientSecret")
                }

                val config = PaymentSheet.Configuration(merchantDisplayName = "ParkSpot")
                paymentSheet.presentWithPaymentIntent(clientSecret, config)

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