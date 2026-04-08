package com.comp3074_101384549.projectui.ui.listings

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.comp3074_101384549.projectui.BuildConfig
import com.comp3074_101384549.projectui.R
import com.comp3074_101384549.projectui.data.local.AppDatabase
import com.comp3074_101384549.projectui.data.local.AuthPreferences
import com.comp3074_101384549.projectui.data.remote.ApiClient
import com.comp3074_101384549.projectui.data.remote.ApiService
import com.comp3074_101384549.projectui.data.remote.AuthInterceptor
import com.comp3074_101384549.projectui.model.BookingEntity
import com.comp3074_101384549.projectui.model.CreateBookingRequest
import com.comp3074_101384549.projectui.repository.BookingRepository
import retrofit2.HttpException
import com.comp3074_101384549.projectui.utils.DirectionsApiHelper
import com.comp3074_101384549.projectui.utils.MapUtils
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Polyline
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

class ListingDetailsFragment : Fragment(), OnMapReadyCallback {
    private var googleMap: GoogleMap? = null
    private var currentPolyline: Polyline? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var destinationLatLng: LatLng? = null

    private lateinit var bookingRepository: BookingRepository
    private lateinit var authPreferences: AuthPreferences

    // Listing details
    private var listingId: String = ""
    private var address: String = ""
    private var pricePerHour: Double = 0.0

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        val db = AppDatabase.getDatabase(context)
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
        bookingRepository = BookingRepository(apiService, db.bookingDao())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_listing_details, container, false)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        val mapFragment = childFragmentManager.findFragmentById(R.id.mapView) as? SupportMapFragment
        mapFragment?.getMapAsync(this)

        // Get listing data from arguments
        listingId = arguments?.getString("listingId") ?: UUID.randomUUID().toString()
        address = arguments?.getString("address") ?: "123 Main St"
        pricePerHour = arguments?.getDouble("price") ?: 0.0
        val availability = arguments?.getString("availability") ?: "N/A"
        val description = arguments?.getString("description") ?: "No description"

        // Display listing data
        view.findViewById<TextView>(R.id.textAddress)?.text = "Address: $address"
        view.findViewById<TextView>(R.id.textPrice)?.text = "Price: $$pricePerHour/hour"
        view.findViewById<TextView>(R.id.textAvailability)?.text = "Available: $availability"
        view.findViewById<TextView>(R.id.textDescription)?.text = description

        destinationLatLng = MapUtils.getLatLngFromAddress(requireContext(), address)

        setupButtons(view)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupButtons(view: View) {
        view.findViewById<Button>(R.id.buttonShowRoute)?.setOnClickListener {
            showRoute()
        }

        view.findViewById<Button>(R.id.buttonNavigate)?.setOnClickListener {
            navigateToDestination()
        }

        view.findViewById<Button>(R.id.buttonReserve)?.setOnClickListener {
            showBookingDialog()
        }

        view.findViewById<Button>(R.id.buttonGoBack)?.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun showBookingDialog() {
        val calendar = Calendar.getInstance()
        val today = String.format(
            "%04d-%02d-%02d",
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH) + 1,
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        val nowHour = calendar.get(Calendar.HOUR_OF_DAY)
        val nowMinute = calendar.get(Calendar.MINUTE)

        // Date picker
        DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                val selectedDate = String.format("%04d-%02d-%02d", year, month + 1, day)
                val isToday = selectedDate == today

                // Start time picker
                TimePickerDialog(
                    requireContext(),
                    { _, startHour, startMinute ->

                        // Reject start times in the past when booking for today
                        if (isToday && (startHour * 60 + startMinute) < (nowHour * 60 + nowMinute)) {
                            Toast.makeText(
                                requireContext(),
                                "Start time must be in the future",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@TimePickerDialog
                        }

                        val startTime = String.format("%02d:%02d", startHour, startMinute)

                        // End time picker — default to one hour after the chosen start
                        val defaultEndHour = (startHour + 1).coerceAtMost(23)
                        TimePickerDialog(
                            requireContext(),
                            { _, endHour, endMinute ->
                                val endTime = String.format("%02d:%02d", endHour, endMinute)

                                // Calculate duration and total price
                                val startMinutes = startHour * 60 + startMinute
                                val endMinutes = endHour * 60 + endMinute
                                val durationHours = (endMinutes - startMinutes) / 60.0

                                if (durationHours <= 0) {
                                    Toast.makeText(requireContext(), "End time must be after start time", Toast.LENGTH_SHORT).show()
                                    return@TimePickerDialog
                                }

                                val totalPrice = durationHours * pricePerHour

                                // Show confirmation dialog
                                showBookingConfirmation(selectedDate, startTime, endTime, durationHours, totalPrice)
                            },
                            defaultEndHour,
                            0,
                            true
                        ).apply {
                            setTitle("Select End Time")
                        }.show()
                    },
                    if (isToday) nowHour else 9,
                    if (isToday) nowMinute else 0,
                    true
                ).apply {
                    setTitle("Select Start Time")
                }.show()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).apply {
            setTitle("Select Date")
            datePicker.minDate = System.currentTimeMillis()
        }.show()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun showBookingConfirmation(
        date: String,
        startTime: String,
        endTime: String,
        durationHours: Double,
        totalPrice: Double
    ) {
        if (!isAdded) return

        val message = """
            Address: $address

            Date: $date
            Time: $startTime - $endTime
            Duration: ${String.format("%.1f", durationHours)} hours

            Total: $${String.format("%.2f", totalPrice)}
        """.trimIndent()

        AlertDialog.Builder(requireContext())
            .setTitle("Confirm Booking")
            .setMessage(message)
            .setPositiveButton("Book Now") { _, _ ->
                createBooking(date, startTime, endTime, totalPrice)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createBooking(date: String, startTime: String, endTime: String, totalPrice: Double) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                if (!isAdded) return@launch

                val userId = authPreferences.userId.first()
                if (userId == null) {
                    if (isAdded) {
                        Toast.makeText(requireContext(), "Please login to book", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                val zone = ZoneId.systemDefault()
                val startZdt = LocalDate.parse(date).atTime(LocalTime.parse(startTime)).atZone(zone)
                val endZdt = LocalDate.parse(date).atTime(LocalTime.parse(endTime)).atZone(zone)
                val isoFmt = DateTimeFormatter.ISO_OFFSET_DATE_TIME
                val startIso = startZdt.format(isoFmt)
                val endIso = endZdt.format(isoFmt)

                val api = ApiClient.api(requireContext())
                val response = api.createBooking(
                    CreateBookingRequest(
                        listingId = listingId,
                        startTime = startIso,
                        endTime = endIso,
                        totalPrice = totalPrice,
                    )
                )

                val booking = BookingEntity(
                    id = response.id,
                    listingId = listingId,
                    userId = userId,
                    address = address,
                    pricePerHour = pricePerHour,
                    bookingDate = date,
                    startTime = startTime,
                    endTime = endTime,
                    totalPrice = totalPrice,
                    status = response.status.ifEmpty { "confirmed" },
                    referenceCode = response.referenceCode,
                )

                bookingRepository.saveBookingLocally(booking)

                if (isAdded) {
                    val paymentFragment = com.comp3074_101384549.projectui.ui.payment.PaymentFragment.newInstance(
                        totalPrice = totalPrice,
                        bookingId = response.id,
                        address = address,
                        referenceCode = response.referenceCode,
                        bookingDate = date,
                        startTime = startTime,
                        endTime = endTime
                    )
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.homeFragmentContainer, paymentFragment)
                        .addToBackStack(null)
                        .commit()
                }
            } catch (e: HttpException) {
                if (isAdded) {
                    val msg = if (e.code() == 409) {
                        "This spot is already booked for the selected time. Please choose a different time."
                    } else {
                        try {
                            val body = e.response()?.errorBody()?.string()
                            val json = org.json.JSONObject(body ?: "{}")
                            "Booking failed: ${json.optString("error", e.message())}"
                        } catch (_: Exception) {
                            "Booking failed: ${e.message()}"
                        }
                    }
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                if (isAdded) {
                    Toast.makeText(
                        requireContext(),
                        "Could not complete booking. The spot must exist on the server and times must be valid. ${e.message}",
                        Toast.LENGTH_LONG,
                    ).show()
                }
            }
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        destinationLatLng?.let { destination ->
            MapUtils.addMarker(map, destination, "Parking Spot", "Tap to view details")
            MapUtils.moveCameraToPosition(map, destination, 15f)
        }

        enableMyLocation()
    }

    private fun enableMyLocation() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            googleMap?.isMyLocationEnabled = true
        } else {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun showRoute() {
        val map = googleMap ?: return
        val destination = destinationLatLng ?: return

        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(requireContext(), "Location permission required", Toast.LENGTH_SHORT).show()
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val origin = LatLng(location.latitude, location.longitude)

                lifecycleScope.launch {
                    val result = DirectionsApiHelper.getDirections(origin, destination, BuildConfig.MAPS_API_KEY)

                    if (result != null) {
                        currentPolyline?.remove()
                        currentPolyline = MapUtils.drawRoute(map, result.polylinePoints, Color.BLUE, 10f)

                        val points = listOf(origin, destination)
                        MapUtils.fitBounds(map, points, 150)

                        Toast.makeText(
                            requireContext(),
                            "${result.distance} • ${result.duration}",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        Toast.makeText(requireContext(), "Failed to get route", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(requireContext(), "Unable to get current location", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun navigateToDestination() {
        destinationLatLng?.let { destination ->
            MapUtils.launchGoogleMapsNavigation(requireContext(), destination)
        } ?: run {
            Toast.makeText(requireContext(), "Destination not available", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableMyLocation()
            }
        }
    }
}
