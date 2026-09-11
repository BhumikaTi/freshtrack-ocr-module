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

        // --------------------------------------------------------
        // GET PRODUCT NAME FROM OCR + EXPIRY PARSER
        // --------------------------------------------------------

        val productName =
            intent.getStringExtra(EXTRA_PRODUCT_NAME)

        if (!productName.isNullOrBlank()) {
            etProductName.setText(productName)
        }

        // --------------------------------------------------------
        // GET EXPIRY DATE FROM OCR + EXPIRY PARSER
        // --------------------------------------------------------

        val expiryDate =
            intent.getStringExtra(EXTRA_EXPIRY_DATE)

        if (!expiryDate.isNullOrBlank()) {
            etExpiryDate.setText(expiryDate)
        }

        // --------------------------------------------------------
        // BACK
        // --------------------------------------------------------

        btnBack.setOnClickListener {
            finish()
        }

        // --------------------------------------------------------
        // SAVE PRODUCT
        // --------------------------------------------------------

        btnSaveProduct.setOnClickListener {

            val enteredProductName =
                etProductName.text.toString().trim()

            val enteredExpiryDate =
                etExpiryDate.text.toString().trim()

            if (enteredProductName.isEmpty()) {
                etProductName.error = "Enter product name"
                etProductName.requestFocus()
                return@setOnClickListener
            }

            if (enteredExpiryDate.isEmpty()) {
                etExpiryDate.error = "Enter expiry date"
                etExpiryDate.requestFocus()
                return@setOnClickListener
            }

            Toast.makeText(
                this,
                "Product ready to save!",
                Toast.LENGTH_SHORT
            ).show()

            // Room database integration will be connected here.
        }
    }

    companion object {

        const val EXTRA_EXPIRY_DATE =
            "extra_expiry_date"

        const val EXTRA_PRODUCT_NAME =
            "extra_product_name"
    }
}