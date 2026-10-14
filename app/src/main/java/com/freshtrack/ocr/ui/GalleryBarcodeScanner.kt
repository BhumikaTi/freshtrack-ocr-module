package com.freshtrack.ocr.ui

import android.content.Context
import android.net.Uri
import com.freshtrack.ocr.data.BarcodeOptions
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.suspendCancellableCoroutine


object GalleryBarcodeScanner {

    suspend fun scan(context: Context, imageUri: Uri): String? =
        suspendCancellableCoroutine { continuation ->
            try {
                val inputImage = InputImage.fromFilePath(context, imageUri)
                BarcodeOptions.newScanner().process(inputImage)
                    .addOnSuccessListener { barcodes ->
                        continuation.resumeWith(Result.success(barcodes.firstOrNull()?.rawValue))
                    }
                    .addOnFailureListener {
                        continuation.resumeWith(Result.success(null))
                    }
            } catch (e: Exception) {
                continuation.resumeWith(Result.success(null))
            }
        }
}