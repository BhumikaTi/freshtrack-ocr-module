package com.freshtrack.ocr.ui

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AlertDialog

/** The small menu that opens when the user taps "Scan product to check ingredients". */
object ScanOptionsDialog {

    fun show(activity: Activity) {
        AlertDialog.Builder(activity)
            .setTitle("Check ingredients")
            .setItems(arrayOf("Scan with live camera", "Pick from gallery")) { _, which ->
                val mode = if (which == 0) {
                    Barcodescanactivity.MODE_CAMERA
                } else {
                    Barcodescanactivity.MODE_GALLERY
                }
                activity.startActivity(
                    Intent(activity, Barcodescanactivity::class.java)
                        .putExtra(Barcodescanactivity.EXTRA_MODE, mode)
                )
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}