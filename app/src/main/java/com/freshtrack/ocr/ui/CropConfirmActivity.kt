package com.freshtrack.ocr.ui

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.freshtrack.ocr.ExpiryParser
import com.freshtrack.ocr.ImagePreprocessor
import com.freshtrack.ocr.OcrResultListener
import com.freshtrack.ocr.OcrSource
import com.freshtrack.ocr.OcrTextExtractor
import com.freshtrack.ocr.R
import com.freshtrack.ocr.ScanTarget
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.io.File

class CropConfirmActivity : AppCompatActivity(), OcrResultListener {

    private lateinit var imageView: ImageView
    private lateinit var cropOverlay: CropOverlayView
    private lateinit var btnConfirm: FloatingActionButton
    private lateinit var etManualText: EditText
    private lateinit var ocrTextExtractor: OcrTextExtractor

    private var sourceBitmap: Bitmap? = null
    private var source: OcrSource = OcrSource.GALLERY

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crop_confirm)

        imageView = findViewById(R.id.imageToProcess)
        cropOverlay = findViewById(R.id.cropOverlay)
        btnConfirm = findViewById(R.id.btnConfirm)
        etManualText = findViewById(R.id.etManualText)

        ocrTextExtractor = OcrTextExtractor(listener = this)

        val filePath = intent.getStringExtra(EXTRA_FILE_PATH)
        val uriString = intent.getStringExtra(EXTRA_URI)

        source =
            if (filePath != null) {
                OcrSource.LIVE_CAMERA
            } else {
                OcrSource.GALLERY
            }

        val imageSource: Any? = when {
            filePath != null -> File(filePath)
            uriString != null -> Uri.parse(uriString)
            else -> null
        }

        val bitmap =
            imageSource?.let {
                ImagePreprocessor.decodeScaledBitmap(this, it)
            }

        if (bitmap == null || imageSource == null) {
            Toast.makeText(
                this,
                "Could not load image",
                Toast.LENGTH_SHORT
            ).show()

            finish()
            return
        }

        val corrected =
            ImagePreprocessor.correctOrientation(
                this,
                bitmap,
                imageSource
            )

        sourceBitmap = corrected

        imageView.setImageBitmap(corrected)

        imageView.post {
            cropOverlay.setInitialRect(
                ScanTarget.fractionRect()
            )
        }

        btnConfirm.setOnClickListener {
            confirmCrop()
        }

        findViewById<Button>(R.id.btnUseManualText)
            .setOnClickListener {

                val manualText =
                    etManualText.text.toString()

                if (manualText.isNotBlank()) {
                    onTextExtracted(
                        manualText,
                        source
                    )
                } else {
                    Toast.makeText(
                        this,
                        "Type something first",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

    private fun confirmCrop() {

        val bitmap = sourceBitmap ?: return

        val bitmapRect =
            viewRectToBitmapRect(
                cropOverlay.cropRect,
                imageView,
                bitmap
            )

        val cropped =
            ImagePreprocessor.cropToRect(
                bitmap,
                bitmapRect
            )

        val enhanced =
            ImagePreprocessor.enhanceForOcr(
                cropped
            )

        btnConfirm.isEnabled = false

        ocrTextExtractor.extractFromBitmap(
            enhanced,
            source
        )
    }

    private fun viewRectToBitmapRect(
        viewRect: RectF,
        imageView: ImageView,
        bitmap: Bitmap
    ): Rect {

        val inverse = Matrix()

        imageView.imageMatrix.invert(inverse)

        val points = floatArrayOf(
            viewRect.left,
            viewRect.top,
            viewRect.right,
            viewRect.bottom
        )

        inverse.mapPoints(points)

        return Rect(
            points[0]
                .coerceIn(
                    0f,
                    bitmap.width.toFloat()
                )
                .toInt(),

            points[1]
                .coerceIn(
                    0f,
                    bitmap.height.toFloat()
                )
                .toInt(),

            points[2]
                .coerceIn(
                    0f,
                    bitmap.width.toFloat()
                )
                .toInt(),

            points[3]
                .coerceIn(
                    0f,
                    bitmap.height.toFloat()
                )
                .toInt()
        )
    }

    override fun onTextExtracted(
        rawText: String,
        source: OcrSource
    ) {

        btnConfirm.isEnabled = true

        if (rawText.isBlank()) {

            Toast.makeText(
                this,
                "No text found — try adjusting the box",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val expiryDate =
            ExpiryParser.extractExpiryDate(rawText)

        val intent =
            Intent(
                this,
                ResultActivity::class.java
            )

        if (expiryDate != null) {

            intent.putExtra(
                ResultActivity.EXTRA_EXPIRY_DATE,
                expiryDate
            )
        }

        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        ocrTextExtractor.close()
    }

    companion object {
        const val EXTRA_FILE_PATH = "extra_file_path"
        const val EXTRA_URI = "extra_uri"
    }
}