package com.freshtrack.ocr.ui

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.freshtrack.ocr.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AddProductActivity : AppCompatActivity() {

    private lateinit var etProductName: TextInputEditText
    private lateinit var etExpiryDate: TextInputEditText

    private var oldProductValue: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_product)

        etProductName = findViewById(R.id.etProductName)
        etExpiryDate = findViewById(R.id.etExpiryDate)

        oldProductValue = intent.getStringExtra("productValue")

        oldProductValue?.let {
            val parts = it.split("|")

            if (parts.size >= 2) {
                etProductName.setText(parts[0])
                etExpiryDate.setText(parts[1])
            }
        }

        findViewById<MaterialButton>(R.id.btnBack).setOnClickListener {
            finish()
        }

        etExpiryDate.setOnClickListener {
            showDatePicker()
        }

        findViewById<MaterialButton>(R.id.btnSaveProduct).setOnClickListener {
            saveProduct()
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()

        val datePicker = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->

                calendar.set(year, month, dayOfMonth)

                val dateFormat = SimpleDateFormat(
                    "dd MMM yyyy",
                    Locale.getDefault()
                )

                etExpiryDate.setText(
                    dateFormat.format(calendar.time)
                )
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        datePicker.show()
    }

    private fun saveProduct() {
        val productName = etProductName.text.toString().trim()
        val expiryDate = etExpiryDate.text.toString().trim()

        if (productName.isEmpty()) {
            etProductName.error = "Enter product name"
            return
        }

        if (expiryDate.isEmpty()) {
            etExpiryDate.error = "Select expiry date"
            return
        }

        val sharedPreferences = getSharedPreferences(
            "FreshTrackPrefs",
            MODE_PRIVATE
        )

        val products = sharedPreferences
            .getStringSet("products", emptySet())
            ?.toMutableSet()
            ?: mutableSetOf()

        val newProductValue = "$productName|$expiryDate"

        oldProductValue?.let {
            products.remove(it)
        }

        products.add(newProductValue)

        sharedPreferences.edit()
            .putStringSet("products", products)
            .apply()

        Toast.makeText(
            this,
            if (oldProductValue == null) {
                "Product saved successfully!"
            } else {
                "Product updated successfully!"
            },
            Toast.LENGTH_SHORT
        ).show()

        val intent = Intent(
            this,
            ProductListActivity::class.java
        )

        intent.flags =
            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP

        startActivity(intent)
        finish()
    }
}

