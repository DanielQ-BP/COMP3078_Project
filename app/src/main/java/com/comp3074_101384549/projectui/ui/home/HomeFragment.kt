package com.comp3074_101384549.projectui.ui.home

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.comp3074_101384549.projectui.R
import com.comp3074_101384549.projectui.data.local.AppDatabase
import com.comp3074_101384549.projectui.data.local.AuthPreferences
import com.comp3074_101384549.projectui.data.remote.ApiService
import com.comp3074_101384549.projectui.databinding.FragmentHomeBinding

import com.comp3074_101384549.projectui.repository.ListingRepository
import com.comp3074_101384549.projectui.model.Listing

import com.comp3074_101384549.projectui.ui.adapter.ListingAdapter
import com.comp3074_101384549.projectui.ui.listings.ListingDetailsFragment
import com.comp3074_101384549.projectui.utils.MapUtils
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class HomeFragment : Fragment(), OnMapReadyCallback {

    private lateinit var listingRepository: ListingRepository
    private lateinit var authPreferences: AuthPreferences

    // If not using DI, you would need to instantiate or provide it here.

    private var googleMap: GoogleMap? = null
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var listingAdapter: ListingAdapter

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1002
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        val db = AppDatabase.getDatabase(context)
        val listingDao = db.listingDao()

        // TODO: replace with your real backend base URL when ready
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
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        super.onViewCreated(view, savedInstanceState)

        val mapFragment = childFragmentManager.findFragmentById(R.id.mapView) as? SupportMapFragment
        mapFragment?.getMapAsync(this)

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
        val recyclerView = view.findViewById<RecyclerView>(R.id.listViewListings)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = listingAdapter

        val addressInput = view.findViewById<EditText>(R.id.editTextAddress)
        val maxPriceInput = view.findViewById<EditText>(R.id.editTextMaxPrice)
        val searchButton = view.findViewById<Button>(R.id.buttonSearch)

        // Load all listings on startup

        loadAllListings()


        searchButton.setOnClickListener {
            val address = addressInput.text.toString().trim()
            val maxPrice = maxPriceInput.text.toString().toDoubleOrNull()

            // Perform search
            lifecycleScope.launch {
                val userId = authPreferences.userId.first()
                if (userId == null) {
                    Toast.makeText(requireContext(), "Please login to search listings", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val results = listingRepository.searchListings(userId, address, maxPrice)

                if (results.isEmpty()) {
                    Toast.makeText(requireContext(), "No parking spots found", Toast.LENGTH_SHORT).show()
                    updateListings(emptyList())
                } else {
                    Toast.makeText(requireContext(), "Found ${results.size} parking spot(s)", Toast.LENGTH_SHORT).show()
                    updateListings(results)

                }
            }
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            map.isMyLocationEnabled = true
        } else {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }

        val defaultLocation = LatLng(43.6532, -79.3832)
        MapUtils.moveCameraToPosition(map, defaultLocation, 12f)
    }

    override fun onResume() {
        super.onResume()
        // Reload all listings when returning to this fragment
        loadAllListings()
    }

    private fun loadAllListings() {
        lifecycleScope.launch {
            val userId = authPreferences.userId.first()
            if (userId == null) {
                Toast.makeText(requireContext(), "Please login to view listings", Toast.LENGTH_SHORT).show()
                updateListings(emptyList())
                return@launch
            }

            val listings = listingRepository.getAllListings(userId)
            updateListings(listings)
        }
    }

    private fun updateListings(listings: List<Listing>) {
        // Update RecyclerView
        listingAdapter.updateListings(listings)

        // Update markers on the map
        googleMap?.let { map ->
            map.clear()
            listings.forEach { listing ->
                val latLng =
                    MapUtils.getLatLngFromAddress(requireContext(), listing.address)
                latLng?.let {
                    MapUtils.addMarker(
                        map,
                        it,
                        listing.address,
                        "$${listing.pricePerHour}/hr"
                    )
                }
            }

            if (listings.isNotEmpty()) {
                val firstLocation =
                    MapUtils.getLatLngFromAddress(requireContext(), listings[0].address)
                firstLocation?.let {
                    MapUtils.moveCameraToPosition(map, it, 12f)
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED
            ) {
                googleMap?.isMyLocationEnabled = true
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}