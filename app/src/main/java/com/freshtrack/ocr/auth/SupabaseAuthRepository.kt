package com.freshtrack.ocr.auth

import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.auth

class SupabaseAuthRepository : AuthRepository {

    private val auth = SupabaseClientProvider.client.auth

    companion object {
        const val AUTH_REDIRECT_URL = "freshtrack://auth-callback"
    }

    override suspend fun signUp(email: String, password: String) {
        auth.signUpWith(
            Email,
            redirectUrl = AUTH_REDIRECT_URL
        ) {
            this.email = email
            this.password = password
        }
    }

    override suspend fun login(email: String, password: String) {
        auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
    }

    override suspend fun logout() {
        auth.signOut()
    }

    override suspend fun resetPassword(email: String) {
        auth.resetPasswordForEmail(
            email,
            redirectUrl = AUTH_REDIRECT_URL
        )
    }

    override suspend fun updatePassword(newPassword: String) {
        auth.updateUser {
            password = newPassword
        }
    }
}
