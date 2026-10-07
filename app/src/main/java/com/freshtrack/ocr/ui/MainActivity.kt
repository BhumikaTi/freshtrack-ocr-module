
package com.freshtrack.ocr.ui

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.freshtrack.ocr.R
import com.google.android.material.button.MaterialButton
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

class MainActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setApplicationLocales(
            LocaleListCompat.forLanguageTags("en")
        )

        setContentView(R.layout.activity_main)

        tvStatus = findViewById(R.id.tvStatus)

        // Scan a product
        findViewById<MaterialButton>(R.id.btnScanCamera).setOnClickListener {
            tvStatus.text = ""

            startActivity(
                Intent(this, AddOptionsActivity::class.java)
            )
        }

        // Open My Products
        findViewById<MaterialButton>(R.id.btnMyProducts).setOnClickListener {
            startActivity(
                Intent(this, ProductListActivity::class.java)
            )
        }


        }
    }

