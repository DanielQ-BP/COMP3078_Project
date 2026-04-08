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
import com.comp3074_101384549.projectui.model.User
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
            binding.adminIdInputLayout.visibility = if (checked) View.VISIBLE else View.GONE
            if (!checked) binding.adminIdEditText.text?.clear()
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
                lifecycleScope.launch {
                    performUserLogin(username, password)
                }
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

            // Fetch listings to determine if user already has an owner account
            val hasOwner = try {
                val sharedPrefs = getSharedPreferences("MockUserDB", Context.MODE_PRIVATE)
                val wasRegisteredOwner = sharedPrefs.getBoolean("isOwner_$userId", false)
                wasRegisteredOwner || api.getUserListings(userId).isNotEmpty()
            } catch (_: Exception) {
                val sharedPrefs = getSharedPreferences("MockUserDB", Context.MODE_PRIVATE)
                sharedPrefs.getBoolean("isOwner_$userId", false)
            }

            authPreferences.saveAuthDetails(
                token = token,
                userId = userId,
                username = uname,
                email = email,
                hasOwnerAccount = hasOwner,
                currentMode = AuthPreferences.MODE_DRIVER, // admins don't need owner mode
                role = role,
            )

            getSharedPreferences("ParkSpotPrefs", Context.MODE_PRIVATE).edit()
                .putString("username", uname)
                .apply()

            Toast.makeText(this@LoginActivity, "Admin login successful", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this@LoginActivity, AdminActivity::class.java))
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

    private suspend fun performUserLogin(username: String, password: String) {
        val retrofit = Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val api = retrofit.create(ApiService::class.java)

        try {
            val token = api.login(User(username = username, password = password))
            val payload = JwtPayloadUtil.readPayload(token)
            if (payload == null) {
                Toast.makeText(this@LoginActivity, "Invalid server response", Toast.LENGTH_SHORT).show()
                return
            }
            val userId = payload.optString("id", "")
            val uname = payload.optString("username", username)
            val email = payload.optString("email", "")
            val role = payload.optString("role", "user")
            if (userId.isEmpty()) {
                Toast.makeText(this@LoginActivity, "Invalid token", Toast.LENGTH_SHORT).show()
                return
            }

            // Fetch profile to check if user already has an owner account
            val hasOwner = try {
                val sharedPrefs = getSharedPreferences("MockUserDB", Context.MODE_PRIVATE)
                val wasRegisteredOwner = sharedPrefs.getBoolean("isOwner_$userId", false)
                wasRegisteredOwner || api.getUserListings(userId).isNotEmpty()
            } catch (_: Exception) {
                val sharedPrefs = getSharedPreferences("MockUserDB", Context.MODE_PRIVATE)
                sharedPrefs.getBoolean("isOwner_$userId", false)
            }

            authPreferences.saveAuthDetails(
                token = token,
                userId = userId,
                username = uname,
                email = email,
                hasOwnerAccount = hasOwner,
                currentMode = if (hasOwner) AuthPreferences.MODE_OWNER else AuthPreferences.MODE_DRIVER,
                role = role,
            )
            getSharedPreferences("ParkSpotPrefs", Context.MODE_PRIVATE).edit()
                .putString("username", uname)
                .apply()

            Toast.makeText(this@LoginActivity, "Login Successful!", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this@LoginActivity, HomeActivity::class.java))
            finish()
        } catch (e: HttpException) {
            val msg = when (e.code()) {
                401 -> "Invalid username or password"
                403 -> "Admin accounts must use the Admin login"
                else -> "Login failed (${e.code()})"
            }
            Toast.makeText(this@LoginActivity, msg, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(
                this@LoginActivity,
                "Cannot reach server. Check that the backend is running.",
                Toast.LENGTH_LONG,
            ).show()
        }
    }
}