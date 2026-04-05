package com.comp3074_101384549.projectui.ui.listings

import android.content.Context
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.comp3074_101384549.projectui.R
import com.comp3074_101384549.projectui.data.local.AuthPreferences
import com.comp3074_101384549.projectui.ui.home.HomeFragment
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BecomeOwnerFragment : Fragment() {

    private lateinit var authPreferences: AuthPreferences
    private var checkTerms: CheckBox? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        authPreferences = AuthPreferences(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_become_owner, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            if (authPreferences.hasOwnerAccount.first()) {
                Toast.makeText(
                    requireContext(),
                    "Use the menu to switch to Owner Mode.",
                    Toast.LENGTH_LONG
                ).show()
                parentFragmentManager.beginTransaction()
                    .replace(R.id.homeFragmentContainer, HomeFragment())
                    .commit()
                return@launch
            }
            bindBecomeOwnerForm(view)
        }
    }

    private fun bindBecomeOwnerForm(view: View) {
        val editName        = view.findViewById<TextInputEditText>(R.id.editOwnerName)
        val editPhone       = view.findViewById<TextInputEditText>(R.id.editOwnerPhone)
        val editSpotAddress = view.findViewById<TextInputEditText>(R.id.editOwnerSpotAddress)
        checkTerms          = view.findViewById(R.id.checkTerms)
        val btnRegister     = view.findViewById<Button>(R.id.buttonRegisterOwner)
        val btnBack         = view.findViewById<Button>(R.id.buttonBack)

        // Make "Terms and Conditions" text clickable inside the checkbox label
        setupTermsCheckbox()

        // Listen for acceptance result coming back from TermsAndConditionsFragment
        parentFragmentManager.setFragmentResultListener("terms_result", viewLifecycleOwner) { _, bundle ->
            if (bundle.getBoolean("accepted", false)) {
                checkTerms?.isChecked = true
            }
        }

        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        btnRegister.setOnClickListener {
            val name        = editName.text.toString().trim()
            val phone       = editPhone.text.toString().trim()
            val spotAddress = editSpotAddress.text.toString().trim()

            when {
                name.isEmpty() -> {
                    editName.error = "Please enter your full name"
                    return@setOnClickListener
                }
                phone.isEmpty() -> {
                    editPhone.error = "Please enter a contact number"
                    return@setOnClickListener
                }
                spotAddress.isEmpty() -> {
                    editSpotAddress.error = "Please enter your parking spot address"
                    return@setOnClickListener
                }
                checkTerms?.isChecked != true -> {
                    Toast.makeText(
                        requireContext(),
                        "Please read and accept the Spot Owner Terms",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }
            }

            viewLifecycleOwner.lifecycleScope.launch {
                if (!isAdded) return@launch

                val userId = authPreferences.userId.first()
                if (userId == null) {
                    Toast.makeText(requireContext(), "Session error. Please log in again.", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                // Persist in MockUserDB so it survives logout → login
                val ctx = context ?: return@launch
                val sharedPrefs = ctx.getSharedPreferences("MockUserDB", Context.MODE_PRIVATE)
                sharedPrefs.edit().putBoolean("isOwner_$userId", true).apply()

                // Update DataStore — hasOwnerAccount = true, currentMode = "owner"
                authPreferences.registerAsOwner()

                if (!isAdded) return@launch

                Toast.makeText(
                    requireContext(),
                    "Welcome, Spot Owner! You can now create and manage listings.",
                    Toast.LENGTH_LONG
                ).show()

                val homeActivity = activity as? com.comp3074_101384549.projectui.HomeActivity
                homeActivity?.openFragment(HomeFragment())
            }
        }
    }

    /**
     * Makes "Terms and Conditions" inside the CheckBox label a tappable link
     * that opens TermsAndConditionsFragment.
     */
    private fun setupTermsCheckbox() {
        val checkbox = checkTerms ?: return
        val fullText = "I have read and agree to the Spot Owner Terms and Conditions"
        val linkText = "Terms and Conditions"
        val spannable = SpannableString(fullText)
        val start = fullText.indexOf(linkText)
        val end = start + linkText.length

        val clickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                parentFragmentManager.beginTransaction()
                    .replace(R.id.homeFragmentContainer, TermsAndConditionsFragment())
                    .addToBackStack(null)
                    .commit()
            }
        }

        val green = ContextCompat.getColor(requireContext(), R.color.parkspot_green)
        spannable.setSpan(clickableSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannable.setSpan(ForegroundColorSpan(green), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        // CheckBox doesn't support MovementMethod directly — use a sibling TextView trick
        checkbox.text = ""
        val termsLabel = view?.findViewById<TextView>(R.id.textTermsLabel) ?: return
        termsLabel.text = spannable
        termsLabel.movementMethod = LinkMovementMethod.getInstance()
    }
}