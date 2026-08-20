package com.freshtrack.ocr.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.TextView
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.freshtrack.ocr.R
import com.google.android.material.button.MaterialButton

class MainActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView

    private val requestCameraPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) startCameraScan() else tvStatus.text = getString(R.string.camera_permission_denied)
        }

    private val pickGalleryImage =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
            if (uri != null) {
                startActivity(Intent(this, CropConfirmActivity::class.java).apply {
                    putExtra(CropConfirmActivity.EXTRA_URI, uri.toString())
                })
            } else {
                tvStatus.text = getString(R.string.no_image_selected)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvStatus = findViewById(R.id.tvStatus)

        findViewById<MaterialButton>(R.id.btnScanCamera).setOnClickListener {
            tvStatus.text = ""
            onScanCameraClicked()
        }
        findViewById<MaterialButton>(R.id.btnChooseGallery).setOnClickListener {
            tvStatus.text = ""
            pickGalleryImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
    }

    private fun onScanCameraClicked() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCameraScan()
        } else {
            requestCameraPermission.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCameraScan() = startActivity(Intent(this, CameraScanActivity::class.java))
}