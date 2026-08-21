package com.freshtrack.ocr.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.TorchState
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera_scan)

        previewView = findViewById(R.id.previewView)
        btnCapture = findViewById(R.id.btnCapture)
        btnFlashlight = findViewById(R.id.btnFlashlight)

        bindCameraUseCases()
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

    private fun bindCameraUseCases() {
        val providerFuture = ProcessCameraProvider.getInstance(this)
        providerFuture.addListener({
            val provider = providerFuture.get()
            cameraProvider = provider

            val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
            val capture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
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
                    startActivity(Intent(this@CameraScanActivity, CropConfirmActivity::class.java).apply {
                        putExtra(CropConfirmActivity.EXTRA_FILE_PATH, photoFile.path)
                    })
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