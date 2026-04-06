package com.comp3074_101384549.projectui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.comp3074_101384549.projectui.data.local.AuthPreferences
import com.comp3074_101384549.projectui.databinding.ActivityAdminBinding
import com.comp3074_101384549.projectui.ui.admin.AdminBookingsFragment
import com.comp3074_101384549.projectui.ui.admin.AdminUsersFragment
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class AdminActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminBinding
    private lateinit var authPreferences: AuthPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        authPreferences = AuthPreferences(applicationContext)
        val role = runBlocking { authPreferences.role.first() ?: "" }
        if (role != "admin") {
            Toast.makeText(this, "Admin access required", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        binding = ActivityAdminBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.buttonAdminLogout.setOnClickListener {
            showLogoutConfirmation()
        }

        binding.adminBottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.admin_nav_reservations -> {
                    openRootFragment(AdminBookingsFragment())
                    true
                }
                R.id.admin_nav_users -> {
                    openRootFragment(AdminUsersFragment())
                    true
                }
                else -> false
            }
        }

        if (savedInstanceState == null) {
            binding.adminBottomNav.selectedItemId = R.id.admin_nav_reservations
            openRootFragment(AdminBookingsFragment())
        }
    }

    private fun openRootFragment(fragment: Fragment) {
        supportFragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
        supportFragmentManager.beginTransaction()
            .replace(R.id.adminFragmentContainer, fragment)
            .commit()
    }

    private fun showLogoutConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Sign out of the admin dashboard?")
            .setPositiveButton("Logout") { _, _ -> performLogout() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performLogout() {
        lifecycleScope.launch {
            authPreferences.clearAuthDetails()
            Toast.makeText(this@AdminActivity, "Logged out", Toast.LENGTH_SHORT).show()
            startActivity(
                Intent(this@AdminActivity, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                },
            )
            finish()
        }
    }
}
