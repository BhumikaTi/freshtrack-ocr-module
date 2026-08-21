package com.freshtrack.ocr.ui

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.LayoutInflater
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.freshtrack.ocr.R
import com.google.android.material.bottomsheet.BottomSheetDialog

object OcrResultSheet {

    fun show(
        activity: Activity,
        expiryDate: String?,
        extractedText: String
    ) {

        val dialog = BottomSheetDialog(activity)

        val view = LayoutInflater.from(activity)
            .inflate(android.R.layout.simple_list_item_2, null)

        val titleView =
            view.findViewById<TextView>(android.R.id.text1)

        val contentView =
            view.findViewById<TextView>(android.R.id.text2)

        titleView.text = if (expiryDate != null) {
            "Expiry Date Detected"
        } else {
            "Expiry Date Not Found"
        }

        titleView.textSize = 20f
        titleView.setTextColor(
            ContextCompat.getColor(
                activity,
                android.R.color.black
            )
        )

        contentView.text = if (expiryDate != null) {
            "Expiry Date: $expiryDate\n\n$extractedText"
        } else {
            "We couldn't detect an expiry date.\n\n$extractedText"
        }

        contentView.textSize = 16f
        contentView.setPadding(0, 24, 0, 32)
        contentView.setTextIsSelectable(true)

        view.setPadding(48, 48, 48, 48)

        view.setOnClickListener {

            val clipboard =
                activity.getSystemService(
                    Context.CLIPBOARD_SERVICE
                ) as ClipboardManager

            clipboard.setPrimaryClip(
                ClipData.newPlainText(
                    "Extracted Text",
                    extractedText
                )
            )

            Toast.makeText(
                activity,
                "Copied!",
                Toast.LENGTH_SHORT
            ).show()
        }

        dialog.setContentView(view)
        dialog.show()
    }
}