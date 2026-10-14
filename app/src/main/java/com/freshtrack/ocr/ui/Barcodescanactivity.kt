package com.freshtrack.ocr.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Size
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.freshtrack.ocr.R
import com.freshtrack.ocr.data.BarcodeAnalyzer
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class Barcodescanactivity : AppCompatActivity() {

    private lateinit var cameraExecutor: ExecutorService
    private var camera: Camera? = null
    private var torchOn = false
    private var galleryOnly = false

    private val requestCameraPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                startCamera()
            } else {
                Toast.makeText(this, "Camera permission is required to scan barcodes", Toast.LENGTH_LONG).show()
                finish()
            }
        }

    // System photo picker: no storage permission needed.
    private val pickImage =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            when {
                uri != null -> scanGalleryImage(uri)
                galleryOnly -> finish() // user closed the picker without choosing anything
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_barcode_scan)

        cameraExecutor = Executors.newSingleThreadExecutor()
        galleryOnly = intent.getStringExtra(EXTRA_MODE) == MODE_GALLERY

        findViewById<ImageButton>(R.id.backButton).setOnClickListener { finish() }
        findViewById<ImageButton>(R.id.flashlightButton).setOnClickListener { toggleFlashlight() }
        findViewById<ImageButton>(R.id.galleryButton).setOnClickListener { openGalleryPicker() }

        if (galleryOnly) {
            showGalleryOnlyUi()
            // savedInstanceState != null means a rotation: don't open the picker twice.
            if (savedInstanceState == null) openGalleryPicker()
            return
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            requestCameraPermission.launch(Manifest.permission.CAMERA)
        }
    }

    private fun showGalleryOnlyUi() {
        findViewById<PreviewView>(R.id.previewView).visibility = View.GONE
        findViewById<ImageButton>(R.id.flashlightButton).visibility = View.GONE
        findViewById<ImageButton>(R.id.galleryButton).visibility = View.GONE
        findViewById<TextView>(R.id.scanHint).text = "Choose a photo of the barcode"
    }

    private fun openGalleryPicker() {
        pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    private fun startCamera() {
        val previewView = findViewById<PreviewView>(R.id.previewView)
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val analyzer = BarcodeAnalyzer { barcodeValue ->
                runOnUiThread { onBarcodeFound(barcodeValue) }
            }

            val imageAnalysis = ImageAnalysis.Builder()
                .setTargetResolution(Size(1280, 720))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { it.setAnalyzer(cameraExecutor, analyzer) }

            try {
                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(
                    this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalysis
                )

                // No flash unit on this phone: hide the button instead of showing a dead one.
                findViewById<ImageButton>(R.id.flashlightButton).visibility =
                    if (camera?.cameraInfo?.hasFlashUnit() == true) View.VISIBLE else View.GONE

            } catch (e: Exception) {
                Toast.makeText(this, "Could not start camera: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun toggleFlashlight() {
        val cam = camera ?: return
        if (!cam.cameraInfo.hasFlashUnit()) return
        torchOn = !torchOn
        cam.cameraControl.enableTorch(torchOn)
    }

    private fun scanGalleryImage(uri: Uri) {
        val hint = findViewById<TextView>(R.id.scanHint)
        lifecycleScope.launch {
            hint.text = "Reading barcode…"
            val barcodeValue = GalleryBarcodeScanner.scan(this@Barcodescanactivity, uri)
            if (barcodeValue != null) {
                onBarcodeFound(barcodeValue)
            } else {
                Toast.makeText(
                    this@Barcodescanactivity,
                    "No barcode found in that image. Try a clearer photo.",
                    Toast.LENGTH_LONG
                ).show()
                if (galleryOnly) finish() else hint.text = "Point your camera at a barcode"
            }
        }
    }

    private fun onBarcodeFound(barcodeValue: String) {
        startActivity(
            Intent(this, ProductResultActivity::class.java)
                .putExtra(ProductResultActivity.EXTRA_BARCODE, barcodeValue)
        )
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        const val EXTRA_MODE = "extra_mode"
        const val MODE_CAMERA = "camera"
        const val MODE_GALLERY = "gallery"
    }
}