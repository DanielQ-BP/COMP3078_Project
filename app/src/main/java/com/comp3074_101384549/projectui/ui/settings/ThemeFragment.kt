package com.comp3074_101384549.projectui.ui.settings

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.comp3074_101384549.projectui.databinding.FragmentThemeBinding

class ThemeFragment : Fragment() {

    private var _binding: FragmentThemeBinding? = null
    private val binding get() = _binding!!

    private val PREFS_NAME = "AppThemePrefs"
    private val KEY_THEME_MODE = "theme_mode" // "light" or "dark"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentThemeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentMode = prefs.getString(KEY_THEME_MODE, "light")

        if (currentMode == "dark") {
            binding.rbDark.isChecked = true
        } else {
            binding.rbLight.isChecked = true
        }

        binding.btnApplyTheme.setOnClickListener {
            val selectedId = binding.themeRadioGroup.checkedRadioButtonId
            val newMode = if (selectedId == binding.rbDark.id) "dark" else "light"

            prefs.edit().putString(KEY_THEME_MODE, newMode).apply()

            if (newMode == "dark") {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }

            Toast.makeText(requireContext(), "Theme updated", Toast.LENGTH_SHORT).show()

            requireActivity().recreate()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
