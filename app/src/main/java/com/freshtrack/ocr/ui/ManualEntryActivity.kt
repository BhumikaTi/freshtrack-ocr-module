package com.freshtrack.ocr.ui

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.freshtrack.ocr.R

class ManualEntryActivity : AppCompatActivity() {

    private lateinit var etProductName: EditText
    private lateinit var etExpiryDate: EditText
    private lateinit var btnSave: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_manual_entry)

        etProductName =
            findViewById(R.id.etProductName)

        etExpiryDate =
            findViewById(R.id.etExpiryDate)

        btnSave =
            findViewById(R.id.btnSave)


        btnSave.setOnClickListener {

            val productName =
                etProductName.text.toString().trim()

            val expiryDate =
                etExpiryDate.text.toString().trim()


            if (productName.isEmpty()) {

                etProductName.error =
                    "Enter product name"

                etProductName.requestFocus()

                return@setOnClickListener
            }


            if (expiryDate.isEmpty()) {

                etExpiryDate.error =
                    "Enter expiry date"

                etExpiryDate.requestFocus()

                return@setOnClickListener
            }


            Toast.makeText(
                this,
                "Product added successfully",
                Toast.LENGTH_SHORT
            ).show()

            finish()
        }
    }
}