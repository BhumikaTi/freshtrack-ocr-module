package com.freshtrack.ocr.ui

import android.os.Bundle
import android.content.Intent
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.freshtrack.ocr.R
import com.freshtrack.ocr.data.Product
import com.freshtrack.ocr.data.ProductDatabase
import com.freshtrack.ocr.data.ProductDao
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

class ResultActivity : AppCompatActivity() {

    private lateinit var productDao: ProductDao

    private lateinit var etProductName: EditText
    private lateinit var etExpiryDate: EditText
    private lateinit var btnSaveProduct: MaterialButton
    private lateinit var btnBack: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_result)

        val database = ProductDatabase.getDatabase(this)
        productDao = database.productDao()

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
                return@setOnClickListener
            }

            if (enteredExpiryDate.isEmpty()) {
                etExpiryDate.error = "Enter expiry date"
                return@setOnClickListener
            }

            lifecycleScope.launch {
                val product = Product(
                    productName = enteredProductName,
                    expiryDate = enteredExpiryDate,
                    rawOcrText = ""
                )

                productDao.insertProduct(product)

                Toast.makeText(
                    this@ResultActivity,
                    "Product saved successfully!",
                    Toast.LENGTH_SHORT
                ).show()

                // Open My Products after saving
                startActivity(
                    Intent(
                        this@ResultActivity,
                        ProductListActivity::class.java
                    )
                )

                finish()
            }
        }
    }

    companion object {

        const val EXTRA_EXPIRY_DATE =
            "extra_expiry_date"

        const val EXTRA_PRODUCT_NAME =
            "extra_product_name"
    }
}