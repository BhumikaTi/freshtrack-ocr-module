package com.freshtrack.ocr.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.freshtrack.ocr.R
import com.freshtrack.ocr.auth.PasswordValidator
import com.freshtrack.ocr.auth.SupabaseAuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.freshtrack.ocr.auth.PasswordVisibility

class SignupActivity : AppCompatActivity() {

    private val authRepository = SupabaseAuthRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        val name = findViewById<EditText>(R.id.etSignupName)
        val email = findViewById<EditText>(R.id.etSignupEmail)
        val password = findViewById<EditText>(R.id.etSignupPassword)
        val confirmPassword = findViewById<EditText>(R.id.etConfirmPassword)
        val createAccount = findViewById<Button>(R.id.btnCreateAccount)
        val goToLogin = findViewById<TextView>(R.id.tvGoToLogin)
        PasswordVisibility.attach(password)
        PasswordVisibility.attach(confirmPassword)

        createAccount.setOnClickListener {
            val userName = name.text.toString().trim()
            val userEmail = email.text.toString().trim()
            val userPassword = password.text.toString()
            val confirm = confirmPassword.text.toString()
            val passwordError = PasswordValidator.validate(userPassword)

            when {
                userName.isEmpty() ->
                    name.error = "Enter your name"

                userEmail.isEmpty() ->
                    email.error = "Enter your email"

                passwordError != null ->
                    password.error = passwordError

                userPassword != confirm ->
                    confirmPassword.error = "Passwords do not match"

                else -> {
                    createAccount.isEnabled = false

                    lifecycleScope.launch {
                        try {
                            withContext(Dispatchers.IO) {
                                authRepository.signUp(userEmail, userPassword)
                            }

                            Toast.makeText(
                                this@SignupActivity,
                                "Account created! Please check your email for confirmation.",
                                Toast.LENGTH_LONG
                            ).show()

                            startActivity(
                                Intent(this@SignupActivity, LoginActivity::class.java)
                            )
                            finish()

                        } catch (e: Exception) {
                            Toast.makeText(
                                this@SignupActivity,
                                e.message ?: "Signup failed",
                                Toast.LENGTH_LONG
                            ).show()
                        } finally {
                            createAccount.isEnabled = true
                        }
                    }
                }
            }
        }

        goToLogin.setOnClickListener {
            finish()
        }
    }
}