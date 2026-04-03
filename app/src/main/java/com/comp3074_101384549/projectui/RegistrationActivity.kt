package com.comp3074_101384549.projectui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.comp3074_101384549.projectui.databinding.ActivityRegistrationBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RegistrationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegistrationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegistrationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.registerConfirmButton.setOnClickListener {
            val username = binding.usernameEditText.text.toString().trim()
            val email = binding.emailEditText.text.toString().trim()
            val password = binding.passwordEditText.text.toString()

            if (username.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty()) {

                val memberSince = SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(Date())

                val sharedPrefs = getSharedPreferences("MockUserDB", Context.MODE_PRIVATE)
                with(sharedPrefs.edit()) {
                    putString("user_$username", password)
                    putString("email_$username", email)
                    putBoolean("isOwner_$username", false)
                    putString("memberSince_$username", memberSince)
                    apply()
                }

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
