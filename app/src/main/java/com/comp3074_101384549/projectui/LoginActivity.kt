package com.comp3074_101384549.projectui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.comp3074_101384549.projectui.BuildConfig
import com.comp3074_101384549.projectui.data.local.AuthPreferences
import com.comp3074_101384549.projectui.data.remote.ApiService
import com.comp3074_101384549.projectui.databinding.ActivityLoginBinding
import com.comp3074_101384549.projectui.model.AdminLoginRequest
import com.comp3074_101384549.projectui.utils.JwtPayloadUtil
import kotlinx.coroutines.launch
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var authPreferences: AuthPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        authPreferences = AuthPreferences(applicationContext)

        binding.adminLoginCheckBox.setOnCheckedChangeListener { _, checked ->
            binding.adminIdEditText.visibility = if (checked) View.VISIBLE else View.GONE
            if (!checked) binding.adminIdEditText.text.clear()
        }

        binding.loginButton.setOnClickListener {
            val username = binding.usernameEditText.text.toString().trim()
            val password = binding.passwordEditText.text.toString()
            val adminId = binding.adminIdEditText.text.toString().trim()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter username and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (binding.adminLoginCheckBox.isChecked) {
                if (adminId.isEmpty()) {
                    Toast.makeText(this, "Please enter your Admin ID", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                lifecycleScope.launch {
                    performAdminLogin(username, password, adminId)
                }
            } else {
                performMockUserLogin(username, password)
            }
        }

        binding.logoutButton.setOnClickListener {
            finish()
        }
    }

    private suspend fun performAdminLogin(username: String, password: String, adminId: String) {
        val retrofit = Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val api = retrofit.create(ApiService::class.java)

        try {
            val token = api.adminLogin(AdminLoginRequest(username, password, adminId))
            val payload = JwtPayloadUtil.readPayload(token)
            if (payload == null) {
                Toast.makeText(this@LoginActivity, "Invalid server response", Toast.LENGTH_SHORT).show()
                return
            }
            val userId = payload.optString("id", "")
            val uname = payload.optString("username", username)
            val email = payload.optString("email", "")
            val role = payload.optString("role", "admin")
            if (userId.isEmpty()) {
                Toast.makeText(this@LoginActivity, "Invalid token", Toast.LENGTH_SHORT).show()
                return
            }

            authPreferences.saveAuthDetails(
                token = token,
                userId = userId,
                username = uname,
                email = email,
                role = role,
            )
            getSharedPreferences("ParkSpotPrefs", Context.MODE_PRIVATE).edit()
                .putString("username", uname)
                .apply()

            Toast.makeText(this@LoginActivity, "Admin login successful", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this@LoginActivity, HomeActivity::class.java))
            finish()
        } catch (_: HttpException) {
            Toast.makeText(this@LoginActivity, "Invalid credentials or server error", Toast.LENGTH_SHORT).show()
        } catch (_: Exception) {
            Toast.makeText(
                this@LoginActivity,
                "Cannot reach server. Check API_BASE_URL in local.properties and that the API is running.",
                Toast.LENGTH_LONG,
            ).show()
        }
    }

    private fun performMockUserLogin(username: String, password: String) {
        val sharedPrefs = getSharedPreferences("MockUserDB", Context.MODE_PRIVATE)
        val storedPassword = sharedPrefs.getString("user_$username", null)
        val storedEmail = sharedPrefs.getString("email_$username", "user@example.com")

        if (storedPassword == password) {
            lifecycleScope.launch {
                authPreferences.saveAuthDetails(
                    token = "dummy_token_123",
                    userId = username,
                    username = username,
                    email = storedEmail ?: "",
                    role = "user",
                )
                getSharedPreferences("ParkSpotPrefs", Context.MODE_PRIVATE).edit()
                    .putString("username", username)
                    .apply()

                Toast.makeText(this@LoginActivity, "Login Successful!", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this@LoginActivity, HomeActivity::class.java))
                finish()
            }
        } else {
            Toast.makeText(this, "Invalid Username or Password", Toast.LENGTH_SHORT).show()
        }
    }
}
