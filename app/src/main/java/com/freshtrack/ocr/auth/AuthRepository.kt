package com.freshtrack.ocr.auth

interface AuthRepository {
    suspend fun signUp(email: String, password: String)
    suspend fun login(email: String, password: String)
    suspend fun logout()
    suspend fun resetPassword(email: String)
    suspend fun updatePassword(newPassword: String)
}