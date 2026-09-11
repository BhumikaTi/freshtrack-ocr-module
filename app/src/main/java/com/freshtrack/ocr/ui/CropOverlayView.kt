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

class CropOverlayView(
    context: Context,
    attrs: AttributeSet?
) : View(context, attrs) {

    var cropRect = RectF()
        private set

    private var cropVisible = false

    private val handleRadius = 36f
    private val handleTouchRadius = 60f
    private val minCropSize = 80f

    private var activeHandle = Handle.NONE

    private var lastTouchX = 0f
    private var lastTouchY = 0f

    private enum class Handle {
        NONE,
        MOVE,
        TOP_LEFT,
        TOP_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_RIGHT
    }

    private val scrimPaint =
        Paint().apply {
            color = Color.parseColor("#66000000")
        }

    private val boxPaint =
        Paint().apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 3f
            isAntiAlias = true
        }

    private val handlePaint =
        Paint().apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 7f
            strokeCap = Paint.Cap.SQUARE
            isAntiAlias = true
        }

    override fun onDraw(
        canvas: Canvas
    ) {
        super.onDraw(canvas)

        if (!cropVisible) {
            return
        }

        /*
         * Darken everything outside the crop area.
         */
        val path =
            Path().apply {

                addRect(
                    0f,
                    0f,
                    width.toFloat(),
                    height.toFloat(),
                    Path.Direction.CW
                )

                addRect(
                    cropRect,
                    Path.Direction.CCW
                )
            }

        canvas.drawPath(
            path,
            scrimPaint
        )

        /*
         * Crop border.
         */
        canvas.drawRect(
            cropRect,
            boxPaint
        )

        /*
         * Four corner handles.
         */
        drawCorner(
            canvas,
            cropRect.left,
            cropRect.top,
            1f,
            1f
        )

        drawCorner(
            canvas,
            cropRect.right,
            cropRect.top,
            -1f,
            1f
        )

        drawCorner(
            canvas,
            cropRect.left,
            cropRect.bottom,
            1f,
            -1f
        )

        drawCorner(
            canvas,
            cropRect.right,
            cropRect.bottom,
            -1f,
            -1f
        )
    }

    private fun drawCorner(
        canvas: Canvas,
        x: Float,
        y: Float,
        horizontalDirection: Float,
        verticalDirection: Float
    ) {

        val length = 28f

        canvas.drawLine(
            x,
            y,
            x + length * horizontalDirection,
            y,
            handlePaint
        )

        canvas.drawLine(
            x,
            y,
            x,
            y + length * verticalDirection,
            handlePaint
        )
    }

    fun setCropRect(
        rect: RectF
    ) {

        cropRect =
            RectF(rect)

        clampRectToBounds()

        cropVisible = true

        invalidate()
    }

    fun hideCrop() {

        cropVisible = false

        activeHandle =
            Handle.NONE

        invalidate()
    }

    fun isCropVisible(): Boolean {
        return cropVisible
    }

    override fun onTouchEvent(
        event: MotionEvent
    ): Boolean {

        if (!cropVisible) {
            return false
        }

        when (event.actionMasked) {

            MotionEvent.ACTION_DOWN -> {

                activeHandle =
                    detectHandle(
                        event.x,
                        event.y
                    )

                if (
                    activeHandle ==
                    Handle.NONE
                ) {
                    return false
                }

                lastTouchX =
                    event.x

                lastTouchY =
                    event.y

                parent?.requestDisallowInterceptTouchEvent(
                    true
                )

                return true
            }

            MotionEvent.ACTION_MOVE -> {

                if (
                    activeHandle ==
                    Handle.NONE
                ) {
                    return false
                }

                val dx =
                    event.x -
                            lastTouchX

                val dy =
                    event.y -
                            lastTouchY

                when (activeHandle) {

                    /*
                     * Move the entire crop box.
                     */
                    Handle.MOVE -> {

                        cropRect.offset(
                            dx,
                            dy
                        )
                    }

                    /*
                     * Resize from the corners.
                     */
                    Handle.TOP_LEFT -> {

                        cropRect.left += dx
                        cropRect.top += dy
                    }

                    Handle.TOP_RIGHT -> {

                        cropRect.right += dx
                        cropRect.top += dy
                    }

                    Handle.BOTTOM_LEFT -> {

                        cropRect.left += dx
                        cropRect.bottom += dy
                    }

                    Handle.BOTTOM_RIGHT -> {

                        cropRect.right += dx
                        cropRect.bottom += dy
                    }

                    Handle.NONE -> {
                        return false
                    }
                }

                enforceMinSize()

                lastTouchX =
                    event.x

                lastTouchY =
                    event.y

                invalidate()

                return true
            }

            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {

                activeHandle =
                    Handle.NONE

                parent?.requestDisallowInterceptTouchEvent(
                    false
                )

                return true
            }
        }

        return true
    }

    private fun detectHandle(
        x: Float,
        y: Float
    ): Handle {

        fun near(
            handleX: Float,
            handleY: Float
        ): Boolean {

            return abs(x - handleX) <=
                    handleTouchRadius &&
                    abs(y - handleY) <=
                    handleTouchRadius
        }

        return when {

            /*
             * Corners take priority.
             */
            near(
                cropRect.left,
                cropRect.top
            ) ->
                Handle.TOP_LEFT

            near(
                cropRect.right,
                cropRect.top
            ) ->
                Handle.TOP_RIGHT

            near(
                cropRect.left,
                cropRect.bottom
            ) ->
                Handle.BOTTOM_LEFT

            near(
                cropRect.right,
                cropRect.bottom
            ) ->
                Handle.BOTTOM_RIGHT

            /*
             * Anywhere inside the crop box
             * moves the entire box.
             */
            cropRect.contains(
                x,
                y
            ) ->
                Handle.MOVE

            else ->
                Handle.NONE
        }
    }

    private fun enforceMinSize() {

        /*
         * Minimum width.
         */
        if (
            cropRect.width() <
            minCropSize
        ) {

            when (activeHandle) {

                Handle.TOP_LEFT,
                Handle.BOTTOM_LEFT -> {

                    cropRect.left =
                        cropRect.right -
                                minCropSize
                }

                Handle.TOP_RIGHT,
                Handle.BOTTOM_RIGHT -> {

                    cropRect.right =
                        cropRect.left +
                                minCropSize
                }

                /*
                 * MOVE does not resize.
                 */
                Handle.MOVE,
                Handle.NONE -> Unit
            }
        }

        /*
         * Minimum height.
         */
        if (
            cropRect.height() <
            minCropSize
        ) {

            when (activeHandle) {

                Handle.TOP_LEFT,
                Handle.TOP_RIGHT -> {

                    cropRect.top =
                        cropRect.bottom -
                                minCropSize
                }

                Handle.BOTTOM_LEFT,
                Handle.BOTTOM_RIGHT -> {

                    cropRect.bottom =
                        cropRect.top +
                                minCropSize
                }

                Handle.MOVE,
                Handle.NONE -> Unit
            }
        }

        clampRectToBounds()
    }

    private fun clampRectToBounds() {

        if (
            width <= 0 ||
            height <= 0
        ) {
            return
        }

        /*
         * Keep the whole crop box inside
         * the overlay when moving.
         */
        if (cropRect.left < 0f) {

            val amount =
                -cropRect.left

            cropRect.left += amount
            cropRect.right += amount
        }

        if (cropRect.top < 0f) {

            val amount =
                -cropRect.top

            cropRect.top += amount
            cropRect.bottom += amount
        }

        if (cropRect.right > width) {

            val amount =
                cropRect.right -
                        width

            cropRect.left -= amount
            cropRect.right -= amount
        }

        if (cropRect.bottom > height) {

            val amount =
                cropRect.bottom -
                        height

            cropRect.top -= amount
            cropRect.bottom -= amount
        }

        /*
         * Final safety bounds.
         */
        cropRect.left =
            cropRect.left.coerceAtLeast(
                0f
            )

        cropRect.top =
            cropRect.top.coerceAtLeast(
                0f
            )

        cropRect.right =
            cropRect.right.coerceAtMost(
                width.toFloat()
            )

        cropRect.bottom =
            cropRect.bottom.coerceAtMost(
                height.toFloat()
            )
    }
}