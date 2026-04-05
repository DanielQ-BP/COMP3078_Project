package com.comp3074_101384549.projectui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import com.comp3074_101384549.projectui.data.local.AuthPreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private lateinit var authPreferences: AuthPreferences

    private val THEME_PREFS = "AppThemePrefs"
    private val KEY_THEME_MODE = "theme_mode"

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply saved theme before inflating
        applySavedTheme()

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        authPreferences = AuthPreferences(applicationContext)

        // Animate the logo
        val logo = findViewById<View>(R.id.splashLogo)
        val title = findViewById<View>(R.id.splashTitle)

        val fadeIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)
        fadeIn.duration = 1000
        logo.startAnimation(fadeIn)
        title.startAnimation(fadeIn)

        // Check login status and navigate after delay
        Handler(Looper.getMainLooper()).postDelayed({
            checkLoginAndNavigate()
        }, 2000) // 2 second splash
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

    private fun checkLoginAndNavigate() {
        lifecycleScope.launch {
            val userId = authPreferences.userId.first()
            val role = authPreferences.role.first() ?: "user"

            val intent = when {
                userId != null && role == "admin" ->
                    Intent(this@SplashActivity, AdminActivity::class.java)
                userId != null ->
                    Intent(this@SplashActivity, HomeActivity::class.java)
                else ->
                    Intent(this@SplashActivity, MainActivity::class.java)
            }

            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }
    }
}
