package com.freshtrack.ocr

fun interface OcrResultListener {
    fun onTextExtracted(rawText: String, source: OcrSource)
}

enum class OcrSource {
    GALLERY,
    LIVE_CAMERA
}