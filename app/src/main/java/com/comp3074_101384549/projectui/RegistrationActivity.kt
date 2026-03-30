package com.comp3074_101384549.projectui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.comp3074_101384549.projectui.data.local.AuthPreferences
import com.comp3074_101384549.projectui.databinding.ActivityRegistrationBinding
import kotlinx.coroutines.launch

class RegistrationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegistrationBinding
    private lateinit var authPreferences: AuthPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegistrationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        authPreferences = AuthPreferences(applicationContext)

        binding.registerConfirmButton.setOnClickListener {
            val username = binding.usernameEditText.text.toString()
            val email = binding.emailEditText.text.toString()
            val password = binding.passwordEditText.text.toString()

            if (username.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty()) {
                
                // Save to SharedPreferences for "Login" to check later (Simple Mock Database)
                val sharedPrefs = getSharedPreferences("MockUserDB", Context.MODE_PRIVATE)
                with(sharedPrefs.edit()) {
                    putString("user_$username", password) // Store password key as "user_username"
                    putString("email_$username", email)
                    apply()
                }

                // Also auto-login by saving to AuthPreferences immediately if you want, 
                // OR just redirect to login. Let's redirect to login to simulate flow.
                Toast.makeText(this, "Registration Successful! Please Login.", Toast.LENGTH_SHORT).show()
                
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
                finish()

            } else {
                Toast.makeText(this, "Please enter username, email, and password", Toast.LENGTH_SHORT).show()
            }
        }
    }
}