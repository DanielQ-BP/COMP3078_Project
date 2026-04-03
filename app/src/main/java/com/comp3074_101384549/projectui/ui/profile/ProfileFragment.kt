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
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.comp3074_101384549.projectui.HomeActivity
import com.comp3074_101384549.projectui.R
import com.comp3074_101384549.projectui.data.local.AppDatabase
import com.comp3074_101384549.projectui.data.local.AuthPreferences
import com.comp3074_101384549.projectui.databinding.FragmentProfileBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val prefsName = "ParkSpotPrefs"
    private val keyProfileImageUri = "profile_image_uri"
    private val mockUserDbName = "MockUserDB"
    private val keyMemberSincePrefix = "memberSince_"

    private lateinit var prefs: SharedPreferences
    private lateinit var authPreferences: AuthPreferences

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null && _binding != null) {
                binding.profileImage.setImageURI(uri)
                saveProfileImageUri(uri)
            }
        }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        authPreferences = AuthPreferences(context)
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

        binding.profileAvatarContainer.setOnClickListener { pickImageLauncher.launch("image/*") }
        binding.cameraOverlay.setOnClickListener { pickImageLauncher.launch("image/*") }

        binding.buttonEditProfile.setOnClickListener { enterEditMode() }
        binding.buttonCancelEdit.setOnClickListener { cancelEditMode() }
        binding.buttonSaveProfile.setOnClickListener { savePersonalInfo() }

        binding.buttonLogout.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.logout)
                .setMessage(R.string.logout_confirm_message)
                .setPositiveButton(R.string.logout) { _, _ ->
                    (activity as? HomeActivity)?.performLogout()
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }

        loadProfileImage()
        refreshProfileContent()
    }

    override fun onResume() {
        super.onResume()
        refreshProfileContent()
    }

    private fun refreshProfileContent() {
        viewLifecycleOwner.lifecycleScope.launch {
            val username = authPreferences.username.first() ?: "User"
            val email = authPreferences.email.first() ?: ""
            val userId = authPreferences.userId.first()
            val inOwnerMode = authPreferences.isInOwnerMode.first()
            val hasOwnerAccount = authPreferences.hasOwnerAccount.first()

            if (!isAdded) return@launch

            binding.headerUsername.text = username
            binding.headerEmail.text = email.ifEmpty { getString(R.string.profile_no_email) }

            if (inOwnerMode) {
                binding.roleBadge.text = getString(R.string.role_badge_owner)
                binding.roleBadge.setBackgroundResource(R.drawable.bg_role_badge_owner)
                binding.roleBadge.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.parkspot_dark_green)
                )
            } else {
                binding.roleBadge.text = getString(R.string.role_badge_driver)
                binding.roleBadge.setBackgroundResource(R.drawable.bg_role_badge_driver)
                binding.roleBadge.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.white)
                )
            }

            val mockDb = requireContext().getSharedPreferences(mockUserDbName, Context.MODE_PRIVATE)
            val memberSince = userId?.let { mockDb.getString("$keyMemberSincePrefix$it", null) }
            binding.memberSinceValue.text = memberSince ?: getString(R.string.member_since_unknown)
            binding.accountEmailValue.text = email.ifEmpty { getString(R.string.profile_no_email) }

            binding.ownerModeBanner.visibility =
                if (hasOwnerAccount && !inOwnerMode) View.VISIBLE else View.GONE

            val bio = prefs.getString("bio", "") ?: ""
            val phone = prefs.getString("phone", "") ?: ""
            if (binding.personalEditGroup.visibility != View.VISIBLE) {
                binding.bioDisplay.text = bio.ifEmpty { getString(R.string.profile_bio_placeholder) }
                binding.phoneDisplay.text = phone.ifEmpty { getString(R.string.profile_phone_placeholder) }
                binding.bioEditText.setText(bio)
                binding.phoneEditText.setText(phone)
            }

            if (userId == null) {
                if (!isAdded) return@launch
                binding.statTotalListings.text = "0"
                binding.statActiveListings.text = "0"
                binding.statBookingsMade.text = "0"
                return@launch
            }

            val db = AppDatabase.getDatabase(requireContext())
            val bookings = db.bookingDao().getAllBookings(userId).first()
            if (!isAdded) return@launch
            binding.statBookingsMade.text = bookings.size.toString()

            if (inOwnerMode) {
                val listings = db.listingDao().getAllListings(userId).first()
                if (!isAdded) return@launch
                binding.statTotalListings.text = listings.size.toString()
                binding.statActiveListings.text = listings.count { it.isActive }.toString()
            } else {
                binding.statTotalListings.text = "0"
                binding.statActiveListings.text = "0"
            }
        }
    }

    private fun enterEditMode() {
        val bio = prefs.getString("bio", "") ?: ""
        val phone = prefs.getString("phone", "") ?: ""
        binding.bioEditText.setText(bio)
        binding.phoneEditText.setText(phone)
        binding.personalViewGroup.visibility = View.GONE
        binding.personalEditGroup.visibility = View.VISIBLE
        binding.buttonEditProfile.visibility = View.GONE
    }

    private fun cancelEditMode() {
        binding.personalEditGroup.visibility = View.GONE
        binding.personalViewGroup.visibility = View.VISIBLE
        binding.buttonEditProfile.visibility = View.VISIBLE
    }

    private fun savePersonalInfo() {
        val newBio = binding.bioEditText.text?.toString()?.trim() ?: ""
        val newPhone = binding.phoneEditText.text?.toString()?.trim() ?: ""

        prefs.edit()
            .putString("bio", newBio)
            .putString("phone", newPhone)
            .apply()

        cancelEditMode()
        binding.bioDisplay.text = newBio.ifEmpty { getString(R.string.profile_bio_placeholder) }
        binding.phoneDisplay.text = newPhone.ifEmpty { getString(R.string.profile_phone_placeholder) }

        Toast.makeText(requireContext(), R.string.profile_updated_toast, Toast.LENGTH_SHORT).show()
    }

    private fun saveProfileImageUri(uri: Uri) {
        prefs.edit()
            .putString(keyProfileImageUri, uri.toString())
            .apply()
    }

    private fun loadProfileImage() {
        val uriString = prefs.getString(keyProfileImageUri, null)
        if (!uriString.isNullOrEmpty()) {
            try {
                val uri = Uri.parse(uriString)
                binding.profileImage.setImageURI(uri)
            } catch (_: Exception) {
                prefs.edit().remove(keyProfileImageUri).apply()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
