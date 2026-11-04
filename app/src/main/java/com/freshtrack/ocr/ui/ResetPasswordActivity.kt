package com.freshtrack.ocr.ui

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.freshtrack.ocr.R
import com.freshtrack.ocr.auth.SupabaseAuthRepository
import com.freshtrack.ocr.auth.SupabaseClientProvider
import com.google.android.material.button.MaterialButton
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch
import io.github.jan.supabase.auth.parseSessionFromUrl

class ResetPasswordActivity : AppCompatActivity() {

    private val authRepository = SupabaseAuthRepository()

    private lateinit var etNewPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var btnUpdatePassword: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_reset_password)

        etNewPassword = findViewById(R.id.etNewPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        btnUpdatePassword = findViewById(R.id.btnUpdatePassword)

        btnUpdatePassword.isEnabled = false

        // Process the Supabase password recovery link
        processRecoveryLink(intent.data?.toString())

        btnUpdatePassword.setOnClickListener {
            updatePassword()
        }
    }

    private fun processRecoveryLink(callbackUrl: String?) {

        if (callbackUrl.isNullOrBlank()) {
            showInvalidLinkMessage()
            return
        }

        lifecycleScope.launch {
            try {
                val auth = SupabaseClientProvider.client.auth

                // Parse the session from the incoming recovery URL
                val session = auth.parseSessionFromUrl(callbackUrl)

                // Import the session into Supabase
                auth.importSession(session)

                // Enable password update after successful verification
                btnUpdatePassword.isEnabled = true

            } catch (e: Exception) {
                showInvalidLinkMessage(
                    e.message ?: "Could not verify reset link."
                )
            }
        }
    }

    private fun showInvalidLinkMessage(
        message: String = "Invalid or expired reset link. Request a new one."
    ) {
        Toast.makeText(
            this,
            message,
            Toast.LENGTH_LONG
        ).show()

        finish()
    }

    private fun updatePassword() {

        val password = etNewPassword.text.toString()
        val confirmPassword = etConfirmPassword.text.toString()

        if (password.isBlank() || confirmPassword.isBlank()) {
            Toast.makeText(
                this,
                "Please fill both fields.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (password.length < 6) {
            Toast.makeText(
                this,
                "Password must be at least 6 characters.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (password != confirmPassword) {
            etConfirmPassword.error = "Passwords do not match"
            return
        }

        btnUpdatePassword.isEnabled = false

        lifecycleScope.launch {
            try {
                // Update the user's password
                authRepository.updatePassword(password)

                Toast.makeText(
                    this@ResetPasswordActivity,
                    "Password updated successfully!",
                    Toast.LENGTH_LONG
                ).show()

                // Sign out after password update
                authRepository.logout()

                // Return to login screen
                val loginIntent = Intent(
                    this@ResetPasswordActivity,
                    LoginActivity::class.java
                )

                loginIntent.flags =
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TASK

                startActivity(loginIntent)
                finish()

            } catch (e: Exception) {

                Toast.makeText(
                    this@ResetPasswordActivity,
                    e.message ?: "Could not update password.",
                    Toast.LENGTH_LONG
                ).show()

                btnUpdatePassword.isEnabled = true
            }
        }
    }
}
