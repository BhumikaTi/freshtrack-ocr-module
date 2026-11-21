package com.freshtrack.ocr.ui

import android.content.Intent
import android.graphics.Paint
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.freshtrack.ocr.R
import com.freshtrack.ocr.auth.GuestSession
import com.freshtrack.ocr.auth.SupabaseClientProvider
import com.google.android.material.button.MaterialButton
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WelcomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // If the user was in guest mode when the app was closed, go straight to home.
        GuestSession.restore(this)
        if (GuestSession.isGuestActive) {
            openHome()
            return
        }

        setContentView(R.layout.activity_welcome)

        val btnLogin = findViewById<MaterialButton>(R.id.btnWelcomeLogin)
        val btnSignup = findViewById<MaterialButton>(R.id.btnWelcomeSignup)
        val tvGuest = findViewById<TextView>(R.id.tvContinueGuest)

        // Underline the guest option, like a link.
        tvGuest.paintFlags = tvGuest.paintFlags or Paint.UNDERLINE_TEXT_FLAG

        btnLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        btnSignup.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }

        tvGuest.setOnClickListener {
            GuestSession.start(this)
            openHome()
        }

        // If the user is already logged in, skip this screen.
        checkExistingSession()
    }

    private fun checkExistingSession() {
        lifecycleScope.launch {
            try {
                val loggedIn = withContext(Dispatchers.IO) {
                    SupabaseClientProvider.client.auth.awaitInitialization()
                    SupabaseClientProvider.client.auth.currentSessionOrNull() != null
                }

                if (loggedIn) {
                    GuestSession.clear(this@WelcomeActivity)
                    openHome()
                }
            } catch (e: Exception) {
                // If the check fails, keep the welcome screen visible.
                Log.e(TAG, "Session check failed", e)
            }
        }
    }

    private fun openHome() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    companion object {
        private const val TAG = "WelcomeActivity"
    }
}