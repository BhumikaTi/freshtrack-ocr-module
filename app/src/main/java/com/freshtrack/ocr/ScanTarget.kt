package com.freshtrack.ocr

import android.graphics.RectF

object ScanTarget {
    private const val HORIZONTAL_MARGIN = 0.10f
    private const val BOX_HEIGHT_FRACTION = 0.30f

    fun fractionRect(): RectF {
        val top = (1f - BOX_HEIGHT_FRACTION) / 2f
        val bottom = top + BOX_HEIGHT_FRACTION
        return RectF(HORIZONTAL_MARGIN, top, 1f - HORIZONTAL_MARGIN, bottom)
    }
}