package com.freshtrack.ocr.auth

import io.github.jan.supabase.auth.auth

object CurrentUserProvider {

    const val GUEST_USER_ID = "guest"

    /**
     * Logged-in user's ID if there is a session.
     * Otherwise "guest" when guest mode is active.
     * Null only when nobody is logged in and guest mode is off.
     */
    fun getUserId(): String? {
        val loggedInId = SupabaseClientProvider.client.auth.currentUserOrNull()?.id
        if (!loggedInId.isNullOrBlank()) return loggedInId

        return if (GuestSession.isGuestActive) GUEST_USER_ID else null
    }
}