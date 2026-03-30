package com.comp3074_101384549.projectui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.comp3074_101384549.projectui.databinding.ActivityMainBinding
import com.comp3074_101384549.projectui.data.local.AuthPreferences // CORRECTED IMPORT
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var authPreferences: AuthPreferences // NEW FIELD

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize AuthPreferences (NEW)
        authPreferences = AuthPreferences(applicationContext)

        // Check for existing session and redirect immediately (NEW LOGIC)
        lifecycleScope.launch {
            val token = authPreferences.authToken.first()
            if (token != null) {
                // Token found, user is already logged in. Redirect to Home.
                startActivity(Intent(this@MainActivity, HomeActivity::class.java))
                finish()
                return@launch
            }
        }
        // END NEW LOGIC

        // Existing button listeners
        binding.registerButton.setOnClickListener {
            val intent = Intent(this, RegistrationActivity::class.java)
            startActivity(intent)
        }

        binding.loginSkipButton.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }
    }
}