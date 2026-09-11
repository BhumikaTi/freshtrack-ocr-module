package com.freshtrack.ocr.ui

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.freshtrack.ocr.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ProductListActivity : AppCompatActivity() {

    private lateinit var productListContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_product_list)

        productListContainer =
            findViewById(R.id.productListContainer)

        findViewById<MaterialButton>(
            R.id.btnBack
        ).setOnClickListener {
            finish()
        }

        findViewById<MaterialButton>(
            R.id.btnAddProduct
        ).setOnClickListener {
            startActivity(
                Intent(
                    this,
                    AddProductActivity::class.java
                )
            )
        }

        loadProducts()
    }

    override fun onResume() {
        super.onResume()

        if (::productListContainer.isInitialized) {
            loadProducts()
        }
    }

    private fun loadProducts() {

        productListContainer.removeAllViews()

        val sharedPreferences =
            getSharedPreferences(
                "FreshTrackPrefs",
                MODE_PRIVATE
            )

        val products =
            sharedPreferences
                .getStringSet(
                    "products",
                    emptySet()
                )
                ?.toList()
                ?: emptyList()

        if (products.isEmpty()) {
            showEmptyMessage()
            return
        }

        for (product in products) {

            val parts = product.split("|")

            if (parts.size < 2) {
                continue
            }

            val productName = parts[0]
            val expiryDate = parts[1]

            createProductCard(
                productName = productName,
                expiryDate = expiryDate,
                fullProductValue = product
            )
        }
    }

    private fun showEmptyMessage() {

        val emptyText = TextView(this)

        emptyText.text = "No products added yet."

        emptyText.textSize = 14f

        emptyText.gravity =
            Gravity.CENTER

        emptyText.setTextColor(
            getColor(R.color.text_secondary)
        )

        emptyText.setPadding(
            0,
            dp(12),
            0,
            dp(12)
        )

        productListContainer.addView(
            emptyText
        )
    }

    private fun createProductCard(
        productName: String,
        expiryDate: String,
        fullProductValue: String
    ) {

        // -------------------------
        // CARD
        // -------------------------

        val card =
            MaterialCardView(this)

        card.setCardBackgroundColor(
            getColor(R.color.card_dark)
        )

        card.radius =
            dp(12).toFloat()

        card.strokeWidth =
            dp(1)

        card.strokeColor =
            getColor(R.color.border_light)


        // -------------------------
        // CARD CONTENT
        // -------------------------

        val cardLayout =
            LinearLayout(this)

        cardLayout.orientation =
            LinearLayout.VERTICAL

        cardLayout.setPadding(
            dp(12),
            dp(9),
            dp(12),
            dp(9)
        )


        // -------------------------
        // PRODUCT NAME
        // -------------------------

        val nameText =
            TextView(this)

        nameText.text =
            productName

        nameText.textSize =
            17f

        nameText.setTextColor(
            getColor(R.color.text_primary)
        )


        // -------------------------
        // EXPIRY DATE
        // -------------------------

        val expiryText =
            TextView(this)

        expiryText.text =
            "Expires: $expiryDate"

        expiryText.textSize =
            13f

        expiryText.setTextColor(
            getColor(R.color.text_secondary)
        )

        val expiryParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

        expiryParams.topMargin =
            dp(2)


        // -------------------------
        // STATUS
        // -------------------------

        val status =
            getExpiryStatus(expiryDate)

        val statusText =
            TextView(this)

        statusText.text =
            status

        statusText.textSize =
            12f

        when {

            status.startsWith("Expired") -> {

                statusText.setTextColor(
                    getColor(R.color.error_bright)
                )
            }

            status.startsWith("Expires today") ||
                    status.startsWith("Expires soon") -> {

                statusText.setTextColor(
                    getColor(R.color.warning_amber)
                )
            }

            status.startsWith("Fresh") -> {

                statusText.setTextColor(
                    getColor(R.color.green_primary)
                )
            }

            else -> {

                statusText.setTextColor(
                    getColor(R.color.text_secondary)
                )
            }
        }

        val statusParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

        statusParams.topMargin =
            dp(1)


        // -------------------------
        // BUTTON LAYOUT
        // -------------------------

        val buttonLayout =
            LinearLayout(this)

        buttonLayout.orientation =
            LinearLayout.HORIZONTAL

        buttonLayout.gravity =
            Gravity.CENTER_VERTICAL


        // -------------------------
        // EDIT BUTTON
        // -------------------------

        val editButton =
            MaterialButton(
                this,
                null,
                com.google.android.material.R.attr.materialButtonOutlinedStyle
            )

        editButton.text =
            "Edit"

        editButton.textSize =
            11f

        editButton.gravity =
            Gravity.CENTER

        editButton.setTextColor(
            getColor(R.color.green_primary)
        )

        editButton.minHeight = 0
        editButton.minWidth = 0

        editButton.insetTop = 0
        editButton.insetBottom = 0

        editButton.setPadding(
            0,
            0,
            0,
            0
        )

        editButton.strokeWidth =
            dp(1)

        editButton.cornerRadius =
            dp(9)


        // -------------------------
        // DELETE BUTTON
        // -------------------------

        val deleteButton =
            MaterialButton(
                this,
                null,
                com.google.android.material.R.attr.materialButtonOutlinedStyle
            )

        deleteButton.text =
            "Delete"

        deleteButton.textSize =
            11f

        deleteButton.gravity =
            Gravity.CENTER

        deleteButton.setTextColor(
            getColor(R.color.green_primary)
        )

        deleteButton.minHeight = 0
        deleteButton.minWidth = 0

        deleteButton.insetTop = 0
        deleteButton.insetBottom = 0

        deleteButton.setPadding(
            0,
            0,
            0,
            0
        )

        deleteButton.strokeWidth =
            dp(1)

        deleteButton.cornerRadius =
            dp(9)


        // -------------------------
        // EDIT CLICK
        // -------------------------

        editButton.setOnClickListener {

            val intent =
                Intent(
                    this,
                    AddProductActivity::class.java
                )

            intent.putExtra(
                "productValue",
                fullProductValue
            )

            startActivity(intent)
        }


        // -------------------------
        // DELETE CLICK
        // -------------------------

        deleteButton.setOnClickListener {

            deleteProduct(
                fullProductValue
            )
        }


        // -------------------------
        // BUTTON SIZES
        // -------------------------

        val editParams =
            LinearLayout.LayoutParams(
                0,
                dp(34),
                1f
            )

        editParams.setMargins(
            0,
            dp(5),
            dp(4),
            0
        )


        val deleteParams =
            LinearLayout.LayoutParams(
                0,
                dp(34),
                1f
            )

        deleteParams.setMargins(
            dp(4),
            dp(5),
            0,
            0
        )


        buttonLayout.addView(
            editButton,
            editParams
        )

        buttonLayout.addView(
            deleteButton,
            deleteParams
        )


        // -------------------------
        // ADD CONTENT TO CARD
        // -------------------------

        cardLayout.addView(
            nameText
        )

        cardLayout.addView(
            expiryText,
            expiryParams
        )

        cardLayout.addView(
            statusText,
            statusParams
        )

        cardLayout.addView(
            buttonLayout
        )

        card.addView(
            cardLayout
        )


        // -------------------------
        // CARD SPACING
        // -------------------------

        val cardParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

        cardParams.setMargins(
            0,
            0,
            0,
            dp(7)
        )

        productListContainer.addView(
            card,
            cardParams
        )
    }


    // -------------------------
    // EXPIRY STATUS
    // -------------------------

    private fun getExpiryStatus(
        expiryDate: String
    ): String {

        return try {

            val dateFormat =
                SimpleDateFormat(
                    "dd MMM yyyy",
                    Locale.getDefault()
                )

            val expiry =
                dateFormat.parse(expiryDate)
                    ?: return "Status unavailable"


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


            val expiryCalendar =
                Calendar.getInstance()

            expiryCalendar.time =
                expiry

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


            val difference =
                expiryCalendar.timeInMillis -
                        today.timeInMillis


            val daysLeft =
                difference /
                        (1000 * 60 * 60 * 24)


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


    // -------------------------
    // DELETE PRODUCT
    // -------------------------

    private fun deleteProduct(
        productValue: String
    ) {

        val sharedPreferences =
            getSharedPreferences(
                "FreshTrackPrefs",
                MODE_PRIVATE
            )

        val products =
            sharedPreferences
                .getStringSet(
                    "products",
                    emptySet()
                )
                ?.toMutableSet()
                ?: mutableSetOf()


        products.remove(
            productValue
        )


        sharedPreferences
            .edit()
            .putStringSet(
                "products",
                products
            )
            .apply()


        loadProducts()
    }


    // -------------------------
    // DP HELPER
    // -------------------------

    private fun dp(
        value: Int
    ): Int {

        return (
                value *
                        resources.displayMetrics.density
                ).toInt()
    }
}



