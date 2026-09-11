package com.freshtrack.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

class OcrTextExtractor(
    private val listener: OcrResultListener
) {

    private val recognizer: TextRecognizer =
        TextRecognition.getClient(
            TextRecognizerOptions.DEFAULT_OPTIONS
        )

    fun extractFromBitmap(
        bitmap: Bitmap,
        source: OcrSource
    ) {

        if (
            bitmap.width <= 0 ||
            bitmap.height <= 0
        ) {

            Log.e(
                TAG,
                "Invalid bitmap dimensions"
            )

            listener.onTextRegionsDetected(
                emptyList(),
                source
            )

            listener.onTextExtracted(
                "",
                source
            )

            return
        }

        Log.d(
            TAG,
            "OCR starting: " +
                    "${bitmap.width}x${bitmap.height}"
        )

        /*
         * First attempt:
         *
         * Use the original image exactly as supplied.
         *
         * This is important because preprocessing can sometimes
         * destroy small characters or printed dates.
         */
        runOcr(
            bitmap = bitmap,
            source = source,
            allowFallback = true
        )
    }

    private fun runOcr(
        bitmap: Bitmap,
        source: OcrSource,
        allowFallback: Boolean
    ) {

        val image =
            InputImage.fromBitmap(
                bitmap,
                0
            )

        recognizer
            .process(image)
            .addOnSuccessListener { visionText ->

                val text =
                    visionText.text

                Log.d(
                    TAG,
                    "OCR pass completed. " +
                            "Characters: ${text.length}"
                )

                Log.d(
                    TAG,
                    "OCR TEXT:\n$text"
                )

                val regions =
                    extractRegions(
                        visionText
                    )

                Log.d(
                    TAG,
                    "OCR regions: ${regions.size}"
                )

                /*
                 * If we got useful text, stop here.
                 */
                if (
                    text.isNotBlank()
                ) {

                    listener.onTextRegionsDetected(
                        regions,
                        source
                    )

                    listener.onTextExtracted(
                        text,
                        source
                    )

                    return@addOnSuccessListener
                }

                /*
                 * No text.
                 *
                 * Try a second pass using an enlarged,
                 * grayscale/contrast-enhanced image.
                 */
                if (allowFallback) {

                    Log.d(
                        TAG,
                        "OCR returned no text. " +
                                "Starting enhanced fallback."
                    )

                    val enhanced =
                        createEnhancedBitmap(
                            bitmap
                        )

                    runOcr(
                        bitmap = enhanced,
                        source = source,
                        allowFallback = false
                    )

                } else {

                    listener.onTextRegionsDetected(
                        emptyList(),
                        source
                    )

                    listener.onTextExtracted(
                        "",
                        source
                    )
                }
            }
            .addOnFailureListener { exception ->

                Log.e(
                    TAG,
                    "OCR pass failed",
                    exception
                )

                /*
                 * If the original pass failed, still give
                 * the enhanced image one chance.
                 */
                if (allowFallback) {

                    Log.d(
                        TAG,
                        "Starting enhanced OCR fallback " +
                                "after recognition failure."
                    )

                    val enhanced =
                        createEnhancedBitmap(
                            bitmap
                        )

                    runOcr(
                        bitmap = enhanced,
                        source = source,
                        allowFallback = false
                    )

                } else {

                    listener.onTextRegionsDetected(
                        emptyList(),
                        source
                    )

                    listener.onTextExtracted(
                        "",
                        source
                    )
                }
            }
    }

    private fun extractRegions(
        visionText:
        com.google.mlkit.vision.text.Text
    ): List<OcrTextRegion> {

        val regions =
            mutableListOf<OcrTextRegion>()

        for (
        block in visionText.textBlocks
        ) {

            for (
            line in block.lines
            ) {

                val box =
                    line.boundingBox

                if (box != null) {

                    val region =
                        OcrTextRegion(
                            text = line.text,
                            left = box.left,
                            top = box.top,
                            right = box.right,
                            bottom = box.bottom
                        )

                    regions.add(
                        region
                    )

                    Log.d(
                        TAG,
                        "OCR REGION: " +
                                "\"${region.text}\" " +
                                "BOX: " +
                                "(${region.left}, " +
                                "${region.top}) - " +
                                "(${region.right}, " +
                                "${region.bottom})"
                    )
                }
            }
        }

        return regions
    }

    private fun createEnhancedBitmap(
        source: Bitmap
    ): Bitmap {

        /*
         * First apply the existing enhancement.
         */
        val enhanced =
            ImagePreprocessor.enhanceForOcr(
                source
            )

        /*
         * If the image is still relatively small,
         * enlarge it another time before OCR.
         *
         * Small expiry stamps benefit significantly
         * from having more pixels per character.
         */
        val minHeight =
            1000

        if (
            enhanced.height >= minHeight
        ) {
            return enhanced
        }

        val scale =
            minHeight.toFloat() /
                    enhanced.height.toFloat()

        val newWidth =
            (
                    enhanced.width *
                            scale
                    ).toInt()

        val newHeight =
            (
                    enhanced.height *
                            scale
                    ).toInt()

        return Bitmap.createScaledBitmap(
            enhanced,
            newWidth,
            newHeight,
            true
        )
    }

    fun extractFromGalleryUri(
        context: Context,
        uri: Uri
    ) {

        val bitmap =
            context.contentResolver
                .openInputStream(uri)
                ?.use {
                    BitmapFactory.decodeStream(it)
                }

        if (bitmap == null) {

            Log.e(
                TAG,
                "Could not decode gallery image"
            )

            listener.onTextRegionsDetected(
                emptyList(),
                OcrSource.GALLERY
            )

            listener.onTextExtracted(
                "",
                OcrSource.GALLERY
            )

            return
        }

        extractFromBitmap(
            bitmap,
            OcrSource.GALLERY
        )
    }

    fun close() {
        recognizer.close()
    }

    companion object {

        private const val TAG =
            "OCR_DEBUG"
    }
}