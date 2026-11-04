package com.freshtrack.ocr.auth

import io.github.jan.supabase.auth.auth

object CurrentUserProvider {

    fun getUserId(): String? {
        return SupabaseClientProvider.client.auth
            .currentUserOrNull()
            ?.id
    }
}