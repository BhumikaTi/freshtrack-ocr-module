package com.freshtrack.ocr

interface OcrResultListener {

    fun onTextExtracted(
        rawText: String,
        source: OcrSource
    )

    /*
     * Optional callback for OCR bounding boxes.
     *
     * Existing OCR listeners do not need to implement
     * this yet. CropConfirmActivity will use it when
     * we add automatic crop positioning.
     */
    fun onTextRegionsDetected(
        regions: List<OcrTextRegion>,
        source: OcrSource
    ) {
        // Optional callback.
    }
}

data class OcrTextRegion(
    val text: String,
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
) {
    val width: Int
        get() = right - left

    val height: Int
        get() = bottom - top
}

enum class OcrSource {
    GALLERY,
    LIVE_CAMERA
}