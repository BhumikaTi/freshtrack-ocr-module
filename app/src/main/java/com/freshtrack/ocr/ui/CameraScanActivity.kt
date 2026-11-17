package com.freshtrack.ocr.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.TorchState
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.freshtrack.ocr.R
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.io.File

class CameraScanActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var btnCapture: FloatingActionButton
    private lateinit var btnFlashlight: FloatingActionButton

    private var imageCapture: ImageCapture? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null

    // True when the Add Product screen opened this one only to read an expiry
    // date. The date is then passed back instead of opening the Result screen.
    private var returnDateOnly = false

    // Used only in that mode: the crop screen returns the date, and this screen
    // relays it to the Add Product screen and closes.
    private val cropLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            when (result.resultCode) {

                // A date was found: pass it on to the Add Product screen.
                RESULT_OK -> {
                    setResult(RESULT_OK, result.data)
                    finish()
                }

                // The user chose ENTER DATE: close the camera too, so they
                // land on the Add Product screen.
                CropConfirmActivity.RESULT_ENTER_MANUALLY -> {
                    setResult(RESULT_CANCELED)
                    finish()
                }
            }
        }

    // Asks for camera access the first time; without it CameraX would just show a black screen.
    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                bindCameraUseCases()
            } else {
                Toast.makeText(this, "Camera access is needed to scan. You can enter details manually instead.", Toast.LENGTH_LONG).show()
                finish()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera_scan)

        returnDateOnly = intent.getBooleanExtra(CropConfirmActivity.EXTRA_RETURN_DATE_ONLY, false)

        previewView = findViewById(R.id.previewView)
        btnCapture = findViewById(R.id.btnCapture)
        btnFlashlight = findViewById(R.id.btnFlashlight)

        deleteOldScanPhotos()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            bindCameraUseCases()
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
        enableTapToFocus()
        btnCapture.setOnClickListener { captureAndProcess() }
        btnFlashlight.setOnClickListener {
            val hasFlash = camera?.cameraInfo?.hasFlashUnit() ?: false
            if (hasFlash) {
                val isOn = camera?.cameraInfo?.torchState?.value == TorchState.ON
                camera?.cameraControl?.enableTorch(!isOn)
            } else {
                Toast.makeText(this, "Flashlight not available", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Scan photos are only needed while the crop/AI screen is open, so remove old ones.
    private fun deleteOldScanPhotos() {
        val cutoff = System.currentTimeMillis() - 60 * 60 * 1000L
        cacheDir.listFiles { f -> f.name.startsWith("scan_") && f.name.endsWith(".jpg") && f.lastModified() < cutoff }
            ?.forEach { it.delete() }
    }

    // Tap the preview to focus there. Helps a lot with small print on curved packs.
    @android.annotation.SuppressLint("ClickableViewAccessibility")
    private fun enableTapToFocus() {
        previewView.setOnTouchListener { _, event ->
            if (event.action == android.view.MotionEvent.ACTION_UP) {
                val point = previewView.meteringPointFactory.createPoint(event.x, event.y)
                val action = FocusMeteringAction.Builder(point).build()
                camera?.cameraControl?.startFocusAndMetering(action)
            }
            true
        }
    }

    private fun bindCameraUseCases() {
        val providerFuture = ProcessCameraProvider.getInstance(this)
        providerFuture.addListener({
            val provider = providerFuture.get()
            cameraProvider = provider

            val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
            // Ask for the biggest picture the camera can take, so small print stays readable.
            val resolution = ResolutionSelector.Builder()
                .setResolutionStrategy(ResolutionStrategy.HIGHEST_AVAILABLE_STRATEGY)
                .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
                .build()
            val capture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .setResolutionSelector(resolution)
                .build()
            imageCapture = capture

            try {
                provider.unbindAll()
                camera = provider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, capture)
            } catch (e: Exception) {
                Log.e(TAG, "CameraX binding failed", e)
                Toast.makeText(this, "Could not start camera", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun captureAndProcess() {
        val capture = imageCapture ?: return
        btnCapture.isEnabled = false

        val photoFile = File(cacheDir, "scan_${System.currentTimeMillis()}.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        capture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    btnCapture.isEnabled = true

                    val cropIntent = Intent(this@CameraScanActivity, CropConfirmActivity::class.java).apply {
                        putExtra(CropConfirmActivity.EXTRA_FILE_PATH, photoFile.path)
                    }

                    if (returnDateOnly) {
                        // Ask the crop screen to hand the date back to us.
                        cropIntent.putExtra(CropConfirmActivity.EXTRA_RETURN_DATE_ONLY, true)
                        cropLauncher.launch(cropIntent)
                    } else {
                        startActivity(cropIntent)
                    }
                }
                override fun onError(exception: ImageCaptureException) {
                    btnCapture.isEnabled = true
                    Toast.makeText(this@CameraScanActivity, exception.message ?: "Capture failed", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraProvider?.unbindAll()
    }

    companion object {
        private const val TAG = "CameraScanActivity"
    }
}