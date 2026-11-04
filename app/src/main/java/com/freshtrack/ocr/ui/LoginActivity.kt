package com.freshtrack.ocr.ui

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.freshtrack.ocr.R
import com.freshtrack.ocr.auth.SupabaseAuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.freshtrack.ocr.auth.SupabaseClientProvider
import io.github.jan.supabase.auth.auth

class LoginActivity : AppCompatActivity() {

    private val authRepository = SupabaseAuthRepository()

    private lateinit var btnEmailTab: MaterialButton
    private lateinit var btnPhoneTab: MaterialButton
    private lateinit var emailLayout: LinearLayout
    private lateinit var phoneLayout: LinearLayout
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var etPhone: EditText
    private lateinit var btnLogin: MaterialButton
    private lateinit var btnSendOtp: MaterialButton
    private lateinit var btnSignup: MaterialButton
    private lateinit var tvForgotPassword: TextView

    private var isPhoneMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    SupabaseClientProvider.client.auth.awaitInitialization()
                }

                val session = SupabaseClientProvider.client.auth.currentSessionOrNull()

                if (session != null) {
                    startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                    finish()
                }
            } catch (e: Exception) {
                // If session check fails, keep the login screen available.
            }
        }
        setContentView(R.layout.activity_login)

        btnEmailTab = findViewById(R.id.btnEmailTab)
        btnPhoneTab = findViewById(R.id.btnPhoneTab)
        emailLayout = findViewById(R.id.emailLoginLayout)
        phoneLayout = findViewById(R.id.phoneLoginLayout)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        etPhone = findViewById(R.id.etPhone)
        btnLogin = findViewById(R.id.btnLogin)
        btnSendOtp = findViewById(R.id.btnSendOtp)
        btnSignup = findViewById(R.id.btnSignup)
        tvForgotPassword = findViewById(R.id.tvForgotPassword)

        showEmailMode()

        btnEmailTab.setOnClickListener {
            showEmailMode()
        }

        btnPhoneTab.setOnClickListener {
            showPhoneMode()
        }

        btnLogin.setOnClickListener {
            loginWithEmail()
        }

        btnSendOtp.setOnClickListener {
            Toast.makeText(
                this,
                "Phone OTP will be available after SMS setup.",
                Toast.LENGTH_LONG
            ).show()
        }

        tvForgotPassword.setOnClickListener {
            resetPassword()
        }

        btnSignup.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }
    }

    private fun showEmailMode() {
        isPhoneMode = false
        emailLayout.visibility = View.VISIBLE
        phoneLayout.visibility = View.GONE
        tvForgotPassword.visibility = View.VISIBLE
        btnLogin.visibility = View.VISIBLE

        updateTabColors()
    }

    private fun showPhoneMode() {
        isPhoneMode = true
        emailLayout.visibility = View.GONE
        phoneLayout.visibility = View.VISIBLE
        tvForgotPassword.visibility = View.GONE
        btnLogin.visibility = View.GONE

        updateTabColors()
    }

    private fun updateTabColors() {
        val selectedColor = ContextCompat.getColor(this, R.color.green_primary)
        val unselectedColor = ContextCompat.getColor(this, R.color.card_dark)
        val selectedText = android.graphics.Color.WHITE
        val unselectedText = ContextCompat.getColor(this, R.color.text_primary)

        btnEmailTab.backgroundTintList = ColorStateList.valueOf(
            if (!isPhoneMode) selectedColor else unselectedColor
        )
        btnPhoneTab.backgroundTintList = ColorStateList.valueOf(
            if (isPhoneMode) selectedColor else unselectedColor
        )

        btnEmailTab.setTextColor(if (!isPhoneMode) selectedText else unselectedText)
        btnPhoneTab.setTextColor(if (isPhoneMode) selectedText else unselectedText)
    }

    private fun loginWithEmail() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter email and password.", Toast.LENGTH_SHORT).show()
            return
        }

        btnLogin.isEnabled = false

        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    authRepository.login(email, password)
                }

                Toast.makeText(
                    this@LoginActivity,
                    "Login successful!",
                    Toast.LENGTH_SHORT
                ).show()

                startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                finish()

            } catch (e: Exception) {
                Toast.makeText(
                    this@LoginActivity,
                    e.message ?: "Login failed. Please try again.",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                btnLogin.isEnabled = true
            }
        }
    }

    private fun resetPassword() {
        val email = etEmail.text.toString().trim()

        if (email.isEmpty()) {
            Toast.makeText(this, "Enter your email first.", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    authRepository.resetPassword(email)
                }
                Toast.makeText(
                    this@LoginActivity,
                    "Password reset email requested.",
                    Toast.LENGTH_LONG
                ).show()
            } catch (e: Exception) {
                Toast.makeText(
                    this@LoginActivity,
                    e.message ?: "Could not send reset email.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
