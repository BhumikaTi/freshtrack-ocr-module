package com.freshtrack.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

class OcrTextExtractor(private val listener: OcrResultListener) {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    fun extractFromBitmap(bitmap: Bitmap, source: OcrSource) {
        val image = InputImage.fromBitmap(bitmap, 0)
        recognizer.process(image)
            .addOnSuccessListener { visionText -> listener.onTextExtracted(visionText.text, source) }
            .addOnFailureListener { listener.onTextExtracted("", source) }
    }

    fun extractFromGalleryUri(context: Context, uri: Uri) {
        val bitmap = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
        if (bitmap == null) {
            listener.onTextExtracted("", OcrSource.GALLERY)
            return
        }
        val enhanced = ImagePreprocessor.enhanceForOcr(bitmap)
        extractFromBitmap(enhanced, OcrSource.GALLERY)
    }

    fun close() = recognizer.close()
}