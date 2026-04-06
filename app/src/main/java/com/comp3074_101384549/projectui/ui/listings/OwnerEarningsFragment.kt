package com.comp3074_101384549.projectui.ui.listings

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.comp3074_101384549.projectui.BuildConfig
import com.comp3074_101384549.projectui.R
import com.comp3074_101384549.projectui.data.local.AuthPreferences
import com.comp3074_101384549.projectui.data.remote.ApiService
import com.comp3074_101384549.projectui.data.remote.AuthInterceptor
import com.comp3074_101384549.projectui.model.EarningsListing
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class OwnerEarningsFragment : Fragment() {

    private lateinit var apiService: ApiService
    private lateinit var authPreferences: AuthPreferences

    override fun onAttach(context: Context) {
        super.onAttach(context)
        authPreferences = AuthPreferences(context)
        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(authPreferences)).build()
        apiService = Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_owner_earnings, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recycler = view.findViewById<RecyclerView>(R.id.recyclerEarnings)
        recycler.layoutManager = LinearLayoutManager(requireContext())

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val userId = authPreferences.userId.first() ?: return@launch
                val data = apiService.getOwnerEarnings(userId)

                if (!isAdded) return@launch

                view.findViewById<TextView>(R.id.textTotalEarnings).text =
                    "$${String.format("%.2f", data.totalEarnings)}"
                view.findViewById<TextView>(R.id.textFineEarnings).text =
                    "$${String.format("%.2f", data.totalFineEarnings)}"
                view.findViewById<TextView>(R.id.textTotalBookings).text =
                    "${data.totalBookings}"

                if (data.listings.isEmpty()) {
                    view.findViewById<LinearLayout>(R.id.emptyEarnings).visibility = View.VISIBLE
                    recycler.visibility = View.GONE
                    view.findViewById<TextView>(R.id.textByListing).visibility = View.GONE
                } else {
                    recycler.adapter = EarningsAdapter(data.listings)
                }
            } catch (_: Exception) {
                if (isAdded) {
                    view.findViewById<LinearLayout>(R.id.emptyEarnings).visibility = View.VISIBLE
                    view.findViewById<RecyclerView>(R.id.recyclerEarnings).visibility = View.GONE
                }
            }
        }
    }

    private inner class EarningsAdapter(private val items: List<EarningsListing>) :
        RecyclerView.Adapter<EarningsAdapter.VH>() {

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val address: TextView      = v.findViewById(R.id.textEarningAddress)
            val bookings: TextView     = v.findViewById(R.id.textEarningBookings)
            val earnings: TextView     = v.findViewById(R.id.textEarningAmount)
            val fineEarnings: TextView = v.findViewById(R.id.textEarningFines)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
            VH(LayoutInflater.from(parent.context).inflate(R.layout.item_earning, parent, false))

        override fun getItemCount() = items.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = items[position]
            holder.address.text  = item.address
            holder.bookings.text = "${item.bookingCount} booking${if (item.bookingCount != 1) "s" else ""}"
            holder.earnings.text = "$${String.format("%.2f", item.earnings)}"
            if (item.fineEarnings > 0) {
                holder.fineEarnings.visibility = View.VISIBLE
                holder.fineEarnings.text = "+$${String.format("%.2f", item.fineEarnings)} fines"
            } else {
                holder.fineEarnings.visibility = View.GONE
            }
        }
    }
}
