package com.freshtrack.ocr.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.freshtrack.ocr.R
import com.google.android.material.button.MaterialButton

class AddOptionsActivity : AppCompatActivity() {

    private val pickGalleryImage =
        registerForActivityResult(
            ActivityResultContracts.PickVisualMedia()
        ) { uri ->

            if (uri != null) {
                // For now, send the selected image to the OCR flow.
                val intent = Intent(this, CropConfirmActivity::class.java)
                intent.putExtra(CropConfirmActivity.EXTRA_URI, uri.toString())
                startActivity(intent)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_options)

        // Back button
        findViewById<MaterialButton>(R.id.btnBack).setOnClickListener {
            finish()
        }

        // Live OCR / Camera
        findViewById<MaterialButton>(R.id.btnLiveOcr).setOnClickListener {
            startActivity(
                Intent(this, CameraScanActivity::class.java)
            )
        }

        // Gallery / Upload
        findViewById<MaterialButton>(R.id.btnGallery).setOnClickListener {
            pickGalleryImage.launch(
                PickVisualMediaRequest(
                    ActivityResultContracts.PickVisualMedia.ImageOnly
                )
            )
        }

        // Add Manually
        findViewById<MaterialButton>(R.id.btnAddManually).setOnClickListener {
            startActivity(
                Intent(this, AddProductActivity::class.java)
            )
        }
    }
}