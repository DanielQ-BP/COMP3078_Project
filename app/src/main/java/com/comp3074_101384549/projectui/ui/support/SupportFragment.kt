package com.comp3074_101384549.projectui.ui.support

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.comp3074_101384549.projectui.R
import com.comp3074_101384549.projectui.databinding.FragmentSupportBinding

class SupportFragment : Fragment() {

    private var _binding: FragmentSupportBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSupportBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
    }

    private fun setupClickListeners() {
        // Email click - open email client
        binding.textViewEmail.setOnClickListener {
            val email = "groupdias.parking@gmail.com"
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:$email")
                putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
                putExtra(Intent.EXTRA_SUBJECT, "ParkSpot Support Request")
            }
            try {
                startActivity(Intent.createChooser(intent, "Send email via"))
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "No email app found", Toast.LENGTH_SHORT).show()
            }
        }

        // Phone click - open dialer
        binding.textViewPhone.setOnClickListener {
            val phoneNumber = "+12345678910"
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$phoneNumber")
            }
            try {
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Unable to open dialer", Toast.LENGTH_SHORT).show()
            }
        }

        // Social media icons (placeholder - can be updated with actual links)
        binding.iconTwitter.setOnClickListener {
            Toast.makeText(requireContext(), "Twitter: @ParkSpot", Toast.LENGTH_SHORT).show()
        }

        binding.iconInstagram.setOnClickListener {
            Toast.makeText(requireContext(), "Instagram: @ParkSpot", Toast.LENGTH_SHORT).show()
        }

        binding.iconYouTube.setOnClickListener {
            Toast.makeText(requireContext(), "YouTube: ParkSpot Channel", Toast.LENGTH_SHORT).show()
        }

        binding.iconLinkedIn.setOnClickListener {
            Toast.makeText(requireContext(), "LinkedIn: ParkSpot", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
