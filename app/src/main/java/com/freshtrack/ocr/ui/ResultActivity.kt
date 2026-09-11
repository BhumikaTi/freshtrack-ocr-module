package com.freshtrack.ocr.ui

import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.freshtrack.ocr.R
import com.google.android.material.button.MaterialButton

class ResultActivity : AppCompatActivity() {

    private lateinit var etProductName: EditText
    private lateinit var etExpiryDate: EditText
    private lateinit var btnSaveProduct: MaterialButton
    private lateinit var btnBack: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_result)

        etProductName = findViewById(R.id.etProductName)
        etExpiryDate = findViewById(R.id.etExpiryDate)
        btnSaveProduct = findViewById(R.id.btnSaveProduct)
        btnBack = findViewById(R.id.btnBack)

        /*
         * Get expiry date detected by OCR + parser.
         */
        val expiryDate = intent.getStringExtra(EXTRA_EXPIRY_DATE)

        if (!expiryDate.isNullOrBlank()) {
            etExpiryDate.setText(expiryDate)
        }

        /*
         * Get product name detected by OCR + parser.
         *
         * Product name is best-effort.
         * If parser doesn't find one, user can enter it manually.
         */
        val productName = intent.getStringExtra(EXTRA_PRODUCT_NAME)

        if (!productName.isNullOrBlank()) {
            etProductName.setText(productName)
        }

        /*
         * Back button
         */
        btnBack.setOnClickListener {
            finish()
        }

        /*
         * Save button
         */
        btnSaveProduct.setOnClickListener {

            val enteredProductName =
                etProductName.text.toString().trim()

            val enteredExpiryDate =
                etExpiryDate.text.toString().trim()

            /*
             * Validate product name
             */
            if (enteredProductName.isEmpty()) {

                etProductName.error = "Enter product name"
                etProductName.requestFocus()

                return@setOnClickListener
            }

            /*
             * Validate expiry date
             */
            if (enteredExpiryDate.isEmpty()) {

                etExpiryDate.error = "Enter expiry date"
                etExpiryDate.requestFocus()

                return@setOnClickListener
            }

            /*
             * For now, just confirm the data.
             *
             * Database / My Products integration
             * will be connected separately.
             */
            Toast.makeText(
                this,
                "Product ready to save!",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    companion object {

        const val EXTRA_EXPIRY_DATE =
            "extra_expiry_date"

        const val EXTRA_PRODUCT_NAME =
            "extra_product_name"
    }
}