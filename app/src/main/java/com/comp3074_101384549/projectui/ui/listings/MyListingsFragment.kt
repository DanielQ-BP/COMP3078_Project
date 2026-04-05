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
import com.comp3074_101384549.projectui.BuildConfig
import com.comp3074_101384549.projectui.R
import com.comp3074_101384549.projectui.data.local.AppDatabase
import com.comp3074_101384549.projectui.data.local.AuthPreferences
import com.comp3074_101384549.projectui.data.remote.ApiService
import com.comp3074_101384549.projectui.data.remote.AuthInterceptor
import com.comp3074_101384549.projectui.repository.ListingRepository
import com.comp3074_101384549.projectui.ui.adapter.MyListingAdapter
import com.comp3074_101384549.projectui.ui.home.HomeFragment
import com.comp3074_101384549.projectui.model.Listing
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MyListingsFragment : Fragment() {

    private lateinit var listingRepository: ListingRepository
    private lateinit var listingAdapter: MyListingAdapter
    private lateinit var authPreferences: AuthPreferences

    private var emptyState: View? = null
    private var recyclerView: RecyclerView? = null
    private var deleteAllButton: Button? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)

        val db = AppDatabase.getDatabase(context)
        val listingDao = db.listingDao()

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
        listingRepository = ListingRepository(apiService, listingDao)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_my_listings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ── Role guard ──────────────────────────────────────────────
        viewLifecycleOwner.lifecycleScope.launch {
            val inOwnerMode = authPreferences.isInOwnerMode.first()
            if (!inOwnerMode) {
                val hasOwner = authPreferences.hasOwnerAccount.first()
                if (hasOwner) {
                    Toast.makeText(
                        requireContext(),
                        "Switch to Owner Mode from the menu to manage your listings.",
                        Toast.LENGTH_LONG
                    ).show()
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.homeFragmentContainer, HomeFragment())
                        .commit()
                } else {
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.homeFragmentContainer, BecomeOwnerFragment())
                        .commit()
                }
                return@launch
            }
            setupListings(view)
        }
    }

    private fun setupListings(view: View) {
        emptyState = view.findViewById(R.id.emptyState)
        recyclerView = view.findViewById(R.id.recyclerViewListings)
        deleteAllButton = view.findViewById(R.id.buttonDeleteAll)

        recyclerView?.layoutManager = LinearLayoutManager(requireContext())

        // Initialize adapter with edit/delete callbacks
        listingAdapter = MyListingAdapter(
            listings = emptyList(),
            onEditClick = { listing -> onEditListing(listing) },
            onDeleteClick = { listing -> showDeleteConfirmation(listing) }
        )
        recyclerView?.adapter = listingAdapter

        // Delete All button
        deleteAllButton?.setOnClickListener {
            showDeleteAllConfirmationDialog()
        }

        // Create First Listing button (in empty state)
        view.findViewById<Button>(R.id.buttonCreateFirst)?.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.homeFragmentContainer, CreateListingFragment())
                .addToBackStack(null)
                .commit()
        }

        // Load data immediately
        loadListings()
    }

    private fun showEmptyState(show: Boolean) {
        emptyState?.visibility = if (show) View.VISIBLE else View.GONE
        recyclerView?.visibility = if (show) View.GONE else View.VISIBLE
        deleteAllButton?.visibility = if (show) View.GONE else View.VISIBLE
    }

    private fun onEditListing(listing: Listing) {
        val editFragment = EditListingFragment().apply {
            arguments = bundleOf(
                "listingId" to listing.id,
                "address" to listing.address,
                "price" to listing.pricePerHour,
                "availability" to listing.availability,
                "description" to listing.description,
                "isActive" to listing.isActive,
                "latitude" to listing.latitude,
                "longitude" to listing.longitude
            )
        }
        parentFragmentManager.beginTransaction()
            .replace(R.id.homeFragmentContainer, editFragment)
            .addToBackStack(null)
            .commit()
    }

    private fun showDeleteConfirmation(listing: Listing) {
        if (!isAdded) return
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Listing")
            .setMessage("Are you sure you want to delete this listing?\n\n${listing.address}")
            .setPositiveButton("Delete") { _, _ ->
                deleteListing(listing)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteListing(listing: Listing) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                listingRepository.deleteListing(listing.id)
                if (isAdded) {
                    Toast.makeText(requireContext(), "Listing deleted", Toast.LENGTH_SHORT).show()
                    loadListings()
                }
            } catch (e: Exception) {
                if (isAdded) {
                    Toast.makeText(requireContext(), "Error deleting listing", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showDeleteAllConfirmationDialog() {
        if (!isAdded) return
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
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                if (!isAdded) return@launch

                val userId = authPreferences.userId.first()
                if (userId == null) {
                    if (isAdded) {
                        Toast.makeText(requireContext(), "Please login to delete listings", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                listingRepository.deleteAllListings(userId)
                if (isAdded) {
                    Toast.makeText(requireContext(), "All listings deleted", Toast.LENGTH_SHORT).show()
                    loadListings()
                }
            } catch (e: Exception) {
                if (isAdded) {
                    Toast.makeText(requireContext(), "Error deleting listings: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadListings()
    }

    private fun loadListings() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                if (!isAdded) return@launch

                val userId = authPreferences.userId.first()
                if (userId == null) {
                    if (isAdded) {
                        Toast.makeText(requireContext(), "Please login to view your listings", Toast.LENGTH_SHORT).show()
                    }
                    listingAdapter.updateListings(emptyList())
                    showEmptyState(true)
                    return@launch
                }

                val listings = listingRepository.getAllListings(userId)
                if (isAdded) {
                    listingAdapter.updateListings(listings)
                    showEmptyState(listings.isEmpty())
                }
            } catch (e: Exception) {
                if (isAdded) {
                    Toast.makeText(requireContext(), "Error loading listings", Toast.LENGTH_SHORT).show()
                    listingAdapter.updateListings(emptyList())
                    showEmptyState(true)
                }
            }
        }
    }
}