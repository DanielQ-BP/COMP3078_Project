package com.comp3074_101384549.projectui

import android.content.Intent
import android.content.Context

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.comp3074_101384549.projectui.data.local.AuthPreferences
import com.comp3074_101384549.projectui.databinding.ActivityLoginBinding

import com.comp3074_101384549.projectui.HomeActivity
import com.comp3074_101384549.projectui.data.remote.ApiService
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var authPreferences: AuthPreferences

    // In a Hilt setup, this would be @Inject lateInit var
    private lateinit var apiService: ApiService

    


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        authPreferences = AuthPreferences(applicationContext)


        binding.loginButton.setOnClickListener {
            val username = binding.usernameEditText.text.toString()
            val password = binding.passwordEditText.text.toString()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter both fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }


            // Check against "MockUserDB"
            val sharedPrefs = getSharedPreferences("MockUserDB", Context.MODE_PRIVATE)
            val storedPassword = sharedPrefs.getString("user_$username", null)
            val storedEmail = sharedPrefs.getString("email_$username", "user@example.com")

            if (storedPassword == password) {
                // Login Success!
                lifecycleScope.launch {
                    // Save session details
                    authPreferences.saveAuthDetails(
                        token = "dummy_token_123",
                        userId = username,
                        username = username,
                        email = storedEmail ?: ""
                    )

                    // Also update ProfileFragment's separate SharedPreferences so it shows up immediately
                    val profilePrefs = getSharedPreferences("ParkSpotPrefs", Context.MODE_PRIVATE)
                    with(profilePrefs.edit()) {
                        putString("username", username)
                        apply()
                    }

                    Toast.makeText(this@LoginActivity, "Login Successful!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this@LoginActivity, HomeActivity::class.java))
                    finish()
                }
            } else {
                Toast.makeText(this, "Invalid Username or Password", Toast.LENGTH_SHORT).show()
            }
        }

        binding.logoutButton.setOnClickListener {
            finish()
        }
    }
}
