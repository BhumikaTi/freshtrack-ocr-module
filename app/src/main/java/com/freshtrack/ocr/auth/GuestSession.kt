package com.freshtrack.ocr.auth

import android.content.Context
import com.freshtrack.ocr.data.ProductDatabase

object GuestSession {

    private const val PREFS_NAME = "freshtrack_prefs"
    private const val KEY_IS_GUEST = "is_guest"

    // In-memory flag, read by CurrentUserProvider.
    @Volatile
    var isGuestActive: Boolean = false
        private set

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun start(context: Context) {
        prefs(context).edit().putBoolean(KEY_IS_GUEST, true).apply()
        isGuestActive = true
    }

    // Call on app start, so guest mode survives closing the app.
    fun restore(context: Context) {
        isGuestActive = prefs(context).getBoolean(KEY_IS_GUEST, false)
    }

    fun isGuest(context: Context): Boolean {
        return prefs(context).getBoolean(KEY_IS_GUEST, false)
    }

    fun clear(context: Context) {
        prefs(context).edit().putBoolean(KEY_IS_GUEST, false).apply()
        isGuestActive = false
    }

    /**
     * Moves all guest products and shopping items to the logged-in user,
     * then ends guest mode. Call this after a successful login.
     */
    suspend fun moveGuestDataTo(context: Context, userId: String) {
        val database = ProductDatabase.getDatabase(context)
        val guestId = CurrentUserProvider.GUEST_USER_ID

        database.productDao().moveGuestProducts(
            newUserId = userId,
            guestId = guestId
        )

        database.shoppingItemDao().moveGuestItems(
            newUserId = userId,
            guestId = guestId
        )

        clear(context)
    }
}