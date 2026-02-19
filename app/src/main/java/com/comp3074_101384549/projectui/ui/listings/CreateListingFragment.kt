package com.comp3074_101384549.projectui.ui.listings

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.comp3074_101384549.projectui.repository.ListingRepository
import com.comp3074_101384549.projectui.R
import com.comp3074_101384549.projectui.data.local.AppDatabase
import com.comp3074_101384549.projectui.data.local.AuthPreferences
import com.comp3074_101384549.projectui.data.remote.ApiService
import com.comp3074_101384549.projectui.model.Listing
import com.comp3074_101384549.projectui.utils.MapUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.UUID

class CreateListingFragment : Fragment() {

    private lateinit var listingRepository: ListingRepository
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
        return inflater.inflate(R.layout.fragment_create_listing, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val address = view.findViewById<TextInputEditText>(R.id.editTextAddress)
        val price = view.findViewById<TextInputEditText>(R.id.editTextPrice)
        val availability = view.findViewById<TextInputEditText>(R.id.editTextAvailability)
        val timeWindow = view.findViewById<TextInputEditText>(R.id.editTextTimeWindow)
        val description = view.findViewById<TextInputEditText>(R.id.editTextDescription)
        val activeSwitch = view.findViewById<SwitchMaterial>(R.id.switchActive)
        val createButton = view.findViewById<Button>(R.id.buttonCreateListing)
        val cancelButton = view.findViewById<Button>(R.id.buttonCancel)

        // Cancel button handler
        cancelButton.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        createButton.setOnClickListener {

            // Collect all user inputs
            val addr = address.text.toString()
            val priceValue = price.text.toString().toDoubleOrNull() ?: 0.0
            val avail = availability.text.toString()
            val time = timeWindow.text.toString()
            val desc = description.text.toString()

            if (addr.isEmpty() || avail.isEmpty() || time.isEmpty() || desc.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill out all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // FIX: Launch a coroutine to call the suspending function
            lifecycleScope.launch {
                try {
                    // Get current user ID
                    val userId = authPreferences.userId.first()
                    if (userId == null) {
                        Toast.makeText(requireContext(), "Please login to create a listing", Toast.LENGTH_SHORT).show()
                        return@launch
                    }

                    // Geocode the address to get latitude/longitude
                    val latLng = try {
                        MapUtils.getLatLngFromAddress(requireContext(), addr)
                    } catch (e: Exception) {
                        Log.e("CreateListingFragment", "Geocoding failed: $e", e)
                        null
                    }

                    if (latLng == null) {
                        Log.w("CreateListingFragment", "Could not geocode address: $addr. Creating listing with default coordinates.")
                    }

                    val newListing = Listing(
                        id = UUID.randomUUID().toString(), // Generate a unique ID
                        pricePerHour = priceValue,
                        availability = avail,
                        description = desc,
                        isActive = activeSwitch.isChecked, // Use switch value
                        latitude = latLng?.latitude ?: 43.6532, // Default to Toronto if geocoding fails
                        longitude = latLng?.longitude ?: -79.3832,
                        address = addr,
                        userId = userId // Associate listing with current user
                    )

                    // Call the new suspending function
                    listingRepository.saveNewListing(newListing)

                    val message = if (latLng != null) {
                        "Listing created!"
                    } else {
                        "Listing created (location may not be accurate)"
                    }
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                    requireActivity().onBackPressedDispatcher.onBackPressed()

                } catch (e: Exception) {
                    // Handle API/DB errors gracefully
                    Log.e("CreateListingFragment", "Error creating listing: $e", e)
                    Toast.makeText(requireContext(), "Failed to create listing: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}