package com.freshtrack.ocr.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.freshtrack.ocr.R
import com.freshtrack.ocr.auth.PasswordValidator
import com.freshtrack.ocr.auth.SupabaseAuthRepository
import com.freshtrack.ocr.auth.SupabaseClientProvider
import com.google.android.material.button.MaterialButton
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

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

        btnUpdatePassword.setOnClickListener {
            updatePassword()
        }

        // LoginActivity handles the reset link and creates the recovery session.
        // Here we wait for that session before allowing a password change.
        waitForRecoverySession()
    }

    private fun waitForRecoverySession() {
        lifecycleScope.launch {
            try {
                val auth = SupabaseClientProvider.client.auth

                withContext(Dispatchers.IO) {
                    auth.awaitInitialization()
                }

                // The session from the email link can take a moment to be saved.
                // Wait up to 8 seconds for it instead of failing right away.
                val authenticated = withTimeoutOrNull(8_000L) {
                    auth.sessionStatus.first { it is SessionStatus.Authenticated }
                }

                if (authenticated != null) {
                    btnUpdatePassword.isEnabled = true
                } else {
                    Log.e(TAG, "No recovery session after waiting")
                    showInvalidLinkMessage()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Recovery session check failed", e)
                showInvalidLinkMessage()
            }
        }
    }

    private fun showInvalidLinkMessage() {
        Toast.makeText(
            this,
            "Invalid or expired reset link. Request a new one.",
            Toast.LENGTH_LONG
        ).show()
        finish()
    }

    private fun updatePassword() {
        val password = etNewPassword.text.toString()
        val confirmPassword = etConfirmPassword.text.toString()

        if (password.isBlank() || confirmPassword.isBlank()) {
            Toast.makeText(this, "Please fill both fields.", Toast.LENGTH_SHORT).show()
            return
        }

        val error = PasswordValidator.validate(password)
        if (error != null) {
            etNewPassword.error = error
            return
        }

        if (password != confirmPassword) {
            etConfirmPassword.error = "Passwords do not match"
            return
        }

        btnUpdatePassword.isEnabled = false

        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    authRepository.updatePassword(password)
                    authRepository.logout()
                }

                Toast.makeText(
                    this@ResetPasswordActivity,
                    "Password updated successfully! Please log in.",
                    Toast.LENGTH_LONG
                ).show()

                val loginIntent = Intent(this@ResetPasswordActivity, LoginActivity::class.java)
                loginIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(loginIntent)
                finish()

            } catch (e: Exception) {
                Log.e(TAG, "Password update failed", e)
                Toast.makeText(
                    this@ResetPasswordActivity,
                    e.message ?: "Could not update password.",
                    Toast.LENGTH_LONG
                ).show()
                btnUpdatePassword.isEnabled = true
            }
        }
    }

    companion object {
        private const val TAG = "ResetPasswordActivity"
    }
}