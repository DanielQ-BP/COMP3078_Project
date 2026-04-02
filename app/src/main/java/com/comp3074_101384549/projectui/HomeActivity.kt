package com.comp3074_101384549.projectui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.comp3074_101384549.projectui.data.local.AuthPreferences
import com.comp3074_101384549.projectui.databinding.ActivityHomeBinding
import com.comp3074_101384549.projectui.ui.home.HomeFragment
import com.comp3074_101384549.projectui.ui.listings.BecomeOwnerFragment
import com.comp3074_101384549.projectui.ui.listings.CreateListingFragment
import com.comp3074_101384549.projectui.ui.listings.MyListingsFragment
import com.comp3074_101384549.projectui.ui.payment.PaymentFragment
import com.comp3074_101384549.projectui.ui.profile.ProfileFragment
import com.comp3074_101384549.projectui.ui.reservations.ReservedListingsFragment
import com.comp3074_101384549.projectui.ui.settings.SettingsFragment
import com.comp3074_101384549.projectui.ui.support.SupportFragment
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var authPreferences: AuthPreferences

    private val THEME_PREFS = "AppThemePrefs"
    private val KEY_THEME_MODE = "theme_mode"

    override fun onCreate(savedInstanceState: Bundle?) {
        applySavedTheme()
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        authPreferences = AuthPreferences(applicationContext)

        setupBottomNav()
        setupDrawerMenu()
        applyRoleToDrawer()

        if (savedInstanceState == null) {
            openFragment(HomeFragment())
            binding.bottomNav.selectedItemId = R.id.homeFragment
        }
    }

    override fun onResume() {
        super.onResume()
        applyRoleToDrawer()
    }

    private fun applySavedTheme() {
        val prefs = getSharedPreferences(THEME_PREFS, MODE_PRIVATE)
        val mode = prefs.getString(KEY_THEME_MODE, "light")
        if (mode == "dark") {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }

    /**
     * Owner-only items (Create Listing, My Listings) show when [AuthPreferences.currentMode] is owner.
     * The single [R.id.nav_become_owner] row becomes Become / Switch to Owner / Switch to Driver.
     */
    private fun applyRoleToDrawer() {
        lifecycleScope.launch {
            val hasOwner = authPreferences.hasOwnerAccount.first()
            val mode = authPreferences.currentMode.first()
            val menu = binding.navigationView.menu

            val inOwnerMode = mode == AuthPreferences.MODE_OWNER

            menu.findItem(R.id.nav_listings_created)?.isVisible = inOwnerMode
            menu.findItem(R.id.nav_my_listings)?.isVisible = inOwnerMode

            val switchItem = menu.findItem(R.id.nav_become_owner)
            switchItem?.isVisible = true
            switchItem?.title = when {
                !hasOwner -> "⭐ Become a Spot Owner"
                !inOwnerMode -> "Switch to Owner Mode"
                else -> "Switch to Driver Mode"
            }
        }
    }

    private fun setupBottomNav() {
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.homeFragment -> {
                    openFragment(HomeFragment())
                    true
                }
                R.id.addFragment -> {
                    lifecycleScope.launch {
                        val inOwnerMode = authPreferences.isInOwnerMode.first()
                        val hasOwner = authPreferences.hasOwnerAccount.first()
                        when {
                            inOwnerMode -> openFragment(CreateListingFragment())
                            !hasOwner -> openFragment(BecomeOwnerFragment())
                            else -> Toast.makeText(
                                this@HomeActivity,
                                "Switch to Owner Mode from the menu to create listings.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                    true
                }
                R.id.drawerMenu -> {
                    binding.drawerLayout.openDrawer(GravityCompat.END)
                    true
                }
                else -> false
            }
        }
    }

    private fun setupDrawerMenu() {
        binding.navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {

                R.id.nav_profile -> {
                    openFragment(ProfileFragment())
                    binding.drawerLayout.closeDrawers()
                    true
                }

                R.id.nav_become_owner -> {
                    binding.drawerLayout.closeDrawers()
                    lifecycleScope.launch {
                        val hasOwner = authPreferences.hasOwnerAccount.first()
                        val inOwnerMode = authPreferences.isInOwnerMode.first()
                        when {
                            !hasOwner -> openFragment(BecomeOwnerFragment())
                            !inOwnerMode -> {
                                authPreferences.setCurrentMode(AuthPreferences.MODE_OWNER)
                                applyRoleToDrawer()
                                Toast.makeText(
                                    this@HomeActivity,
                                    "Switched to Owner Mode",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            else -> {
                                authPreferences.setCurrentMode(AuthPreferences.MODE_DRIVER)
                                applyRoleToDrawer()
                                Toast.makeText(
                                    this@HomeActivity,
                                    "Switched to Driver Mode",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                    true
                }

                R.id.nav_listings_created -> {
                    openFragment(CreateListingFragment())
                    binding.drawerLayout.closeDrawers()
                    true
                }

                R.id.nav_listings_reserved -> {
                    openFragment(ReservedListingsFragment())
                    binding.drawerLayout.closeDrawers()
                    true
                }

                R.id.nav_my_listings -> {
                    openFragment(MyListingsFragment())
                    binding.drawerLayout.closeDrawers()
                    true
                }

                R.id.nav_payment_methods -> {
                    openFragment(PaymentFragment())
                    binding.drawerLayout.closeDrawers()
                    true
                }

                R.id.nav_settings -> {
                    openFragment(SettingsFragment())
                    binding.drawerLayout.closeDrawers()
                    true
                }

                R.id.nav_logout -> {
                    binding.drawerLayout.closeDrawers()
                    showLogoutConfirmation()
                    true
                }

                R.id.nav_help -> {
                    openFragment(SupportFragment())
                    binding.drawerLayout.closeDrawers()
                    true
                }

                else -> false
            }
        }
    }

    fun openFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                android.R.anim.fade_in,
                android.R.anim.fade_out
            )
            .replace(R.id.homeFragmentContainer, fragment)
            .commit()
    }

    private fun showLogoutConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout") { _, _ -> performLogout() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performLogout() {
        lifecycleScope.launch {
            authPreferences.clearAuthDetails()
            Toast.makeText(this@HomeActivity, "Logged out successfully", Toast.LENGTH_SHORT).show()
            val intent = Intent(this@HomeActivity, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }
    }
}
