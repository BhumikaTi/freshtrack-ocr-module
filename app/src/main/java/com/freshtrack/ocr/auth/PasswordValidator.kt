package com.freshtrack.ocr.auth

object PasswordValidator {

    const val MIN_LENGTH = 6

    /**
     * Returns null if the password is valid, otherwise a message explaining what's wrong.
     */
    fun validate(password: String): String? {
        if (password.length < MIN_LENGTH) {
            return "Password must be at least $MIN_LENGTH characters."
        }
        if (!password.any { it.isUpperCase() }) {
            return "Password must contain at least 1 uppercase letter."
        }
        if (!password.any { it.isDigit() }) {
            return "Password must contain at least 1 number."
        }
        return null
    }
}