package com.freshtrack.ocr.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.freshtrack.ocr.ExpiryNotificationWorker
import com.freshtrack.ocr.R
import com.google.android.material.button.MaterialButton
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    companion object {
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1001
        private const val EXPIRY_WORK_NAME = "expiry_notification_work"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AppCompatDelegate.setApplicationLocales(
            LocaleListCompat.forLanguageTags("en")
        )

        setContentView(R.layout.activity_main)

        // Ask for notification permission
        requestNotificationPermission()

        // Schedule daily expiry check
        scheduleExpiryNotifications()

        // Scan a product
        findViewById<MaterialButton>(
            R.id.btnScanCamera
        ).setOnClickListener {

            startActivity(
                Intent(
                    this,
                    AddOptionsActivity::class.java
                )
            )
        }

        // My Products
        findViewById<MaterialButton>(
            R.id.btnMyProducts
        ).setOnClickListener {

            startActivity(
                Intent(
                    this,
                    ProductListActivity::class.java
                )
            )
        }
    }

    private fun requestNotificationPermission() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            if (
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {

                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        Manifest.permission.POST_NOTIFICATIONS
                    ),
                    NOTIFICATION_PERMISSION_REQUEST_CODE
                )
            }
        }
    }

    private fun scheduleExpiryNotifications() {

        val workRequest =
            PeriodicWorkRequestBuilder<ExpiryNotificationWorker>(
                1,
                TimeUnit.DAYS
            ).build()

        WorkManager
            .getInstance(this)
            .enqueueUniquePeriodicWork(
                EXPIRY_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                workRequest
            )
    }
}