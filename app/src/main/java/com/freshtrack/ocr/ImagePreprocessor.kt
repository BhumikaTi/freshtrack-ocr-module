package com.freshtrack.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.net.Uri
import androidx.exifinterface.media.ExifInterface

object ImagePreprocessor {

    fun decodeScaledBitmap(
        context: Context,
        source: Any,
        maxDimension: Int = 1600
    ): Bitmap? {

        val boundsOptions =
            BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }

        decodeInto(
            context,
            source,
            boundsOptions
        )

        val width = boundsOptions.outWidth
        val height = boundsOptions.outHeight

        if (width <= 0 || height <= 0) {
            return null
        }

        var sampleSize = 1

        while (
            width / sampleSize > maxDimension ||
            height / sampleSize > maxDimension
        ) {
            sampleSize *= 2
        }

        val decodeOptions =
            BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

        return decodeInto(
            context,
            source,
            decodeOptions
        )
    }

    private fun decodeInto(
        context: Context,
        source: Any,
        options: BitmapFactory.Options
    ): Bitmap? {

        return when (source) {

            is java.io.File ->
                BitmapFactory.decodeFile(
                    source.path,
                    options
                )

            is Uri ->
                context.contentResolver
                    .openInputStream(source)
                    ?.use {
                        BitmapFactory.decodeStream(
                            it,
                            null,
                            options
                        )
                    }

            else -> null
        }
    }

    /**
     * Reads EXIF orientation metadata and rotates
     * the bitmap so that the pixel data matches
     * the physical orientation of the photo.
     */
    fun correctOrientation(
        context: Context,
        bitmap: Bitmap,
        source: Any
    ): Bitmap {

        val stream =
            when (source) {

                is java.io.File ->
                    java.io.FileInputStream(source)

                is Uri ->
                    context.contentResolver
                        .openInputStream(source)

                else -> return bitmap
            } ?: return bitmap

        val exif =
            ExifInterface(stream)

        stream.close()

        val orientation =
            exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )

        val degrees =
            when (orientation) {

                ExifInterface.ORIENTATION_ROTATE_90 ->
                    90f

                ExifInterface.ORIENTATION_ROTATE_180 ->
                    180f

                ExifInterface.ORIENTATION_ROTATE_270 ->
                    270f

                else -> 0f
            }

        if (degrees == 0f) {
            return bitmap
        }

        val matrix =
            Matrix().apply {
                postRotate(degrees)
            }

        return Bitmap.createBitmap(
            bitmap,
            0,
            0,
            bitmap.width,
            bitmap.height,
            matrix,
            true
        )
    }

    fun cropToRect(
        source: Bitmap,
        rect: Rect
    ): Bitmap {

        if (source.width <= 0 || source.height <= 0) {
            return source
        }

        val left =
            rect.left.coerceIn(
                0,
                source.width - 1
            )

        val top =
            rect.top.coerceIn(
                0,
                source.height - 1
            )

        val right =
            rect.right.coerceIn(
                left + 1,
                source.width
            )

        val bottom =
            rect.bottom.coerceIn(
                top + 1,
                source.height
            )

        return Bitmap.createBitmap(
            source,
            left,
            top,
            right - left,
            bottom - top
        )
    }

    /**
     * Prepares a cropped image for OCR.
     *
     * The goal is to make small printed dates
     * easier for ML Kit to recognize without
     * destroying the original image.
     */
    fun enhanceForOcr(
        source: Bitmap
    ): Bitmap {

        if (
            source.width <= 0 ||
            source.height <= 0
        ) {
            return source
        }

        /*
         * Upscale small crops.
         *
         * 600 px is a safer target than the old
         * 300 px minimum for small printed dates.
         */
        val minReadableHeight = 600

        val scale =
            if (source.height < minReadableHeight) {
                minReadableHeight.toFloat() /
                        source.height.toFloat()
            } else {
                1f
            }

        val scaled =
            if (scale > 1f) {

                Bitmap.createScaledBitmap(
                    source,
                    (source.width * scale).toInt(),
                    (source.height * scale).toInt(),
                    true
                )

            } else {
                source
            }

        /*
         * Convert to grayscale and apply moderate
         * contrast enhancement.
         *
         * 1.35 is intentionally less aggressive
         * than the previous 1.6 value.
         */
        val contrast = 1.35f

        val translate =
            (-0.5f * contrast + 0.5f) * 255f

        val colorMatrix =
            ColorMatrix().apply {
                setSaturation(0f)
            }

        val contrastMatrix =
            ColorMatrix(
                floatArrayOf(
                    contrast, 0f, 0f, 0f, translate,
                    0f, contrast, 0f, 0f, translate,
                    0f, 0f, contrast, 0f, translate,
                    0f, 0f, 0f, 1f, 0f
                )
            )

        colorMatrix.postConcat(
            contrastMatrix
        )

        val output =
            Bitmap.createBitmap(
                scaled.width,
                scaled.height,
                Bitmap.Config.ARGB_8888
            )

        val canvas =
            Canvas(output)

        val paint =
            Paint().apply {
                colorFilter =
                    ColorMatrixColorFilter(
                        colorMatrix
                    )

                isAntiAlias = true

                isFilterBitmap = true
            }

        canvas.drawBitmap(
            scaled,
            0f,
            0f,
            paint
        )

        /*
         * Do not recycle the source bitmap here.
         *
         * The source may still be needed by the
         * crop screen or Android image pipeline.
         */
        return output
    }
}