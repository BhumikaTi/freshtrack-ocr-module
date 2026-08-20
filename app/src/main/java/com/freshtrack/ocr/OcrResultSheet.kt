package com.freshtrack.ocr.ui

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.LayoutInflater
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetDialog

object OcrResultSheet {
    fun show(activity: Activity, extractedText: String) {
        val dialog = BottomSheetDialog(activity)
        val view = LayoutInflater.from(activity).inflate(android.R.layout.simple_list_item_2, null)

        val titleView = view.findViewById<TextView>(android.R.id.text1)
        val contentView = view.findViewById<TextView>(android.R.id.text2)

        titleView.text = "Extracted Text (tap to copy)"
        titleView.textSize = 18f
        titleView.setTextColor(ContextCompat.getColor(activity, android.R.color.black))

        contentView.text = extractedText
        contentView.textSize = 14f
        contentView.setPadding(0, 16, 0, 16)
        contentView.setTextIsSelectable(true)
        view.setPadding(48, 48, 48, 48)

        view.setOnClickListener {
            val clipboard = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("OCR Text", extractedText))
            Toast.makeText(activity, "Copied!", Toast.LENGTH_SHORT).show()
        }

        dialog.setContentView(view)
        dialog.show()
    }
}