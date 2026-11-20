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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.freshtrack.ocr.auth.PasswordVisibility

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
        PasswordVisibility.attach(etNewPassword)
        PasswordVisibility.attach(etConfirmPassword)

        btnUpdatePassword.isEnabled = false

        btnUpdatePassword.setOnClickListener {
            updatePassword()
        }

        // LoginActivity already processed the reset link and created the
        // recovery session. Here we only confirm that the session exists.
        checkRecoverySession()
    }

    private fun checkRecoverySession() {
        lifecycleScope.launch {
            try {
                val hasSession = withContext(Dispatchers.IO) {
                    SupabaseClientProvider.client.auth.awaitInitialization()
                    SupabaseClientProvider.client.auth.currentSessionOrNull() != null
                }

                if (hasSession) {
                    btnUpdatePassword.isEnabled = true
                } else {
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