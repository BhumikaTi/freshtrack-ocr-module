package com.freshtrack.ocr.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.freshtrack.ocr.ScanTarget

class TargetOverlayView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    private val bracketPaint = Paint().apply {
        color = Color.parseColor("#39FF14")   // was Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 10f
        strokeCap = Paint.Cap.ROUND
        isAntiAlias = true
        setShadowLayer(4f, 0f, 0f, Color.parseColor("#8039FF14"))  // green glow instead of black shadow
    }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val f = ScanTarget.fractionRect()
        val left = f.left * width; val top = f.top * height
        val right = f.right * width; val bottom = f.bottom * height
        val lineLen = 60f

        canvas.drawLine(left, top, left + lineLen, top, bracketPaint)
        canvas.drawLine(left, top, left, top + lineLen, bracketPaint)
        canvas.drawLine(right, top, right - lineLen, top, bracketPaint)
        canvas.drawLine(right, top, right, top + lineLen, bracketPaint)
        canvas.drawLine(left, bottom, left + lineLen, bottom, bracketPaint)
        canvas.drawLine(left, bottom, left, bottom - lineLen, bracketPaint)
        canvas.drawLine(right, bottom, right - lineLen, bottom, bracketPaint)
        canvas.drawLine(right, bottom, right, bottom - lineLen, bracketPaint)
    }
}