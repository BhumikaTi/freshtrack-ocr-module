package com.freshtrack.ocr.ui

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.freshtrack.ocr.R

class ResultActivity : AppCompatActivity() {

    private lateinit var etProductName: EditText
    private lateinit var etExpiryDate: EditText
    private lateinit var btnSaveProduct: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)

        etProductName = findViewById(R.id.etProductName)
        etExpiryDate = findViewById(R.id.etExpiryDate)
        btnSaveProduct = findViewById(R.id.btnSaveProduct)

        // Get the expiry date sent from CropConfirmActivity
        val expiryDate = intent.getStringExtra(EXTRA_EXPIRY_DATE)

        if (expiryDate != null) {
            etExpiryDate.setText(expiryDate)
        }

        btnSaveProduct.setOnClickListener {

            val productName = etProductName.text.toString().trim()
            val enteredExpiryDate = etExpiryDate.text.toString().trim()

            if (productName.isEmpty()) {
                etProductName.error = "Enter product name"
                return@setOnClickListener
            }

            if (enteredExpiryDate.isEmpty()) {
                etExpiryDate.error = "Enter expiry date"
                return@setOnClickListener
            }

            Toast.makeText(
                this,
                "Product ready to save!",
                Toast.LENGTH_SHORT
            ).show()

            // We'll connect Room database here next
        }
    }

    companion object {
        const val EXTRA_EXPIRY_DATE = "extra_expiry_date"
    }
}