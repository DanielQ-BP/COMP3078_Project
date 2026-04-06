package com.comp3074_101384549.projectui.ui.reservations

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.comp3074_101384549.projectui.R

class BookingConfirmationFragment : Fragment() {

    companion object {
        fun newInstance(
            referenceCode: String,
            address: String,
            bookingDate: String,
            startTime: String,
            endTime: String,
            totalPrice: Double
        ) = BookingConfirmationFragment().apply {
            arguments = Bundle().apply {
                putString("referenceCode", referenceCode)
                putString("address", address)
                putString("bookingDate", bookingDate)
                putString("startTime", startTime)
                putString("endTime", endTime)
                putDouble("totalPrice", totalPrice)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_booking_confirmation, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val referenceCode = arguments?.getString("referenceCode") ?: ""
        val address = arguments?.getString("address") ?: ""
        val bookingDate = arguments?.getString("bookingDate") ?: ""
        val startTime = arguments?.getString("startTime") ?: ""
        val endTime = arguments?.getString("endTime") ?: ""
        val totalPrice = arguments?.getDouble("totalPrice") ?: 0.0

        view.findViewById<TextView>(R.id.textReferenceCode).text = "Reference: #$referenceCode"
        view.findViewById<TextView>(R.id.textAddress).text = "Address: $address"
        view.findViewById<TextView>(R.id.textDate).text = "Date: $bookingDate"
        view.findViewById<TextView>(R.id.textTime).text = "Time: $startTime - $endTime"
        view.findViewById<TextView>(R.id.textTotal).text = "Total: $${String.format("%.2f", totalPrice)}"

        view.findViewById<Button>(R.id.buttonViewBookings).setOnClickListener {
            val reservedFragment = ReservedListingsFragment()
            parentFragmentManager.beginTransaction()
                .replace(R.id.homeFragmentContainer, reservedFragment)
                .addToBackStack(null)
                .commit()
        }

        view.findViewById<Button>(R.id.buttonBackToHome).setOnClickListener {
            parentFragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
            parentFragmentManager.beginTransaction()
                .replace(R.id.homeFragmentContainer, com.comp3074_101384549.projectui.ui.home.HomeFragment())
                .commit()
            (activity as? com.comp3074_101384549.projectui.HomeActivity)?.let { homeActivity ->
                homeActivity.findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNav)?.selectedItemId = R.id.homeFragment
            }
        }
    }
}
