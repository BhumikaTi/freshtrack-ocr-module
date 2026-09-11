package com.freshtrack.ocr.ui

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import android.os.Bundle
import com.google.android.material.button.MaterialButton
import android.util.Log
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.freshtrack.ocr.ExpiryParser
import com.freshtrack.ocr.ImagePreprocessor
import com.freshtrack.ocr.OcrResultListener
import com.freshtrack.ocr.OcrSource
import com.freshtrack.ocr.OcrTextExtractor
import com.freshtrack.ocr.R
import com.freshtrack.ocr.ui.CropOverlayView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlin.math.max
import kotlin.math.min

class CropConfirmActivity :
    AppCompatActivity(),
    OcrResultListener {

    companion object {

        private const val TAG =
            "EXPIRY_FLOW_DEBUG"

        const val EXTRA_FILE_PATH =
            "extra_file_path"

        const val EXTRA_URI =
            "extra_uri"

        private const val MIN_SCALE =
            1f

        private const val MAX_SCALE =
            5f
    }

    private lateinit var imageView:
            ImageView

    private lateinit var cropOverlay:
            CropOverlayView

    private lateinit var btnRotate:
            FloatingActionButton

    private lateinit var btnConfirm:
            FloatingActionButton

    private lateinit var btnCrop:
            MaterialButton

    private lateinit var ocrTextExtractor:
            OcrTextExtractor

    private var sourceBitmap:
            Bitmap? = null

    private var source:
            OcrSource =
        OcrSource.GALLERY

    private val imageMatrix =
        Matrix()

    private var currentScale =
        1f

    private var lastTouchX =
        0f

    private var lastTouchY =
        0f

    private var isDragging =
        false

    private lateinit var scaleGestureDetector:
            ScaleGestureDetector

    /*
     * True only while the user has explicitly
     * entered crop mode.
     */
    private var cropModeActive =
        false

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(
            savedInstanceState
        )

        setContentView(
            R.layout.activity_crop_confirm
        )

        imageView =
            findViewById(
                R.id.imageToProcess
            )

        cropOverlay =
            findViewById(
                R.id.cropOverlay
            )

        btnRotate =
            findViewById(
                R.id.btnRotate
            )

        btnConfirm =
            findViewById(
                R.id.btnConfirm
            )

        btnCrop =
            findViewById(
                R.id.btnCrop
            )

        ocrTextExtractor =
            OcrTextExtractor(
                this
            )

        loadSourceImage()

        setupButtons()

        setupZoomGesture()
    }

    /*
     * ------------------------------------------------------
     * LOAD IMAGE
     * ------------------------------------------------------
     */

    private fun loadSourceImage() {

        val filePath =
            intent.getStringExtra(
                EXTRA_FILE_PATH
            )

        val uriString =
            intent.getStringExtra(
                EXTRA_URI
            )

        when {

            !filePath.isNullOrBlank() -> {

                source =
                    OcrSource.LIVE_CAMERA

                val file =
                    java.io.File(
                        filePath
                    )

                val decoded =
                    ImagePreprocessor
                        .decodeScaledBitmap(
                            this,
                            file
                        )

                if (decoded == null) {

                    Toast.makeText(
                        this,
                        "Could not load image",
                        Toast.LENGTH_LONG
                    ).show()

                    finish()
                    return
                }

                sourceBitmap =
                    ImagePreprocessor
                        .correctOrientation(
                            this,
                            decoded,
                            file
                        )
            }

            !uriString.isNullOrBlank() -> {

                source =
                    OcrSource.GALLERY

                val uri =
                    Uri.parse(
                        uriString
                    )

                val decoded =
                    ImagePreprocessor
                        .decodeScaledBitmap(
                            this,
                            uri
                        )

                if (decoded == null) {

                    Toast.makeText(
                        this,
                        "Could not load image",
                        Toast.LENGTH_LONG
                    ).show()

                    finish()
                    return
                }

                sourceBitmap =
                    ImagePreprocessor
                        .correctOrientation(
                            this,
                            decoded,
                            uri
                        )
            }

            else -> {

                Toast.makeText(
                    this,
                    "No image selected",
                    Toast.LENGTH_LONG
                ).show()

                finish()
                return
            }
        }

        val bitmap =
            sourceBitmap

        if (bitmap == null) {

            Toast.makeText(
                this,
                "Could not load image",
                Toast.LENGTH_LONG
            ).show()

            finish()
            return
        }

        imageView.setImageBitmap(
            bitmap
        )

        imageView.post {

            setupInitialImage()
        }
    }

    /*
     * ------------------------------------------------------
     * INITIAL IMAGE FIT
     * ------------------------------------------------------
     */

    private fun setupInitialImage() {

        val bitmap =
            sourceBitmap
                ?: return

        val viewWidth =
            imageView.width.toFloat()

        val viewHeight =
            imageView.height.toFloat()

        if (
            viewWidth <= 0f ||
            viewHeight <= 0f
        ) {
            return
        }

        val bitmapWidth =
            bitmap.width.toFloat()

        val bitmapHeight =
            bitmap.height.toFloat()

        val scale =
            min(
                viewWidth / bitmapWidth,
                viewHeight / bitmapHeight
            )

        val scaledWidth =
            bitmapWidth * scale

        val scaledHeight =
            bitmapHeight * scale

        val dx =
            (viewWidth - scaledWidth) / 2f

        val dy =
            (viewHeight - scaledHeight) / 2f

        imageMatrix.reset()

        imageMatrix.postScale(
            scale,
            scale
        )

        imageMatrix.postTranslate(
            dx,
            dy
        )

        imageView.imageMatrix =
            imageMatrix

        currentScale =
            1f
    }

    /*
     * ------------------------------------------------------
     * BUTTONS
     * ------------------------------------------------------
     */

    private fun setupButtons() {

        btnRotate.setOnClickListener {

            rotateImage()
        }

        btnCrop.setOnClickListener {
            if (cropModeActive) {
                cancelCropMode()
            } else {
                enterCropMode()
            }
        }

        btnConfirm.setOnClickListener {

            confirmScan()
        }
    }

    /*
     * ------------------------------------------------------
     * CROP MODE
     * ------------------------------------------------------
     */

    private fun cancelCropMode() {
        cropModeActive = false
        cropOverlay.hideCrop()
        btnCrop.text = "CROP"

        Toast.makeText(
            this,
            "Crop cancelled",
            Toast.LENGTH_SHORT
        ).show()
    }
    private fun enterCropMode() {

        val width = cropOverlay.width.toFloat()
        val height = cropOverlay.height.toFloat()

        if (width <= 0f || height <= 0f) {
            Toast.makeText(
                this,
                "Image is not ready",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        // Start with a centered crop area.
        // 75% of the view width and 55% of the view height.
        val cropWidth = width * 0.75f
        val cropHeight = height * 0.55f

        val left = (width - cropWidth) / 2f
        val top = (height - cropHeight) / 2f
        val right = left + cropWidth
        val bottom = top + cropHeight

        cropOverlay.setCropRect(
            RectF(
                left,
                top,
                right,
                bottom
            )
        )

        cropModeActive = true

        btnCrop.text = "CANCEL CROP"
    }

    /*
     * ------------------------------------------------------
     * CONFIRM SCAN
     * ------------------------------------------------------
     */

    private fun confirmScan() {

        val bitmap = sourceBitmap

        if (bitmap == null) {
            Toast.makeText(
                this,
                "Image not available",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        /*
         * ============================================================
         * NORMAL MODE
         *
         * User did NOT activate crop mode.
         * OCR the complete image.
         * ============================================================
         */

        if (!cropModeActive) {

            Log.d(
                TAG,
                "CONFIRM: full-image OCR"
            )

            val enhancedFull =
                ImagePreprocessor.enhanceForOcr(
                    bitmap
                )

            ocrTextExtractor.extractFromBitmap(
                enhancedFull,
                source
            )

            return
        }

        /*
         * ============================================================
         * CROP MODE
         *
         * User activated crop mode.
         * Convert the crop rectangle from the ImageView's
         * coordinates into the original bitmap's coordinates.
         * ============================================================
         */

        Log.d(
            TAG,
            "CONFIRM: CROP OCR"
        )

        val cropRect =
            cropOverlay.cropRect

        Log.d(
            TAG,
            "Crop view rect: $cropRect"
        )

        val bitmapRect =
            viewRectToBitmapRect(
                cropRect
            )

        Log.d(
            TAG,
            "Crop bitmap rect: $bitmapRect"
        )

        if (
            bitmapRect.width() <= 0 ||
            bitmapRect.height() <= 0
        ) {

            Toast.makeText(
                this,
                "Invalid crop area",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val croppedBitmap =
            ImagePreprocessor.cropToRect(
                bitmap,
                bitmapRect
            )

        Log.d(
            TAG,
            "Cropped bitmap size: " +
                    "${croppedBitmap.width}x" +
                    "${croppedBitmap.height}"
        )

        /*
         * IMPORTANT:
         *
         * Enhance ONLY the cropped bitmap.
         * Do NOT enhance the original bitmap here.
         */

        val enhancedBitmap =
            ImagePreprocessor.enhanceForOcr(
                croppedBitmap
            )

        ocrTextExtractor.extractFromBitmap(
            enhancedBitmap,
            source
        )
    }

    /*
     * ------------------------------------------------------
     * VIEW RECT -> BITMAP RECT
     * ------------------------------------------------------
     */

    private fun viewRectToBitmapRect(
        viewRect: RectF
    ): Rect {

        val inverse =
            Matrix()

        val inverted =
            imageMatrix.invert(
                inverse
            )

        if (!inverted) {

            Log.e(
                TAG,
                "Could not invert image matrix"
            )

            return Rect(
                0,
                0,
                1,
                1
            )
        }

        val points =
            floatArrayOf(

                viewRect.left,
                viewRect.top,

                viewRect.right,
                viewRect.top,

                viewRect.left,
                viewRect.bottom,

                viewRect.right,
                viewRect.bottom
            )

        inverse.mapPoints(
            points
        )

        var left =
            points[0]

        var top =
            points[1]

        var right =
            points[0]

        var bottom =
            points[1]

        var i =
            2

        while (
            i < points.size
        ) {

            val x =
                points[i]

            val y =
                points[i + 1]

            left =
                min(
                    left,
                    x
                )

            top =
                min(
                    top,
                    y
                )

            right =
                max(
                    right,
                    x
                )

            bottom =
                max(
                    bottom,
                    y
                )

            i += 2
        }

        val bitmap =
            sourceBitmap
                ?: return Rect(
                    0,
                    0,
                    1,
                    1
                )

        left =
            left.coerceIn(
                0f,
                bitmap.width.toFloat()
            )

        top =
            top.coerceIn(
                0f,
                bitmap.height.toFloat()
            )

        right =
            right.coerceIn(
                0f,
                bitmap.width.toFloat()
            )

        bottom =
            bottom.coerceIn(
                0f,
                bitmap.height.toFloat()
            )

        if (right <= left) {

            right =
                min(
                    bitmap.width.toFloat(),
                    left + 1f
                )
        }

        if (bottom <= top) {

            bottom =
                min(
                    bitmap.height.toFloat(),
                    top + 1f
                )
        }

        return Rect(
            left.toInt(),
            top.toInt(),
            right.toInt(),
            bottom.toInt()
        )
    }

    /*
     * ------------------------------------------------------
     * ROTATION
     * ------------------------------------------------------
     */

    private fun rotateImage() {

        val bitmap =
            sourceBitmap
                ?: return

        val centerX =
            imageView.width / 2f

        val centerY =
            imageView.height / 2f

        imageMatrix.postRotate(
            90f,
            centerX,
            centerY
        )

        imageView.imageMatrix =
            imageMatrix

        Log.d(
            TAG,
            "Image rotated 90 degrees"
        )
    }

    /*
     * ------------------------------------------------------
     * ZOOM / PAN
     * ------------------------------------------------------
     */

    private fun setupZoomGesture() {

        scaleGestureDetector =
            ScaleGestureDetector(
                this,
                object :
                    ScaleGestureDetector
                    .SimpleOnScaleGestureListener() {

                    override fun onScale(
                        detector:
                        ScaleGestureDetector
                    ): Boolean {

                        val factor =
                            detector.scaleFactor

                        val oldScale =
                            currentScale

                        currentScale =
                            (
                                    currentScale *
                                            factor
                                    ).coerceIn(
                                    MIN_SCALE,
                                    MAX_SCALE
                                )

                        val actualFactor =
                            currentScale /
                                    oldScale

                        val focusX =
                            detector.focusX

                        val focusY =
                            detector.focusY

                        imageMatrix.postScale(
                            actualFactor,
                            actualFactor,
                            focusX,
                            focusY
                        )

                        imageView.imageMatrix =
                            imageMatrix

                        return true
                    }
                }
            )

        imageView.setOnTouchListener {
                _,
                event ->

            scaleGestureDetector
                .onTouchEvent(
                    event
                )

            when (
                event.actionMasked
            ) {

                MotionEvent.ACTION_DOWN -> {

                    lastTouchX =
                        event.x

                    lastTouchY =
                        event.y

                    isDragging =
                        true

                    true
                }

                MotionEvent.ACTION_MOVE -> {

                    if (
                        isDragging &&
                        event.pointerCount == 1
                    ) {

                        val dx =
                            event.x -
                                    lastTouchX

                        val dy =
                            event.y -
                                    lastTouchY

                        imageMatrix.postTranslate(
                            dx,
                            dy
                        )

                        imageView.imageMatrix =
                            imageMatrix

                        lastTouchX =
                            event.x

                        lastTouchY =
                            event.y
                    }

                    true
                }

                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL -> {

                    isDragging =
                        false

                    true
                }

                else -> true
            }
        }
    }

    /*
     * ------------------------------------------------------
     * OCR CALLBACK
     * ------------------------------------------------------
     */

    override fun onTextExtracted(
        rawText: String,
        source: OcrSource
    ) {

        Log.d(
            TAG,
            "========== OCR RESULT RECEIVED =========="
        )

        Log.d(
            TAG,
            "SOURCE: $source"
        )

        Log.d(
            TAG,
            "RAW OCR TEXT:\n$rawText"
        )

        val parsed =
            ExpiryParser.parse(
                rawText
            )

        Log.d(
            TAG,
            "========== PARSER RESULT =========="
        )

        Log.d(
            TAG,
            "PRODUCT NAME: ${parsed.productName}"
        )

        Log.d(
            TAG,
            "EXPIRY DATE: ${parsed.expiryDate}"
        )

        Log.d(
            TAG,
            "CONFIDENCE: ${parsed.confidence}"
        )

        Log.d(
            TAG,
            "======================================"
        )

        val expiryDate =
            parsed.expiryDate

        if (
            expiryDate.isNullOrBlank()
        ) {

            Log.d(
                TAG,
                "Parser returned NO expiry date"
            )

            runOnUiThread {

                showExpiryNotFoundDialog()
            }

            return
        }

        Log.d(
            TAG,
            "SUCCESS: expiry = $expiryDate"
        )

        runOnUiThread {

            val intent =
                Intent(
                    this,
                    ResultActivity::class.java
                )

            intent.putExtra(
                ResultActivity.EXTRA_EXPIRY_DATE,
                expiryDate
            )

            /*
             * Pass the parser's best-effort
             * product name to ResultActivity.
             */
            if (
                !parsed.productName.isNullOrBlank()
            ) {

                intent.putExtra(
                    ResultActivity.EXTRA_PRODUCT_NAME,
                    parsed.productName
                )
            }

            startActivity(
                intent
            )

            /*
             * IMPORTANT:
             *
             * Do NOT call finish() here.
             *
             * Keeping this activity alive means:
             *
             * ResultActivity
             *      ↓ Back
             * CropConfirmActivity
             *
             * instead of returning directly
             * to MainActivity.
             */
        }
    }

    /*
     * ------------------------------------------------------
     * OCR REGION CALLBACK
     * ------------------------------------------------------
     */

    override fun onTextRegionsDetected(
        regions:
        List<com.freshtrack.ocr.OcrTextRegion>,
        source: OcrSource
    ) {

        Log.d(
            TAG,
            "OCR REGIONS DETECTED: ${regions.size}"
        )

        for (region in regions) {

            Log.d(
                TAG,
                "REGION: \"${region.text}\" " +
                        "BOX=(" +
                        "${region.left}," +
                        "${region.top}," +
                        "${region.right}," +
                        "${region.bottom})"
            )
        }
    }

    /*
     * ------------------------------------------------------
     * EXPIRY NOT FOUND
     * ------------------------------------------------------
     */

    private fun showExpiryNotFoundDialog() {

        AlertDialog.Builder(
            this
        )
            .setTitle(
                "Failed to detect expiry date"
            )
            .setMessage(
                "We found text, but couldn't identify an expiry date."
            )
            .setNegativeButton(
                "CANCEL",
                null
            )
            .setPositiveButton(
                "ADD MANUALLY"
            ) { _, _ ->

                val intent =
                    Intent(
                        this,
                        ManualEntryActivity::class.java
                    )

                startActivity(
                    intent
                )

                finish()
            }
            .show()
    }

    /*
     * ------------------------------------------------------
     * CLEANUP
     * ------------------------------------------------------
     */

    override fun onDestroy() {

        ocrTextExtractor.close()

        super.onDestroy()
    }
}