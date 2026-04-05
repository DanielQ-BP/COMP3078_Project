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
import com.comp3074_101384549.projectui.BuildConfig
import com.comp3074_101384549.projectui.R
import com.comp3074_101384549.projectui.data.local.AppDatabase
import com.comp3074_101384549.projectui.data.local.AuthPreferences
import com.comp3074_101384549.projectui.data.remote.ApiService
import com.comp3074_101384549.projectui.data.remote.AuthInterceptor
import com.comp3074_101384549.projectui.model.Listing
import com.comp3074_101384549.projectui.utils.MapUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class EditListingFragment : Fragment() {

    private lateinit var listingRepository: ListingRepository
    private lateinit var authPreferences: AuthPreferences

    private var listingId: String = ""
    private var originalLatitude: Double = 0.0
    private var originalLongitude: Double = 0.0

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
        return inflater.inflate(R.layout.fragment_edit_listing, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Get listing data from arguments
        listingId = arguments?.getString("listingId") ?: ""
        val address = arguments?.getString("address") ?: ""
        val price = arguments?.getDouble("price") ?: 0.0
        val availability = arguments?.getString("availability") ?: ""
        val description = arguments?.getString("description") ?: ""
        val isActive = arguments?.getBoolean("isActive") ?: true
        originalLatitude = arguments?.getDouble("latitude") ?: 0.0
        originalLongitude = arguments?.getDouble("longitude") ?: 0.0

        // Get views
        val addressInput = view.findViewById<TextInputEditText>(R.id.editTextAddress)
        val priceInput = view.findViewById<TextInputEditText>(R.id.editTextPrice)
        val availabilityInput = view.findViewById<TextInputEditText>(R.id.editTextAvailability)
        val descriptionInput = view.findViewById<TextInputEditText>(R.id.editTextDescription)
        val activeSwitch = view.findViewById<SwitchMaterial>(R.id.switchActive)
        val saveButton = view.findViewById<Button>(R.id.buttonSave)
        val cancelButton = view.findViewById<Button>(R.id.buttonCancel)

        // Pre-fill the form
        addressInput.setText(address)
        priceInput.setText(price.toString())
        availabilityInput.setText(availability)
        descriptionInput.setText(description)
        activeSwitch.isChecked = isActive

        // Cancel button
        cancelButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Save button
        saveButton.setOnClickListener {
            val newAddress = addressInput.text.toString()
            val newPrice = priceInput.text.toString().toDoubleOrNull() ?: 0.0
            val newAvailability = availabilityInput.text.toString()
            val newDescription = descriptionInput.text.toString()
            val newIsActive = activeSwitch.isChecked

            if (newAddress.isEmpty() || newAvailability.isEmpty() || newDescription.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill out all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    if (!isAdded) return@launch

                    val userId = authPreferences.userId.first()
                    if (userId == null) {
                        if (isAdded) {
                            Toast.makeText(requireContext(), "Please login", Toast.LENGTH_SHORT).show()
                        }
                        return@launch
                    }

                    // Re-geocode if address changed
                    val ctx = context ?: return@launch
                    var newLatitude = originalLatitude
                    var newLongitude = originalLongitude

                    if (newAddress != address) {
                        val latLng = try {
                            MapUtils.getLatLngFromAddress(ctx, newAddress)
                        } catch (e: Exception) {
                            Log.e("EditListingFragment", "Geocoding failed: $e", e)
                            null
                        }
                        if (latLng != null) {
                            newLatitude = latLng.latitude
                            newLongitude = latLng.longitude
                        }
                    }

                    val updatedListing = Listing(
                        id = listingId,
                        address = newAddress,
                        pricePerHour = newPrice,
                        availability = newAvailability,
                        description = newDescription,
                        isActive = newIsActive,
                        latitude = newLatitude,
                        longitude = newLongitude,
                        userId = userId
                    )

                    listingRepository.updateListing(updatedListing)

                    if (isAdded) {
                        Toast.makeText(requireContext(), "Listing updated!", Toast.LENGTH_SHORT).show()
                        parentFragmentManager.popBackStack()
                    }

                } catch (e: Exception) {
                    Log.e("EditListingFragment", "Error updating listing: $e", e)
                    if (isAdded) {
                        Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}
