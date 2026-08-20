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

    fun decodeScaledBitmap(context: Context, source: Any, maxDimension: Int = 1600): Bitmap? {
        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        decodeInto(context, source, boundsOptions)
        val w = boundsOptions.outWidth
        val h = boundsOptions.outHeight
        if (w <= 0 || h <= 0) return null

        var sampleSize = 1
        while (w / sampleSize > maxDimension || h / sampleSize > maxDimension) sampleSize *= 2
        val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        return decodeInto(context, source, decodeOptions)
    }

    private fun decodeInto(context: Context, source: Any, options: BitmapFactory.Options): Bitmap? {
        return when (source) {
            is java.io.File -> BitmapFactory.decodeFile(source.path, options)
            is Uri -> context.contentResolver.openInputStream(source)?.use {
                BitmapFactory.decodeStream(it, null, options)
            }
            else -> null
        }
    }

    /**
     * Reads EXIF orientation metadata and rotates the bitmap to match.
     * Fixes the "sideways/upside-down photo → OCR finds nothing" problem,
     * since decoded bitmap pixel data isn't auto-rotated by Android.
     */
    fun correctOrientation(context: Context, bitmap: Bitmap, source: Any): Bitmap {
        val stream = when (source) {
            is java.io.File -> java.io.FileInputStream(source)
            is Uri -> context.contentResolver.openInputStream(source)
            else -> return bitmap
        } ?: return bitmap

        val exif = ExifInterface(stream)
        stream.close()
        val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)

        val degrees = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        if (degrees == 0f) return bitmap

        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    fun cropToRect(source: Bitmap, rect: Rect): Bitmap {
        val left = rect.left.coerceIn(0, source.width - 1)
        val top = rect.top.coerceIn(0, source.height - 1)
        val right = rect.right.coerceIn(left + 1, source.width)
        val bottom = rect.bottom.coerceIn(top + 1, source.height)
        return Bitmap.createBitmap(source, left, top, right - left, bottom - top)
    }

    fun enhanceForOcr(source: Bitmap): Bitmap {
        val minReadableHeight = 300
        val scale = if (source.height < minReadableHeight) minReadableHeight.toFloat() / source.height else 1f
        val scaled = if (scale > 1f) {
            Bitmap.createScaledBitmap(source, (source.width * scale).toInt(), (source.height * scale).toInt(), true)
        } else source

        val contrast = 1.6f
        val translate = (-0.5f * contrast + 0.5f) * 255f
        val colorMatrix = ColorMatrix().apply { setSaturation(0f) }
        val contrastMatrix = ColorMatrix(
            floatArrayOf(
                contrast, 0f, 0f, 0f, translate,
                0f, contrast, 0f, 0f, translate,
                0f, 0f, contrast, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            )
        )
        colorMatrix.postConcat(contrastMatrix)

        val output = Bitmap.createBitmap(scaled.width, scaled.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint().apply { colorFilter = ColorMatrixColorFilter(colorMatrix); isAntiAlias = true }
        canvas.drawBitmap(scaled, 0f, 0f, paint)
        return output
    }
}