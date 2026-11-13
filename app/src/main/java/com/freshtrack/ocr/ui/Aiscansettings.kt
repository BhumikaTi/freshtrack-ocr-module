package com.freshtrack.ocr.ui

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * The user's choice about sending label photos to Google Gemini, plus a simple internet check.
 *
 * Nothing is sent unless the user said yes. The choice is asked once and can be changed later
 * with [showToggleDialog] (call it from a menu item or a settings button).
 */
object AiScanSettings {

    private const val PREFS = "freshtrack_ai_scan"
    private const val KEY_ANSWERED = "answered"
    private const val KEY_ALLOWED = "allowed"

    fun hasAnswered(context: Context): Boolean =
        try {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getBoolean(KEY_ANSWERED, false)
        } catch (e: Exception) {
            false
        }

    fun isAllowed(context: Context): Boolean =
        try {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getBoolean(KEY_ALLOWED, false)
        } catch (e: Exception) {
            false
        }

    fun setAllowed(context: Context, allowed: Boolean) {
        try {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_ANSWERED, true)
                .putBoolean(KEY_ALLOWED, allowed)
                .apply()
        } catch (e: Exception) {
            // The choice just applies to this scan if it cannot be saved.
        }
    }

    /**
     * True when the phone has a working internet connection. If the check itself is not
     * possible (for example the ACCESS_NETWORK_STATE permission is missing) it returns true,
     * and the AI step then simply fails and falls back to offline scanning.
     */
    fun isOnline(context: Context): Boolean =
        try {
            val manager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val caps = manager.getNetworkCapabilities(manager.activeNetwork)
            caps != null &&
                    caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } catch (e: Exception) {
            true
        }

    /** First-use question. [onAnswer] gets true for "Allow", false for "Use offline only". */
    fun showConsentDialog(context: Context, onAnswer: (Boolean) -> Unit): AlertDialog =
        MaterialAlertDialogBuilder(context)
            .setTitle("Scan with AI?")
            .setMessage(
                "To read dates accurately, FreshTrack sends the photo you take or pick to " +
                        "Google's Gemini AI service, which processes it under Google's Gemini API " +
                        "terms. Only the photo is sent, not your name or account.\n\n" +
                        "Please avoid photos that show people or personal information.\n\n" +
                        "If you choose offline only, the photo stays on your phone, but dates " +
                        "are read less accurately. You can change this later."
            )
            .setCancelable(false)
            .setPositiveButton("ALLOW AI SCANNING") { _, _ ->
                setAllowed(context, true)
                onAnswer(true)
            }
            .setNegativeButton("USE OFFLINE ONLY") { _, _ ->
                setAllowed(context, false)
                onAnswer(false)
            }
            .show()

    /** Lets the user change their mind later. Call from a menu item or settings button. */
    fun showToggleDialog(context: Context, onDone: () -> Unit = {}) {
        val allowed = isAllowed(context)
        MaterialAlertDialogBuilder(context)
            .setTitle("AI label scanning")
            .setMessage(
                if (allowed) {
                    "AI scanning is ON. Label photos are sent to Google Gemini to read dates."
                } else {
                    "AI scanning is OFF. Photos stay on your phone and dates are read offline, " +
                            "which is less accurate."
                }
            )
            .setPositiveButton(if (allowed) "TURN OFF" else "TURN ON") { _, _ ->
                setAllowed(context, !allowed)
                onDone()
            }
            .setNegativeButton("CLOSE", null)
            .show()
    }
}