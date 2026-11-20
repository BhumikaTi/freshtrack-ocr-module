package com.freshtrack.ocr.auth

import android.view.MotionEvent
import android.view.View
import android.text.method.PasswordTransformationMethod
import android.widget.EditText
import androidx.core.content.ContextCompat

object PasswordVisibility {

    /**
     * Adds an eye icon at the end of the EditText. Tapping it toggles
     * between hidden and visible password.
     */
    fun attach(editText: EditText) {
        var isVisible = false
        val icon = ContextCompat.getDrawable(editText.context, android.R.drawable.ic_menu_view)

        fun render() {
            editText.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, icon, null)
            editText.transformationMethod =
                if (isVisible) null else PasswordTransformationMethod.getInstance()
            // Keep the cursor where it was at the end of the text
            editText.setSelection(editText.text.length)
        }

        render()

        editText.setOnTouchListener { view, event ->
            if (event.action == MotionEvent.ACTION_UP &&
                event.x >= view.width - editText.totalPaddingEnd
            ) {
                isVisible = !isVisible
                render()
                view.performClick()
                true
            } else {
                false
            }
        }
    }
}