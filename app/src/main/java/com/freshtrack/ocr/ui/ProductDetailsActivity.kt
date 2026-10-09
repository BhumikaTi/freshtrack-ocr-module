package com.freshtrack.ocr.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.freshtrack.ocr.R
import com.freshtrack.ocr.data.ProductDao
import com.freshtrack.ocr.data.ProductDatabase
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ProductDetailsActivity : AppCompatActivity() {

    private lateinit var productDao: ProductDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_product_details)

        // Initialize Room database
        val database = ProductDatabase.getDatabase(this)
        productDao = database.productDao()

        // Get selected product ID
        val productId = intent.getIntExtra(
            "productId",
            -1
        )

        // Back button
        findViewById<View>(
            R.id.btnBack
        ).setOnClickListener {
            finish()
        }

        // Load product from Room
        if (productId != -1) {

            lifecycleScope.launch {

                val product =
                    productDao.getProductById(productId)

                if (product != null) {

                    // -------------------------
                    // PRODUCT NAME
                    // -------------------------

                    findViewById<TextView>(
                        R.id.tvProductName
                    ).text = product.productName


                    // -------------------------
                    // CATEGORY
                    // -------------------------

                    findViewById<TextView>(
                        R.id.tvCategory
                    ).text = product.category


                    // -------------------------
                    // PRODUCT INFORMATION
                    // -------------------------

                    findViewById<TextView>(
                        R.id.tvInfoProductName
                    ).text = product.productName

                    findViewById<TextView>(
                        R.id.tvInfoCategory
                    ).text = product.category

                    findViewById<TextView>(
                        R.id.tvExpiryDate
                    ).text = product.expiryDate


                    // -------------------------
                    // EXPIRY STATUS
                    // -------------------------

                    val expiryStatus =
                        getExpiryStatus(
                            product.expiryDate
                        )

                    findViewById<TextView>(
                        R.id.tvStatus
                    ).text = expiryStatus


                    // -------------------------
                    // DAYS REMAINING
                    // -------------------------

                    val daysRemainingText = when {

                        expiryStatus == "Expired" -> {
                            "Expired"
                        }

                        expiryStatus == "Expires today" -> {
                            "Expires today"
                        }

                        expiryStatus.contains("days left") -> {
                            expiryStatus.substringAfter("—").trim()
                        }

                        else -> {
                            "Status unavailable"
                        }
                    }

                    findViewById<TextView>(
                        R.id.tvDaysRemaining
                    ).text = daysRemainingText

                    findViewById<TextView>(R.id.tvLocation).text =
                        if (product.location.isBlank()) "Not specified" else product.location


                    // -------------------------
                    // EDIT PRODUCT
                    // -------------------------

                    findViewById<View>(
                        R.id.btnEditProduct
                    ).setOnClickListener {

                        val intent = Intent(
                            this@ProductDetailsActivity,
                            AddProductActivity::class.java
                        )

                        intent.putExtra(
                            "productId",
                            product.id
                        )

                        intent.putExtra(
                            "productName",
                            product.productName
                        )

                        intent.putExtra(
                            "expiryDate",
                            product.expiryDate
                        )
                        intent.putExtra("location", product.location)

                        startActivity(intent)
                    }

                    // -------------------------
// DELETE PRODUCT
// -------------------------

                    // -------------------------
// DELETE PRODUCT
// -------------------------

                    findViewById<View>(
                        R.id.btnDeleteProduct
                    ).setOnClickListener {

                        val dialog =
                            androidx.appcompat.app.AlertDialog.Builder(
                                this@ProductDetailsActivity
                            )
                                .setCustomTitle(
                                    TextView(this@ProductDetailsActivity).apply {
                                        text = "Delete Product"
                                        setTextColor(android.graphics.Color.BLACK)
                                        textSize = 20f
                                        setTypeface(
                                            null,
                                            android.graphics.Typeface.BOLD
                                        )

                                        val horizontalPadding =
                                            (24 * resources.displayMetrics.density).toInt()

                                        val verticalPadding =
                                            (20 * resources.displayMetrics.density).toInt()

                                        setPadding(
                                            horizontalPadding,
                                            verticalPadding,
                                            horizontalPadding,
                                            0
                                        )
                                    }
                                )
                                .setMessage(
                                    "Are you sure you want to delete ${product.productName}?"
                                )
                                .setNegativeButton(
                                    "Cancel",
                                    null
                                )
                                .setPositiveButton(
                                    "Delete"
                                ) { _, _ ->

                                    lifecycleScope.launch {

                                        productDao.deleteProduct(product)

                                        Toast.makeText(
                                            this@ProductDetailsActivity,
                                            "Product deleted",
                                            Toast.LENGTH_SHORT
                                        ).show()

                                        finish()
                                    }
                                }
                                .create()

                        dialog.setOnShowListener {

                            // Dialog background
                            dialog.window?.setBackgroundDrawable(
                                android.graphics.drawable.ColorDrawable(
                                    android.graphics.Color.parseColor("#FAF9F4")
                                )
                            )

                            // Dialog width
                            val widthInDp = 320

                            val widthInPixels =
                                (
                                        widthInDp *
                                                resources.displayMetrics.density
                                        ).toInt()

                            dialog.window?.setLayout(
                                widthInPixels,
                                android.view.WindowManager.LayoutParams.WRAP_CONTENT
                            )

                            // Message - black
                            dialog.findViewById<TextView>(
                                android.R.id.message
                            )?.setTextColor(
                                android.graphics.Color.BLACK
                            )

                            // Cancel - existing color
                            dialog.getButton(
                                androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE
                            ).setTextColor(
                                getColor(R.color.text_secondary)
                            )

                            // Delete - red
                            dialog.getButton(
                                androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE
                            ).setTextColor(
                                getColor(R.color.error_bright)
                            )
                        }

                        dialog.show()
                    }

                }
            }
        }
    }


    // =====================================================
    // EXPIRY STATUS
    // =====================================================

    private fun getExpiryStatus(
        expiryDate: String
    ): String {

        return try {

            val date =
                expiryDate.trim()

            val formats = listOf(
                "dd/MM/yyyy",
                "d/M/yyyy",
                "dd MMMM yyyy",
                "d MMMM yyyy",
                "dd MMM yyyy",
                "d MMM yyyy",
                "MM/yyyy",
                "M/yyyy",
                "MMM yyyy",
                "MMMM yyyy",
                "MMM/yyyy",
                "MMMM/yyyy",
                "MMM yy",
                "MMMM yy",
                "MM/yy",
                "M/yy"
            )

            var expiryCalendar: Calendar? =
                null

            for (format in formats) {

                val formatter =
                    SimpleDateFormat(
                        format,
                        Locale.ENGLISH
                    )

                formatter.isLenient =
                    false

                val parsedDate: Date? =
                    try {

                        formatter.parse(date)

                    } catch (e: Exception) {

                        null
                    }

                if (parsedDate != null) {

                    val calendar =
                        Calendar.getInstance()

                    calendar.time =
                        parsedDate

                    // Month-only formats use
                    // the last day of the month
                    if (
                        format == "MM/yyyy" ||
                        format == "M/yyyy" ||
                        format == "MMM yyyy" ||
                        format == "MMMM yyyy" ||
                        format == "MMM/yyyy" ||
                        format == "MMMM/yyyy" ||
                        format == "MMM yy" ||
                        format == "MMMM yy" ||
                        format == "MM/yy" ||
                        format == "M/yy"
                    ) {

                        calendar.set(
                            Calendar.DAY_OF_MONTH,
                            calendar.getActualMaximum(
                                Calendar.DAY_OF_MONTH
                            )
                        )
                    }

                    expiryCalendar =
                        calendar

                    break
                }
            }


            // Could not parse date
            if (expiryCalendar == null) {

                return "Status unavailable"
            }


            // -------------------------
            // TODAY
            // -------------------------

            val today =
                Calendar.getInstance()

            today.set(
                Calendar.HOUR_OF_DAY,
                0
            )

            today.set(
                Calendar.MINUTE,
                0
            )

            today.set(
                Calendar.SECOND,
                0
            )

            today.set(
                Calendar.MILLISECOND,
                0
            )


            // -------------------------
            // EXPIRY DATE
            // -------------------------

            expiryCalendar.set(
                Calendar.HOUR_OF_DAY,
                0
            )

            expiryCalendar.set(
                Calendar.MINUTE,
                0
            )

            expiryCalendar.set(
                Calendar.SECOND,
                0
            )

            expiryCalendar.set(
                Calendar.MILLISECOND,
                0
            )


            // -------------------------
            // CALCULATE DAYS
            // -------------------------

            val difference =
                expiryCalendar.timeInMillis -
                        today.timeInMillis

            val daysLeft =
                difference /
                        (1000 * 60 * 60 * 24)


            // -------------------------
            // STATUS
            // -------------------------

            when {

                daysLeft < 0L -> {

                    "Expired"
                }

                daysLeft == 0L -> {

                    "Expires today"
                }

                daysLeft <= 7L -> {

                    "Expires soon — $daysLeft days left"
                }

                else -> {

                    "Fresh — $daysLeft days left"
                }
            }

        } catch (e: Exception) {

            "Status unavailable"
        }
    }
}