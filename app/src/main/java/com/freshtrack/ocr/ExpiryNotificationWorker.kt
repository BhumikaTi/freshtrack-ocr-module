package com.freshtrack.ocr

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.freshtrack.ocr.data.ProductDatabase
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit
import java.util.Locale

class ExpiryNotificationWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val CHANNEL_ID = "expiry_notifications"
        private const val CHANNEL_NAME = "Expiry Reminders"

        private val NOTIFICATION_DAYS = setOf(
            7L,
            3L,
            1L,
            0L
        )
    }

    override suspend fun doWork(): Result {

        createNotificationChannel()

        val database =
            ProductDatabase.getDatabase(applicationContext)

        val products =
            database.productDao().getAllProducts()

        val today = LocalDate.now()

        for (product in products) {

            val expiryDate =
                parseExpiryDate(product.expiryDate)
                    ?: continue

            val daysRemaining =
                ChronoUnit.DAYS.between(
                    today,
                    expiryDate
                )

            if (daysRemaining in NOTIFICATION_DAYS) {

                sendExpiryNotification(
                    product = product,
                    daysRemaining = daysRemaining
                )
            }
        }

        return Result.success()
    }

    @SuppressLint("MissingPermission")
    private fun sendExpiryNotification(
        product: com.freshtrack.ocr.data.Product,
        daysRemaining: Long
    ) {

        val preferenceKey =
            "notification_${product.id}_${product.expiryDate}_$daysRemaining"

        val preferences =
            applicationContext.getSharedPreferences(
                "ExpiryNotificationPrefs",
                Context.MODE_PRIVATE
            )

        // Prevent the same reminder from being sent more than once.
        if (preferences.getBoolean(preferenceKey, false)) {
            return
        }

        val message = when (daysRemaining) {

            7L ->
                "${product.productName} expires in 7 days."

            3L ->
                "${product.productName} expires in 3 days."

            1L ->
                "${product.productName} expires tomorrow."

            0L ->
                "${product.productName} expires today."

            else ->
                return
        }

        val notification =
            NotificationCompat.Builder(
                applicationContext,
                CHANNEL_ID
            )
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("FreshTrack Expiry Reminder")
                .setContentText(message)
                .setPriority(
                    NotificationCompat.PRIORITY_DEFAULT
                )
                .setAutoCancel(true)
                .build()

        // Check notification permission before sending.
        if (
            android.os.Build.VERSION.SDK_INT >=
            android.os.Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        NotificationManagerCompat
            .from(applicationContext)
            .notify(
                product.id * 10 + daysRemaining.toInt(),
                notification
            )

        preferences
            .edit()
            .putBoolean(
                preferenceKey,
                true
            )
            .apply()
    }

    private fun createNotificationChannel() {

        if (
            android.os.Build.VERSION.SDK_INT >=
            android.os.Build.VERSION_CODES.O
        ) {

            val channel =
                NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {

                    description =
                        "Notifications for products approaching their expiry date"
                }

            val manager =
                applicationContext.getSystemService(
                    Context.NOTIFICATION_SERVICE
                ) as NotificationManager

            manager.createNotificationChannel(channel)
        }
    }

    private fun parseExpiryDate(
        dateString: String
    ): LocalDate? {

        val cleaned =
            dateString
                .trim()
                .replace(
                    "Sept",
                    "Sep",
                    ignoreCase = true
                )

        // dd/MM/yyyy
        try {
            return LocalDate.parse(
                cleaned,
                DateTimeFormatter.ofPattern(
                    "dd/MM/yyyy"
                )
            )
        } catch (_: DateTimeParseException) {
        }

        // dd MMM yyyy
        try {
            return LocalDate.parse(
                cleaned,
                DateTimeFormatter.ofPattern(
                    "dd MMM yyyy",
                    Locale.ENGLISH
                )
            )
        } catch (_: DateTimeParseException) {
        }

        // dd MMMM yyyy
        try {
            return LocalDate.parse(
                cleaned,
                DateTimeFormatter.ofPattern(
                    "dd MMMM yyyy",
                    Locale.ENGLISH
                )
            )
        } catch (_: DateTimeParseException) {
        }

        // MM/yyyy
        // Treat as the last day of that month.
        try {

            val parts =
                cleaned.split("/")

            if (parts.size == 2) {

                val month =
                    parts[0].toInt()

                val year =
                    parts[1].toInt()

                val firstDay =
                    LocalDate.of(
                        year,
                        month,
                        1
                    )

                return firstDay.withDayOfMonth(
                    firstDay.lengthOfMonth()
                )
            }

        } catch (_: Exception) {
        }

        // MMM yyyy
        // Treat as the last day of that month.
        try {

            val formatter =
                DateTimeFormatter.ofPattern(
                    "MMM yyyy",
                    Locale.ENGLISH
                )

            val monthYear =
                java.time.YearMonth.parse(
                    cleaned,
                    formatter
                )

            return monthYear.atEndOfMonth()

        } catch (_: DateTimeParseException) {
        }

        return null
    }
}