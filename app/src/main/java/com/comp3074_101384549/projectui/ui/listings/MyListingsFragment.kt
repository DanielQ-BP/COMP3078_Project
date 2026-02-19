package com.comp3074_101384549.projectui.ui.listings

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.comp3074_101384549.projectui.R
import com.comp3074_101384549.projectui.data.local.AppDatabase
import com.comp3074_101384549.projectui.data.local.AuthPreferences
import com.comp3074_101384549.projectui.data.remote.ApiService
import com.comp3074_101384549.projectui.repository.ListingRepository
import com.comp3074_101384549.projectui.ui.adapter.ListingAdapter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MyListingsFragment : Fragment() {

    private lateinit var listingRepository: ListingRepository
    private lateinit var listingAdapter: ListingAdapter
    private lateinit var authPreferences: AuthPreferences

    override fun onAttach(context: Context) {
        super.onAttach(context)

        val db = AppDatabase.getDatabase(context)
        val listingDao = db.listingDao()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://example.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val apiService = retrofit.create(ApiService::class.java)
        listingRepository = ListingRepository(apiService, listingDao)
        authPreferences = AuthPreferences(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_my_listings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerViewListings)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Initialize adapter with an empty list initially
        listingAdapter = ListingAdapter(emptyList()) { listing ->
            // When a listing is clicked, navigate to details fragment
            val detailsFragment = ListingDetailsFragment().apply {
                arguments = bundleOf(
                    "address" to listing.address,
                    "price" to listing.pricePerHour,
                    "availability" to listing.availability,
                    "description" to listing.description
                )
            }
            parentFragmentManager.beginTransaction()
                .replace(R.id.homeFragmentContainer, detailsFragment)
                .addToBackStack(null)
                .commit()
        }
        recyclerView.adapter = listingAdapter

        // Delete All button
        view.findViewById<Button>(R.id.buttonDeleteAll)?.setOnClickListener {
            showDeleteConfirmationDialog()
        }

        // Load data immediately
        loadListings()
    }

    private fun showDeleteConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete All Listings")
            .setMessage("Are you sure you want to delete ALL listings? This cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                deleteAllListings()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteAllListings() {
        lifecycleScope.launch {
            try {
                val userId = authPreferences.userId.first()
                if (userId == null) {
                    Toast.makeText(requireContext(), "Please login to delete listings", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                listingRepository.deleteAllListings(userId)
                Toast.makeText(requireContext(), "All listings deleted", Toast.LENGTH_SHORT).show()
                // Reload the list (will be empty)
                loadListings()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error deleting listings: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Reload data whenever the fragment is brought back into view
        loadListings()
    }

    private fun loadListings() {
        // FIX: Wrap the suspending function call in a coroutine scope
        lifecycleScope.launch {
            // Get current user ID
            val userId = authPreferences.userId.first()
            if (userId == null) {
                Toast.makeText(requireContext(), "Please login to view your listings", Toast.LENGTH_SHORT).show()
                listingAdapter.updateListings(emptyList())
                return@launch
            }

            // Call the suspend function to fetch data from the repository (Room/DB)
            val listings = listingRepository.getAllListings(userId)
            // Update the adapter with the new data
            listingAdapter.updateListings(listings)
        }
    }
}