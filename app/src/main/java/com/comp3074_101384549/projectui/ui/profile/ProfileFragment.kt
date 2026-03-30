package com.comp3074_101384549.projectui.ui.profile

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.comp3074_101384549.projectui.data.local.AppDatabase
import com.comp3074_101384549.projectui.data.local.AuthPreferences
import com.comp3074_101384549.projectui.databinding.FragmentProfileBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    // SharedPreferences keys
    private val PREFS_NAME = "ParkSpotPrefs"
    private val KEY_PROFILE_IMAGE_URI = "profile_image_uri"
    private val KEY_MEMBER_SINCE = "member_since"

    private lateinit var prefs: SharedPreferences
    private lateinit var authPreferences: AuthPreferences

    // Store selected image URI (current session)
    private var selectedImageUri: Uri? = null



    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) {
                selectedImageUri = uri
                binding.profileImage.setImageURI(uri)
                saveProfileImageUri(uri)
            }
        }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        // Initialize SharedPreferences and AuthPreferences
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        authPreferences = AuthPreferences(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Load profile data
        loadProfile()
        loadProfileImage()
        loadUserStats()

        // By default, Change Photo button is hidden (only visible in edit mode)
        binding.btnChangePhoto.visibility = View.GONE

        // Change photo: open gallery
        binding.btnChangePhoto.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        binding.editProfileButton.setOnClickListener {
            binding.readOnlyContainer.visibility = View.GONE
            binding.editContainer.visibility = View.VISIBLE
            binding.editProfileButton.visibility = View.GONE
            binding.btnChangePhoto.visibility = View.VISIBLE
        }

        binding.saveButton.setOnClickListener {
            val newBio = binding.bioEditText.text.toString().trim()
            val newPhone = binding.phoneEditText.text.toString().trim()

            if (newBio.isEmpty()) {
                Toast.makeText(
                    requireContext(),
                    "Please enter a bio",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            with(prefs.edit()) {
                putString("bio", newBio)
                putString("phone", newPhone)
                apply()
            }

            binding.readOnlyContainer.visibility = View.VISIBLE
            binding.editContainer.visibility = View.GONE
            binding.editProfileButton.visibility = View.VISIBLE
            binding.btnChangePhoto.visibility = View.GONE

            loadProfile()

            Toast.makeText(
                requireContext(),
                "Profile updated",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun loadProfile() {
        lifecycleScope.launch {
            // Get username and email from AuthPreferences (persists from login)
            val username = authPreferences.username.first() ?: "User"
            val email = authPreferences.email.first() ?: ""

            // Get additional profile info from SharedPreferences
            val bio = prefs.getString("bio", "") ?: ""
            val phone = prefs.getString("phone", "") ?: ""
            var memberSince = prefs.getString(KEY_MEMBER_SINCE, null)

            // Set member since date if not set (first time loading)
            if (memberSince == null) {
                val currentDate = java.text.SimpleDateFormat("MMM yyyy", java.util.Locale.getDefault())
                    .format(java.util.Date())
                prefs.edit().putString(KEY_MEMBER_SINCE, currentDate).apply()
                memberSince = currentDate
            }

            // Update UI
            binding.usernameTextView.text = username
            binding.emailTextView.text = email.ifEmpty { "No email provided" }
            binding.bioReadOnly.text = bio.ifEmpty { "No bio yet. Click Edit Profile to add one." }
            binding.phoneReadOnly.text = phone.ifEmpty { "Not provided" }
            binding.memberSinceTextView.text = "Member since $memberSince"

            // Set edit fields
            binding.bioEditText.setText(bio)
            binding.phoneEditText.setText(phone)
        }
    }

    private fun loadUserStats() {
        lifecycleScope.launch {
            val userId = authPreferences.userId.first()
            if (userId != null) {
                val db = AppDatabase.getDatabase(requireContext())
                val listingDao = db.listingDao()
                
                // Count user's listings
                val listings = listingDao.getAllListings(userId).first()
                val totalListings = listings.size
                val activeListings = listings.count { it.isActive }

                binding.totalListingsTextView.text = totalListings.toString()
                binding.activeListingsTextView.text = activeListings.toString()
            } else {
                binding.totalListingsTextView.text = "0"
                binding.activeListingsTextView.text = "0"
            }
        }
    }

    private fun saveProfileImageUri(uri: Uri) {
        prefs.edit()
            .putString(KEY_PROFILE_IMAGE_URI, uri.toString())
            .apply()
    }

    private fun loadProfileImage() {
        val uriString = prefs.getString(KEY_PROFILE_IMAGE_URI, null)
        if (!uriString.isNullOrEmpty()) {
            try {
                val uri = Uri.parse(uriString)
                selectedImageUri = uri
                binding.profileImage.setImageURI(uri)
            } catch (e: Exception) {
                // If we fail to load (e.g. no permission anymore), just clear it and use default
                prefs.edit().remove(KEY_PROFILE_IMAGE_URI).apply()
                // profileImage will keep the default src from XML
            }
        }

    }

    override fun onResume() {
        super.onResume()
        // Refresh profile data when returning to this fragment
        loadProfile()
        loadUserStats()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
