package com.freshtrack.ocr.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs

class CropOverlayView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    var cropRect = RectF(0f, 0f, 0f, 0f)
        private set

    private val handleRadius = 40f
    private var activeHandle = Handle.NONE
    private var lastTouchX = 0f
    private var lastTouchY = 0f

    private enum class Handle { NONE, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, MOVE }

    private val scrimPaint = Paint().apply { color = Color.parseColor("#B0000000") }
    private val boxPaint = Paint().apply {
        color = Color.parseColor("#39FF14")
        style = Paint.Style.STROKE
        strokeWidth = 4f
        isAntiAlias = true
    }
    private val handlePaint = Paint().apply {
        color = Color.parseColor("#39FF14")
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (cropRect.width() == 0f) {
            cropRect = RectF(w * 0.10f, h * 0.35f, w * 0.90f, h * 0.65f)
        }
    }

    fun setInitialRect(fraction: RectF) {
        cropRect = RectF(
            fraction.left * width, fraction.top * height,
            fraction.right * width, fraction.bottom * height
        )
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val path = Path().apply {
            addRect(0f, 0f, width.toFloat(), height.toFloat(), Path.Direction.CW)
            addRect(cropRect, Path.Direction.CCW)
        }
        canvas.drawPath(path, scrimPaint)
        canvas.drawRect(cropRect, boxPaint)
        canvas.drawCircle(cropRect.left, cropRect.top, handleRadius / 2, handlePaint)
        canvas.drawCircle(cropRect.right, cropRect.top, handleRadius / 2, handlePaint)
        canvas.drawCircle(cropRect.left, cropRect.bottom, handleRadius / 2, handlePaint)
        canvas.drawCircle(cropRect.right, cropRect.bottom, handleRadius / 2, handlePaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                activeHandle = detectHandle(event.x, event.y)
                lastTouchX = event.x; lastTouchY = event.y
                return activeHandle != Handle.NONE
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - lastTouchX
                val dy = event.y - lastTouchY
                when (activeHandle) {
                    Handle.MOVE -> { cropRect.offset(dx, dy); clampRectToBounds() }
                    Handle.TOP_LEFT -> { cropRect.left += dx; cropRect.top += dy }
                    Handle.TOP_RIGHT -> { cropRect.right += dx; cropRect.top += dy }
                    Handle.BOTTOM_LEFT -> { cropRect.left += dx; cropRect.bottom += dy }
                    Handle.BOTTOM_RIGHT -> { cropRect.right += dx; cropRect.bottom += dy }
                    Handle.NONE -> return false
                }
                enforceMinSize()
                lastTouchX = event.x; lastTouchY = event.y
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> activeHandle = Handle.NONE
        }
        return super.onTouchEvent(event)
    }

    private fun detectHandle(x: Float, y: Float): Handle {
        fun near(hx: Float, hy: Float) = abs(x - hx) < handleRadius && abs(y - hy) < handleRadius
        return when {
            near(cropRect.left, cropRect.top) -> Handle.TOP_LEFT
            near(cropRect.right, cropRect.top) -> Handle.TOP_RIGHT
            near(cropRect.left, cropRect.bottom) -> Handle.BOTTOM_LEFT
            near(cropRect.right, cropRect.bottom) -> Handle.BOTTOM_RIGHT
            cropRect.contains(x, y) -> Handle.MOVE
            else -> Handle.NONE
        }
    }

    private fun enforceMinSize() {
        val minSize = 100f
        if (cropRect.width() < minSize) {
            if (activeHandle == Handle.TOP_LEFT || activeHandle == Handle.BOTTOM_LEFT)
                cropRect.left = cropRect.right - minSize
            else cropRect.right = cropRect.left + minSize
        }
        if (cropRect.height() < minSize) {
            if (activeHandle == Handle.TOP_LEFT || activeHandle == Handle.TOP_RIGHT)
                cropRect.top = cropRect.bottom - minSize
            else cropRect.bottom = cropRect.top + minSize
        }
        clampRectToBounds()
    }

    private fun clampRectToBounds() {
        if (cropRect.left < 0) { cropRect.right -= cropRect.left; cropRect.left = 0f }
        if (cropRect.top < 0) { cropRect.bottom -= cropRect.top; cropRect.top = 0f }
        if (cropRect.right > width) { cropRect.left -= (cropRect.right - width); cropRect.right = width.toFloat() }
        if (cropRect.bottom > height) { cropRect.top -= (cropRect.bottom - height); cropRect.bottom = height.toFloat() }
    }
}