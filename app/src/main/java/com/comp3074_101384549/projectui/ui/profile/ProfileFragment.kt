package com.comp3074_101384549.projectui.ui.profile

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.comp3074_101384549.projectui.BuildConfig
import com.comp3074_101384549.projectui.MainActivity
import com.comp3074_101384549.projectui.R
import com.comp3074_101384549.projectui.data.local.AppDatabase
import com.comp3074_101384549.projectui.data.local.AuthPreferences
import com.comp3074_101384549.projectui.data.remote.ApiService
import com.comp3074_101384549.projectui.data.remote.AuthInterceptor
import com.comp3074_101384549.projectui.databinding.FragmentProfileBinding
import com.comp3074_101384549.projectui.ui.support.MyTicketsFragment
import com.comp3074_101384549.projectui.ui.support.SubmitTicketFragment
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val PREFS_NAME = "ParkSpotPrefs"
    private val KEY_PROFILE_IMAGE_URI = "profile_image_uri"
    private val KEY_MEMBER_SINCE = "member_since"

    private lateinit var prefs: SharedPreferences
    private lateinit var authPreferences: AuthPreferences
    private lateinit var apiService: ApiService

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null && _binding != null) {
                binding.profileImage.setImageURI(uri)
                prefs.edit().putString(KEY_PROFILE_IMAGE_URI, uri.toString()).apply()
            }
        }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        authPreferences = AuthPreferences(context)
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(authPreferences))
            .build()
        val retrofit = Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        apiService = retrofit.create(ApiService::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadProfile()
        loadProfileImage()
        loadUserStats()

        // Change photo via camera overlay
        binding.cameraOverlay.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        // Edit profile button
        binding.buttonEditProfile.setOnClickListener {
            binding.personalViewGroup.visibility = View.GONE
            binding.personalEditGroup.visibility = View.VISIBLE
            binding.buttonEditProfile.visibility = View.GONE
        }

        // Cancel edit
        binding.buttonCancelEdit.setOnClickListener {
            binding.personalViewGroup.visibility = View.VISIBLE
            binding.personalEditGroup.visibility = View.GONE
            binding.buttonEditProfile.visibility = View.VISIBLE
        }

        // Save profile
        binding.buttonSaveProfile.setOnClickListener {
            val newBio = binding.bioEditText.text.toString().trim()
            val newPhone = binding.phoneEditText.text.toString().trim()

            with(prefs.edit()) {
                putString("bio", newBio)
                putString("phone", newPhone)
                apply()
            }

            binding.personalViewGroup.visibility = View.VISIBLE
            binding.personalEditGroup.visibility = View.GONE
            binding.buttonEditProfile.visibility = View.VISIBLE

            loadProfile()
            Toast.makeText(requireContext(), "Profile updated", Toast.LENGTH_SHORT).show()
        }

        // Support tickets
        binding.buttonSubmitTicket.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.homeFragmentContainer, SubmitTicketFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.buttonViewTickets.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.homeFragmentContainer, MyTicketsFragment())
                .addToBackStack(null)
                .commit()
        }

        // Logout
        binding.buttonLogout.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.logout)
                .setMessage(R.string.logout_confirm_message)
                .setPositiveButton(R.string.logout) { _, _ ->
                    lifecycleScope.launch {
                        authPreferences.clearAuthDetails()
                        val intent = Intent(requireContext(), MainActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                    }
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
    }

    override fun onResume() {
        super.onResume()
        loadProfile()
        loadUserStats()
    }

    private fun loadProfile() {
        lifecycleScope.launch {
            val userId = authPreferences.userId.first()

            // Refresh from backend
            if (userId != null) {
                try {
                    val userProfile = apiService.getUserProfile(userId)
                    val token = authPreferences.authToken.first() ?: ""
                    val mode = authPreferences.currentMode.first() ?: AuthPreferences.MODE_DRIVER
                    val hasOwner = authPreferences.hasOwnerAccount.first()
                    val role = authPreferences.role.first() ?: "user"
                    authPreferences.saveAuthDetails(
                        token = token,
                        userId = userId,
                        username = userProfile.username,
                        email = userProfile.email ?: "",
                        hasOwnerAccount = hasOwner,
                        currentMode = mode,
                        role = role
                    )
                    // Parse member since from backend createdAt
                    userProfile.createdAt?.let { createdAt ->
                        try {
                            val inputFmt = java.text.SimpleDateFormat(
                                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.getDefault()
                            )
                            inputFmt.timeZone = java.util.TimeZone.getTimeZone("UTC")
                            val date = inputFmt.parse(createdAt)
                            if (date != null) {
                                val outputFmt = java.text.SimpleDateFormat("MMM yyyy", java.util.Locale.getDefault())
                                prefs.edit().putString(KEY_MEMBER_SINCE, outputFmt.format(date)).apply()
                            }
                        } catch (e: Exception) { /* keep existing */ }
                    }
                } catch (e: Exception) {
                    // Fall back to cached values
                }
            }

            val username = authPreferences.username.first() ?: "User"
            val email = authPreferences.email.first() ?: ""
            val bio = prefs.getString("bio", "") ?: ""
            val phone = prefs.getString("phone", "") ?: ""
            val isOwnerMode = authPreferences.isInOwnerMode.first()
            val hasOwner = authPreferences.hasOwnerAccount.first()
            var memberSince = prefs.getString(KEY_MEMBER_SINCE, null)

            if (memberSince == null) {
                val currentDate = java.text.SimpleDateFormat("MMM yyyy", java.util.Locale.getDefault())
                    .format(java.util.Date())
                prefs.edit().putString(KEY_MEMBER_SINCE, currentDate).apply()
                memberSince = currentDate
            }

            if (_binding == null) return@launch

            binding.headerUsername.text = username
            binding.headerEmail.text = email.ifEmpty { "No email set" }
            binding.accountEmailValue.text = email.ifEmpty { "Not provided" }
            binding.memberSinceValue.text = memberSince ?: ""
            binding.bioDisplay.text = bio.ifEmpty { "No bio yet. Tap Edit Profile to add one." }
            binding.phoneDisplay.text = phone.ifEmpty { "Not provided" }
            binding.bioEditText.setText(bio)
            binding.phoneEditText.setText(phone)

            // Role badge
            if (isOwnerMode) {
                binding.roleBadge.text = "Owner Mode"
                binding.roleBadge.setBackgroundResource(R.drawable.bg_role_badge_owner)
                binding.roleBadge.setTextColor(requireContext().getColor(android.R.color.white))
            } else {
                binding.roleBadge.text = "Driver Mode"
                binding.roleBadge.setBackgroundResource(R.drawable.bg_role_badge_driver)
                binding.roleBadge.setTextColor(requireContext().getColor(android.R.color.white))
            }

            // Owner banner
            if (hasOwner) {
                binding.ownerModeBanner.visibility = View.VISIBLE
                binding.ownerBannerText.text = if (isOwnerMode)
                    "You are in Owner Mode. Manage your listings from the Listings tab."
                else
                    "You have an owner account. Switch to Owner Mode to manage listings."
            } else {
                binding.ownerModeBanner.visibility = View.GONE
            }
        }
    }

    private fun loadUserStats() {
        lifecycleScope.launch {
            val userId = authPreferences.userId.first()
            if (userId != null) {
                val db = AppDatabase.getDatabase(requireContext())
                val listings = db.listingDao().getAllListings(userId).first()
                val bookings = db.bookingDao().getAllBookings(userId).first()
                if (_binding == null) return@launch
                binding.statTotalListings.text = listings.size.toString()
                binding.statActiveListings.text = listings.count { it.isActive }.toString()
                binding.statBookingsMade.text = bookings.size.toString()
            } else {
                if (_binding == null) return@launch
                binding.statTotalListings.text = "0"
                binding.statActiveListings.text = "0"
                binding.statBookingsMade.text = "0"
            }
        }
    }

    private fun loadProfileImage() {
        val uriString = prefs.getString(KEY_PROFILE_IMAGE_URI, null)
        if (!uriString.isNullOrEmpty()) {
            try {
                binding.profileImage.setImageURI(Uri.parse(uriString))
            } catch (e: Exception) {
                prefs.edit().remove(KEY_PROFILE_IMAGE_URI).apply()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
