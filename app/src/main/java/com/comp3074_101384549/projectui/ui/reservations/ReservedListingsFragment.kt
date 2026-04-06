package com.comp3074_101384549.projectui.ui.reservations

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.comp3074_101384549.projectui.BuildConfig
import com.comp3074_101384549.projectui.R
import com.comp3074_101384549.projectui.data.local.AppDatabase
import com.comp3074_101384549.projectui.data.local.AuthPreferences
import com.comp3074_101384549.projectui.data.remote.ApiService
import com.comp3074_101384549.projectui.data.remote.AuthInterceptor
import com.comp3074_101384549.projectui.model.BookingEntity
import com.comp3074_101384549.projectui.repository.BookingRepository
import com.comp3074_101384549.projectui.ui.adapter.BookingAdapter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ReservedListingsFragment : Fragment() {

    private lateinit var bookingRepository: BookingRepository
    private lateinit var authPreferences: AuthPreferences
    private lateinit var bookingAdapter: BookingAdapter
    private var emptyState: LinearLayout? = null
    private var recyclerView: RecyclerView? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)

        val db = AppDatabase.getDatabase(context)
        val bookingDao = db.bookingDao()

        authPreferences = AuthPreferences(context)
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(authPreferences))
            .build()
        val retrofit = Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val apiService = retrofit.create(ApiService::class.java)

        bookingRepository = BookingRepository(apiService, bookingDao)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_reserved_listing, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        emptyState = view.findViewById(R.id.emptyState)
        recyclerView = view.findViewById(R.id.recyclerViewBookings)

        recyclerView?.layoutManager = LinearLayoutManager(requireContext())

        bookingAdapter = BookingAdapter(
            bookings = emptyList(),
            onCancelClick = { booking -> showCancelConfirmation(booking) }
        )
        recyclerView?.adapter = bookingAdapter

        loadBookings()
    }

    override fun onResume() {
        super.onResume()
        loadBookings()
    }

    private fun loadBookings() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                if (!isAdded) return@launch

                val userId = authPreferences.userId.first()
                if (userId == null) {
                    showEmptyState(true)
                    return@launch
                }

                val bookings = bookingRepository.getAllBookings(userId)

                if (isAdded) {
                    if (bookings.isEmpty()) {
                        showEmptyState(true)
                    } else {
                        showEmptyState(false)
                        bookingAdapter.updateBookings(bookings)
                    }
                }
            } catch (e: Exception) {
                if (isAdded) {
                    Toast.makeText(requireContext(), "Error loading bookings", Toast.LENGTH_SHORT).show()
                    showEmptyState(true)
                }
            }
        }
    }

    private fun showEmptyState(show: Boolean) {
        emptyState?.visibility = if (show) View.VISIBLE else View.GONE
        recyclerView?.visibility = if (show) View.GONE else View.VISIBLE
    }

    private fun showCancelConfirmation(booking: BookingEntity) {
        if (!isAdded) return
        AlertDialog.Builder(requireContext())
            .setTitle("Cancel Booking")
            .setMessage("Are you sure you want to cancel this booking?\n\n${booking.address}\n${booking.bookingDate}\n\nNote: If you paid online, a refund will be processed to your original payment method within 3-5 business days.")
            .setPositiveButton("Yes, Cancel") { _, _ ->
                cancelBooking(booking)
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun cancelBooking(booking: BookingEntity) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                bookingRepository.cancelBooking(booking.id)
                if (isAdded) {
                    Toast.makeText(requireContext(), "Booking cancelled", Toast.LENGTH_SHORT).show()
                    loadBookings()
                }
            } catch (e: Exception) {
                if (isAdded) {
                    Toast.makeText(requireContext(), "Error cancelling booking", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
