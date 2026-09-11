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

        productListContainer = findViewById(R.id.productListContainer)

        findViewById<MaterialButton>(R.id.btnBack).setOnClickListener {
            finish()
        }

        findViewById<MaterialButton>(R.id.btnAddProduct).setOnClickListener {
            startActivity(
                Intent(this, AddProductActivity::class.java)
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

        val sharedPreferences = getSharedPreferences(
            "FreshTrackPrefs",
            MODE_PRIVATE
        )

        val products = sharedPreferences
            .getStringSet("products", emptySet())
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
        emptyText.textSize = 16f
        emptyText.gravity = Gravity.CENTER
        emptyText.setTextColor(
            Color.parseColor("#AAB7AC")
        )
        emptyText.setPadding(0, 24, 0, 24)

        productListContainer.addView(emptyText)
    }

    private fun createProductCard(
        productName: String,
        expiryDate: String,
        fullProductValue: String
    ) {
        val card = MaterialCardView(this)

        val cardLayout = LinearLayout(this)
        cardLayout.orientation = LinearLayout.VERTICAL
        cardLayout.setPadding(24, 20, 24, 20)

        val nameText = TextView(this)
        nameText.text = productName
        nameText.textSize = 20f
        nameText.setTextColor(
            Color.parseColor("#F1F5EF")
        )

        val expiryText = TextView(this)
        expiryText.text = "Expires: $expiryDate"
        expiryText.textSize = 15f
        expiryText.setTextColor(
            Color.parseColor("#AAB7AC")
        )

        val status = getExpiryStatus(expiryDate)

        val statusText = TextView(this)
        statusText.text = status
        statusText.textSize = 14f

        when {
            status.startsWith("Expired") -> {
                statusText.setTextColor(
                    Color.parseColor("#C98585")
                )
            }

            status.startsWith("Expires today") ||
                    status.startsWith("Expires soon") -> {
                statusText.setTextColor(
                    Color.parseColor("#D8BC78")
                )
            }

            else -> {
                statusText.setTextColor(
                    Color.parseColor("#C2C7A5")
                )
            }
        }

        val buttonLayout = LinearLayout(this)
        buttonLayout.orientation = LinearLayout.HORIZONTAL

        val editButton = MaterialButton(
            this,
            null,
            com.google.android.material.R.attr.materialButtonOutlinedStyle
        )

        editButton.text = "Edit"
        editButton.textSize = 13f
        editButton.gravity = Gravity.CENTER

        val deleteButton = MaterialButton(
            this,
            null,
            com.google.android.material.R.attr.materialButtonOutlinedStyle
        )

        deleteButton.text = "Delete"
        deleteButton.textSize = 13f
        deleteButton.gravity = Gravity.CENTER

        editButton.setOnClickListener {
            val intent = Intent(
                this,
                AddProductActivity::class.java
            )

            intent.putExtra(
                "productValue",
                fullProductValue
            )

            startActivity(intent)
        }

        deleteButton.setOnClickListener {
            deleteProduct(fullProductValue)
        }

        val editParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f
        )

        editParams.setMargins(0, 8, 8, 0)

        val deleteParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f
        )

        deleteParams.setMargins(8, 8, 0, 0)

        buttonLayout.addView(
            editButton,
            editParams
        )

        buttonLayout.addView(
            deleteButton,
            deleteParams
        )

        cardLayout.addView(nameText)
        cardLayout.addView(expiryText)
        cardLayout.addView(statusText)
        cardLayout.addView(buttonLayout)

        card.addView(cardLayout)

        val cardParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        cardParams.setMargins(0, 0, 0, 16)

        productListContainer.addView(
            card,
            cardParams
        )
    }

    private fun getExpiryStatus(expiryDate: String): String {
        return try {
            val dateFormat = SimpleDateFormat(
                "dd MMM yyyy",
                Locale.getDefault()
            )

            val expiry = dateFormat.parse(expiryDate)
                ?: return "Status unavailable"

            val today = Calendar.getInstance()

            today.set(Calendar.HOUR_OF_DAY, 0)
            today.set(Calendar.MINUTE, 0)
            today.set(Calendar.SECOND, 0)
            today.set(Calendar.MILLISECOND, 0)

            val expiryCalendar = Calendar.getInstance()
            expiryCalendar.time = expiry

            expiryCalendar.set(Calendar.HOUR_OF_DAY, 0)
            expiryCalendar.set(Calendar.MINUTE, 0)
            expiryCalendar.set(Calendar.SECOND, 0)
            expiryCalendar.set(Calendar.MILLISECOND, 0)

            val difference =
                expiryCalendar.timeInMillis - today.timeInMillis

            val daysLeft = difference /
                    (1000 * 60 * 60 * 24)

            when {
                daysLeft < 0 -> {
                    "Expired"
                }

                daysLeft == 0L -> {
                    "Expires today"
                }

                daysLeft <= 7 -> {
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

    private fun deleteProduct(productValue: String) {
        val sharedPreferences = getSharedPreferences(
            "FreshTrackPrefs",
            MODE_PRIVATE
        )

        val products = sharedPreferences
            .getStringSet("products", emptySet())
            ?.toMutableSet()
            ?: mutableSetOf()

        products.remove(productValue)

        sharedPreferences.edit()
            .putStringSet("products", products)
            .apply()

        loadProducts()
    }
}



