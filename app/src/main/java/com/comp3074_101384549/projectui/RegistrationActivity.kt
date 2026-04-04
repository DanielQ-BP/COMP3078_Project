package com.comp3074_101384549.projectui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.comp3074_101384549.projectui.data.local.AuthPreferences
import com.comp3074_101384549.projectui.data.remote.ApiService
import com.comp3074_101384549.projectui.databinding.ActivityRegistrationBinding
import com.comp3074_101384549.projectui.model.User
import com.comp3074_101384549.projectui.utils.JwtPayloadUtil
import kotlinx.coroutines.launch
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class RegistrationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegistrationBinding
    private lateinit var authPreferences: AuthPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegistrationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        authPreferences = AuthPreferences(applicationContext)

        binding.registerConfirmButton.setOnClickListener {
            val username = binding.usernameEditText.text.toString().trim()
            val email = binding.emailEditText.text.toString().trim()
            val password = binding.passwordEditText.text.toString()

            if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter username, email, and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                performRegistration(username, email, password)
            }
        }
    }

    private suspend fun performRegistration(username: String, email: String, password: String) {
        val retrofit = Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val api = retrofit.create(ApiService::class.java)

        try {
            val token = api.register(User(username, email, password))
            val payload = JwtPayloadUtil.readPayload(token)
            if (payload == null) {
                Toast.makeText(this, "Registration failed: invalid server response", Toast.LENGTH_SHORT).show()
                return
            }
            val userId = payload.optString("id", "")
            val uname = payload.optString("username", username)
            val emailFromToken = payload.optString("email", email)
            val role = payload.optString("role", "user")

            authPreferences.saveAuthDetails(
                token = token,
                userId = userId,
                username = uname,
                email = emailFromToken,
                hasOwnerAccount = false,
                currentMode = AuthPreferences.MODE_DRIVER,
                role = role,
            )

            Toast.makeText(this, "Registration Successful!", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        } catch (e: HttpException) {
            val msg = when (e.code()) {
                409 -> "Username or email already taken"
                else -> "Registration failed (${e.code()})"
            }
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Cannot reach server. Make sure the backend is running.", Toast.LENGTH_LONG).show()
        }
    }
}
